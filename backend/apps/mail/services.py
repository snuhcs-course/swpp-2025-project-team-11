"""Gmail API integration service"""

import base64
import html
import re
from email.header import Header
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.utils import parsedate_to_datetime

from google.oauth2.credentials import Credentials
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError


class GmailService:
    """Service for fetching emails using Gmail API"""

    def __init__(self, access_token: str):
        """
        Initialize Gmail API client

        Args:
            access_token: Google OAuth2 access token
        """
        credentials = Credentials(token=access_token)
        self.service = build("gmail", "v1", credentials=credentials)

    def _html_to_text(self, html_str: str) -> str:
        s = re.sub(r"(?i)<\s*br\s*/?>", "\n", html_str)
        s = re.sub(r"(?i)</\s*p\s*>", "\n\n", s)
        s = re.sub(r"(?s)<[^>]+>", "", s)
        return html.unescape(s).strip()

    def _text_to_html(self, text_str: str) -> str:
        esc = html.escape(text_str)
        return esc.replace("\n", "<br>")

    def list_messages(self, max_results: int = 20, page_token: str = None, label_ids: list = None):
        """
        List messages from Gmail

        Args:
            max_results: Maximum number of results
            page_token: Pagination token
            label_ids: Label filters (default: ['INBOX'])

        Returns:
            dict: {
                'messages': [...],
                'nextPageToken': '...',
                'resultSizeEstimate': 100
            }

        Raises:
            HttpError: Gmail API error
        """
        if label_ids is None:
            label_ids = ["INBOX"]

        try:
            results = (
                self.service.users()
                .messages()
                .list(userId="me", maxResults=max_results, pageToken=page_token, labelIds=label_ids)
                .execute()
            )
            return results
        except HttpError:
            raise

    def get_message(self, message_id: str):
        """
        Get message details

        Args:
            message_id: Gmail message ID

        Returns:
            dict: Parsed message details

        Raises:
            HttpError: Gmail API error
        """
        try:
            message = (
                self.service.users()
                .messages()
                .get(userId="me", id=message_id, format="full")
                .execute()
            )
            return self._parse_message(message)
        except HttpError:
            raise

    def _parse_message(self, message: dict) -> dict:
        """
        Parse Gmail API response

        Args:
            message: Gmail API raw message

        Returns:
            dict: Parsed message info
        """
        headers = message["payload"]["headers"]
        headers_dict = {h["name"].lower(): h["value"] for h in headers}

        # Extract body
        body = self._get_body(message["payload"])

        # Parse date
        date_str = headers_dict.get("date", "")
        try:
            received_at = parsedate_to_datetime(date_str)
        except Exception as e:
            # Log the error for debugging
            import logging

            logging.warning(f"Failed to parse date '{date_str}' for message {message['id']}: {e}")
            received_at = None

        return {
            "id": message["id"],
            "thread_id": message["threadId"],
            "label_ids": message.get("labelIds", []),
            "snippet": message.get("snippet", ""),
            "subject": headers_dict.get("subject", "(No Subject)"),
            "from": headers_dict.get("from", ""),
            "to": headers_dict.get("to", ""),
            "date": received_at.isoformat() if received_at else None,
            "date_raw": date_str,
            "body": body,
            "is_unread": "UNREAD" in message.get("labelIds", []),
        }

    def _get_body(self, payload: dict) -> str:
        """
        Extract message body (recursive parts traversal)
        Priority: text/plain > text/html

        Args:
            payload: message payload

        Returns:
            str: Decoded body text
        """
        if "body" in payload and "data" in payload["body"]:
            return self._decode_body(payload["body"]["data"])

        if "parts" in payload:
            # Try to find text/plain first
            for part in payload["parts"]:
                if part["mimeType"] == "text/plain":
                    if "data" in part["body"]:
                        return self._decode_body(part["body"]["data"])
                elif part["mimeType"] == "multipart/alternative":
                    # Recursive search
                    body = self._get_body(part)
                    if body:
                        return body

            # If no text/plain, try text/html
            for part in payload["parts"]:
                if part["mimeType"] == "text/html":
                    if "data" in part["body"]:
                        return self._decode_body(part["body"]["data"])
                elif part["mimeType"] == "multipart/alternative":
                    # Recursive search for HTML
                    body = self._get_body(part)
                    if body:
                        return body

        return ""

    def _decode_body(self, data: str) -> str:
        """
        Base64 URL-safe decoding

        Args:
            data: base64 encoded string

        Returns:
            str: decoded string
        """
        try:
            decoded = base64.urlsafe_b64decode(data.encode("ASCII"))
            return decoded.decode("utf-8", errors="ignore")
        except Exception:
            return ""

    def send_message(
        self,
        to: list[str],
        subject: str,
        body: str,
        is_html: bool = True,
        cc: list[str] | None = None,
        bcc: list[str] | None = None,
    ):
        """
        Send an email via Gmail API

        Args:
            to: Recipient email address
            subject: Email subject
            body: Email body (plain text)

        Returns:
            dict: Sent message info including message ID

        Raises:
            HttpError: Gmail API error
        """

        try:
            cc = cc or []
            bcc = bcc or []

            if is_html:
                html_body = body
                text_body = self._html_to_text(body)
            else:
                text_body = body
                html_body = self._text_to_html(body)

            message = MIMEMultipart("alternative")
            message["Subject"] = str(Header(subject, "utf-8"))
            message["To"] = ", ".join(to)
            if cc:
                message["Cc"] = ", ".join(cc)
            if bcc:
                message["Bcc"] = ", ".join(bcc)

            # 순서 중요: plain 먼저, html 나중
            message.attach(MIMEText(text_body, "plain", "utf-8"))
            message.attach(MIMEText(html_body, "html", "utf-8"))

            raw = base64.urlsafe_b64encode(message.as_bytes()).decode("utf-8")
            result = self.service.users().messages().send(userId="me", body={"raw": raw}).execute()

            return {
                "id": result["id"],
                "threadId": result.get("threadId"),
                "labelIds": result.get("labelIds", []),
            }
        except HttpError:
            raise

    def mark_as_read(self, message_id: str):
        """
        Mark a message as read by removing UNREAD label

        Args:
            message_id: Gmail message ID

        Returns:
            dict: Modified message info

        Raises:
            HttpError: Gmail API error
        """
        try:
            result = (
                self.service.users()
                .messages()
                .modify(userId="me", id=message_id, body={"removeLabelIds": ["UNREAD"]})
                .execute()
            )
            return result
        except HttpError:
            raise

    def mark_as_unread(self, message_id: str):
        """
        Mark a message as unread by adding UNREAD label

        Args:
            message_id: Gmail message ID

        Returns:
            dict: Modified message info

        Raises:
            HttpError: Gmail API error
        """
        try:
            result = (
                self.service.users()
                .messages()
                .modify(userId="me", id=message_id, body={"addLabelIds": ["UNREAD"]})
                .execute()
            )
            return result
        except HttpError:
            raise
