import json
from typing import Any

from django.db.models import Prefetch

from apps.contact.models import Contact, PromptOption


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


def collect_prompt_context(user, to_emails: list[str]) -> dict[str, Any]:
    """
    유저 + 수신자 이메일 리스트를 기반으로 프롬프트 구성용 컨텍스트 수집.

    규칙:
      - 수신자 1명: 개인 프롬프트 + 관계/역할 정보 + 그룹 옵션 포함
      - 여러 명, 같은 그룹: 그룹 옵션만 포함
      - 여러 명, 여러 그룹: 공통 옵션만 포함 (없으면 fallback)
      - 미등록 이메일은 그룹 계산에서 제외
    반환:
      {
        "recipients": [...],
        "scope": "single" | "multi_same" | "multi_common" | "fallback",
        "group_name": str,
        "group_description": str,
        "prompt_options": [ ... ],
        "personal_prompt": str,
        "relationship": str,
        "language": str,
      }
    """
    contacts = list(
        Contact.objects.select_related("context", "group")
        .prefetch_related(Prefetch("group__options", queryset=PromptOption.objects.all()))
        .filter(user=user, email__in=to_emails)
    )

    recipients = [c.name for c in contacts]
    groups = [c.group for c in contacts if c.group_id]
    unique_groups = list({g.id: g for g in groups}.values())

    def get_group_opts(g):
        return list(g.options.all()) if g else []

    # ========== 1. fallback ==========
    if not contacts or not groups:
        opts = list(PromptOption.objects.filter(created_by__isnull=True))
        return {
            "scope": "fallback",
            "recipients": recipients,
            "group_name": "",
            "group_description": "",
            "prompt_options": [{"id": o.id, "name": o.name, "prompt": o.prompt} for o in opts],
            "personal_prompt": "",
            "relationship": "",
            "language": "",
        }

    # ========== 2. 단일 수신자 ==========
    if len(to_emails) == 1:
        c = contacts[0]
        opts = get_group_opts(c.group)
        ctx = getattr(c, "context", None)
        relationship_parts = []
        if ctx:
            if ctx.sender_role or ctx.recipient_role:
                relationship_parts.append(
                    f"You are the '{ctx.sender_role}' writing to the '{ctx.recipient_role}'."
                )
            if ctx.relationship_details:
                relationship_parts.append(ctx.relationship_details)
        relationship = "\n".join(relationship_parts)

        return {
            "scope": "single",
            "recipients": recipients,
            "group_name": c.group.name if c.group else "",
            "group_description": c.group.description if c.group else "",
            "prompt_options": [{"id": o.id, "name": o.name, "prompt": o.prompt} for o in opts],
            "personal_prompt": getattr(ctx, "personal_prompt", "") or "",
            "relationship": relationship.strip(),
            "language": getattr(ctx, "language_preference", "") or "",
        }

    # ========== 3. 여러 명, 같은 그룹 ==========
    if len(unique_groups) == 1:
        g = unique_groups[0]
        opts = get_group_opts(g)
        return {
            "scope": "multi_same",
            "recipients": recipients,
            "group_name": g.name,
            "group_description": g.description,
            "prompt_options": [{"id": o.id, "name": o.name, "prompt": o.prompt} for o in opts],
            "personal_prompt": "",
            "relationship": "",
            "language": "",
        }

    # ========== 4. 여러 그룹 (공통 옵션 교집합) ==========
    id_sets: list[set[int]] = [set(o.id for o in get_group_opts(g)) for g in unique_groups]
    common_ids = set.intersection(*id_sets) if id_sets else set()

    if not common_ids:
        opts = list(PromptOption.objects.filter(created_by__isnull=True))
        scope = "fallback"
    else:
        opts = list(PromptOption.objects.filter(id__in=common_ids))
        scope = "multi_common"

    group_names = ", ".join(g.name for g in unique_groups if g.name)
    return {
        "scope": scope,
        "recipients": recipients,
        "group_name": group_names,
        "group_description": "",
        "prompt_options": [{"id": o.id, "name": o.name, "prompt": o.prompt} for o in opts],
        "personal_prompt": "",
        "relationship": "",
        "language": "",
    }


DEFAULT_LANGUAGE = "user's original language"


def build_prompt_inputs(ctx: dict[str, Any]) -> dict[str, Any]:
    language = ctx.get("language") or DEFAULT_LANGUAGE
    prompt_lines = [p["prompt"].strip() for p in ctx["prompt_options"] if p.get("prompt")]

    # 최종 프롬프트 구성
    prompt_text = "\n".join(prompt_lines)
    if ctx.get("personal_prompt"):
        prompt_text += "\n\n" + ctx["personal_prompt"]
    if ctx.get("relationship"):
        prompt_text += "\n\n[Relationship]\n" + ctx["relationship"]

    return {
        "recipients": ", ".join(ctx["recipients"]),
        "group_name": ctx["group_name"],
        "group_description": ctx["group_description"],
        "prompt_text": prompt_text.strip(),
        "relationship": ctx["relationship"],
        "language": language,
        "scope": ctx["scope"],
    }
