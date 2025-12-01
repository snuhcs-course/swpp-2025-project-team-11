import hashlib
import io
import json
import os
import tempfile
from typing import Any

import pandas as pd
from django.db.models import Prefetch
from django.db.models.functions import Length
from langchain_community.document_loaders import CSVLoader, Docx2txtLoader, PDFPlumberLoader, TextLoader

from apps.ai.models import ContactAnalysisResult, GroupAnalysisResult
from apps.contact.models import Contact, PromptOption
from apps.mail.models import AttachmentAnalysis, SentMail
from apps.user.models import UserProfile


def sse_event(name, payload, *, eid=None, retry_ms=None):
    lines = []
    if eid is not None:
        lines.append(f"id: {eid}")
    if retry_ms is not None:
        lines.append(f"retry: {int(retry_ms)}")
    lines.append(f"event: {name}")
    lines.append(f"data: {json.dumps(payload, ensure_ascii=False)}")
    return "\n".join(lines) + "\n\n"


def heartbeat():
    return ":\n\n"


def collect_prompt_context(
    user,
    to_emails: list[str],
    include_analysis: bool = True,
    include_fewshots: bool = False,
    fewshot_k: int = 3,
    min_body_len: int = 0,
) -> dict[str, Any]:
    """
    유저 + 수신자 이메일 리스트를 기반으로 프롬프트 컨텍스트 수집.

      - 각 수신자에 대해:
          * 등록 연락처의 name이 있으면 name 사용
          * 그렇지 않으면 "Recipient {idx}" 형태의 대체 레이블 사용
      - 단일 수신자일 때만 sender_role/recipient_role/personal_prompt/language 제공
      - 그룹 계산은 등록된 연락처만 사용(미등록 이메일 제외)

    반환 스키마:
      {
        "recipients": list[str],
        "group_name": str | None,
        "group_description": str | None,
        "prompt_options": list[str],
        "personal_prompt": str | None,
        "sender_role": str | None,
        "recipient_role": str | None,
        "language": str | None,
      }
    """
    contacts = list(
        Contact.objects.select_related("context", "group")
        .prefetch_related(Prefetch("group__options", queryset=PromptOption.objects.all()))
        .filter(user=user, email__in=to_emails)
    )

    # email -> contact 매핑
    by_email = {c.email: c for c in contacts}

    # 1) recipients: 연락처 있으면 이름, 없으면 "Recipient {i}"
    recipients: list[str] = []
    for i, em in enumerate(to_emails, start=1):
        c = by_email.get(em)
        label = c.name.strip() if c and getattr(c, "name", None) else None
        recipients.append(label if label else f"Recipient {i}")

    # 그룹 계산용 등록 그룹만 추출
    groups = [c.group for c in contacts if c.group_id]
    unique_groups = list({g.id: g for g in groups}.values())

    def _clean(s: Any) -> str | None:
        if not isinstance(s, str):
            return None
        s2 = s.strip()
        return s2 if s2 else None

    def serialize_opts(opts):
        return [o.prompt.strip() for o in opts]

    def get_group_opts(g):
        return list(g.options.all()) if g else []

    language = None
    profile = None
    user_profile = UserProfile.objects.filter(user=user).first()
    if user_profile:
        language = _clean(user_profile.language_preference)
        profile = {
            "display_name": _clean(user_profile.display_name),
            "info": _clean(user_profile.info),
        }

    base = {
        "recipients": recipients,
        "group_name": None,
        "group_description": None,
        "prompt_options": [],
        "personal_prompt": None,
        "sender_role": None,
        "recipient_role": None,
        "language": language,
        "fewshots": [],
        "analysis": None,
        "profile": profile,
    }

    if not contacts:
        return base

    # ========== 단일 '등록' 수신자 ==========
    if len(contacts) == 1 and len(to_emails) == 1:
        c = contacts[0]
        g = c.group
        ctx = getattr(c, "context", None)

        out = {
            **base,
            "group_name": _clean(g.name) if g else None,
            "group_description": _clean(g.description) if g else None,
            "prompt_options": serialize_opts(get_group_opts(g)) if g else [],
            "personal_prompt": _clean(getattr(ctx, "personal_prompt", None)),
            "sender_role": _clean(getattr(ctx, "sender_role", None)),
            "recipient_role": _clean(getattr(ctx, "recipient_role", None)),
            "language": _clean(getattr(ctx, "language_preference", None)) or base["language"],
        }

        if include_fewshots:
            out["fewshots"] = _fetch_fewshot_bodies_for_single(user, c, fewshot_k, min_body_len)
        if include_analysis:
            out["analysis"] = _fetch_analysis_for_single(user, c)
        return out

    # ========== 여러 명, 같은 그룹 ==========
    if len(unique_groups) == 1:
        g = unique_groups[0]
        out = {
            **base,
            "group_name": _clean(g.name),
            "group_description": _clean(g.description),
            "prompt_options": serialize_opts(get_group_opts(g)),
        }
        if include_fewshots:
            out["fewshots"] = _fetch_fewshot_bodies_for_group(user, g, fewshot_k, min_body_len)
        if include_analysis:
            out["analysis"] = _fetch_analysis_for_group(user, g)
        return out

    # ========== 여러 그룹: 공통 옵션 교집합 ==========
    id_sets: list[set[int]] = [set(o.id for o in get_group_opts(g)) for g in unique_groups]
    common_ids = set.intersection(*id_sets) if id_sets else set()

    opts = None
    if common_ids:
        opts = PromptOption.objects.filter(id__in=common_ids)

    group_names = ", ".join([g.name for g in unique_groups if g and g.name]) or None

    return {
        **base,
        "group_name": group_names,
        "group_description": None,
        "prompt_options": serialize_opts(opts) if opts else [],
    }


