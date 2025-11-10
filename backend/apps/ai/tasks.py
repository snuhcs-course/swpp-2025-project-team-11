from datetime import timedelta

from celery import shared_task
from django.contrib.auth import get_user_model
from django.db import transaction
from django.db.models import Count
from django.utils import timezone

from apps.mail.models import AttachmentAnalysis

from ..contact.models import Contact
from .models import ContactAnalysisResult, GroupAnalysisResult, MailAnalysisResult
from .services.analysis import analyze_speech_llm, integrate_analysis

User = get_user_model()


@shared_task(bind=True, max_retries=3)
def analyze_speech(self, user_id, subject, body, to_emails):
    try:
        user = User.objects.get(id=user_id)
    except User.DoesNotExist:
        return None

    # langchain 이용하여 주어진 메일로 사용자의 말투를 분석한다.
    try:
        analysis_result = analyze_speech_llm(subject, body).model_dump()
        contacts = Contact.objects.filter(email__in=to_emails, user_id=user_id)

        with transaction.atomic():
            for contact in contacts:
                result = MailAnalysisResult.objects.create(
                    user=user,
                    contact=contact,
                    lexical_style=analysis_result["lexical_style"],
                    grammar_patterns=analysis_result["grammar_patterns"],
                    emotional_tone=analysis_result["emotional_tone"],
                    figurative_usage=analysis_result["figurative_usage"],
                    long_sentence_ratio=analysis_result["long_sentence_ratio"],
                    representative_sentences=analysis_result["representative_sentences"],
                )

                # 해당 (user, contact)에 해당하는 ContactAnalysisResult이 없다면 방금 분석된 결과를 필드에 그대로 넣어줌
                if not ContactAnalysisResult.objects.filter(user=user, contact=contact).exists():
                    ContactAnalysisResult.objects.create(
                        user=user,
                        contact=contact,
                        last_analysis_id=result.id,
                        lexical_style=result.lexical_style,
                        grammar_patterns=result.grammar_patterns,
                        emotional_tone=result.emotional_tone,
                        figurative_usage=result.figurative_usage,
                        long_sentence_ratio=result.long_sentence_ratio,
                        representative_sentences=result.representative_sentences,
                    )

                # 해당 (user, contact.group)에 해당하는 GroupAnalysisResult이 없다면 방금 분석된 결과를 필드에 그대로 넣어줌
                if not GroupAnalysisResult.objects.filter(user=user, group=contact.group).exists():
                    GroupAnalysisResult.objects.create(
                        user=user,
                        group=contact.group,
                        last_analysis_id=result.id,
                        lexical_style=result.lexical_style,
                        grammar_patterns=result.grammar_patterns,
                        emotional_tone=result.emotional_tone,
                        figurative_usage=result.figurative_usage,
                        long_sentence_ratio=result.long_sentence_ratio,
                        representative_sentences=result.representative_sentences,
                    )

    except Exception as e:
        self.retry(exc=e, countdown=2**self.request.retries)

    return analysis_result


