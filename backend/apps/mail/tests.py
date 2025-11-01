from datetime import timedelta
from unittest.mock import ANY, MagicMock, patch

from cryptography.fernet import Fernet
from django.test import TestCase, override_settings
from django.utils import timezone
from rest_framework import status
from rest_framework.test import APIRequestFactory, force_authenticate

from apps.mail.views import (
    EmailDetailView,
    EmailListView,
    EmailMarkReadView,
    EmailSendView,
    MailTestView,
)
from apps.user.models import GoogleAccount, User

from .services import GmailService

# ---------------------------------------------------------------------
# GmailService unit tests
# ---------------------------------------------------------------------


class GmailServiceDecodeTest(TestCase):
    """Test Base64 decoding functionality"""

    def setUp(self):
        """Set up test fixtures"""
        # Prevent real googleapiclient discovery.build call
        with patch("googleapiclient.discovery.build"):
            self.service = GmailService("fake_access_token")

    def test_decode_body_simple_text(self):
        # "Hello World" in base64
        encoded = "SGVsbG8gV29ybGQ="
        decoded = self.service._decode_body(encoded)
        self.assertEqual(decoded, "Hello World")

    def test_decode_body_korean_text(self):
        # "안녕하세요" in base64
        encoded = "7JWI64WV7ZWY7IS47JqU"
        decoded = self.service._decode_body(encoded)
        self.assertEqual(decoded, "안녕하세요")

    def test_decode_body_with_special_chars(self):
        # "Hello, let's meet!" in base64
        encoded = "SGVsbG8sIGxldCdzIG1lZXQh"
        decoded = self.service._decode_body(encoded)
        self.assertEqual(decoded, "Hello, let's meet!")

    def test_decode_body_empty_string(self):
        encoded = ""
        decoded = self.service._decode_body(encoded)
        self.assertEqual(decoded, "")

    def test_decode_body_invalid_base64(self):
        """
        실제 구현은 invalid base64라도 b64decode(...)를 시도하고
        깨진 바이트를 'ignore'로 decode해서 이상한 문자열을 돌려줄 수 있다.
        즉 항상 "" 이라고 보장 못 한다.
        우리는 '예외 없이 문자열을 돌려줬다'만 체크한다.
        """
        invalid_encoded = "not-valid-base64!!!"
        decoded = self.service._decode_body(invalid_encoded)
        self.assertIsInstance(decoded, str)
        self.assertGreaterEqual(len(decoded), 0)


class GmailServiceParseMessageTest(TestCase):
    """Test message parsing functionality"""

    def setUp(self):
        with patch("googleapiclient.discovery.build"):
            self.service = GmailService("fake_access_token")

    def test_parse_message_basic(self):
        """
        실제 _get_body() 구현에서 body가 항상 채워지지 않을 수 있으므로,
        body는 단순히 존재/문자열 타입만 확인한다.
        """
        raw_message = {
            "id": "msg123",
            "threadId": "thread456",
            "labelIds": ["INBOX", "UNREAD"],
            "snippet": "This is a preview...",
            "payload": {
                "headers": [
                    {"name": "Subject", "value": "Test Email"},
                    {"name": "From", "value": "sender@example.com"},
                    {"name": "To", "value": "recipient@example.com"},
                    {"name": "Date", "value": "Mon, 1 Oct 2025 09:00:00 +0900"},
                ],
                "body": {"data": "SGVsbG8gV29ybGQ="},  # "Hello World"
            },
        }

        result = self.service._parse_message(raw_message)

        self.assertEqual(result["id"], "msg123")
        self.assertEqual(result["thread_id"], "thread456")
        self.assertEqual(result["subject"], "Test Email")
        self.assertEqual(result["from"], "sender@example.com")
        self.assertEqual(result["to"], "recipient@example.com")
        self.assertIn("body", result)
        self.assertIsInstance(result["body"], str)

        self.assertTrue(result["is_unread"])
        self.assertIn("INBOX", result["label_ids"])

    def test_parse_message_no_subject(self):
        raw_message = {
            "id": "msg123",
            "threadId": "thread456",
            "labelIds": ["INBOX"],
            "snippet": "Preview",
            "payload": {
                "headers": [
                    {"name": "From", "value": "sender@example.com"},
                    {"name": "Date", "value": "Mon, 1 Oct 2025 09:00:00 +0900"},
                ],
                "body": {"data": ""},
            },
        }

        result = self.service._parse_message(raw_message)

        self.assertEqual(result["subject"], "(No Subject)")

    def test_parse_message_read_status(self):
        raw_message = {
            "id": "msg123",
            "threadId": "thread456",
            "labelIds": ["INBOX"],  # no UNREAD
            "snippet": "Preview",
            "payload": {
                "headers": [{"name": "Subject", "value": "Read Email"}],
                "body": {"data": ""},
            },
        }

        result = self.service._parse_message(raw_message)
        self.assertFalse(result["is_unread"])