def _fetch_fewshot_bodies_for_single(user, contact, k: int, min_body_len: int) -> list[str]:
    qs = SentMail.objects.filter(user=user, contact=contact).exclude(body__isnull=True).exclude(body__exact="").order_by("-sent_at")
    if min_body_len > 0:
        qs = qs.annotate(body_len=Length("body")).filter(body_len__gte=min_body_len)
    bodies = list(qs.values_list("body", flat=True)[:k])

    if not bodies and getattr(contact, "group_id", None):
        bodies = _fetch_fewshot_bodies_for_group(user, contact.group, k, min_body_len)
    return bodies


def _fetch_fewshot_bodies_for_group(user, group, k: int, min_body_len: int) -> list[str]:
    qs = SentMail.objects.filter(user=user, contact__group=group).exclude(body__isnull=True).exclude(body__exact="").order_by("-sent_at")
    if min_body_len > 0:
        qs = qs.annotate(body_len=Length("body")).filter(body_len__gte=min_body_len)
    return list(qs.values_list("body", flat=True)[:k])


def _fetch_analysis_for_single(user, contact) -> dict | None:
    # (user, contact) 조합은 최대 1개
    obj = ContactAnalysisResult.objects.filter(user=user, contact=contact).first()
    if obj:
        return {
            "lexical_style": obj.lexical_style,
            "grammar_patterns": obj.grammar_patterns,
            "emotional_tone": obj.emotional_tone,
            "representative_sentences": obj.representative_sentences,
        }

    if getattr(contact, "group_id", None):
        return _fetch_analysis_for_group(user, contact.group)

    return None


def _fetch_analysis_for_group(user, group) -> dict | None:
    obj = GroupAnalysisResult.objects.filter(user=user, group=group).first()
    if not obj:
        return None

    return {
        "lexical_style": obj.lexical_style,
        "grammar_patterns": obj.grammar_patterns,
        "emotional_tone": obj.emotional_tone,
        "representative_sentences": obj.representative_sentences,
    }


DEFAULT_LANGUAGE = "user's original language"


