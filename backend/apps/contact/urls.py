from django.urls import path

from apps.contact.views import (
    ContactContextByContactView,
    ContactDetailView,
    ContactListCreateView,
    GroupDetailView,
    GroupListCreateView,
    PromptOptionDetailView,
    PromptOptionListCreateView,
    TemplateDetailView,
    TemplateListCreateView,
)

urlpatterns = [
    path("groups/", GroupListCreateView.as_view(), name="group-list"),
    path("groups/<int:pk>/", GroupDetailView.as_view(), name="group-detail"),
    path("", ContactListCreateView.as_view(), name="contact-list"),
    path("<int:pk>/", ContactDetailView.as_view(), name="contact-detail"),
    path(
        "<int:contact_id>/context/",
        ContactContextByContactView.as_view(),
        name="contact-context-by-contact",
    ),
    # Prompt Options
    path("prompt-options/", PromptOptionListCreateView.as_view(), name="prompt-option-list"),
    path("prompt-options/<int:pk>/", PromptOptionDetailView.as_view(), name="prompt-option-detail"),
    # Templates
    path("templates/", TemplateListCreateView.as_view(), name="template-list"),
    path("templates/<int:pk>/", TemplateDetailView.as_view(), name="template-detail"),
]
