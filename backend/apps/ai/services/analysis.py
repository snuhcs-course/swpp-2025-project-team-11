from typing import Any

from apps.ai.services.chains import analysis_chain, integrate_chain


def analyze_speech_llm(
    subject: str | None,
    body: str | None,
):
    analysis_input = {
        "incoming_subject": subject,
        "incoming_body": body,
    }

    analysis_result = analysis_chain.invoke(analysis_input)
    return analysis_result


def integrate_analysis(analysis_results: list[dict[str, Any]]):
    integrated_result = integrate_chain.invoke({"analysis_results": analysis_results})
    # 하나의 통합된 AnalysisResult를 반환한다.

    return integrated_result
