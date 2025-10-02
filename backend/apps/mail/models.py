from django.db import models


class Email(models.Model):
    """Gmail message cache model (optional)"""

    user = models.ForeignKey("user.User", on_delete=models.CASCADE, related_name="emails")
    gmail_id = models.CharField(max_length=255, unique=True)  # Gmail message ID
    thread_id = models.CharField(max_length=255)
    subject = models.TextField(blank=True)
    sender = models.CharField(max_length=255)
    recipient = models.CharField(max_length=255)
    snippet = models.TextField(blank=True)
    body = models.TextField(blank=True)
    received_at = models.DateTimeField()
    is_unread = models.BooleanField(default=True)  # Unread status
    labels = models.JSONField(default=list)  # ['INBOX', 'UNREAD', etc]

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["-received_at"]
        indexes = [
            models.Index(fields=["user", "-received_at"]),
            models.Index(fields=["gmail_id"]),
        ]

    def __str__(self):
        return f"{self.subject} - {self.sender}"
