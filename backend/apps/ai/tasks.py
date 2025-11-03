from celery import shared_task

from .models import AnaylsisResult
from .services.langchain import analyze_speech_llm


@shared_task
def analyze_speech():
    # langchain 이용하여 주어진 메일로 사용자의 말투를 분석한다.

    analysis_result = analyze_speech_llm()

    AnaylsisResult.objects.create()
    # DB에 분석 결과 저장 (형태 및 모델 지정 필요함)
    return analysis_result
