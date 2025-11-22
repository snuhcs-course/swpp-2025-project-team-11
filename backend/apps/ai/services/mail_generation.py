import time
from collections.abc import Generator
from typing import Any

from apps.ai.services.chains import body_chain, plan_chain, subject_chain, validator_chain
from apps.ai.services.graph import mail_graph
from apps.ai.services.models import ValidationResult
from apps.ai.services.pii_masker import PiiMasker, make_req_id, unmask_stream
from apps.ai.services.utils import build_prompt_inputs, collect_prompt_context, heartbeat, sse_event


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
        locked_title = (subject_chain.invoke(masked_inputs) or "").strip()
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
        "plan_text": "",
        "analysis": raw_inputs.get("analysis", None),
        "fewshots": raw_inputs.get("fewshots"),
        "profile": raw_inputs.get("profile"),
    }

    try:
        raw_stream = body_chain.stream(locked_inputs)

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


def stream_mail_generation_with_timestamp(
    user,
    subject: str | None,
    body: str | None,
    to_emails: list[str],
) -> Generator[str, None, None]:
    """
    기존 stream_mail_generation 과 동일한 로직이지만,
    각 SSE 이벤트에 server_ts / elapsed_ms / (옵션) delta_ms 를 포함해서
    타이밍 디버깅용으로 사용하는 버전.
    """

    ctx = collect_prompt_context(user, to_emails)
    raw_inputs = build_prompt_inputs(ctx)
    raw_inputs["subject"] = subject or ""
    raw_inputs["body"] = body or ""

    req_id = make_req_id()
    masker = PiiMasker(req_id)
    masked_inputs, mapping = masker.mask_inputs(raw_inputs)

    # 스트리밍 기준 시간 (서버 기준)
    stream_start_ts = time.time()

    ready_ts = time.time()
    yield sse_event(
        "ready",
        {
            "ts": int(ready_ts * 1000),
            "server_ts": int(ready_ts * 1000),
            "elapsed_ms": int((ready_ts - stream_start_ts) * 1000),
        },
        retry_ms=5000,
    )

    try:
        locked_title = (subject_chain.invoke(masked_inputs) or "").strip()
    except Exception:
        locked_title = ""

    unmasked_title = locked_title and masker and "".join(unmask_stream([locked_title], req_id, mapping)) or locked_title

    merged_subject = f"{unmasked_title}\n\n" if unmasked_title else ""

    subject_ts = time.time()
    yield sse_event(
        "subject",
        {
            "title": unmasked_title,
            "text": merged_subject,
            "server_ts": int(subject_ts * 1000),
            "elapsed_ms": int((subject_ts - stream_start_ts) * 1000),
        },
        eid="0",
    )

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
        "plan_text": "",
        "analysis": raw_inputs.get("analysis", None),
        "fewshots": raw_inputs.get("fewshots"),
        "profile": raw_inputs.get("profile"),
    }

    try:
        raw_stream = body_chain.stream(locked_inputs)

        prev_chunk_ts: float | None = None

        for chunk in unmask_stream(raw_stream, req_id, mapping):
            if chunk:
                now_ts = time.time()
                now_ms = int(now_ts * 1000)
                elapsed_ms = int((now_ts - stream_start_ts) * 1000)

                if prev_chunk_ts is not None:
                    delta_ms = int((now_ts - prev_chunk_ts) * 1000)
                else:
                    delta_ms = None

                prev_chunk_ts = now_ts

                payload = {
                    "seq": seq - 1,
                    "text": chunk,
                    "server_ts": now_ms,
                    "elapsed_ms": elapsed_ms,
                }
                if delta_ms is not None:
                    payload["delta_ms"] = delta_ms

                yield sse_event("body.delta", payload, eid=str(seq))
                seq += 1

            if time.monotonic() - last_ping > 10:
                yield heartbeat()
                last_ping = time.monotonic()

    except Exception as e:
        err_ts = time.time()
        yield sse_event(
            "error",
            {
                "message": str(e),
                "server_ts": int(err_ts * 1000),
                "elapsed_ms": int((err_ts - stream_start_ts) * 1000),
            },
            eid=str(seq),
        )
    finally:
        mapping.clear()
        done_ts = time.time()
        yield sse_event(
            "done",
            {
                "reason": "stop",
                "server_ts": int(done_ts * 1000),
                "elapsed_ms": int((done_ts - stream_start_ts) * 1000),
            },
            eid=str(seq + 1),
        )


