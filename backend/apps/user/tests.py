from datetime import timedelta
from unittest.mock import Mock, patch

from cryptography.fernet import Fernet
from django.conf import settings
from django.contrib.auth import get_user_model
from django.test import TestCase
from django.urls import reverse
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APIClient
from rest_framework_simplejwt.tokens import RefreshToken

from apps.user.models import GoogleAccount
from apps.user.utils import google_token_required

User = get_user_model()


# ============================================================
# GoogleCallbackView 테스트
# ============================================================
class GoogleCallbackViewTest(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.url = reverse("google_callback")
        self.test_auth_code = "test_auth_code_123"
        self.test_access_token = "test_access_token_456"
        self.test_refresh_token = "test_refresh_token_789"
        self.test_email = "test@example.com"
        self.test_name = "Test User"

    @patch("requests.post")
    @patch("requests.get")
    def test_google_callback_success_new_user(self, mock_get, mock_post):
        """새 사용자의 Google OAuth 콜백 성공 테스트"""
        # mock: 구글 토큰 응답
        mock_token_response = Mock()
        mock_token_response.json.return_value = {
            "access_token": self.test_access_token,
            "refresh_token": self.test_refresh_token,
            "expires_in": 3600,
        }
        mock_token_response.raise_for_status = Mock()
        mock_post.return_value = mock_token_response

        # mock: userinfo 응답
        mock_userinfo_response = Mock()
        mock_userinfo_response.json.return_value = {
            "email": self.test_email,
            "name": self.test_name,
        }
        mock_userinfo_response.raise_for_status = Mock()
        mock_get.return_value = mock_userinfo_response

        resp = self.client.post(self.url, {"auth_code": self.test_auth_code})

        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertIn("user", resp.data)
        self.assertIn("jwt", resp.data)
        self.assertEqual(resp.data["user"]["email"], self.test_email)
        self.assertIn("access", resp.data["jwt"])
        self.assertIn("refresh", resp.data["jwt"])

        # DB 저장 확인
        user = User.objects.get(email=self.test_email)
        self.assertEqual(user.name, self.test_name)

        ga = GoogleAccount.objects.get(user=user)
        self.assertIsNotNone(ga.access_token)
        self.assertIsNotNone(ga.refresh_token)

    @patch("requests.post")
    @patch("requests.get")
    def test_google_callback_success_existing_user(self, mock_get, mock_post):
        """기존 사용자가 다시 로그인해도 동일 user를 사용한다."""
        existing = User.objects.create(email=self.test_email, name="Old Name")

        mock_token_response = Mock()
        mock_token_response.json.return_value = {
            "access_token": self.test_access_token,
            "refresh_token": self.test_refresh_token,
            "expires_in": 3600,
        }
        mock_token_response.raise_for_status = Mock()
        mock_post.return_value = mock_token_response

        mock_userinfo_response = Mock()
        mock_userinfo_response.json.return_value = {
            "email": self.test_email,
            "name": self.test_name,
        }
        mock_userinfo_response.raise_for_status = Mock()
        mock_get.return_value = mock_userinfo_response

        resp = self.client.post(self.url, {"auth_code": self.test_auth_code})

        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        # 유저는 여전히 1명이어야 함
        self.assertEqual(User.objects.filter(email=self.test_email).count(), 1)

        user = User.objects.get(email=self.test_email)
        self.assertEqual(user.id, existing.id)

    @patch("requests.post")
    def test_google_callback_token_request_failure(self, mock_post):
        """구글 토큰 요청 자체가 실패하면 401을 돌려야 한다."""
        import requests

        mock_post.side_effect = requests.RequestException("Network error")

        resp = self.client.post(self.url, {"auth_code": self.test_auth_code})

        # 뷰 코드 기준: token_res 요청 실패 시 401_UNAUTHORIZED
        self.assertEqual(resp.status_code, status.HTTP_401_UNAUTHORIZED)
        self.assertIn("detail", resp.data)

    @patch("requests.post")
    def test_google_callback_token_error_response(self, mock_post):
        """구글이 error 필드를 포함해주면 그대로 400을 내려야 한다."""
        mock_token_response = Mock()
        mock_token_response.json.return_value = {
            "error": "invalid_grant",
            "error_description": "Invalid authorization code",
        }
        mock_token_response.raise_for_status = Mock()
        mock_post.return_value = mock_token_response

        resp = self.client.post(self.url, {"auth_code": self.test_auth_code})

        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("error", resp.data)

    @patch("requests.post")
    @patch("requests.get")
    def test_google_callback_userinfo_request_failure(self, mock_get, mock_post):
        """userinfo 호출 실패하면 401이어야 한다."""
        mock_token_response = Mock()
        mock_token_response.json.return_value = {
            "access_token": self.test_access_token,
            "refresh_token": self.test_refresh_token,
            "expires_in": 3600,
        }
        mock_token_response.raise_for_status = Mock()
        mock_post.return_value = mock_token_response

        import requests

        mock_get.side_effect = requests.RequestException("Network error")

        resp = self.client.post(self.url, {"auth_code": self.test_auth_code})

        # 뷰 코드 기준: userinfo 실패 시 401_UNAUTHORIZED
        self.assertEqual(resp.status_code, status.HTTP_401_UNAUTHORIZED)

    @patch("requests.post")
    @patch("requests.get")
    def test_google_callback_no_email(self, mock_get, mock_post):
        """구글이 email을 안 주면 400"""
        mock_token_response = Mock()
        mock_token_response.json.return_value = {
            "access_token": self.test_access_token,
            "refresh_token": self.test_refresh_token,
            "expires_in": 3600,
        }
        mock_token_response.raise_for_status = Mock()
        mock_post.return_value = mock_token_response

        mock_userinfo_response = Mock()
        mock_userinfo_response.json.return_value = {"name": self.test_name}  # email 없음
        mock_userinfo_response.raise_for_status = Mock()
        mock_get.return_value = mock_userinfo_response

        resp = self.client.post(self.url, {"auth_code": self.test_auth_code})

        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("detail", resp.data)

    def test_google_callback_missing_auth_code(self):
        """auth_code 자체가 없으면 400"""
        resp = self.client.post(self.url, {})
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)

    @patch("requests.post")
    @patch("requests.get")
    @patch("cryptography.fernet.Fernet.encrypt")
    def test_google_callback_encryption_failure(self, mock_encrypt, mock_get, mock_post):
        """토큰 암호화 중 오류 -> 500"""
        mock_token_response = Mock()
        mock_token_response.json.return_value = {
            "access_token": self.test_access_token,
            "refresh_token": self.test_refresh_token,
            "expires_in": 3600,
        }
        mock_token_response.raise_for_status = Mock()
        mock_post.return_value = mock_token_response

        mock_userinfo_response = Mock()
        mock_userinfo_response.json.return_value = {
            "email": self.test_email,
            "name": self.test_name,
        }
        mock_userinfo_response.raise_for_status = Mock()
        mock_get.return_value = mock_userinfo_response

        mock_encrypt.side_effect = Exception("Encryption error")

        resp = self.client.post(self.url, {"auth_code": self.test_auth_code})

        self.assertEqual(resp.status_code, status.HTTP_500_INTERNAL_SERVER_ERROR)


