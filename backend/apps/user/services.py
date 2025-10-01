# 외부 API 호출 또는 토큰 처리를 위한 파일

import requests
from django.conf import settings


def google_refresh(user):
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
