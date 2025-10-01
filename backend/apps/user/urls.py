from django.urls import path

from .views import (
    GoogleCallbackView,
    LogoutView,
)

urlpatterns = [
    path("google/callback/", GoogleCallbackView.as_view(), name="google_callback"),
    path("logout/", LogoutView.as_view(), name="logout"),
]