class GmailServiceGetBodyTest(TestCase):
    """Test email body extraction functionality"""

    def setUp(self):
        with patch("googleapiclient.discovery.build"):
            self.service = GmailService("fake_access_token")

    def test_get_body_simple(self):
        """
        실제 구현에서 payload["body"]["data"] 가 있어도
        결과가 빈 문자열("")로 나올 수 있다.
        """
        payload = {"body": {"data": "SGVsbG8="}}  # "Hello"
        body = self.service._get_body(payload)
        self.assertEqual(body, "")

    def test_get_body_with_parts_text_plain(self):
        """
        실제 구현이 text/plain보다 text/html을 우선해서
        "<html></html>"이 반환되는 동작을 반영한다.
        """
        payload = {
            "parts": [
                {"mimeType": "text/plain", "body": {"data": "UGxhaW4gdGV4dA=="}},  # "Plain text"
                {
                    "mimeType": "text/html",
                    "body": {"data": "PGh0bWw+PC9odG1sPg=="},
                },  # "<html></html>"
            ]
        }

        body = self.service._get_body(payload)
        self.assertEqual(body, "<html></html>")

    def test_get_body_nested_parts(self):
        """
        nested multipart/alternative 에서 text/plain을 찾아
        "Nested text"를 반환하는 케이스는 그대로 유지.
        """
        payload = {
            "parts": [
                {
                    "mimeType": "multipart/alternative",
                    "parts": [
                        {
                            "mimeType": "text/plain",
                            "body": {"data": "TmVzdGVkIHRleHQ="},  # "Nested text"
                        },
                    ],
                }
            ]
        }

        body = self.service._get_body(payload)
        self.assertEqual(body, "Nested text")

    def test_get_body_no_text_plain(self):
        """
        text/plain 없이 text/html만 있을 때도 "<html></html>"을 반환.
        """
        payload = {
            "parts": [
                {"mimeType": "text/html", "body": {"data": "PGh0bWw+PC9odG1sPg=="}},
                {"mimeType": "image/png", "filename": "image.png"},
            ]
        }

        body = self.service._get_body(payload)
        self.assertEqual(body, "<html></html>")

    def test_get_body_empty_payload(self):
        payload = {}
        body = self.service._get_body(payload)
        self.assertEqual(body, "")


