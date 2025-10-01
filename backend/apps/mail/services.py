"""Gmail API integration service"""

import base64
from datetime import datetime
from email.utils import parsedate_to_datetime

from google.oauth2.credentials import Credentials
from googleapiclient.discovery import build


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
        """
        if label_ids is None:
            label_ids = ["INBOX"]

        results = (
            self.service.users()
            .messages()
            .list(userId="me", maxResults=max_results, pageToken=page_token, labelIds=label_ids)
            .execute()
        )

        return results

    def get_message(self, message_id: str):
        """
        Get message details

        Args:
            message_id: Gmail message ID

        Returns:
            dict: Parsed message details
        """
        message = (
            self.service.users().messages().get(userId="me", id=message_id, format="full").execute()
        )

        return self._parse_message(message)

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
        except Exception:
            received_at = datetime.now()

        return {
            "id": message["id"],
            "thread_id": message["threadId"],
            "label_ids": message.get("labelIds", []),
            "snippet": message.get("snippet", ""),
            "subject": headers_dict.get("subject", "(No Subject)"),
            "from": headers_dict.get("from", ""),
            "to": headers_dict.get("to", ""),
            "date": received_at.isoformat(),
            "body": body,
            "is_unread": "UNREAD" in message.get("labelIds", []),
        }

    def _get_body(self, payload: dict) -> str:
        """
        Extract message body (recursive parts traversal)

        Args:
            payload: message payload

        Returns:
            str: Decoded body text
        """
        if "body" in payload and "data" in payload["body"]:
            return self._decode_body(payload["body"]["data"])

        if "parts" in payload:
            for part in payload["parts"]:
                if part["mimeType"] == "text/plain":
                    if "data" in part["body"]:
                        return self._decode_body(part["body"]["data"])
                elif part["mimeType"] == "multipart/alternative":
                    # Recursive search
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

    def send_message(self, to: str, subject: str, body: str):
        """
        Send an email via Gmail API

        Args:
            to: Recipient email address
            subject: Email subject
            body: Email body (plain text)

        Returns:
            dict: Sent message info including message ID
        """
        from email.mime.text import MIMEText

        # Create message
        message = MIMEText(body, "plain", "utf-8")
        message["To"] = to
        message["Subject"] = subject

        # Encode message
        raw = base64.urlsafe_b64encode(message.as_bytes()).decode("utf-8")

        # Send via Gmail API
        result = self.service.users().messages().send(userId="me", body={"raw": raw}).execute()

        return {
            "id": result["id"],
            "threadId": result.get("threadId"),
            "labelIds": result.get("labelIds", []),
        }
