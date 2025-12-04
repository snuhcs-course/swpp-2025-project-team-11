from apps.ai.services.chains import suggest_chain
from apps.ai.services.utils import build_prompt_inputs, collect_prompt_context


def suggest_mail_text(
    user,
    *,
    subject: str | None,
    body: str | None,
    to_emails: list[str],
    target: str,
    cursor: int | None = None,
) -> str:

    subject = subject or ""
    body = body or ""

    body_before = body
    body_after = ""
    if target == "body":
        if cursor is not None:
            cursor = max(0, min(cursor, len(body)))
            body_before = body[:cursor]
            body_after = body[cursor:]
        else:
            # cursor 없으면 그냥 끝에 이어쓰기
            body_before = body
            body_after = ""

    ctx = collect_prompt_context(user, to_emails)

    prompt_inputs = build_prompt_inputs(
        ctx,
        extra={
            "subject": subject,
            "body": body,
            "body_before": body_before,
            "body_after": body_after,
        },
    )
    prompt_inputs["target"] = target

    # 3) LLM 호출 (스트리밍 X, 단일 응답)
    suggestion: str = suggest_chain.invoke(prompt_inputs)

    return suggestion
