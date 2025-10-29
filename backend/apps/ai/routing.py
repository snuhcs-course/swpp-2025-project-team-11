from django.urls import re_path

from .consumers import MailGenerateConsumer

websocket_urlpatterns = [
    re_path(r"ws/ai/mail/$", MailGenerateConsumer.as_asgi()),
]
