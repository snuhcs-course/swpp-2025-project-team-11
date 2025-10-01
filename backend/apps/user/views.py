from urllib.parse import urlencode

import requests
from django.conf import settings
from django.contrib.auth import logout as django_logout
from django.shortcuts import redirect
from rest_framework import status
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.tokens import RefreshToken

from .models import User
from .serializers import UserSerializer


# client에서 구글 로그인하는 로직으로 변경시, 해당 뷰는 삭제. 해당 뷰의 역할을 client가 대신함.
class GoogleStartView(APIView):
    def get(self, request):
        base_url = "https://accounts.google.com/o/oauth2/v2/auth"
        params = {
            "client_id": settings.GOOGLE_CLIENT_ID,
            "redirect_uri": "http://localhost/auth/google/callback",
            "response_type": "code",
            "scope": "openid email profile https://www.googleapis.com/auth/gmail.readonly",
            "access_type": "offline",
            "prompt": "consent",
        }
        return redirect(f"{base_url}?{urlencode(params)}")


class GoogleCallbackView(APIView):
    def post(self, request):
        user = request.user
        code = request.data.get("auth_code")

        if not code:
            return Response(
                {"detail": "Authorization code is required."}, status=status.HTTP_400_BAD_REQUEST
            )

        token_url = "https://oauth2.googleapis.com/token"
        data = {
            "code": code,
            "client_id": settings.GOOGLE_CLIENT_ID,
            "client_secret": settings.GOOGLE_CLIENT_SECRET,
            "redirect_uri": settings.GOOGLE_OAUTH_REDIRECT_URI,
            # Android에서 사용한 redirect_uri와 동일
            "grant_type": "authorization_code",
        }

        token_res = requests.post(token_url, data=data)
        token_json = token_res.json()

        if "error" in token_json:
            return Response(token_json, status=status.HTTP_400_BAD_REQUEST)

        # Access/Refresh Token 저장 (DB와 연결)
        access_token = token_json.get("access_token")
        refresh_token = token_json.get("refresh_token")

        # # 예: user 모델에 refresh_token 저장: google token은 서버에서 보관
        # if refresh_token:
        #     user.refresh_token = refresh_token
        #     user.save()

        userinfo_res = requests.get(
            "https://www.googleapis.com/oauth2/v3/userinfo",
            headers={"Authorization": f"Bearer {access_token}"},
        )
        userinfo = userinfo_res.json()
        email = userinfo.get("email")
        name = userinfo.get("name")

        # Users DB 등록
        user, created = User.objects.get_or_create(
            email=email, defaults={"name": name, "refresh_token": refresh_token}
        )

        # JWT 발급
        refresh = RefreshToken.for_user(user)
        jwt_tokens = {
            "access": str(refresh.access_token),
            "refresh": str(refresh),
        }

        return Response(
            {
                "user": UserSerializer(user).data,
                "jwt": jwt_tokens,
            },
            status=status.HTTP_200_OK,
        )

        return Response(
            {
                "access_token": access_token,
                "refresh_token": refresh_token,
            }
        )

    # client 구글 로그인 로직 테스트를 위해 주석 처리 함.
    # def get(self, request):
    #     code = request.GET.get("code")
    #     if not code:
    #         return Response(
    #             {"detail": "Error: no code provided"}, status=status.HTTP_400_BAD_REQUEST
    #         )

    #     token_url = "https://oauth2.googleapis.com/token"
    #     data = {
    #         "code": code,
    #         "client_id": settings.GOOGLE_CLIENT_ID,
    #         "client_secret": settings.GOOGLE_CLIENT_SECRET,
    #         "redirect_uri": "http://localhost/auth/google/callback",
    #         "grant_type": "authorization_code",
    #     }
    #     token_res = requests.post(token_url, data=data)
    #     token_json = token_res.json()

    #     access_token = token_json.get("access_token")
    #     refresh_token = token_json.get("refresh_token")

    #     if not access_token:
    #         return Response(
    #             {"detail": "Failed to get access token"}, status=status.HTTP_400_BAD_REQUEST
    #         )

    #     # 구글 userinfo
    #     userinfo_res = requests.get(
    #         "https://www.googleapis.com/oauth2/v3/userinfo",
    #         headers={"Authorization": f"Bearer {access_token}"},
    #     )
    #     userinfo = userinfo_res.json()
    #     email = userinfo.get("email")
    #     name = userinfo.get("name")

    #     # Users DB 등록
    #     user, created = User.objects.get_or_create(
    #         email=email, defaults={"name": name, "refresh_token": refresh_token}
    #     )

    #     # JWT 발급
    #     refresh = RefreshToken.for_user(user)
    #     jwt_tokens = {
    #         "access": str(refresh.access_token),
    #         "refresh": str(refresh),
    #     }

    #     return Response(
    #         {
    #             "user": UserSerializer(user).data,
    #             "jwt": jwt_tokens,
    #         },
    #         status=status.HTTP_200_OK,
    #     )


def google_refresh(user):
    # permission_classes = [IsAuthenticated]

    if not getattr(user, "refresh_token", None):
        raise ValueError("No Google refresh token found")

    token_url = "https://oauth2.googleapis.com/token"
    data = {
        "client_id": settings.GOOGLE_CLIENT_ID,
        "client_secret": settings.GOOGLE_CLIENT_SECRET,
        "refresh_token": user.refresh_token,
        "grant_type": "refresh_token",
    }
    token_res = requests.post(token_url, data=data)
    token_json = token_res.json()

    access_token = token_json.get("access_token")
    if not access_token:
        raise ValueError("Failed to refresh Google access token")

    return access_token


class LogoutView(APIView):
    permission_classes = [IsAuthenticated]

    def post(self, request):
        try:
            refresh_token = request.data.get("refresh_token")
            if refresh_token:
                # SimpleJWT의 블랙리스트 기능으로 JWT 리프레시 토큰 무효화
                token = RefreshToken(refresh_token)
                token.blacklist()
        except Exception:
            pass

        django_logout(request)
        return Response({"detail": "Successfully logged out"}, status=status.HTTP_200_OK)
