from channels.middleware.base import BaseMiddleware
from django.contrib.auth.models import AnonymousUser
from rest_framework_simplejwt.authentication import JWTAuthentication

jwt_auth = JWTAuthentication()


class JWTAuthMiddleware(BaseMiddleware):
    """
    WebSocket 연결 시 Sec-WebSocket-Protocol에서 Access Token을 읽고 검증.
    만료된 경우 scope["user"]는 AnonymousUser 로 유지하고,
    consumer가 클라이언트에게 "token_invalid" 이벤트 전송하도록 처리 -> 클라이언트는 REST API로 refresh token 보내 access token 재발급 로직 수행
    """

    async def __call__(self, scope, receive, send):

        scope["user"] = AnonymousUser()
        scope["token_invalid"] = False  # 토큰 유효성 결과 저장

        headers = {k.decode().lower(): v.decode() for k, v in scope.get("headers", [])}

        token = headers.get("sec-websocket-protocol")

        if token:
            try:
                validated_token = jwt_auth.get_validated_token(token)
                user = jwt_auth.get_user(validated_token)
                scope["user"] = user

            except Exception:
                scope["token_invalid"] = True

        return await super().__call__(scope, receive, send)