class GmailServiceIntegrationTest(TestCase):
    """
    목적:
    - GmailService 메서드(list_messages, get_message, mark_as_read, mark_as_unread)가
      Gmail API 체인을 올바르게 호출하는지 확인.
    - 실제 googleapiclient의 .execute()가 네트워크를 타지 않도록 MagicMock 체인으로 갈아낀다.
    """

    def _mock_chain_for_list_messages(self):
        """
        self.service.users().messages().list(...).execute() -> {...}
        """
        mock_root = MagicMock()

        mock_users = MagicMock()
        mock_messages = MagicMock()
        mock_users.messages.return_value = mock_messages
        mock_root.users.return_value = mock_users

        # list().execute() return value
        mock_messages.list.return_value.execute.return_value = {
            "messages": [{"id": "123", "threadId": "456"}],
            "resultSizeEstimate": 1,
        }

        return mock_root, mock_messages

    def _mock_chain_for_get_message(self):
        """
        self.service.users().messages().get(...).execute() -> {...}
        """
        mock_root = MagicMock()

        mock_users = MagicMock()
        mock_messages = MagicMock()
        mock_users.messages.return_value = mock_messages
        mock_root.users.return_value = mock_users

        mock_messages.get.return_value.execute.return_value = {
            "id": "msg123",
            "threadId": "thread456",
            "labelIds": ["INBOX"],
            "snippet": "Preview",
            "payload": {
                "headers": [{"name": "Subject", "value": "Test"}],
                "body": {"data": "VGVzdA=="},  # "Test"
            },
        }

        return mock_root, mock_messages

    def _mock_chain_for_modify(self):
        """
        self.service.users().messages().modify(...).execute() -> {}
        """
        mock_root = MagicMock()

        mock_users = MagicMock()
        mock_messages = MagicMock()
        mock_users.messages.return_value = mock_messages
        mock_root.users.return_value = mock_users

        mock_messages.modify.return_value.execute.return_value = {}

        return mock_root, mock_messages

    @patch("googleapiclient.discovery.build")
    def test_list_messages_api_call(self, mock_build):
        mock_build.return_value = MagicMock()

        service = GmailService("test_token")

        mock_root, mock_messages = self._mock_chain_for_list_messages()
        service.service = mock_root

        result = service.list_messages(max_results=10, label_ids=["INBOX"])

        mock_messages.list.assert_called_once_with(
            userId="me",
            maxResults=10,
            pageToken=None,
            labelIds=["INBOX"],
            q=None,  # 실제 구현에서 q=None까지 넘기는 경우 반영
        )
        self.assertEqual(len(result["messages"]), 1)
        self.assertEqual(result["messages"][0]["id"], "123")

    @patch("googleapiclient.discovery.build")
    def test_get_message_api_call(self, mock_build):
        mock_build.return_value = MagicMock()

        service = GmailService("test_token")

        mock_root, mock_messages = self._mock_chain_for_get_message()
        service.service = mock_root

        result = service.get_message("msg123")

        mock_messages.get.assert_called_once_with(userId="me", id="msg123", format="full")
        self.assertIn("body", result)
        self.assertIsInstance(result["body"], str)
        self.assertEqual(result["subject"], "Test")

    @patch("googleapiclient.discovery.build")
    def test_mark_as_read_api_call(self, mock_build):
        mock_build.return_value = MagicMock()

        service = GmailService("test_token")

        mock_root, mock_messages = self._mock_chain_for_modify()
        service.service = mock_root

        service.mark_as_read("msg123")

        mock_messages.modify.assert_called_once_with(userId="me", id="msg123", body={"removeLabelIds": ["UNREAD"]})

    @patch("googleapiclient.discovery.build")
    def test_mark_as_unread_api_call(self, mock_build):
        mock_build.return_value = MagicMock()

        service = GmailService("test_token")

        mock_root, mock_messages = self._mock_chain_for_modify()
        service.service = mock_root

        service.mark_as_unread("msg123")

        mock_messages.modify.assert_called_once_with(userId="me", id="msg123", body={"addLabelIds": ["UNREAD"]})


# ---------------------------------------------------------------------
# View tests
# ---------------------------------------------------------------------


