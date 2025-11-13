from django.db import models, transaction
from rest_framework import serializers

from apps.contact.models import (
    Contact,
    ContactContext,
    Group,
    PromptOption,
    Template,
)


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


class ContactContextInGroupSerializer(serializers.ModelSerializer):
    class Meta:
        model = ContactContext
        fields = (
            "id",
            "sender_role",
            "recipient_role",
            "relationship_details",
            "personal_prompt",
            "language_preference",
            "created_at",
            "updated_at",
        )
        read_only_fields = (
            "id",
            "sender_role",
            "recipient_role",
            "relationship_details",
            "personal_prompt",
            "language_preference",
            "created_at",
            "updated_at",
        )


class ContactInGroupSerializer(serializers.ModelSerializer):
    context = ContactContextInGroupSerializer(read_only=True, allow_null=True)

    class Meta:
        model = Contact
        fields = ("id", "name", "email", "context", "created_at", "updated_at")
        read_only_fields = ("id", "name", "email", "context", "created_at", "updated_at")


class GroupSerializer(serializers.ModelSerializer):
    options = PromptOptionSerializer(many=True, read_only=True)

    option_ids = serializers.ListField(
        child=serializers.IntegerField(min_value=1),
        required=False,
        allow_empty=True,
        write_only=True,
    )

    contacts = ContactInGroupSerializer(many=True, read_only=True)

    background_color = serializers.CharField(required=False, allow_blank=False, default="#FFFFFF")
    emoji = serializers.CharField(required=False, allow_blank=True, max_length=16)

    class Meta:
        model = Group
        fields = ("id", "name", "description", "background_color", "emoji", "options", "option_ids", "contacts", "created_at", "updated_at")
        read_only_fields = ("id", "contacts", "created_at", "updated_at")

    def _resolve_options_for_user(self, option_ids):
        """
        option_ids를 검증해 PromptOption 객체 리스트로 변환.
        - 시스템 옵션(NULL) 또는 현재 사용자 생성 옵션만 허용
        - 전달 순서 보존, 중복 제거
        """
        if option_ids is None:
            return None

        deduped = list(dict.fromkeys(option_ids))
        if not deduped:
            return []

        user = self.context["request"].user
        qs = PromptOption.objects.filter(id__in=deduped).filter(models.Q(created_by__isnull=True) | models.Q(created_by=user))
        found = set(qs.values_list("id", flat=True))
        missing = [oid for oid in deduped if oid not in found]
        if missing:
            raise serializers.ValidationError({"option_ids": f"ID not allowed or non-existent: {missing}"})

        by_id = {o.id: o for o in qs}
        return [by_id[oid] for oid in deduped]

    def validate(self, attrs):
        if "option_ids" in attrs:
            attrs["_options_to_set"] = self._resolve_options_for_user(attrs.get("option_ids"))
        return attrs

    def create(self, validated_data):
        validated_data["user"] = self.context["request"].user

        options_to_set = validated_data.pop("_options_to_set", None)
        validated_data.pop("option_ids", None)

        group = super().create(validated_data)

        if options_to_set is not None:
            group.options.set(options_to_set)
        return group

    def update(self, instance, validated_data):
        options_to_set = validated_data.pop("_options_to_set", None)
        validated_data.pop("option_ids", None)

        for k, v in validated_data.items():
            setattr(instance, k, v)
        instance.save()

        # option_ids 키가 요청에 있었다면 세트 치환, 없었다면 기존 유지
        if options_to_set is not None:
            instance.options.set(options_to_set)

        return instance


class GroupInContactSerializer(serializers.ModelSerializer):
    options = PromptOptionSerializer(many=True, read_only=True)

    class Meta:
        model = Group
        fields = ("id", "name", "description", "background_color", "emoji", "options", "created_at", "updated_at")
        read_only_fields = ("id", "name", "description", "options", "created_at", "updated_at")


class ContactContextSerializer(serializers.ModelSerializer):
    class Meta:
        model = ContactContext
        fields = (
            "id",
            "sender_role",
            "recipient_role",
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
    group = GroupInContactSerializer(read_only=True)

    group_id = serializers.PrimaryKeyRelatedField(
        queryset=Group.objects.all(),
        source="group",
        required=False,
        allow_null=True,
        write_only=True,
    )
    context = ContactContextSerializer(required=False, allow_null=True)

    class Meta:
        model = Contact
        fields = ("id", "group", "name", "email", "group_id", "context", "created_at", "updated_at")
        read_only_fields = ("id", "group", "created_at", "updated_at")

    def get_fields(self):
        fields = super().get_fields()
        request = self.context.get("request")
        if request and request.user and request.user.is_authenticated:
            fields["group_id"].queryset = Group.objects.filter(user=request.user)
        else:
            fields["group_id"].queryset = Group.objects.none()
        return fields

    def validate(self, attrs):
        email = attrs.get("email")
        if email:
            user = self.context["request"].user
            qs = Contact.objects.filter(user=user, email=email)
            if self.instance:
                qs = qs.exclude(pk=self.instance.pk)
            if qs.exists():
                raise serializers.ValidationError({"email": "The contact information for that email already exists."})
        return attrs

    def validate_group(self, group):
        # Allow null group; otherwise enforce same owner
        if group and group.user_id != self.context["request"].user.id:
            raise serializers.ValidationError("You cannot assign a contact to a group owned by another user.")
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
            raise serializers.ValidationError("You cannot assign a contact to a group owned by another user.")

        contact = super().update(instance, validated_data)

        # If context field is not included: do nothing
        if ctx is serializers.empty:
            return contact

        # If context is explicitly null: do nothing (no delete here)
        if ctx is None:
            return contact

        ContactContext.objects.update_or_create(contact=contact, defaults=ctx)

        return contact


# ===== Template =====
class TemplateSerializer(serializers.ModelSerializer):
    class Meta:
        model = Template
        fields = ("id", "name", "content", "created_at", "updated_at")
        read_only_fields = ("id", "created_at", "updated_at")

    def create(self, validated_data):
        validated_data["user"] = self.context["request"].user
        return super().create(validated_data)
