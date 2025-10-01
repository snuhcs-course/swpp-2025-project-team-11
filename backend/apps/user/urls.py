from django.urls import path

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
    path("logout/", LogoutView.as_view(), name="logout"),
]
