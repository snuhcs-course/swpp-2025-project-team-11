from django.contrib.postgres.fields import ArrayField
from django.db import models

from apps.contact.models import Contact, Group
from apps.core.models import TimeStampedModel
from apps.user.models import User


class MailAnalysisResult(TimeStampedModel):
    # 메일마다 분석 결과를 저장하는 모델
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="mail_anaylsis_result",
        db_index=True,
    )
    contact = models.ForeignKey(
        Contact,
        on_delete=models.CASCADE,
        related_name="mail_anaylsis_result",
        db_index=True,
    )

    lexical_style = models.JSONField()
    grammar_patterns = models.JSONField()
    emotional_tone = models.JSONField()
    representative_sentences = ArrayField(base_field=models.TextField(), default=list)


class ContactAnalysisResult(TimeStampedModel):
    # contact 단위로 통합된 분석 결과를 저장하는 모델
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="contact_anaylsis_result",
        db_index=True,
    )
    contact = models.ForeignKey(
        Contact,
        on_delete=models.CASCADE,
        related_name="contact_anaylsis_result",
        db_index=True,
    )
    last_analysis_id = models.IntegerField(null=True, blank=True)
    # 마지막으로 통합에 사용된 MailAnalysisResult 모델의 id를 저장함

    lexical_style = models.JSONField()
    grammar_patterns = models.JSONField()
    emotional_tone = models.JSONField()
    representative_sentences = ArrayField(base_field=models.TextField(), default=list)


class GroupAnalysisResult(TimeStampedModel):
    # group 단위로 통합된 분석 결과를 저장하는 모델
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="group_anaylsis_result",
        db_index=True,
    )
    group = models.ForeignKey(
        Group,
        on_delete=models.CASCADE,
        related_name="group_analysis_result",
        db_index=True,
    )
    last_analysis_id = models.IntegerField(null=True, blank=True)
    # 마지막으로 통합에 사용된 MailAnalysisResult 모델의 id를 저장함

    lexical_style = models.JSONField()
    grammar_patterns = models.JSONField()
    emotional_tone = models.JSONField()
    representative_sentences = ArrayField(base_field=models.TextField(), default=list)
