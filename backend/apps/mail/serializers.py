from rest_framework import serializers

from .models import Email


class EmailListSerializer(serializers.Serializer):
    """Email list response serializer"""

    id = serializers.CharField()
    thread_id = serializers.CharField()
    subject = serializers.CharField()
    from_email = serializers.CharField(source="from")
    snippet = serializers.CharField()
    date = serializers.DateTimeField(allow_null=True)
    date_raw = serializers.CharField()
    is_unread = serializers.BooleanField()
    label_ids = serializers.ListField(child=serializers.CharField())


class EmailDetailSerializer(serializers.Serializer):
    """Email detail response serializer"""

    id = serializers.CharField()
    thread_id = serializers.CharField()
    subject = serializers.CharField()
    from_email = serializers.CharField(source="from")
    to = serializers.CharField()
    date = serializers.DateTimeField(allow_null=True)
    date_raw = serializers.CharField()
    body = serializers.CharField()
    snippet = serializers.CharField()
    is_unread = serializers.BooleanField()
    label_ids = serializers.ListField(child=serializers.CharField())


class EmailModelSerializer(serializers.ModelSerializer):
    """Email model serializer for DB storage (optional)"""

    class Meta:
        model = Email
        fields = [
            "id",
            "gmail_id",
            "thread_id",
            "subject",
            "sender",
            "recipient",
            "snippet",
            "body",
            "received_at",
            "is_unread",
            "labels",
            "created_at",
            "updated_at",
        ]
        read_only_fields = ["id", "created_at", "updated_at"]


class EmailSendSerializer(serializers.Serializer):
    """Email send request serializer"""

    to = serializers.EmailField(help_text="Recipient email address")
    subject = serializers.CharField(max_length=500, help_text="Email subject")
    body = serializers.CharField(help_text="Email body (plain text)")


class EmailSendResponseSerializer(serializers.Serializer):
    """Email send response serializer"""

    id = serializers.CharField(help_text="Sent message ID")
    threadId = serializers.CharField(help_text="Thread ID")
    labelIds = serializers.ListField(child=serializers.CharField(), help_text="Label IDs")


class EmailListQuerySerializer(serializers.Serializer):
    max_results = serializers.IntegerField(required=False, default=20, min_value=1, max_value=100)
    page_token = serializers.CharField(required=False, allow_blank=True)
    labels = serializers.CharField(
        required=False,
        allow_blank=True,
        help_text='Comma-separated labels (e.g., "INBOX,UNREAD"). Default: "INBOX"',
    )


class EmailMarkReadRequestSerializer(serializers.Serializer):
    is_read = serializers.BooleanField(required=False, default=True)


class EmailMarkReadResponseSerializer(serializers.Serializer):
    id = serializers.CharField()
    labelIds = serializers.ListField(child=serializers.CharField())
