# flake8: noqa: F401, F403, F405
from ..enums import ServerEnvironmentType
from .base import *

ENVIRONMENT = ServerEnvironmentType.DEV

ALLOWED_HOSTS = ["xend-fiveis-dev.duckdns.org", "15.164.93.45"]
