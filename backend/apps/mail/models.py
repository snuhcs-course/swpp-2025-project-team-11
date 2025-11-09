from datetime import timedelta

from django.db import models
from django.db.models import Q
from django.utils import timezone

from apps.contact.models import Contact
from apps.user.models import User


class SentMail(models.Model):
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="sent_mails",
        db_index=True,
    )
    contact = models.ForeignKey(
        Contact,
        on_delete=models.SET_NULL,
        null=True,
        related_name="sent_mails",
        db_index=True,
    )
    provider = models.CharField(
        max_length=20,
        default="gmail",
        db_index=True,
    )
    external_message_id = models.CharField(
        max_length=255,
        blank=True,
    )
    thread_id = models.CharField(
        max_length=255,
        blank=True,
    )
    subject = models.CharField(max_length=300, blank=True)
    body = models.TextField()
    headers = models.JSONField(default=dict, blank=True)
    sent_at = models.DateTimeField(db_index=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=["provider", "external_message_id"],
                name="uniq_sentmail_provider_external_message_id_nonblank",
                condition=~Q(external_message_id=""),
            ),
        ]

        indexes = [
            models.Index(fields=["user", "sent_at"]),
            models.Index(fields=["provider", "thread_id"]),
        ]
        ordering = ["-sent_at", "-id"]

    def __str__(self):
        return f"[{self.provider}] {self.subject or '(no subject)'}"


class AttachmentAnalysis(models.Model):
    user = models.ForeignKey(User, on_delete=models.CASCADE)
    message_id = models.CharField(max_length=255, blank=True, default="")
    attachment_id = models.CharField(max_length=800, blank=True, default="")
    content_key = models.CharField(max_length=255, blank=True, default="")

    filename = models.CharField(max_length=255, blank=True, default="")
    mime_type = models.CharField(max_length=100, blank=True, default="")

    summary = models.TextField(blank=True, default="")
    insights = models.TextField(blank=True, default="")
    mail_guide = models.TextField(blank=True, default="")

    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        indexes = [
            models.Index(fields=["user", "attachment_id"]),
            models.Index(fields=["user", "content_key"]),
        ]

    @classmethod
    def get_recent_by_attachment(cls, user, attachment_id: str):
        one_day_ago = timezone.now() - timedelta(days=1)
        return cls.objects.filter(
            user=user,
            attachment_id=attachment_id,
            created_at__gte=one_day_ago,
        ).first()

    @classmethod
    def get_recent_by_content_key(cls, user, content_key: str):
        one_day_ago = timezone.now() - timedelta(days=1)
        return cls.objects.filter(
            user=user,
            content_key=content_key,
            created_at__gte=one_day_ago,
        ).first()
