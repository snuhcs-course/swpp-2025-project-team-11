import asyncio
import queue
import threading
import time
from collections.abc import Generator

from apps.ai.services.chains import plan_chain, reply_body_chain
from apps.ai.services.pii_masker import PiiMasker, make_req_id, unmask_stream
from apps.ai.services.utils import build_prompt_inputs, collect_prompt_context, sse_event
from apps.core.utils.async_stream import as_async_stream


@as_async_stream
def stream_reply_options_llm(
    *,
    user,
    subject: str | None,
    body: str | None,
    to_email: str,
) -> Generator[str]:
    ctx = collect_prompt_context(user, [to_email])
    raw = build_prompt_inputs(ctx)
    raw["incoming_subject"] = subject or ""
    raw["incoming_body"] = body or ""

    req_id = make_req_id()
    masker = PiiMasker(req_id)
    masked_inputs, mapping = masker.mask_inputs(raw)

    def unmask(chunks: list[str]) -> list[str]:
        return list(unmask_stream(chunks, req_id, mapping))

    # ready
    yield sse_event("ready", {"ts": int(time.time() * 1000)}, retry_ms=5000)

    # 옵션 설계
    plan_inputs = {
        "incoming_subject": masked_inputs["incoming_subject"],
        "incoming_body": masked_inputs["incoming_body"],
        "language": masked_inputs.get("language"),
        "recipients": masked_inputs.get("recipients"),
        "group_description": masked_inputs.get("group_description"),
        "prompt_text": masked_inputs.get("prompt_text"),
        "sender_role": masked_inputs.get("sender_role"),
        "recipient_role": masked_inputs.get("recipient_role"),
    }
    try:
        plan = plan_chain.invoke(plan_inputs)
    except Exception:
        plan = type(
            "FallbackPlan",
            (object,),
            {
                "language": masked_inputs.get("language") or "user's original language",
                "options": [
                    type("Opt", (object,), {"type": "Concise reply", "title": "Quick confirmation"})(),
                    type("Opt", (object,), {"type": "Follow-up", "title": "A few clarifications"})(),
                ],
            },
        )()

    items = []
    for i, opt in enumerate(plan.options[:4]):
        title = "".join(unmask([opt.title])).strip().rstrip(" .」")
        otype = "".join(unmask([opt.type])).strip()
        items.append({"id": i, "type": otype, "title": title})

    next_eid = 0
    yield sse_event("options", {"count": len(items), "items": items}, eid=str(next_eid))
    next_eid += 1

    q: queue.Queue[tuple[str, dict]] = queue.Queue()

    masked_common = {
        "incoming_subject": masked_inputs["incoming_subject"],
        "incoming_body": masked_inputs["incoming_body"],
        "language": plan.language,
        "recipients": masked_inputs.get("recipients"),
        "group_description": masked_inputs.get("group_description"),
        "prompt_text": masked_inputs.get("prompt_text"),
        "sender_role": masked_inputs.get("sender_role"),
        "recipient_role": masked_inputs.get("recipient_role"),
        "analysis": masked_inputs.get("analysis"),
        "fewshots": masked_inputs.get("fewshots"),
        "profile": masked_inputs.get("profile"),
    }

    def worker(opt_idx: int, locked_type: str, locked_title: str):
        async def produce():
            seq = 0
            inputs = {
                **masked_common,
                "locked_type": locked_type,
                "locked_title": locked_title,
            }
            try:
                async for chunk in reply_body_chain.astream(inputs):
                    if not chunk:
                        continue
                    for piece in unmask([chunk]):
                        if piece:
                            q.put(("option.delta", {"id": opt_idx, "seq": seq, "text": piece}))
                            seq += 1
            except Exception as e:
                q.put(("option.error", {"id": opt_idx, "message": str(e)}))
            finally:
                q.put(("option.done", {"id": opt_idx, "total_seq": seq}))

        # 각 워커 스레드 내에서 이벤트 루프 실행
        asyncio.run(produce())

    threads = []
    for it in items:
        t = threading.Thread(
            target=worker,
            args=(it["id"], it["type"], it["title"]),
            daemon=True,
        )
        t.start()
        threads.append(t)

    last_ping = time.monotonic()
    alive = True
    while alive or not q.empty():
        try:
            event, payload = q.get(timeout=0.5)
            # 본문/완료 이벤트들: id 증가
            yield sse_event(event, payload, eid=str(next_eid))
            next_eid += 1
        except queue.Empty:
            pass

        # 10초마다 ping
        if time.monotonic() - last_ping > 10:
            yield sse_event("ping", {})
            last_ping = time.monotonic()

        alive = any(t.is_alive() for t in threads)

    mapping.clear()
    yield sse_event("done", {"reason": "all_options_finished"}, eid=str(next_eid))
