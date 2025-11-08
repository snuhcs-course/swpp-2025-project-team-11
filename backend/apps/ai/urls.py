from django.urls import path

from .views import EmailPromptPreviewView, MailGenerateStreamView, MailGenerateWithPlanStreamView, ReplyOptionsStreamView

urlpatterns = [
    path("mail/generate/stream/", MailGenerateStreamView.as_view(), name="mail-generate-stream"),
    path("mail/generate-with-plan/stream/", MailGenerateWithPlanStreamView.as_view(), name="mail-generate-with-plan-stream"),
    path("mail/reply/stream/", ReplyOptionsStreamView.as_view(), name="mail-reply-stream"),
    path("mail/prompts/preview/", EmailPromptPreviewView.as_view()),
]
