from django.db import models

from apps.core.models import TimeStampedModel
from apps.user.models import User


class Group(TimeStampedModel):
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="contact_groups",
    )
    name = models.CharField(max_length=120)
    description = models.TextField(blank=True)

    options = models.ManyToManyField(
        "PromptOption",
        related_name="groups",
    )

    class Meta:
        ordering = ["user_id", "name", "id"]
        constraints = [
            models.UniqueConstraint(
                fields=["user", "name"],
                name="uniq_contacts_group_user_name",
            ),
        ]

    def __str__(self):
        return self.name


class Contact(TimeStampedModel):
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="contact",
    )
    group = models.ForeignKey(
        Group,
        on_delete=models.SET_NULL,
        null=True,
        blank=True,
        related_name="contact",
    )
    name = models.CharField(max_length=120)
    email = models.EmailField()

    class Meta:
        ordering = ["user_id", "name", "id"]
        constraints = [
            models.UniqueConstraint(
                fields=["user", "email"],
                name="uniq_contacts_contact_user_email",
            ),
        ]

    def __str__(self):
        return f"{self.name} <{self.email}>"


class ContactContext(TimeStampedModel):
    contact = models.OneToOneField(
        Contact,
        on_delete=models.CASCADE,
        related_name="context",
    )
    relationship_role = models.CharField(max_length=50, blank=True)
    relationship_details = models.TextField(blank=True)
    personal_prompt = models.TextField(blank=True)
    language_preference = models.CharField(max_length=5, blank=True)

    class Meta:
        ordering = ["contact_id"]

    def __str__(self):
        return f"Context({self.contact_id})"


class PromptOption(TimeStampedModel):
    class Key(models.TextChoices):
        TONE = "tone", "tone"
        FORMAT = "format", "format"
        OTHER = "other", "other"

    # NULL = 시스템(사전정의)
    created_by = models.ForeignKey(
        User,
        null=True,
        blank=True,
        on_delete=models.SET_NULL,
        related_name="prompt_options",
    )

    key = models.CharField(max_length=30, choices=Key.choices)
    name = models.CharField(max_length=120)
    prompt = models.TextField()

    class Meta:
        ordering = ["key", "name", "id"]

    def __str__(self):
        return f"{self.key}:{self.name}"


class Template(TimeStampedModel):
    user = models.ForeignKey(
        User,
        on_delete=models.CASCADE,
        related_name="templates",
    )
    name = models.CharField(max_length=120)
    content = models.TextField()

    class Meta:
        ordering = ["user_id", "name", "id"]

    def __str__(self):
        return self.name
