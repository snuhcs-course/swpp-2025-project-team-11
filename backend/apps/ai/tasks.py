from celery import shared_task
from django.db import transaction

from ..contact.models import Contact
from .models import AnalysisResult
from .services.langchain import analyze_speech_llm


@shared_task(bind=True, max_retries=3)
def analyze_speech(self, user, subject, body, to_emails):
    # langchain 이용하여 주어진 메일로 사용자의 말투를 분석한다.
    try:
        analysis_result = analyze_speech_llm(user, subject, body, to_emails)
        contacts = Contact.objects.filter(email__in=to_emails, user_id=user.id)

        with transaction.atomic():
            for contact in contacts:
                AnalysisResult.objects.create(
                    user_id=user.id,
                    contact=contact,
                    lexical_style=analysis_result["lexical_style"],
                    grammar_patterns=analysis_result["grammar_patterns"],
                    emotional_tone=analysis_result["emotional_tone"],
                    figurative_usage=analysis_result["figurative_usage"],
                    long_sentence_ratio=analysis_result["long_sentence_ratio"],
                    representative_sentences=analysis_result["representative_sentences"],
                )
    except Exception as e:
        self.retry(exc=e, countdown=2**self.request.retries)

    return analysis_result
