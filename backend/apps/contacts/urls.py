from django.urls import path

from apps.contacts.views import (
    ContactContextByContactView,
    ContactDetailView,
    ContactListCreateView,
    GroupDetailView,
    GroupListCreateView,
    GroupOptionMapDetailView,
    GroupOptionMapListCreateView,
    PromptOptionDetailView,
    PromptOptionListCreateView,
    TemplateDetailView,
    TemplateListCreateView,
)

urlpatterns = [
    path("groups/", GroupListCreateView.as_view(), name="group-list"),
    path("groups/<int:pk>/", GroupDetailView.as_view(), name="group-detail"),
    path("contacts/", ContactListCreateView.as_view(), name="contact-list"),
    path("contacts/<int:pk>/", ContactDetailView.as_view(), name="contact-detail"),
    path(
        "contacts/<int:contact_id>/context/",
        ContactContextByContactView.as_view(),
        name="contact-context-by-contact",
    ),
    # Prompt Options
    path("prompt-options/", PromptOptionListCreateView.as_view(), name="prompt-option-list"),
    path("prompt-options/<int:pk>/", PromptOptionDetailView.as_view(), name="prompt-option-detail"),
    # Group-Option Maps
    path(
        "group-option-maps/", GroupOptionMapListCreateView.as_view(), name="group-option-map-list"
    ),
    path(
        "group-option-maps/<int:pk>/",
        GroupOptionMapDetailView.as_view(),
        name="group-option-map-detail",
    ),
    # Templates
    path("templates/", TemplateListCreateView.as_view(), name="template-list"),
    path("templates/<int:pk>/", TemplateDetailView.as_view(), name="template-detail"),
]