def build_prompt_inputs(ctx: dict[str, Any], extra: dict[str, Any] | None = None) -> dict[str, Any]:
    def _clean(s: Any) -> str | None:
        if not isinstance(s, str):
            return None
        s2 = s.strip()
        return s2 if s2 else None

    language = ctx.get("language") or DEFAULT_LANGUAGE
    prompt_lines = [line.strip() for line in ctx.get("prompt_options", []) if line and line.strip()]

    personal = _clean(ctx.get("personal_prompt"))
    if personal:
        prompt_lines.append("")
        prompt_lines.append(personal)

    prompt_text = "\n".join(prompt_lines).strip() or None

    base = {
        "recipients": ctx.get("recipients"),
        "group_name": _clean(ctx.get("group_name")),
        "group_description": _clean(ctx.get("group_description")),
        "prompt_text": prompt_text,
        "sender_role": _clean(ctx.get("sender_role")),
        "recipient_role": _clean(ctx.get("recipient_role")),
        "language": language,
        "analysis": ctx.get("analysis"),
        "fewshots": ctx.get("fewshots"),
        "profile": ctx.get("profile"),
    }

    if extra:
        base.update(extra)

    return base


def extract_text_from_bytes(data: bytes, mime_type: str, filename: str) -> str:
    """
    Supported:
    - PDF: LangChain PDFPlumberLoader (text/table friendly, no ML)
    - DOCX: Docx2txtLoader
    - TXT: TextLoader
    - CSV: CSVLoader
    - Excel: pandas → CSV
    """
    mt = (mime_type or "").lower()
    suffix = filename.lower()

    # ===== PDF =====
    if mt == "application/pdf" or suffix.endswith(".pdf"):
        with tempfile.NamedTemporaryFile(mode="wb", delete=False, suffix=".pdf") as tmp:
            tmp.write(data)
            tmp_path = tmp.name
        try:
            loader = PDFPlumberLoader(tmp_path)
            docs = loader.load()
            return "\n\n".join(d.page_content for d in docs if d.page_content)
        finally:
            os.remove(tmp_path)

    # ===== DOCX =====
    if mt in ("application/vnd.openxmlformats-officedocument.wordprocessingml.document",) or suffix.endswith(".docx"):
        with tempfile.NamedTemporaryFile(mode="wb", delete=False, suffix=".docx") as tmp:
            tmp.write(data)
            tmp_path = tmp.name
        try:
            loader = Docx2txtLoader(tmp_path)
            docs = loader.load()
            return "\n".join(d.page_content for d in docs if d.page_content)
        finally:
            os.remove(tmp_path)

    # ===== TEXT =====
    if mt.startswith("text/") or suffix.endswith(".txt"):
        with tempfile.NamedTemporaryFile(delete=False, suffix=".txt") as tmp:
            tmp.write(data)
            tmp_path = tmp.name
        try:
            loader = TextLoader(tmp_path, encoding="utf-8")
            docs = loader.load()
            return "\n".join(d.page_content for d in docs if d.page_content)
        finally:
            os.remove(tmp_path)

    # ===== CSV =====
    if mt in ("text/csv", "application/csv") or suffix.endswith(".csv"):
        with tempfile.NamedTemporaryFile(delete=False, suffix=".csv") as tmp:
            tmp.write(data)
            tmp_path = tmp.name
        try:
            loader = CSVLoader(file_path=tmp_path, encoding="utf-8")
            docs = loader.load()
            return "\n".join(d.page_content for d in docs[:100] if d.page_content)
        finally:
            os.remove(tmp_path)

    # ===== Excel =====
    if mt in (
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-excel",
    ) or suffix.endswith((".xlsx", ".xls")):
        df = pd.read_excel(io.BytesIO(data))
        return df.head(100).to_csv(index=False)

    # ===== fallback =====
    return data.decode("utf-8", errors="ignore")


def hash_bytes(data: bytes) -> str:
    return hashlib.sha1(data).hexdigest()


def build_attachment_context_from_qs(qs) -> list[dict]:
    items = []
    for obj in qs:
        items.append(
            {
                "filename": obj.filename,
                "mime_type": obj.mime_type,
                "summary": obj.summary,
                "insights": obj.insights,
                "mail_guide": obj.mail_guide,
            }
        )
    return items


def get_attachments_for_message(user, message_id: str) -> list[dict]:
    qs = AttachmentAnalysis.objects.filter(user=user, message_id=message_id).order_by("-created_at")
    return build_attachment_context_from_qs(qs)


def get_attachments_for_content_keys(user, content_keys: list[str]) -> list[dict]:
    if not content_keys:
        return []
    qs = AttachmentAnalysis.objects.filter(user=user, content_key__in=content_keys).order_by("-created_at")
    return build_attachment_context_from_qs(qs)
