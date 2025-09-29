from django.urls import path

from . import views

urlpatterns = [
    # GET method
    path("google/start", views.google_start),
    path("google/callback", views.google_callback),
    # POST method
    path("refresh/", views.refresh),
    path("logout/", views.logout),
]
