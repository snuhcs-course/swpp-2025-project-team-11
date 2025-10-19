import os
import time
from collections.abc import Generator

from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

from apps.ai.services.pii_masker import PiiMasker, make_req_id, unmask_stream
from apps.ai.services.prompts import SYSTEM_PROMPT_BODY, SYSTEM_PROMPT_SUBJECT
from apps.ai.services.utils import heartbeat, sse_event


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
            "Generate an email subject from the seeds.\n\n"
            "[Subject seed]\n{subject}\n\n"
            "[Draft]\n{body}\n\n"
            "[Relationship]\n{relationship}\n\n"
            "[Situation]\n{situational}\n\n"
            "[Style]\n{style}\n\n"
            "[Format]\n{format}",
        ),
    ]
)

_body_prompt = ChatPromptTemplate.from_messages(
    [
        ("system", SYSTEM_PROMPT_BODY),
        (
            "user",
            "Write the final email body consistent with <locked_subject>.\n\n"
            "[User draft / notes]\n{body}\n\n"
            "[Relationship]\n{relationship}\n\n"
            "[Situation]\n{situational}\n\n"
            "[Style]\n{style}\n\n"
            "[Format]\n{format}",
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
    subject: str | None,
    body: str | None,
    relationship: str | None,
    situational_prompt: str | None,
    style_prompt: str | None,
    format_prompt: str | None,
    language: str | None = None,
) -> Generator[str, None, None]:

    raw_inputs = _build_inputs(
        subject, body, relationship, situational_prompt, style_prompt, format_prompt, language
    )

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

    # 2) Body 스트리밍
    locked_inputs = {
        "locked_subject": locked_title,
        "body": masked_inputs["body"],
        "relationship": raw_inputs["relationship"],
        "situational": raw_inputs["situational"],
        "style": raw_inputs["style"],
        "format": raw_inputs["format"],
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