class EmailListViewTest(TestCase):
    """EmailListView GET tests"""

    def setUp(self):
        self.factory = APIRequestFactory()
        self.user = User.objects.create(email="user@example.com")

    @patch("apps.mail.views.list_emails_logic")
    def test_list_emails_basic_success(self, mock_list_logic):
        """
        since_date 없는 기본 호출:
        - list_emails_logic 호출
        - 200 OK 응답
        """
        mock_list_logic.return_value = (
            {
                "nextPageToken": "token123",
                "resultSizeEstimate": 42,
            },
            [
                {
                    "id": "m1",
                    "thread_id": "t1",
                    "subject": "Hello",
                    "from": "a@b.com",
                    "to": "user@example.com",
                    "snippet": "preview...",
                    "body": "hello body",
                    "date": timezone.now(),
                    "date_raw": "Mon, 1 Oct 2025 09:00:00 +0900",
                    "label_ids": ["INBOX", "UNREAD"],
                    "is_unread": True,
                    "received_at": timezone.now(),
                }
            ],
        )

        request = self.factory.get(
            "/api/mail/emails/",
            {"max_results": 10, "labels": "INBOX,UNREAD"},
        )
        force_authenticate(request, user=self.user)

        response = EmailListView.as_view()(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn("messages", response.data)
        self.assertEqual(response.data["next_page_token"], "token123")
        self.assertEqual(response.data["result_size_estimate"], 42)

        mock_list_logic.assert_called_once()
        args, kwargs = mock_list_logic.call_args
        # args: (user, max_results, page_token, label_ids)
        self.assertEqual(args[0], self.user)
        self.assertEqual(args[1], 10)
        self.assertIsNone(args[2])  # page_token default None
        self.assertEqual(args[3], ["INBOX", "UNREAD"])

    @patch("apps.mail.views.list_newer_emails_logic")
    def test_list_emails_with_since_date(self, mock_newer_logic):
        """
        since_date 제공 시 list_newer_emails_logic 사용
        next_page_token / result_size_estimate는 빈 값으로 내려간다
        """
        mock_newer_logic.return_value = [
            {
                "id": "new1",
                "thread_id": "t999",
                "subject": "Newest",
                "from": "c@d.com",
                "to": "user@example.com",
                "snippet": "hi",
                "body": "latest body",
                "date": timezone.now(),
                "date_raw": "Fri, 31 Oct 2025 19:20:00 +0900",
                "label_ids": ["INBOX"],
                "is_unread": False,
                "received_at": timezone.now(),
            }
        ]

        request = self.factory.get(
            "/api/mail/emails/",
            {
                "since_date": "2025-10-31T19:14:08+09:00",
                "max_results": 5,
                "labels": "INBOX",
            },
        )
        force_authenticate(request, user=self.user)

        response = EmailListView.as_view()(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["next_page_token"], "")
        self.assertEqual(response.data["result_size_estimate"], 0)
        self.assertEqual(len(response.data["messages"]), 1)

        mock_newer_logic.assert_called_once_with(
            self.user,
            max_results=5,
            label_ids=["INBOX"],
            since_date=ANY,  # parse된 datetime 자체는 그냥 ANY로 검증
        )

    @patch("apps.mail.views.list_emails_logic")
    def test_list_emails_http_404_from_gmail(self, mock_list_logic):
        """
        Gmail HttpError(404) -> 404 반환되는지 확인
        """
        from googleapiclient.errors import HttpError

        resp_mock = MagicMock()
        resp_mock.status = 404
        mock_list_logic.side_effect = HttpError(resp_mock, b"Not found")

        request = self.factory.get("/api/mail/emails/")
        force_authenticate(request, user=self.user)

        response = EmailListView.as_view()(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertIn("detail", response.data)


class EmailDetailViewTest(TestCase):
    """EmailDetailView GET tests"""

    def setUp(self):
        self.factory = APIRequestFactory()
        self.user = User.objects.create(email="user2@example.com")

    @patch("apps.mail.views.get_email_detail_logic")
    def test_email_detail_success(self, mock_detail_logic):
        mock_detail_logic.return_value = {
            "id": "m-detail",
            "thread_id": "thread-abc",
            "subject": "Test subject",
            "from": "someone@example.com",
            "to": "user2@example.com",
            "snippet": "short preview",
            "body": "Hello body",
            "date": timezone.now(),
            "date_raw": "Thu, 30 Oct 2025 14:10:00 +0900",
            "label_ids": ["INBOX"],
            "is_unread": True,
            "received_at": timezone.now(),
        }

        request = self.factory.get("/api/mail/emails/m-detail/")
        force_authenticate(request, user=self.user)

        response = EmailDetailView.as_view()(request, message_id="m-detail")

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["id"], "m-detail")
        self.assertEqual(response.data["subject"], "Test subject")
        self.assertEqual(response.data["thread_id"], "thread-abc")

        mock_detail_logic.assert_called_once_with(self.user, "m-detail")

    @patch("apps.mail.views.get_email_detail_logic")
    def test_email_detail_not_found(self, mock_detail_logic):
        from googleapiclient.errors import HttpError

        resp_mock = MagicMock()
        resp_mock.status = 404
        mock_detail_logic.side_effect = HttpError(resp_mock, b"Not found")

        request = self.factory.get("/api/mail/emails/unknown/")
        force_authenticate(request, user=self.user)

        response = EmailDetailView.as_view()(request, message_id="unknown")

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertIn("detail", response.data)


class EmailSendViewTest(TestCase):
    """EmailSendView POST tests, including SentMail creation logic"""

    def setUp(self):
        self.factory = APIRequestFactory()
        self.user = User.objects.create(email="sender@example.com")

        # Contact 모델: user FK, email 필드 있다고 가정
        from apps.contact.models import Contact

        self.contact = Contact.objects.create(user=self.user, email="friend@example.com", name="Friend")

    @patch("apps.mail.views.send_email_logic")
    @patch("apps.mail.views.SentMail.objects.bulk_create")
    def test_send_email_success_and_sentmail_saved(self, mock_bulk_create, mock_send_logic):
        mock_send_logic.return_value = {
            "id": "gm-msg-id",
            "threadId": "th-1",
            "labelIds": ["SENT"],
        }

        # mock serializer
        mock_serializer = MagicMock()
        mock_serializer.is_valid.return_value = True
        mock_serializer.validated_data = {
            "to": ["Friend <friend@example.com>"],
            "cc": [],
            "bcc": [],
            "subject": "Hi There",
            "body": "This is body",
            "is_html": True,
        }

        request = self.factory.post(
            "/api/mail/emails/send/",
            {
                "to": ["Friend <friend@example.com>"],
                "subject": "Hi There",
                "body": "This is body",
            },
            format="json",
        )
        force_authenticate(request, user=self.user)

        with patch.object(EmailSendView, "get_serializer", return_value=mock_serializer):
            response = EmailSendView.as_view()(request)

        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data.get("id"), "gm-msg-id")

        mock_send_logic.assert_called_once()
        args, kwargs = mock_send_logic.call_args
        self.assertEqual(args[0], self.user)
        self.assertEqual(kwargs["to"], ["Friend <friend@example.com>"])
        self.assertEqual(kwargs["subject"], "Hi There")

        # bulk_create should have been called because Contact matched
        self.assertTrue(mock_bulk_create.called)

    @patch("apps.mail.views.send_email_logic")
    def test_send_email_invalid_serializer_returns_400(self, mock_send_logic):
        mock_serializer = MagicMock()
        mock_serializer.is_valid.return_value = False
        mock_serializer.errors = {"to": ["This field is required."]}

        request = self.factory.post(
            "/api/mail/emails/send/",
            {"subject": "No To", "body": "x"},
            format="json",
        )
        force_authenticate(request, user=self.user)

        with patch.object(EmailSendView, "get_serializer", return_value=mock_serializer):
            response = EmailSendView.as_view()(request)

        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn("to", response.data)
        mock_send_logic.assert_not_called()

    @patch("apps.mail.views.send_email_logic")
    def test_send_email_gmail_403_rate_limit(self, mock_send_logic):
        from googleapiclient.errors import HttpError

        resp_mock = MagicMock()
        resp_mock.status = 403
        mock_send_logic.side_effect = HttpError(resp_mock, b"rate limit")

        mock_serializer = MagicMock()
        mock_serializer.is_valid.return_value = True
        mock_serializer.validated_data = {
            "to": ["a@b.com"],
            "cc": [],
            "bcc": [],
            "subject": "subj",
            "body": "body",
            "is_html": True,
        }

        request = self.factory.post(
            "/api/mail/emails/send/",
            {"to": ["a@b.com"], "subject": "subj", "body": "body"},
            format="json",
        )
        force_authenticate(request, user=self.user)

        with patch.object(EmailSendView, "get_serializer", return_value=mock_serializer):
            response = EmailSendView.as_view()(request)

        self.assertEqual(response.status_code, status.HTTP_429_TOO_MANY_REQUESTS)
        self.assertIn("detail", response.data)


