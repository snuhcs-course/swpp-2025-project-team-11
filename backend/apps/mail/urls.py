from django.urls import path

from .views import EmailDetailView, EmailListView, EmailSendView, MailTestView

urlpatterns = [
    path("test/", MailTestView.as_view(), name="mail_test"),
    path("emails/", EmailListView.as_view(), name="email_list"),
    path("emails/send/", EmailSendView.as_view(), name="email_send"),
    path("emails/<str:message_id>/", EmailDetailView.as_view(), name="email_detail"),
]
