from django.conf import settings
from django.urls import path

from .views import (
    EmailAttachmentDownloadView,
    EmailDetailView,
    EmailListView,
    EmailMarkReadView,
    EmailSendView,
    MailTestView,
)

urlpatterns = [
    path("emails/", EmailListView.as_view(), name="email_list"),
    path("emails/send/", EmailSendView.as_view(), name="email_send"),
    path("emails/<str:message_id>/", EmailDetailView.as_view(), name="email_detail"),
    path("emails/<str:message_id>/read/", EmailMarkReadView.as_view(), name="email_mark_read"),
    path(
        "emails/<str:message_id>/attachments/<str:attachment_id>/",
        EmailAttachmentDownloadView.as_view(),
        name="email_attachment_download",
    ),
]

# Test endpoint only in DEBUG mode
if settings.DEBUG:
    urlpatterns.append(path("test/", MailTestView.as_view(), name="mail_test"))
