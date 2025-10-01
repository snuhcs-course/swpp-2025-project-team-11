# 외부 API 호출 또는 토큰 처리를 위한 파일

from datetime import timedelta

import requests
from cryptography.fernet import Fernet
from django.conf import settings
from django.utils import timezone

from .models import GoogleAccount


def google_refresh(google_account: GoogleAccount):
    fernet = Fernet(settings.ENCRYPTION_KEY)
    try:
        refresh_token = fernet.decrypt(google_account.refresh_token.encode()).decode()
    except Exception as e:
        raise ValueError(f"Failed to decrypt refresh token: {str(e)}") from e

    token_url = "https://oauth2.googleapis.com/token"
    data = {
        "client_id": settings.GOOGLE_CLIENT_ID,
        "client_secret": settings.GOOGLE_CLIENT_SECRET,
        "refresh_token": refresh_token,
        "grant_type": "refresh_token",
    }

    try:
        # connect timeout=3초, read timeout=10초
        res = requests.post(token_url, data=data, timeout=(3, 10))
        res.raise_for_status()
        token_json = res.json()
    except requests.Timeout as e:
        raise TimeoutError("Timeout while requesting new access token from Google") from e
    except requests.RequestException as e:
        raise ValueError(f"Failed to refresh Google access token: {str(e)}") from e
    except ValueError as e:
        raise ValueError(f"Invalid JSON response from Google: {str(e)}") from e

    access_token = token_json.get("access_token")
    if not access_token:
        raise ValueError(
            f"Google token refresh failed: {token_json.get('error_description', 'unknown error')}"
        )

    expires_in = token_json.get("expires_in", 3600)
    google_account.access_token = fernet.encrypt(access_token.encode()).decode()
    google_account.expires_at = timezone.now() + timedelta(seconds=expires_in)
    google_account.save()

    return access_token
