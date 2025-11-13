from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView

from .views import (
    GoogleCallbackView,
    LogoutView,
    MeProfileView,
)

urlpatterns = [
    path("google/callback/", GoogleCallbackView.as_view(), name="google_callback"),
    path("refresh/", TokenRefreshView.as_view(), name="token_refresh"),
    path("logout/", LogoutView.as_view(), name="logout"),
    path("me/profile/", MeProfileView.as_view(), name="me-profile"),
]