def stream_mail_generation_with_plan(
    user: dict,
    subject: str | None,
    body: str | None,
    to_emails: list[str],
) -> Generator[str, None, None]:

    yield sse_event("ready", {"ts": int(time.time() * 1000)}, retry_ms=5000)

    state = mail_graph.invoke(
        {
            "user": user,
            "subject": subject,
            "body": body,
            "to_emails": to_emails,
        }
    )

    req_id = state["req_id"]
    mapping = state["mask_mapping"]
    masked_inputs = state["masked_inputs"]
    body_inputs = state["body_inputs"]
    locked_title_masked = state["locked_title"]

    yield sse_event("plan.start", {}, eid="plan-0")

    plan_chunks: list[str] = []

    raw_stream = plan_chain.stream(masked_inputs)

    paragraph_buf = ""
    para_idx = 1

    def flush_paragraph(text: str):
        nonlocal para_idx
        text = text.strip()
        if not text:
            return
        yield sse_event(
            "plan.delta",
            {"idx": para_idx, "text": text + "\n"},
            eid=f"plan-{para_idx}",
        )
        para_idx += 1

    for ch in raw_stream:
        if not ch:
            continue
        plan_chunks.append(ch)
        paragraph_buf += ch

        if "\n\n" in paragraph_buf:
            part, rest = paragraph_buf.split("\n\n", 1)
            for ev in flush_paragraph(part):
                yield ev
            paragraph_buf = rest
        elif "\n[" in paragraph_buf:
            split_idx = paragraph_buf.rfind("\n[")
            part = paragraph_buf[:split_idx]
            rest = paragraph_buf[split_idx:]
            for ev in flush_paragraph(part):
                yield ev
            paragraph_buf = rest

    if paragraph_buf.strip():
        for ev in flush_paragraph(paragraph_buf):
            yield ev

    yield sse_event("plan.done", {}, eid="plan-done")

    plan_text = "".join(plan_chunks)
    body_inputs["plan_text"] = plan_text

    unmasked_title = "".join(unmask_stream([locked_title_masked], req_id, mapping)) if locked_title_masked else ""
    yield sse_event("subject", {"title": unmasked_title, "text": unmasked_title}, eid="0")

    yield sse_event("body.start", {}, eid="1")
    seq = 2
    last_ping = time.monotonic()

    masked_full_chunks: list[str] = []

    try:
        raw_stream = body_chain.stream(body_inputs)

        def capture_body(stream):
            for ch in stream:
                if not ch:
                    continue
                masked_full_chunks.append(ch)
                yield ch

        captured_body_stream = capture_body(raw_stream)

        for chunk in unmask_stream(captured_body_stream, req_id, mapping):
            if not chunk:
                continue
            yield sse_event("body.delta", {"seq": seq - 1, "text": chunk}, eid=str(seq))
            seq += 1

            if time.monotonic() - last_ping > 10:
                yield heartbeat()
                last_ping = time.monotonic()

    except Exception as e:
        yield sse_event("error", {"message": str(e)}, eid=str(seq))
    finally:
        yield sse_event("body.done", {"text": "\n"}, eid=str(seq))
        seq += 1

        masked_body = "".join(masked_full_chunks)

        try:
            judge: ValidationResult = validator_chain.invoke(
                {
                    "subject": locked_title_masked,
                    "body": masked_body,
                    "constraints": body_inputs,
                }
            )
        except Exception:
            judge = None

        if judge is not None and not judge.passed:
            fixed_masked_body = body_chain.invoke(
                {
                    **body_inputs,
                    "body": masked_body,
                    "prompt_text": (body_inputs.get("prompt_text") or "") + "\n" + judge.rewrite_instructions,
                }
            )
            fixed_unmasked = "".join(unmask_stream([fixed_masked_body], req_id, mapping))
            yield sse_event("patched", {"text": fixed_unmasked}, eid=str(seq))
            seq += 1

        yield sse_event("done", {"reason": "stop"}, eid=str(seq + 1))
        mapping.clear()


