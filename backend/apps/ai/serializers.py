from rest_framework import serializers


class MailGenerateRequest(serializers.Serializer):
    subject = serializers.CharField(required=False, allow_blank=True, allow_null=True)
    body = serializers.CharField(required=False, allow_blank=True, allow_null=True)
    to_emails = serializers.ListField(
        child=serializers.EmailField(),
        allow_empty=False,
        required=True,
    )


class ReplyGenerateRequest(serializers.Serializer):
    subject = serializers.CharField(allow_blank=True, required=True)
    body = serializers.CharField(allow_blank=True, required=True)
    to_email = serializers.EmailField(required=True, allow_blank=False)
