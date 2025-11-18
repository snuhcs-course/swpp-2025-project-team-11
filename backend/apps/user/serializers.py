from rest_framework import serializers

from .models import User, UserProfile


class UserSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ("id", "email", "name")


class JWTSerializer(serializers.Serializer):
    access = serializers.CharField()
    refresh = serializers.CharField()


class GoogleCallbackRequestSerializer(serializers.Serializer):
    auth_code = serializers.CharField(required=True, help_text="Google authorization code")


class GoogleCallbackResponseSerializer(serializers.Serializer):
    user = UserSerializer()
    jwt = JWTSerializer()


class LogoutRequestSerializer(serializers.Serializer):
    refresh_token = serializers.CharField(required=False, allow_blank=True)


class UserProfileSerializer(serializers.ModelSerializer):
    display_name = serializers.CharField(required=False, allow_blank=True, max_length=60)
    info = serializers.CharField(
        required=False,
        allow_blank=True,
        trim_whitespace=False,
        help_text="메일 프롬프트에 포함될 자유 텍스트 (소속, 직책, 연락처 등 최대 1000자)",
        max_length=1000,
    )

    language_preference = serializers.CharField(
        required=False,
        allow_blank=True,
        max_length=30,
    )

    class Meta:
        model = UserProfile
        fields = ("display_name", "info", "language_preference", "created_at", "updated_at")
        read_only_fields = ("created_at", "updated_at")
