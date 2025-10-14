from django.conf import settings
from django.urls import include, path
from drf_spectacular.views import SpectacularAPIView, SpectacularRedocView, SpectacularSwaggerView

from config.enums import ServerEnvironmentType

urlpatterns = [
    path("user/", include("apps.user.urls")),
    path("ai/", include("apps.ai.urls")),
    path("mail/", include("apps.mail.urls")),
    path("contact/", include("apps.contact.urls")),
]

if settings.ENVIRONMENT in [ServerEnvironmentType.LOCAL, ServerEnvironmentType.DEV]:
    urlpatterns += [
        path("schema/", SpectacularAPIView.as_view(), name="schema"),
        path("docs/", SpectacularSwaggerView.as_view(url_name="schema"), name="swagger-ui"),
        path("redoc/", SpectacularRedocView.as_view(url_name="schema"), name="redoc"),
    ]
