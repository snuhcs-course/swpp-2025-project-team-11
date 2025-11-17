from datetime import timedelta

import requests
from cryptography.fernet import Fernet
from django.conf import settings
from django.contrib.auth import logout as django_logout
from django.utils import timezone
from drf_spectacular.utils import OpenApiExample, OpenApiResponse, OpenApiTypes, extend_schema
from rest_framework import generics, status
from rest_framework.generics import RetrieveUpdateAPIView
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework_simplejwt.tokens import RefreshToken

from ..core.mixins import AuthRequiredMixin
from .models import GoogleAccount, User, UserProfile
from .serializers import (
    GoogleCallbackRequestSerializer,
    GoogleCallbackResponseSerializer,
    LogoutRequestSerializer,
    UserProfileSerializer,
)


class GoogleCallbackView(generics.GenericAPIView):
    permission_classes = [AllowAny]
    serializer_class = GoogleCallbackRequestSerializer

    @extend_schema(
        summary="Google OAuth callback",
        request=GoogleCallbackRequestSerializer,
        responses={200: GoogleCallbackResponseSerializer},
    )
    def post(self, request):
        body = self.get_serializer(data=request.data)
        body.is_valid(raise_exception=True)
        code = body.validated_data["auth_code"]

        token_url = "https://oauth2.googleapis.com/token"
        # 웹 클라이언트 설정값을 사용해야 함
        data = {
            "code": code,
            "client_id": settings.GOOGLE_CLIENT_ID,
            "client_secret": settings.GOOGLE_CLIENT_SECRET,
            "redirect_uri": settings.SERVER_BASEURL + "api/user/google/callback/",
            # 로컬 테스트용, 추후 환경변수 처리 필요함
            "grant_type": "authorization_code",
        }

        try:
            token_res = requests.post(token_url, data=data, timeout=10)
            token_res.raise_for_status()
            token_json = token_res.json()
        except requests.RequestException as e:
            return Response(
                {"detail": f"Failed to get token from Google: {str(e)}"},
                status=status.HTTP_401_UNAUTHORIZED,
            )

        if "error" in token_json:
            return Response(token_json, status=status.HTTP_400_BAD_REQUEST)

        access_token = token_json.get("access_token")
        refresh_token = token_json.get("refresh_token")
        expires_in = token_json.get("expires_in", 3600)

        # Google userinfo 요청
        try:
            userinfo_res = requests.get(
                "https://www.googleapis.com/oauth2/v3/userinfo",
                headers={"Authorization": f"Bearer {access_token}"},
                timeout=10,
            )
            userinfo_res.raise_for_status()
            userinfo = userinfo_res.json()
        except requests.RequestException as e:
            return Response(
                {"detail": f"Failed to get user info from Google: {str(e)}"},
                status=status.HTTP_401_UNAUTHORIZED,
            )

        email = userinfo.get("email")
        name = userinfo.get("name")

        if not email:
            return Response({"detail": "Email not provided by Google."}, status=status.HTTP_400_BAD_REQUEST)

        # User DB 저장
        user, created = User.objects.get_or_create(email=email, defaults={"name": name})

        # GoogleAccount DB에 access / refresh 토큰 암호화 저장
        try:
            fernet = Fernet(settings.ENCRYPTION_KEY)
            enc_access_token = fernet.encrypt(access_token.encode()).decode()
            enc_refresh_token = fernet.encrypt(refresh_token.encode()).decode()
        except Exception as e:
            return Response(
                {"detail": f"Failed to encrypt tokens: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        expires_at = timezone.now() + timedelta(seconds=expires_in)

        try:
            # ISSUE: ValueError: cannot assign User:<> OutstandingToken.user must be User instance
            google_account, _ = GoogleAccount.objects.update_or_create(
                user=user,
                defaults={
                    "access_token": enc_access_token,
                    "refresh_token": enc_refresh_token,
                    "expires_at": expires_at,
                },
            )
        except Exception as e:
            return Response(
                {"detail": f"Failed to save Google account: {str(e)}"},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            )

        # JWT 발급
        refresh = RefreshToken.for_user(user)
        jwt_tokens = {
            "access": str(refresh.access_token),
            "refresh": str(refresh),
        }

        return Response(
            GoogleCallbackResponseSerializer(
                {"user": user, "jwt": jwt_tokens},
            ).data,
            status=status.HTTP_200_OK,
        )


class LogoutView(AuthRequiredMixin, generics.GenericAPIView):
    serializer_class = LogoutRequestSerializer

    @extend_schema(
        summary="Logout",
        responses={
            200: OpenApiResponse(
                response=OpenApiTypes.OBJECT,
                examples=[
                    OpenApiExample(
                        "Success",
                        value={"detail": "Successfully logged out"},
                        response_only=True,
                    )
                ],
            ),
        },
    )
    def post(self, request):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        refresh_token = serializer.validated_data.get("refresh_token")
        try:
            if refresh_token:
                # SimpleJWT의 블랙리스트 기능으로 JWT 리프레시 토큰 무효화
                token = RefreshToken(refresh_token)
                token.blacklist()
        except Exception:
            pass

        django_logout(request)
        return Response({"detail": "Successfully logged out"}, status=status.HTTP_200_OK)


class MeProfileView(AuthRequiredMixin, RetrieveUpdateAPIView):
    serializer_class = UserProfileSerializer

    def get_object(self):
        user = self.request.user
        profile, _ = UserProfile.objects.get_or_create(user=user, defaults={"display_name": user.name})
        return profile
