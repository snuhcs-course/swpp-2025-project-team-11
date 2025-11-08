from apps.ai.services.chains import prompt_preview_chain
from apps.ai.services.utils import build_prompt_inputs, collect_prompt_context


def generate_prompt_preview(*, user, to_emails: list[str]) -> str:
    """
    수신자 이메일 목록을 받아서, 실제 메일 생성 시 적용될
    컨텍스트/톤/그룹 정보를 한국어 설명문으로 만들어준다.
    """
    ctx = collect_prompt_context(user, to_emails, fewshot_k=0)
    prompt_inputs = build_prompt_inputs(ctx)

    try:
        text = prompt_preview_chain.invoke(prompt_inputs).strip()
    except Exception:
        text = "현재 수신자에 대한 프롬프트 정보가 없습니다.."
    return text