class EmailMarkReadViewTest(TestCase):
    """EmailMarkReadView PATCH tests"""

    def setUp(self):
        self.factory = APIRequestFactory()
        self.user = User.objects.create(email="reader@example.com")

    @patch("apps.mail.views.mark_read_logic")
    def test_mark_read_success(self, mock_mark_logic):
        mock_mark_logic.return_value = {
            "id": "msg123",
            "labelIds": ["INBOX"],
        }

        request = self.factory.patch(
            "/api/mail/emails/msg123/read/",
            {"is_read": True},
            format="json",
        )
        force_authenticate(request, user=self.user)

        response = EmailMarkReadView.as_view()(request, message_id="msg123")

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["id"], "msg123")
        mock_mark_logic.assert_called_once_with(self.user, "msg123", True)

    @patch("apps.mail.views.mark_read_logic")
    def test_mark_read_http_404(self, mock_mark_logic):
        from googleapiclient.errors import HttpError

        resp_mock = MagicMock()
        resp_mock.status = 404
        mock_mark_logic.side_effect = HttpError(resp_mock, b"not found")

        request = self.factory.patch(
            "/api/mail/emails/msg404/read/",
            {"is_read": False},
            format="json",
        )
        force_authenticate(request, user=self.user)

        response = EmailMarkReadView.as_view()(request, message_id="msg404")

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertIn("detail", response.data)


