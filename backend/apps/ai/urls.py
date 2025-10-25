from django.urls import path

from .views import MailGenerateStreamView, ReplyOptionsStreamView

urlpatterns = [
    path("mail/generate/stream/", MailGenerateStreamView.as_view(), name="mail-generate-stream"),
    path("mail/reply/stream/", ReplyOptionsStreamView.as_view(), name="mail-reply-stream"),
]
