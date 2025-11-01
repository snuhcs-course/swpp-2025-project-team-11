from channels.routing import ProtocolTypeRouter, URLRouter
from django.core.asgi import get_asgi_application

from apps.ai.routing import websocket_urlpatterns
from config.utils import set_environment

set_environment()
django_asgi_app = get_asgi_application()
# Django 기본 ASGI application

from config.jwt_middleware import JWTAuthMiddleware  # noqa: E402

# ProtocolTypeRouter: HTTP / WebSocket 분기
application = ProtocolTypeRouter(
    {
        "http": django_asgi_app,
        "websocket": JWTAuthMiddleware(URLRouter(websocket_urlpatterns)),
    }
)
