# common/mixins.py
from rest_framework.permissions import IsAuthenticated
from rest_framework_simplejwt.authentication import JWTAuthentication


class AuthRequiredMixin:
    authentication_classes = [JWTAuthentication]
    permission_classes = [IsAuthenticated]


class OwnerQuerysetMixin:
    """Filter queryset to the current user's own resources."""

    owner_field = "user"

    def get_queryset(self):
        qs = super().get_queryset()
        if self.owner_field:
            return qs.filter(**{self.owner_field: self.request.user})
        return qs
