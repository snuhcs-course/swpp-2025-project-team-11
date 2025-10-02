from django.urls import path

from .views import MailGenerateStreamView

urlpatterns = [
    path("mail/generate/stream", MailGenerateStreamView.as_view()),
]