def debug_mail_generation_analysis(
    user,
    subject: str | None,
    body: str | None,
    to_emails: list[str],
) -> dict[str, Any]:
    base_ctx = collect_prompt_context(
        user,
        to_emails,
        include_analysis=True,
        include_fewshots=True,
    )
    analysis_value = base_ctx.get("analysis")
    fewshots_value = base_ctx.get("fewshots")

    def _run_once(override_ctx: dict[str, Any]) -> dict[str, str]:
        merged_ctx: dict[str, Any] = {**base_ctx, **override_ctx}

        raw_inputs = build_prompt_inputs(
            merged_ctx,
        )
        raw_inputs["subject"] = subject or ""
        raw_inputs["body"] = body or ""

        req_id = make_req_id()
        masker = PiiMasker(req_id)
        masked_inputs, mapping = masker.mask_inputs(raw_inputs)

        try:
            locked_title = (subject_chain.invoke(masked_inputs) or "").strip()
        except Exception:
            locked_title = ""

        unmasked_title = (locked_title and masker and "".join(unmask_stream([locked_title], req_id, mapping))) or locked_title

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
            "plan_text": "",
            "analysis": raw_inputs.get("analysis", None),
            "fewshots": raw_inputs.get("fewshots", None),
            "profile": raw_inputs.get("profile"),
        }

        try:
            raw_body = body_chain.invoke(locked_inputs) or ""
        except Exception:
            raw_body = ""

        unmasked_body = "".join(unmask_stream([raw_body], req_id, mapping)) if raw_body else raw_body

        mapping.clear()

        return {
            "subject": unmasked_title,
            "body": unmasked_body,
        }

    result_without_any = _run_once({"analysis": None, "fewshots": None})

    result_with_analysis = _run_once({"fewshots": None})

    result_with_fewshots = _run_once({"analysis": None})

    return {
        "analysis": analysis_value,
        "fewshots": fewshots_value,
        "without_analysis": result_without_any,
        "with_analysis": result_with_analysis,
        "with_fewshots": result_with_fewshots,
    }


def stream_mail_generation_test() -> Generator[str]:
    """
    테스트용 더미 스트리밍 함수.
    실제 AI API 대신 긴 더미 텍스트를 인위적인 딜레이를 두고 스트리밍합니다.
    """
    import random

    # 더미 텍스트 - 충분히 긴 내용
    dummy_paragraphs = [
        "Dear valued recipient, I hope this email finds you well and in good spirits. "
        "I am writing to you today regarding an important matter that requires your attention. ",
        "As we discussed in our previous meeting, there are several key points that we need to address "
        "moving forward. First and foremost, we need to establish a clear timeline for the project deliverables. ",
        "The stakeholders have expressed their concerns about the current progress, and it is crucial that we "
        "demonstrate our commitment to meeting the established deadlines. ",
        "In light of recent developments, I would like to propose a series of strategic initiatives that could "
        "significantly enhance our operational efficiency. These initiatives include process optimization, "
        "resource allocation improvements, and enhanced communication protocols. ",
        "Furthermore, it is essential that we maintain open lines of communication throughout this process. "
        "Regular status updates and feedback sessions will be instrumental in ensuring that we remain aligned "
        "with our objectives. ",
        "I have attached several documents for your review, which contain detailed information about the proposed "
        "changes and their potential impact on our current operations. Please take the time to thoroughly examine "
        "these materials before our next meeting. ",
        "Your expertise and insights are invaluable to this project, and I genuinely appreciate your continued "
        "dedication and hard work. Together, I am confident that we can overcome any challenges that may arise "
        "and achieve remarkable results. ",
        "Should you have any questions or concerns, please do not hesitate to reach out to me directly. "
        "I am always available to discuss any aspect of this project in greater detail. ",
        "Thank you for your time and consideration. I look forward to our continued collaboration and to seeing "
        "the positive outcomes of our collective efforts. ",
        "Best regards and warm wishes for a productive and successful week ahead. " "Let us continue to strive for excellence in all that we do.",
    ]

    dummy_body = "".join(dummy_paragraphs)
    dummy_subject = "Important Update: Project Status and Next Steps"

    seq = 0

    # 1. ready 이벤트
    yield sse_event("ready", {"ts": int(time.time() * 1000)}, retry_ms=5000)
    time.sleep(0.1)  # 100ms 딜레이

    # 2. subject 이벤트
    yield sse_event("subject", {"title": dummy_subject, "text": dummy_subject + "\n\n"}, eid="0")
    seq += 1
    time.sleep(0.2)  # 200ms 딜레이

    # 3. body 스트리밍 - 단어별로 끊어서 전송
    words = dummy_body.split(" ")
    chunk_size = random.randint(2, 5)  # 2-5 단어씩 묶어서 전송

    for i in range(0, len(words), chunk_size):
        chunk_words = words[i : i + chunk_size]
        chunk_text = " ".join(chunk_words)
        if i + chunk_size < len(words):
            chunk_text += " "  # 마지막이 아니면 공백 추가

        yield sse_event("body.delta", {"seq": seq - 1, "text": chunk_text}, eid=str(seq))
        seq += 1

        # 무작위 딜레이 (50-150ms)
        delay = random.uniform(0.05, 0.15)
        time.sleep(delay)

        # 가끔 heartbeat 전송 (매 20개 청크마다)
        if seq % 20 == 0:
            yield heartbeat()

    # 4. done 이벤트
    yield sse_event("done", {"reason": "stop"}, eid=str(seq + 1))