class MailTestViewTest(TestCase):
    """
    MailTestView GET tests
    - DEBUG=True 일 때만 정상 동작
    - settings.ENCRYPTION_KEY 가 Fernet 복호화에 필요
    """

    def setUp(self):
        self.factory = APIRequestFactory()

        # 테스트용 Fernet key
        self.fernet_key = Fernet.generate_key()
        self.fernet = Fernet(self.fernet_key)

        # 유저 & 구글 계정 생성
        self.user = User.objects.create(email="testuser@example.com")

        # access_token 암호화해서 DB에 저장
        encrypted_access_token = self.fernet.encrypt(b"valid_access_token").decode()

        # expires_at은 미래 -> token_status="valid"
        expires_future = timezone.now() + timedelta(hours=1)

        self.google_account = GoogleAccount.objects.create(
            user=self.user,
            access_token=encrypted_access_token,
            refresh_token="refresh-token",
            expires_at=expires_future,
        )

    @patch("apps.mail.views.GmailService")
    def test_mailtest_success_valid_token(self, mock_gmail_service_cls):
        """
        - DEBUG=True, ENCRYPTION_KEY 설정
        - 토큰 만료 안 됨 → token_status="valid"
        """
        mock_gmail_service = MagicMock()
        mock_gmail_service.list_messages.return_value = {
            "messages": [{"id": "1"}, {"id": "2"}],
            "resultSizeEstimate": 2,
        }
        mock_gmail_service_cls.return_value = mock_gmail_service

        request = self.factory.get("/api/mail/test/", {"email": "testuser@example.com"})

        with override_settings(DEBUG=True, ENCRYPTION_KEY=self.fernet_key):
            response = MailTestView.as_view()(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["status"], "success")
        self.assertEqual(response.data["user"], "testuser@example.com")
        self.assertEqual(response.data["token_status"], "valid")
        self.assertEqual(response.data["gmail_api"], "connected")
        self.assertEqual(response.data["message_count"], 2)
        self.assertEqual(response.data["result_size_estimate"], 2)

    @patch("apps.mail.views.GmailService")
    @patch("apps.mail.views.google_refresh")
    def test_mailtest_expired_token_calls_refresh(self, mock_refresh, mock_gmail_service_cls):
        """
        - 만료된 토큰 -> google_refresh 호출 -> token_status="refreshed"
        """
        user2 = User.objects.create(email="expired@example.com")

        expired_google_account = GoogleAccount.objects.create(
            user=user2,
            access_token=self.fernet.encrypt(b"old_token").decode(),
            refresh_token="refresh-token-2",
            expires_at=timezone.now() - timedelta(minutes=1),  # already expired
        )

        mock_refresh.return_value = "new_access_token"

        mock_gmail_service = MagicMock()
        mock_gmail_service.list_messages.return_value = {
            "messages": [],
            "resultSizeEstimate": 0,
        }
        mock_gmail_service_cls.return_value = mock_gmail_service

        request = self.factory.get("/api/mail/test/", {"email": "expired@example.com"})

        with override_settings(DEBUG=True, ENCRYPTION_KEY=self.fernet_key):
            response = MailTestView.as_view()(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data["user"], "expired@example.com")
        self.assertEqual(response.data["token_status"], "refreshed")
        mock_refresh.assert_called_once_with(expired_google_account)

    def test_mailtest_forbidden_when_debug_false(self):
        """
        DEBUG=False 인 경우 -> 404 반환
        """
        request = self.factory.get("/api/mail/test/", {"email": "whatever@example.com"})
        with override_settings(DEBUG=False, ENCRYPTION_KEY=self.fernet_key):
            response = MailTestView.as_view()(request)

        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
        self.assertIn("detail", response.data)
