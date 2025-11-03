from django.db import models

from apps.contact.models import Contact
from apps.user.models import User


class AnaylsisResult(models.Model):
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

    # 추가 필드 정의 필요함