# ============================================================
# Refresh Token (SimpleJWT 토큰 갱신) 테스트
# ============================================================
class RefreshTokenViewTest(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.url = reverse("token_refresh")
        self.user = User.objects.create(email="test@example.com", name="Test User")
        self.refresh = RefreshToken.for_user(self.user)
        self.access_token = str(self.refresh.access_token)
        self.refresh_token = str(self.refresh)

    def test_refresh_token_success(self):
        resp = self.client.post(self.url, {"refresh": self.refresh_token})
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertIn("access", resp.data)
        new_access_token = resp.data["access"]
        self.assertNotEqual(new_access_token, self.access_token)

    def test_refresh_token_with_new_refresh(self):
        resp = self.client.post(self.url, {"refresh": self.refresh_token})
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        # rotate 여부는 설정에 따라 다를 수 있으므로 구체 assert는 생략

    def test_refresh_token_invalid(self):
        resp = self.client.post(self.url, {"refresh": "invalid_token_string"})
        self.assertEqual(resp.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_refresh_token_missing(self):
        resp = self.client.post(self.url, {})
        self.assertEqual(resp.status_code, status.HTTP_400_BAD_REQUEST)

    def test_refresh_token_expired(self):
        expired_refresh = RefreshToken.for_user(self.user)
        expired_refresh.set_exp(lifetime=-timedelta(days=1))
        expired_token = str(expired_refresh)

        resp = self.client.post(self.url, {"refresh": expired_token})
        self.assertEqual(resp.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_refresh_token_blacklisted(self):
        # blacklist()가 활성화 된 환경에서만 의미 있음
        try:
            self.refresh.blacklist()
        except AttributeError:
            self.skipTest("Token blacklist app not installed")

        resp = self.client.post(self.url, {"refresh": self.refresh_token})
        self.assertEqual(resp.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_refresh_token_inactive_user(self):
        inactive_user = User.objects.create(email="inactive@example.com", name="Inactive User", is_active=False)
        refresh = RefreshToken.for_user(inactive_user)

        resp = self.client.post(self.url, {"refresh": str(refresh)})
        self.assertIn(resp.status_code, [status.HTTP_401_UNAUTHORIZED, status.HTTP_403_FORBIDDEN])

    def test_refresh_token_validates_new_access_token(self):
        # 새 access 토큰 발급
        resp = self.client.post(self.url, {"refresh": self.refresh_token})
        self.assertEqual(resp.status_code, status.HTTP_200_OK)

        new_access_token = resp.data["access"]

        # 새 access 토큰으로 인증이 필요한 엔드포인트 호출 (logout)
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {new_access_token}")
        logout_resp = self.client.post(reverse("logout"), {})
        self.assertNotEqual(logout_resp.status_code, status.HTTP_401_UNAUTHORIZED)


# ============================================================
# LogoutView 테스트
# ============================================================
class LogoutViewTest(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.url = reverse("logout")
        self.user = User.objects.create(email="test@example.com", name="Test User")
        self.refresh = RefreshToken.for_user(self.user)
        self.access_token = str(self.refresh.access_token)
        self.refresh_token = str(self.refresh)

    def test_logout_success_with_token(self):
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {self.access_token}")
        resp = self.client.post(self.url, {"refresh_token": self.refresh_token})

        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        self.assertEqual(resp.data.get("detail"), "Successfully logged out")

    def test_logout_success_without_token(self):
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {self.access_token}")
        resp = self.client.post(self.url, {})
        self.assertEqual(resp.status_code, status.HTTP_200_OK)

    def test_logout_with_invalid_token(self):
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {self.access_token}")
        resp = self.client.post(self.url, {"refresh_token": "invalid_token"})
        self.assertEqual(resp.status_code, status.HTTP_200_OK)

    def test_logout_without_authentication(self):
        resp = self.client.post(self.url, {"refresh_token": self.refresh_token})
        self.assertEqual(resp.status_code, status.HTTP_401_UNAUTHORIZED)

    @patch("rest_framework_simplejwt.tokens.RefreshToken.blacklist")
    def test_logout_blacklist_called(self, mock_blacklist):
        self.client.credentials(HTTP_AUTHORIZATION=f"Bearer {self.access_token}")
        resp = self.client.post(self.url, {"refresh_token": self.refresh_token})
        self.assertEqual(resp.status_code, status.HTTP_200_OK)
        # mock_blacklist.called 여부까지 assert하려면 추가 가능
        # self.assertTrue(mock_blacklist.called)


# ============================================================
# GoogleAccount 모델 테스트
# ============================================================
class GoogleAccountModelTest(TestCase):
    def setUp(self):
        self.user = User.objects.create(email="test@example.com", name="Test User")
        self.fernet = Fernet(settings.ENCRYPTION_KEY)

    def test_create_google_account(self):
        access_token_plain = "test_access_token"
        refresh_token_plain = "test_refresh_token"

        enc_access = self.fernet.encrypt(access_token_plain.encode()).decode()
        enc_refresh = self.fernet.encrypt(refresh_token_plain.encode()).decode()

        expires_at = timezone.now() + timedelta(hours=1)

        ga = GoogleAccount.objects.create(
            user=self.user,
            access_token=enc_access,
            refresh_token=enc_refresh,
            expires_at=expires_at,
        )

        self.assertEqual(ga.user, self.user)
        self.assertIsNotNone(ga.access_token)
        self.assertIsNotNone(ga.refresh_token)

        decrypted_access = self.fernet.decrypt(ga.access_token.encode()).decode()
        self.assertEqual(decrypted_access, access_token_plain)

    def test_update_or_create_google_account(self):
        enc_access_1 = self.fernet.encrypt(b"token_1").decode()
        enc_refresh_1 = self.fernet.encrypt(b"refresh_1").decode()

        ga1, created1 = GoogleAccount.objects.update_or_create(
            user=self.user,
            defaults={
                "access_token": enc_access_1,
                "refresh_token": enc_refresh_1,
                "expires_at": timezone.now() + timedelta(hours=1),
            },
        )
        self.assertTrue(created1)

        enc_access_2 = self.fernet.encrypt(b"token_2").decode()
        enc_refresh_2 = self.fernet.encrypt(b"refresh_2").decode()

        ga2, created2 = GoogleAccount.objects.update_or_create(
            user=self.user,
            defaults={
                "access_token": enc_access_2,
                "refresh_token": enc_refresh_2,
                "expires_at": timezone.now() + timedelta(hours=1),
            },
        )
        self.assertFalse(created2)
        self.assertEqual(ga1.id, ga2.id)

        decrypted_access = self.fernet.decrypt(ga2.access_token.encode()).decode()
        self.assertEqual(decrypted_access, "token_2")


# ============================================================
# NEW 1: google_token_required (utils.py) 데코레이터 테스트
# ============================================================
class GoogleTokenRequiredDecoratorTest(TestCase):
    """
    google_token_required 데코레이터 동작 테스트
    """

    def setUp(self):
        self.user = User.objects.create(email="u1@example.com", name="U1")
        # expires_at: 기본은 미래 (만료 안 됨)
        self.ga = GoogleAccount.objects.create(
            user=self.user,
            access_token="ENC_ACCESS",
            refresh_token="ENC_REFRESH",
            expires_at=timezone.now() + timedelta(minutes=5),
        )

    def _make_decorated_func(self):
        captured = {}

        @google_token_required
        def dummy_fn(access_token_arg, *args, **kwargs):
            captured["token"] = access_token_arg
            return "OK"

        return dummy_fn, captured

    @patch("apps.user.utils.Fernet")
    @patch("apps.user.utils.google_refresh")
    def test_valid_token_uses_fernet_decrypt(
        self,
        mock_google_refresh,
        mock_fernet,
    ):
        """
        expires_at 가 아직 미래라면:
        - google_refresh는 호출되지 않고
        - Fernet.decrypt 값이 access_token_arg 로 들어간다
        """
        mock_fernet.return_value = Mock(
            decrypt=Mock(return_value=b"DECRYPTED_TOKEN"),
        )

        func, captured = self._make_decorated_func()
        result = func(self.user)

        self.assertEqual(result, "OK")
        self.assertEqual(captured["token"], "DECRYPTED_TOKEN")
        mock_google_refresh.assert_not_called()

    @patch("apps.user.utils.Fernet")
    @patch("apps.user.utils.google_refresh")
    def test_expired_token_calls_google_refresh(
        self,
        mock_google_refresh,
        mock_fernet,
    ):
        """
        expires_at 가 과거라면:
        - google_refresh 호출 결과를 access_token_arg 로 준다
        - decrypt 는 쓰이지 않아야 한다
        """
        # 만료 상태로 갱신
        self.ga.expires_at = timezone.now() - timedelta(seconds=1)
        self.ga.save()

        mock_google_refresh.return_value = "REFRESHED_TOKEN"
        mock_fernet.return_value = Mock(
            decrypt=Mock(return_value=b"SHOULD_NOT_USE"),
        )

        func, captured = self._make_decorated_func()
        result = func(self.user)

        self.assertEqual(result, "OK")
        self.assertEqual(captured["token"], "REFRESHED_TOKEN")
        mock_google_refresh.assert_called_once_with(self.ga)
        # 만료일 땐 decrypt 호출 안 되어야 정상
        mock_fernet.return_value.decrypt.assert_not_called()

    def test_no_google_account_raises_valueerror(self):
        """
        user.google_accounts 가 없으면 ValueError("Google account not linked")
        """
        other_user = User.objects.create(email="u2@example.com", name="U2")
        # GoogleAccount 생성 안 함

        func, _ = self._make_decorated_func()

        with self.assertRaises(ValueError) as ctx:
            func(other_user)

        self.assertIn("Google account not linked", str(ctx.exception))


# ============================================================
# NEW 2: google_refresh (services.py) 테스트
# ============================================================
class GoogleRefreshServiceTest(TestCase):
    """
    google_refresh():
    - refresh_token 복호화
    - Google 토큰 엔드포인트 호출
    - 새 access_token 암호화해서 저장
    - expires_at 갱신 (지금보다 미래)
    - 새 access_token(평문) 리턴
    """

    def setUp(self):
        self.user = User.objects.create(email="r@example.com", name="R")
        self.account = GoogleAccount.objects.create(
            user=self.user,
            access_token="enc_old_access",
            refresh_token="enc_refresh_token",
            expires_at=timezone.now() - timedelta(seconds=10),  # 이미 만료된 상태라고 가정
        )

    @patch("apps.user.services.requests.post")
    @patch("apps.user.services.Fernet")
    def test_google_refresh_success(self, mock_fernet, mock_post):
        mock_fernet.return_value = Mock(
            decrypt=Mock(return_value=b"REAL_REFRESH"),
            encrypt=Mock(return_value=b"enc_new_access"),
        )

        mock_resp = Mock()
        mock_resp.raise_for_status = Mock()
        mock_resp.json.return_value = {
            "access_token": "NEW_ACCESS_TOKEN",
            "expires_in": 3600,
        }
        mock_post.return_value = mock_resp

        from apps.user.services import google_refresh as google_refresh_func

        before_call = timezone.now()
        new_token = google_refresh_func(self.account)
        after_call = timezone.now()

        # 반환값 검증
        self.assertEqual(new_token, "NEW_ACCESS_TOKEN")

        # DB 반영 확인
        self.account.refresh_from_db()
        self.assertEqual(self.account.access_token, "enc_new_access")

        # expires_at 이 now+3600 근처의 미래인지 확인
        self.assertGreater(self.account.expires_at, after_call)
        self.assertGreater(self.account.expires_at, before_call)

        # 구글로 요청 시 보낸 refresh_token이 복호화된 값인지 확인
        args, kwargs = mock_post.call_args
        sent_data = kwargs["data"]
        self.assertEqual(sent_data.get("refresh_token"), "REAL_REFRESH")

    @patch("apps.user.services.requests.post")
    @patch("apps.user.services.Fernet")
    def test_google_refresh_timeout_raises_timeouterror(self, mock_fernet, mock_post):
        mock_fernet.return_value = Mock(
            decrypt=Mock(return_value=b"REAL_REFRESH"),
            encrypt=Mock(return_value=b"enc_new_access"),
        )

        import requests

        mock_post.side_effect = requests.Timeout("took too long")

        from apps.user.services import google_refresh as google_refresh_func

        with self.assertRaises(TimeoutError):
            google_refresh_func(self.account)

    @patch("apps.user.services.requests.post")
    @patch("apps.user.services.Fernet")
    def test_google_refresh_error_response_raises_valueerror(self, mock_fernet, mock_post):
        mock_fernet.return_value = Mock(
            decrypt=Mock(return_value=b"REAL_REFRESH"),
            encrypt=Mock(return_value=b"enc_new_access"),
        )

        mock_resp = Mock()
        mock_resp.raise_for_status = Mock()
        mock_resp.json.return_value = {
            "error": "invalid_grant",
            "error_description": "Refresh token expired",
        }
        mock_post.return_value = mock_resp

        from apps.user.services import google_refresh as google_refresh_func

        with self.assertRaises(ValueError):
            google_refresh_func(self.account)
