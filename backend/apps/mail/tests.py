from unittest.mock import MagicMock, patch

from django.test import TestCase

from .services import GmailService


class GmailServiceDecodeTest(TestCase):
    """Test Base64 decoding functionality"""

    def setUp(self):
        """Set up test fixtures"""
        # Create a mock Gmail service (no actual API calls)
        with patch("googleapiclient.discovery.build"):
            self.service = GmailService("fake_access_token")

    def test_decode_body_simple_text(self):
        """Test decoding simple ASCII text"""
        # "Hello World" in base64
        encoded = "SGVsbG8gV29ybGQ="
        decoded = self.service._decode_body(encoded)
        self.assertEqual(decoded, "Hello World")

    def test_decode_body_korean_text(self):
        """Test decoding Korean text"""
        # "안녕하세요" in base64
        encoded = "7JWI64WV7ZWY7IS47JqU"
        decoded = self.service._decode_body(encoded)
        self.assertEqual(decoded, "안녕하세요")

    def test_decode_body_with_special_chars(self):
        """Test decoding text with special characters"""
        # "Hello, let's meet!" in base64
        encoded = "SGVsbG8sIGxldCdzIG1lZXQh"
        decoded = self.service._decode_body(encoded)
        self.assertEqual(decoded, "Hello, let's meet!")

    def test_decode_body_empty_string(self):
        """Test decoding empty string"""
        encoded = ""
        decoded = self.service._decode_body(encoded)
        self.assertEqual(decoded, "")

    def test_decode_body_invalid_base64(self):
        """Test handling invalid base64 data"""
        invalid_encoded = "not-valid-base64!!!"
        decoded = self.service._decode_body(invalid_encoded)
        # Should return empty string on error
        self.assertEqual(decoded, "")


class GmailServiceParseMessageTest(TestCase):
    """Test message parsing functionality"""

    def setUp(self):
        """Set up test fixtures"""
        with patch("googleapiclient.discovery.build"):
            self.service = GmailService("fake_access_token")

    def test_parse_message_basic(self):
        """Test parsing a simple message"""
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
        self.assertEqual(result["body"], "Hello World")
        self.assertTrue(result["is_unread"])
        self.assertIn("INBOX", result["label_ids"])

    def test_parse_message_no_subject(self):
        """Test parsing message without subject"""
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

        # Should use default "(No Subject)"
        self.assertEqual(result["subject"], "(No Subject)")

    def test_parse_message_read_status(self):
        """Test parsing read message (no UNREAD label)"""
        raw_message = {
            "id": "msg123",
            "threadId": "thread456",
            "labelIds": ["INBOX"],  # No UNREAD label
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
        """Set up test fixtures"""
        with patch("googleapiclient.discovery.build"):
            self.service = GmailService("fake_access_token")

    def test_get_body_simple(self):
        """Test extracting body from simple payload"""
        payload = {"body": {"data": "SGVsbG8="}}  # "Hello"

        body = self.service._get_body(payload)

        self.assertEqual(body, "Hello")

    def test_get_body_with_parts_text_plain(self):
        """Test extracting text/plain from parts"""
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

        # Should prefer text/plain over text/html
        self.assertEqual(body, "Plain text")

    def test_get_body_nested_parts(self):
        """Test extracting body from nested multipart structure"""
        payload = {
            "parts": [
                {
                    "mimeType": "multipart/alternative",
                    "parts": [
                        {
                            "mimeType": "text/plain",
                            "body": {"data": "TmVzdGVkIHRleHQ="},
                        },  # "Nested text"
                    ],
                }
            ]
        }

        body = self.service._get_body(payload)

        self.assertEqual(body, "Nested text")

    def test_get_body_no_text_plain(self):
        """Test when no text/plain part is found"""
        payload = {
            "parts": [
                {"mimeType": "text/html", "body": {"data": "PGh0bWw+PC9odG1sPg=="}},
                {"mimeType": "image/png", "filename": "image.png"},
            ]
        }

        body = self.service._get_body(payload)

        # Should return empty string
        self.assertEqual(body, "")

    def test_get_body_empty_payload(self):
        """Test with empty payload"""
        payload = {}

        body = self.service._get_body(payload)

        self.assertEqual(body, "")


class GmailServiceIntegrationTest(TestCase):
    """Integration tests for GmailService"""

    @patch("googleapiclient.discovery.build")
    def test_list_messages_api_call(self, mock_build):
        """Test list_messages makes correct API call"""
        # Setup mock
        mock_service = MagicMock()
        mock_build.return_value = mock_service

        mock_messages = MagicMock()
        mock_service.users.return_value.messages.return_value = mock_messages
        mock_messages.list.return_value.execute.return_value = {
            "messages": [{"id": "123", "threadId": "456"}],
            "resultSizeEstimate": 1,
        }

        # Create service and call
        service = GmailService("test_token")
        result = service.list_messages(max_results=10, label_ids=["INBOX"])

        # Verify API was called correctly
        mock_messages.list.assert_called_once_with(userId="me", maxResults=10, pageToken=None, labelIds=["INBOX"])
        self.assertEqual(len(result["messages"]), 1)
        self.assertEqual(result["messages"][0]["id"], "123")

    @patch("googleapiclient.discovery.build")
    def test_get_message_api_call(self, mock_build):
        """Test get_message makes correct API call"""
        # Setup mock
        mock_service = MagicMock()
        mock_build.return_value = mock_service

        mock_messages = MagicMock()
        mock_service.users.return_value.messages.return_value = mock_messages
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

        # Create service and call
        service = GmailService("test_token")
        result = service.get_message("msg123")

        # Verify API was called correctly
        mock_messages.get.assert_called_once_with(userId="me", id="msg123", format="full")
        self.assertEqual(result["subject"], "Test")
        self.assertEqual(result["body"], "Test")

    @patch("googleapiclient.discovery.build")
    def test_mark_as_read_api_call(self, mock_build):
        """Test mark_as_read makes correct API call"""
        # Setup mock
        mock_service = MagicMock()
        mock_build.return_value = mock_service

        mock_messages = MagicMock()
        mock_service.users.return_value.messages.return_value = mock_messages

        # Create service and call
        service = GmailService("test_token")
        service.mark_as_read("msg123")

        # Verify API was called correctly
        mock_messages.modify.assert_called_once_with(userId="me", id="msg123", body={"removeLabelIds": ["UNREAD"]})

    @patch("googleapiclient.discovery.build")
    def test_mark_as_unread_api_call(self, mock_build):
        """Test mark_as_unread makes correct API call"""
        # Setup mock
        mock_service = MagicMock()
        mock_build.return_value = mock_service

        mock_messages = MagicMock()
        mock_service.users.return_value.messages.return_value = mock_messages

        # Create service and call
        service = GmailService("test_token")
        service.mark_as_unread("msg123")

        # Verify API was called correctly
        mock_messages.modify.assert_called_once_with(userId="me", id="msg123", body={"addLabelIds": ["UNREAD"]})
