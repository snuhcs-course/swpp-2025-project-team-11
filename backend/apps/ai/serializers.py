from rest_framework import serializers


class MailGenerateRequest(serializers.Serializer):
    subject = serializers.CharField(required=False, allow_blank=True, allow_null=True)
    body = serializers.CharField(required=False, allow_blank=True, allow_null=True)
    relationship = serializers.CharField(required=False, allow_blank=True, allow_null=True)
    situational_prompt = serializers.CharField(required=False, allow_blank=True, allow_null=True)
    style_prompt = serializers.CharField(required=False, allow_blank=True, allow_null=True)
    format_prompt = serializers.CharField(required=False, allow_blank=True, allow_null=True)
    language = serializers.CharField(required=False, allow_blank=True, allow_null=True)
