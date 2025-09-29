from urllib.parse import urlencode

import requests
from django.conf import settings
from django.http import HttpResponse
from django.shortcuts import redirect

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
    id_token = token_json.get("id_token")  # noqa: F841

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
    user, created = Users.objects.get_or_create(email=email, defaults={"name": name})

    # # 4. Django 세션 로그인
    # login(request, user)

    return HttpResponse(f"로그인 완료! 환영합니다, {user.name}")


def refresh(request):
    # 리프레시 토큰을 발급함.
    pass


def logout(request):
    # 로그아웃 처리.
    pass
