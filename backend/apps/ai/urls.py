from django.urls import path

from .views import (
    AttachmentAnalyzeFromMailView,
    AttachmentAnalyzeUploadView,
    EmailPromptPreviewView,
    MailGenerateAnalysisTestView,
    MailGenerateStreamTestView,
    MailGenerateStreamView,
    MailGenerateWithPlanStreamView,
    ReplyOptionsStreamView,
)

urlpatterns = [
    path("mail/generate/stream/", MailGenerateStreamView.as_view(), name="mail-generate-stream"),
    path("mail/generate-with-plan/stream/", MailGenerateWithPlanStreamView.as_view(), name="mail-generate-with-plan-stream"),
    path("mail/generate/streamtest/", MailGenerateStreamTestView.as_view(), name="mail-generate-stream-test"),
    path("mail/reply/stream/", ReplyOptionsStreamView.as_view(), name="mail-reply-stream"),
    path("mail/prompts/preview/", EmailPromptPreviewView.as_view()),
    path(
        "mail/attachments/analyze/",
        AttachmentAnalyzeFromMailView.as_view(),
        name="mail-attachment-analyze",
    ),
    path(
        "mail/attachments/analyze-upload/",
        AttachmentAnalyzeUploadView.as_view(),
        name="mail-attachment-analyze-upload",
    ),
    path("mail/generate/test/", MailGenerateAnalysisTestView.as_view(), name="mail-generate-test"),
]
