from rest_framework import serializers

from .models import User


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
