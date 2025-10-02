import json


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
