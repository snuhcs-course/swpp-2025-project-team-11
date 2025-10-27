# myapp/utils.py
from functools import wraps

from cryptography.fernet import Fernet
from django.conf import settings
from django.utils import timezone

from .models import GoogleAccount
from .services import google_refresh  # 필요 시 import


def google_token_required(func):
    @wraps(func)
    def wrapper(user, *args, **kwargs):
        try:
            google_account = user.google_accounts
        except GoogleAccount.DoesNotExist as e:
            raise ValueError("Google account not linked") from e

        if google_account.expires_at <= timezone.now():
            access_token = google_refresh(google_account)
        else:
            fernet = Fernet(settings.ENCRYPTION_KEY)
            access_token = fernet.decrypt(google_account.access_token.encode()).decode()

        return func(access_token, *args, **kwargs)

    return wrapper
