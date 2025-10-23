from django.db import transaction
from django.db.models import Q
from django.shortcuts import get_object_or_404
from rest_framework import generics
from rest_framework.exceptions import PermissionDenied

from apps.contact.models import (
    Contact,
    ContactContext,
    Group,
    PromptOption,
    Template,
)
from apps.contact.serializers import (
    ContactContextSerializer,
    ContactSerializer,
    GroupSerializer,
    PromptOptionSerializer,
    TemplateSerializer,
)
from apps.core.mixins import AuthRequiredMixin, OwnerQuerysetMixin


# ===== Groups =====
class GroupListCreateView(AuthRequiredMixin, OwnerQuerysetMixin, generics.ListCreateAPIView):
    queryset = Group.objects.all()
    serializer_class = GroupSerializer
    owner_field = "user"

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)


class GroupDetailView(AuthRequiredMixin, OwnerQuerysetMixin, generics.RetrieveUpdateDestroyAPIView):
    queryset = Group.objects.all()
    serializer_class = GroupSerializer
    owner_field = "user"


# ===== Contacts (with nested context create/update support) =====
class ContactListCreateView(AuthRequiredMixin, OwnerQuerysetMixin, generics.ListCreateAPIView):
    queryset = Contact.objects.select_related("group")
    serializer_class = ContactSerializer
    owner_field = "user"

    def get_queryset(self):
        qs = super().get_queryset()
        group_id = self.request.query_params.get("group")
        if group_id:
            qs = qs.filter(group_id=group_id)
        return qs

    @transaction.atomic
    def perform_create(self, serializer):
        # serializer.create handles nested context create
        serializer.save(user=self.request.user)


class ContactDetailView(
    AuthRequiredMixin, OwnerQuerysetMixin, generics.RetrieveUpdateDestroyAPIView
):
    queryset = Contact.objects.select_related("group")
    serializer_class = ContactSerializer
    owner_field = "user"

    @transaction.atomic
    def perform_update(self, serializer):
        # serializer.update handles nested context update
        serializer.save()


class ContactContextDetailView(AuthRequiredMixin, generics.RetrieveUpdateDestroyAPIView):
    serializer_class = ContactContextSerializer

    def get_queryset(self):
        return ContactContext.objects.filter(contact__user=self.request.user)


# ===== Nested endpoint: /contact/{contact_id}/context/ =====
class ContactContextByContactView(AuthRequiredMixin, generics.RetrieveUpdateDestroyAPIView):
    """
    GET/PUT/PATCH/DELETE context bound to a contact the user owns.
    - PATCH with {"context": null} on Contact is ignored (no change).
    - To delete the context, call DELETE on this endpoint.
    """

    serializer_class = ContactContextSerializer

    def get_object(self):
        contact = get_object_or_404(Contact, id=self.kwargs["contact_id"], user=self.request.user)
        return get_object_or_404(ContactContext, contact=contact)


# ===== Prompt Options =====
class PromptOptionListCreateView(AuthRequiredMixin, generics.ListCreateAPIView):
    """
    List system-defined (created_by=None) + current user's options.
    """

    serializer_class = PromptOptionSerializer

    def get_queryset(self):
        user = self.request.user
        return PromptOption.objects.filter(Q(created_by=user) | Q(created_by__isnull=True))

    def perform_create(self, serializer):
        # If a system seed is needed, create via management command rather than API.
        serializer.save(created_by=self.request.user)


class PromptOptionDetailView(AuthRequiredMixin, generics.RetrieveUpdateDestroyAPIView):
    serializer_class = PromptOptionSerializer

    def get_queryset(self):
        user = self.request.user
        return PromptOption.objects.filter(created_by=user) | PromptOption.objects.filter(
            created_by__isnull=True
        )

    def perform_update(self, serializer):
        # System-defined options are read-only

        if self.get_object().created_by is None:
            raise PermissionDenied("System-defined options are read-only.")
        serializer.save()

    def perform_destroy(self, instance):
        # System-defined options cannot be deleted
        if instance.created_by is None:
            raise PermissionDenied("System-defined options cannot be deleted.")
        instance.delete()


# ===== Templates =====
class TemplateListCreateView(AuthRequiredMixin, OwnerQuerysetMixin, generics.ListCreateAPIView):
    queryset = Template.objects.all()
    serializer_class = TemplateSerializer

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)


class TemplateDetailView(
    AuthRequiredMixin, OwnerQuerysetMixin, generics.RetrieveUpdateDestroyAPIView
):
    queryset = Template.objects.all()
    serializer_class = TemplateSerializer
