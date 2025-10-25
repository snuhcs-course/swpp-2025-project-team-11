import os
import time
from collections.abc import Generator

from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

from apps.ai.services.pii_masker import PiiMasker, make_req_id, unmask_stream
from apps.ai.services.prompts import (
    BODY_SYSTEM_J2,
    BODY_USER_J2,
    SUBJECT_SYSTEM_J2,
    SUBJECT_USER_J2,
)
from apps.ai.services.utils import build_prompt_inputs, collect_prompt_context, heartbeat, sse_event

_subject_prompt = ChatPromptTemplate.from_messages(
    [
        ("system", SUBJECT_SYSTEM_J2),
        ("user", SUBJECT_USER_J2),
    ],
    template_format="jinja2",
)

_body_prompt = ChatPromptTemplate.from_messages(
    [
        ("system", BODY_SYSTEM_J2),
        ("user", BODY_USER_J2),
    ],
    template_format="jinja2",
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
) -> Generator[str]:

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

    unmasked_title = locked_title and masker and "".join(unmask_stream([locked_title], req_id, mapping)) or locked_title

    merged_subject = f"{unmasked_title}\n\n" if unmasked_title else ""
    yield sse_event("subject", {"title": unmasked_title, "text": merged_subject}, eid="0")

    seq = 1
    last_ping = time.monotonic()

    locked_inputs = {
        "locked_subject": locked_title,
        "body": masked_inputs.get("body", ""),
        "language": raw_inputs.get("language"),
        "recipients": raw_inputs.get("recipients"),
        "group_name": raw_inputs.get("group_name"),
        "group_description": raw_inputs.get("group_description"),
        "prompt_text": raw_inputs.get("prompt_text"),
        "sender_role": raw_inputs.get("sender_role"),
        "recipient_role": raw_inputs.get("recipient_role"),
        "fewshots": raw_inputs.get("fewshots", []),
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
