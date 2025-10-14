from django.db import transaction
from rest_framework import serializers

from apps.contact.models import (
    Contact,
    ContactContext,
    Group,
    GroupOptionMap,
    PromptOption,
    Template,
)


class GroupSerializer(serializers.ModelSerializer):
    class Meta:
        model = Group
        fields = ("id", "name", "description", "created_at", "updated_at")
        read_only_fields = ("id", "created_at", "updated_at")

    def create(self, validated_data):
        # Owner is always the request user
        validated_data["user"] = self.context["request"].user
        return super().create(validated_data)


class ContactContextSerializer(serializers.ModelSerializer):
    class Meta:
        model = ContactContext
        fields = (
            "id",
            "relationship_role",
            "relationship_details",
            "personal_prompt",
            "language_preference",
            "created_at",
            "updated_at",
        )
        read_only_fields = (
            "id",
            "created_at",
            "updated_at",
        )


class ContactSerializer(serializers.ModelSerializer):
    context = ContactContextSerializer(required=False, allow_null=True)

    class Meta:
        model = Contact
        fields = ("id", "group", "name", "email", "context", "created_at", "updated_at")
        read_only_fields = ("id", "created_at", "updated_at")

    def validate_group(self, group):
        # Allow null group; otherwise enforce same owner
        if group and group.user_id != self.context["request"].user.id:
            raise serializers.ValidationError(
                "You cannot assign a contact to a group owned by another user."
            )
        return group

    @transaction.atomic
    def create(self, validated_data):
        ctx = validated_data.pop("context", None)
        validated_data["user"] = self.context["request"].user
        contact = super().create(validated_data)

        if ctx is not None:
            # Create context if payload is provided (even empty dict is fine)
            ContactContext.objects.create(contact=contact, **ctx)
        return contact

    @transaction.atomic
    def update(self, instance, validated_data):
        # Extract nested context payload (if present)
        ctx = validated_data.pop("context", serializers.empty)

        # Re-validate group ownership if group is being changed
        group = validated_data.get("group", None)
        if group and group.user_id != self.context["request"].user.id:
            raise serializers.ValidationError(
                "You cannot assign a contact to a group owned by another user."
            )

        contact = super().update(instance, validated_data)

        # If context field is not included: do nothing
        if ctx is serializers.empty:
            return contact

        # If context is explicitly null: do nothing (no delete here)
        if ctx is None:
            return contact

        # If context payload is provided (dict): upsert behavior
        if hasattr(contact, "context"):
            for k, v in ctx.items():
                setattr(contact.context, k, v)
            contact.context.save()
        else:
            ContactContext.objects.create(contact=contact, **ctx)
        return contact


class PromptOptionSerializer(serializers.ModelSerializer):
    class Meta:
        model = PromptOption
        fields = ("id", "key", "name", "prompt", "created_at", "updated_at")
        read_only_fields = ("id", "created_at", "updated_at")

    def create(self, validated_data):
        # For API-created options, default owner to current user
        validated_data.setdefault("created_by", self.context["request"].user)
        return super().create(validated_data)

    def update(self, instance, validated_data):
        # Keep original owner; do not allow changing created_by via API
        validated_data.pop("created_by", None)
        return super().update(instance, validated_data)


# ===== Group-Option Map =====
class GroupOptionMapSerializer(serializers.ModelSerializer):
    class Meta:
        model = GroupOptionMap
        fields = ("id", "group", "option", "created_at", "updated_at")
        read_only_fields = ("id", "created_at", "updated_at")

    def validate(self, attrs):
        user = self.context["request"].user
        group = attrs.get("group")
        option = attrs.get("option")

        if group.user_id != user.id:
            raise serializers.ValidationError("You can only map prompt options to groups you own.")

        # Allow system-defined (created_by=None) or the current user's options
        if option.created_by and option.created_by_id != user.id:
            raise serializers.ValidationError(
                "You cannot map a prompt option created by another user."
            )
        return attrs


# ===== Template =====
class TemplateSerializer(serializers.ModelSerializer):
    class Meta:
        model = Template
        fields = ("id", "name", "content", "created_at", "updated_at")
        read_only_fields = ("id", "created_at", "updated_at")

    def create(self, validated_data):
        validated_data["user"] = self.context["request"].user
        return super().create(validated_data)
