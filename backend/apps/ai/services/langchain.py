import os
import time
from collections.abc import Generator

from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

from apps.ai.services.pii_masker import PiiMasker, make_req_id, unmask_stream
from apps.ai.services.prompts import SYSTEM_PROMPT_BODY, SYSTEM_PROMPT_SUBJECT
from apps.ai.services.utils import build_prompt_inputs, collect_prompt_context, heartbeat, sse_event


def _build_inputs(
    subject: str | None,
    body: str | None,
    relationship: str | None,
    situational_prompt: str | None,
    style_prompt: str | None,
    format_prompt: str | None,
    language: str | None = None,
):
    return {
        "subject": subject or "",
        "body": body or "",
        "relationship": relationship or "",
        "situational": situational_prompt or "",
        "style": style_prompt or "",
        "format": format_prompt or "",
        "language": language or "User's Original Language",
    }


_subject_prompt = ChatPromptTemplate.from_messages(
    [
        ("system", SYSTEM_PROMPT_SUBJECT),
        (
            "user",
            "You are an expert email writing assistant. "
            "Generate a clear, concise, and natural email subject that matches the email’s intent"
            "and the user’s preferred tone and style.\n\n"
            "Consider the context below carefully.\n\n"
            "=== Context Information ===\n"
            "[Recipients]\n{recipients}\n\n"
            "[Group Information]\nName: {group_name}\nDescription: {group_description}\n\n"
            "[Rules]\n{prompt_text}\n\n\n"
            "[Relationship Context]\n{relationship}\n\n"
            "[User Draft Subject]\n{subject}\n\n"
            "[User Draft Body]\n{body}\n\n"
            "Language: {language}",
        ),
    ]
)

_body_prompt = ChatPromptTemplate.from_messages(
    [
        ("system", SYSTEM_PROMPT_BODY),
        (
            "user",
            "You are a professional email writing assistant. "
            "Write the final version of the email body that is consistent with <locked_subject>"
            "and the user’s desired tone/style.\n\n"
            "[Recipients]\n{recipients}\n\n"
            "[Group Information]\nName: {group_name}\nDescription: {group_description}\n\n"
            "[Rules]\n{prompt_text}\n\n\n"
            "[Relationship Context]\n{relationship}\n\n"
            "[User Draft Body]\n{body}\n\n"
            "=== Output Requirements ===\n"
            "1. Follow the stylistic and tonal rules in [Prompt Options].\n"
            "2. Ensure logical flow, politeness, and professionalism.\n"
            "3. Avoid repeating the subject; start naturally.\n"
            "4. Maintain consistency with <locked_subject>.\n"
            "5. Respect the specified language.\n\n"
            "Language: {language}",
        ),
    ]
)
_model = ChatOpenAI(
    model=os.getenv("OPENAI_MODEL", "gpt-4.1-mini"),
    temperature=float(os.getenv("AI_TEMPERATURE", "0.4")),
)

_subject_model = ChatOpenAI(
    model=os.getenv("OPENAI_MODEL", "gpt-4.1-mini"),
    temperature=float(os.getenv("AI_SUBJECT_TEMPERATURE", "0.2")),
)

_subject_chain = _subject_prompt | _subject_model | StrOutputParser()
_body_chain = _body_prompt | _model | StrOutputParser()


def stream_mail_generation(
    user,
    subject: str | None,
    body: str | None,
    to_emails: list[str],
) -> Generator[str, None, None]:

    ctx = collect_prompt_context(user, to_emails)
    raw_inputs = build_prompt_inputs(ctx)
    raw_inputs["subject"] = subject or ""
    raw_inputs["body"] = body or ""

    # 요청별 req_id 생성 + 마스킹(제목/본문)
    req_id = make_req_id()
    masker = PiiMasker(req_id)
    masked_inputs, mapping = masker.mask_inputs(raw_inputs)

    # Ready + client-side retry hint
    yield sse_event("ready", {"ts": int(time.time() * 1000)}, retry_ms=5000)

    # 1) Subject (non-streaming) — 제목 생성
    try:
        locked_title = (_subject_chain.invoke(masked_inputs) or "").strip()
    except Exception:
        locked_title = ""

    unmasked_title = (
        locked_title
        and masker
        and "".join(unmask_stream([locked_title], req_id, mapping))
        or locked_title
    )

    merged_subject = f"{unmasked_title}\n\n" if unmasked_title else ""
    yield sse_event("subject", {"title": unmasked_title, "text": merged_subject}, eid="0")

    seq = 1
    last_ping = time.monotonic()

    locked_inputs = {
        "locked_subject": locked_title,
        "body": masked_inputs["body"],
        "recipients": ", ".join(ctx["recipients"]),
        "group_name": ctx["group_name"],
        "group_description": ctx["group_description"],
        "prompt_text": raw_inputs["prompt_text"],
        "relationship": raw_inputs["relationship"],
        "language": raw_inputs["language"],
    }

    try:
        raw_stream = _body_chain.stream(locked_inputs)

        for chunk in unmask_stream(raw_stream, req_id, mapping):
            if chunk:
                yield sse_event("body.delta", {"seq": seq - 1, "text": chunk}, eid=str(seq))
                seq += 1

            if time.monotonic() - last_ping > 10:
                yield heartbeat()
                last_ping = time.monotonic()

    except Exception as e:
        yield sse_event("error", {"message": str(e)}, eid=str(seq))
    finally:
        mapping.clear()
        yield sse_event("done", {"reason": "stop"}, eid=str(seq + 1))
