import os
import time
from collections.abc import Generator

from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

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

    inputs = _build_inputs(
        subject, body, relationship, situational_prompt, style_prompt, format_prompt, language
    )

    # Ready + client-side retry hint
    yield sse_event("ready", {"ts": int(time.time() * 1000)}, retry_ms=5000)

    # 1) Subject (single, non-streaming)
    try:
        locked_title = (_subject_chain.invoke(inputs) or "").strip()
    except Exception:
        locked_title = ""

    merged_subject = f"{locked_title}\n\n" if locked_title else ""
    yield sse_event("subject", {"title": locked_title, "text": merged_subject}, eid="0")

    seq = 1
    last_ping = time.monotonic()

    locked_inputs = {
        "locked_subject": locked_title,
        "body": inputs["body"],
        "relationship": inputs["relationship"],
        "situational": inputs["situational"],
        "style": inputs["style"],
        "format": inputs["format"],
        "language": inputs["language"],
    }

    try:
        for chunk in _body_chain.stream(locked_inputs):
            if chunk:
                yield sse_event("body.delta", {"seq": seq - 1, "text": chunk}, eid=str(seq))
                seq += 1
            if time.monotonic() - last_ping > 10:
                yield heartbeat()
                last_ping = time.monotonic()
    except Exception as e:
        yield sse_event("error", {"message": str(e)}, eid=str(seq))
    finally:
        yield sse_event("done", {"reason": "stop"}, eid=str(seq + 1))
