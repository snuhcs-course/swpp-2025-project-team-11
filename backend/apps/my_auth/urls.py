from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView

from . import views

urlpatterns = [
    path("google/start", views.google_start),
    path("google/callback", views.google_callback),
    path("google/refresh", views.google_refresh),
    path("refresh/", TokenRefreshView.as_view()),
    path("logout/", views.logout),
]
