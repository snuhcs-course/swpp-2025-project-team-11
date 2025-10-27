from rest_framework import serializers


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


class EmailSendSerializer(serializers.Serializer):
    """Email send request serializer (single body + auto multipart)"""

    to = serializers.ListField(
        child=serializers.EmailField(),
        min_length=1,
        help_text="Primary recipients (To). One or more email addresses.",
    )
    cc = serializers.ListField(
        child=serializers.EmailField(),
        required=False,
        allow_empty=True,
        help_text="Carbon copy recipients (optional).",
    )
    bcc = serializers.ListField(
        child=serializers.EmailField(),
        required=False,
        allow_empty=True,
        help_text="Blind carbon copy recipients (optional).",
    )

    subject = serializers.CharField(max_length=500, help_text="Email subject")
    body = serializers.CharField(help_text="Email body (HTML or plain text)")
    is_html = serializers.BooleanField(required=False, default=True, help_text="Treat body as HTML")

    def validate(self, attrs):
        def _dedup(emails):
            seen, out = set(), []
            for e in emails or []:
                key = e.strip().lower()
                if key and key not in seen:
                    seen.add(key)
                    out.append(e.strip())
            return out

        attrs["to"] = _dedup(attrs.get("to"))
        attrs["cc"] = _dedup(attrs.get("cc"))
        attrs["bcc"] = _dedup(attrs.get("bcc"))
        if not attrs["to"]:
            raise serializers.ValidationError({"to": "At least one recipient is required."})

        total_rcpts = len(attrs["to"]) + len(attrs["cc"]) + len(attrs["bcc"])
        if total_rcpts > 100:
            raise serializers.ValidationError({"detail": "Too many recipients (max 100)."})

        return attrs


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
