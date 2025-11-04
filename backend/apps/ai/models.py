from django.contrib.postgres.fields import ArrayField
from django.db import models

from apps.contact.models import Contact
from apps.user.models import User


class AnalysisResult(models.Model):
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="anaylsis_result",
        db_index=True,
    )
    contact = models.ForeignKey(
        Contact,
        on_delete=models.SET_NULL,
        null=True,
        related_name="anaylsis_result",
        db_index=True,
    )

    # 프롬프트에서 분석하는 항목에 따라 필드 수정 필요함
    lexical_style = models.TextField()
    grammar_patterns = models.TextField()
    emotional_tone = models.TextField()
    figurative_usage = models.TextField()
    long_sentence_ratio = models.TextField()
    representative_sentences = ArrayField(base_field=models.TextField(), default=list)
