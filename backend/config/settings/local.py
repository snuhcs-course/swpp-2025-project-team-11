# flake8: noqa: F401, F403, F405
from datetime import timedelta

from ..enums import ServerEnvironmentType
from .base import *

ENVIRONMENT = ServerEnvironmentType.LOCAL

SIMPLE_JWT = {
    "ROTATE_REFRESH_TOKENS": True,
    "BLACKLIST_AFTER_ROTATION": True,
    "ACCESS_TOKEN_LIFETIME": timedelta(minutes=120),
    "REFRESH_TOKEN_LIFETIME": timedelta(days=14),
}