# 주기적으로 n개의 AnalysisResult를 통합하여 분석 결과를 만드는 task -> group 단위 + contact 단위
@shared_task(bind=True, max_retries=3)
def unified_analysis(self, n=10):
    try:
        # 모든 (user, contact) 쌍에 대해 MailAnalysisResult에 해당하는 row들을 찾는다.
        # 가장 id가 큰 row의 id와 ContactAanlysisResult의 last_analysis_id 필드를 비교한다. 전자가 더 크면 이후의 작업을 수행한다.
        # 최근 n개의 row를 반환한다. 각 필드별로 리스트를 묶는다.
        # integrate_analysis() 함수를 호출한다.
        # 반환된 결과를 ContactAnalysisResult 필드에 저장(업데이트)한다.

        contact_pairs = MailAnalysisResult.objects.values("user", "contact").distinct()

        for pair in contact_pairs:
            user = pair["user"]
            contact = pair["contact"]

            results_qs = MailAnalysisResult.objects.filter(user=user, contact=contact).order_by("-id")  # id 기준 최신순 정렬

            # 최근 MailAnalysisResult 중 가장 최신 id
            latest_mail_id = results_qs.first().id

            # ContactAnalysisResult 객체 가져오거나 생성 -> 반드시 존재함
            contact_result, _ = ContactAnalysisResult.objects.get_or_create(user=user, contact=contact)

            # 이미 최신 분석이 적용된 경우 skip
            if contact_result.last_analysis_id == latest_mail_id:
                continue

            # 최근 n개를 선택
            latest_n_results = list(results_qs[:n])

            # 각 필드 리스트로 묶기
            data = [
                {
                    "lexical_style": r.lexical_style,
                    "grammar_patterns": r.grammar_patterns,
                    "emotional_tone": r.emotional_tone,
                    "figurative_usage": r.figurative_usage,
                    "long_sentence_ratio": r.long_sentence_ratio,
                    "representative_sentences": r.representative_sentences,
                }
                for r in latest_n_results
            ]

            # 통합 분석 수행
            unified = integrate_analysis(data).model_dump()

            # ContactAnalysisResult 업데이트
            contact_result.lexical_style = unified["lexical_style"]
            contact_result.grammar_patterns = unified["grammar_patterns"]
            contact_result.emotional_tone = unified["emotional_tone"]
            contact_result.figurative_usage = unified["figurative_usage"]
            contact_result.long_sentence_ratio = unified["long_sentence_ratio"]
            contact_result.representative_sentences = unified["representative_sentences"]
            contact_result.last_analysis_id = latest_mail_id

            contact_result.save()

        # 마찬가지로 group 단위 통합 분석을 수행한다.
        group_pairs = MailAnalysisResult.objects.values("user", "contact__group").distinct()

        for pair in group_pairs:
            user = pair["user"]
            group = pair["contact__group"]

            results_qs = MailAnalysisResult.objects.filter(user=user, contact__group=group).order_by("-id")  # id 기준 최신순 정렬

            # 최근 MailAnalysisResult 중 가장 최신 id
            latest_mail_id = results_qs.first().id

            # GroupAnalysisResult 객체 가져오거나 생성 -> 반드시 존재함
            group_result, _ = GroupAnalysisResult.objects.get_or_create(user=user, group=group)

            # 이미 최신 분석이 적용된 경우 skip
            if group_result.last_analysis_id == latest_mail_id:
                continue

            # 최근 n개를 선택
            latest_n_results = list(results_qs[:n])

            # 각 필드 리스트로 묶기
            data = [
                {
                    "lexical_style": r.lexical_style,
                    "grammar_patterns": r.grammar_patterns,
                    "emotional_tone": r.emotional_tone,
                    "figurative_usage": r.figurative_usage,
                    "long_sentence_ratio": r.long_sentence_ratio,
                    "representative_sentences": r.representative_sentences,
                }
                for r in latest_n_results
            ]
            # 통합 분석 수행
            unified = integrate_analysis(data).model_dump()

            # GroupAnalysisResult 업데이트
            group_result.lexical_style = unified["lexical_style"]
            group_result.grammar_patterns = unified["grammar_patterns"]
            group_result.emotional_tone = unified["emotional_tone"]
            group_result.figurative_usage = unified["figurative_usage"]
            group_result.long_sentence_ratio = unified["long_sentence_ratio"]
            group_result.representative_sentences = unified["representative_sentences"]
            group_result.last_analysis_id = latest_mail_id

            group_result.save()

    except Exception as e:
        self.retry(exc=e, countdown=2**self.request.retries)


@shared_task
def delete_up_n(n=10):
    # MailAnalysis를 주기적으로 정리함. (그룹/개인에 대한 row n개 이상일 경우 가장 최근의 n개만 남겨두고 나머지 row는 삭제)

    # Contact 별 정리
    contacts = MailAnalysisResult.objects.values("contact").annotate(count=Count("id")).filter(count__gt=n)

    for entry in contacts:
        extra_count = entry["count"] - n
        MailAnalysisResult.objects.filter(contact=entry["contact"]).order_by("created_at")[:extra_count].delete()

    # Group 별 정리
    groups = MailAnalysisResult.objects.values("group").annotate(count=Count("id")).filter(count__gt=n)

    for entry in groups:
        extra_count = entry["count"] - n
        MailAnalysisResult.objects.filter(contact__group=entry["group"]).order_by("created_at")[:extra_count].delete()


@shared_task
def purge_old_attachment_analysis():
    cutoff = timezone.now() - timedelta(days=1)
    AttachmentAnalysis.objects.filter(created_at__lt=cutoff).delete()
