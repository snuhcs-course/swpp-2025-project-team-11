import os

from rest_framework import serializers

from apps.ai.constants import MAX_FILE_SIZE_MB, SUPPORTED_FILE_TYPES


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


class PromptPreviewRequestSerializer(serializers.Serializer):
    to = serializers.ListField(
        child=serializers.EmailField(),
        allow_empty=False,
    )


class AttachmentAnalyzeFromMailSerializer(serializers.Serializer):
    message_id = serializers.CharField()
    attachment_id = serializers.CharField()
    filename = serializers.CharField()
    mime_type = serializers.CharField(required=False, allow_blank=True, default="")


class AttachmentAnalyzeUploadSerializer(serializers.Serializer):
    file = serializers.FileField()

    def validate_file(self, file):
        max_size = MAX_FILE_SIZE_MB * 1024 * 1024  # MB → bytes
        if file.size > max_size:
            raise serializers.ValidationError(f"File size exceeds {MAX_FILE_SIZE_MB} MB limit.")

        ext = os.path.splitext(file.name)[1].lower().lstrip(".")
        if ext not in SUPPORTED_FILE_TYPES:
            raise serializers.ValidationError(f"Unsupported file type '.{ext}'. " f"Supported types are: {', '.join(SUPPORTED_FILE_TYPES)}")
        return file


class AttachmentAnalysisResponseSerializer(serializers.Serializer):
    summary = serializers.CharField(help_text="첨부파일 요약")
    insights = serializers.CharField(help_text="핵심 인사이트")
    mail_guide = serializers.CharField(help_text="이메일 작성 가이드")
