# flake8: noqa: F401, F403, F405
from datetime import timedelta

from ..enums import ServerEnvironmentType
from .base import *

ENVIRONMENT = ServerEnvironmentType.DEV

ALLOWED_HOSTS = ["xend-fiveis-dev.duckdns.org", "15.164.93.45", "xend1.com"]

SIMPLE_JWT = {
    "ROTATE_REFRESH_TOKENS": True,
    "BLACKLIST_AFTER_ROTATION": True,
    "ACCESS_TOKEN_LIFETIME": timedelta(minutes=20),
}
