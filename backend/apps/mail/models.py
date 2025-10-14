from django.db import models

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
        unique_together = (("provider", "external_message_id"),)
        indexes = [
            models.Index(fields=["user", "sent_at"]),
            models.Index(fields=["provider", "external_message_id"]),
            models.Index(fields=["provider", "thread_id"]),
        ]
        ordering = ["-sent_at", "-id"]

    def __str__(self):
        return f"[{self.provider}] {self.subject or '(no subject)'}"
