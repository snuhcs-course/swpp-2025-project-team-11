from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView

from .views import (
    GoogleCallbackView,
    GoogleStartView,
    LogoutView,
    google_refresh,
)

urlpatterns = [
    path("google/start/", GoogleStartView.as_view(), name="google_start"),
    path("google/callback/", GoogleCallbackView.as_view(), name="google_callback"),
    path("google/refresh/", google_refresh, name="google_refresh"),
    path("refresh/", TokenRefreshView.as_view(), name="token_refresh"),  # DRF JWT 토큰 갱신
    path("logout/", LogoutView.as_view(), name="logout"),
]
