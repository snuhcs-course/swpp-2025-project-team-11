from urllib.parse import urlencode

import requests
from django.conf import settings
from django.contrib.auth import logout as django_logout
from django.http import HttpResponse, JsonResponse
from django.shortcuts import redirect
from rest_framework_simplejwt.tokens import RefreshToken

from .models import Users


def google_start(request):
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


def google_callback(request):
    code = request.GET.get("code")
    if not code:
        return HttpResponse("Error: no code provided")

    token_url = "https://oauth2.googleapis.com/token"
    data = {
        "code": code,
        "client_id": settings.GOOGLE_CLIENT_ID,  # web client
        "client_secret": settings.GOOGLE_CLIENT_SECRET,
        "redirect_uri": "http://localhost/auth/google/callback",
        "grant_type": "authorization_code",
    }
    token_res = requests.post(token_url, data=data)
    token_json = token_res.json()

    access_token = token_json.get("access_token")
    refresh_token = token_json.get("refresh_token")

    if not access_token:
        return HttpResponse("Failed to get access token")

    # 2. access token으로 사용자 정보 조회
    userinfo_res = requests.get(
        "https://www.googleapis.com/oauth2/v3/userinfo",
        headers={"Authorization": f"Bearer {access_token}"},
    )
    userinfo = userinfo_res.json()
    email = userinfo.get("email")
    name = userinfo.get("name")

    # 3. 사용자 DB 확인 / 없으면 생성
    user, created = Users.objects.get_or_create(
        email=email, defaults={"name": name, "refresh_token": refresh_token}
    )

    # 4. JWT 토큰 발급해 클라이언트에게 전달
    refresh = RefreshToken.for_user(user)
    access_token = str(refresh.access_token)
    refresh_token = str(refresh)

    return JsonResponse(
        {
            "user": {"id": user.id, "email": user.email, "name": user.name},
            "jwt": {"access": access_token, "refresh": refresh_token},
        }
    )


def google_refresh(request):
    # 구글 api 접근 시 마다 refresh token을 가지고 새 access token을 발급받아 바로 사용함
    user = request.user
    if not user.is_authenticated:
        return HttpResponse("Unauthorized", status=401)

    if not user.refresh_token:
        return HttpResponse("No Google refresh token found", status=400)

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
        return HttpResponse("Failed to refresh Google access token", status=400)

    return access_token  # 서버 내부에서 이동하는 값


def logout(request):
    # 서버 세션 종료
    django_logout(request)

    # 앱에서 추가로 JWT access/refresh를 삭제
    return HttpResponse("Log-out")
