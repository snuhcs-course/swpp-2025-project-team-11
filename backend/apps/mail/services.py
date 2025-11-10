"""Gmail API integration service"""

import base64
import datetime
import html
import logging
import mimetypes
from email import encoders
from email.header import Header
from email.mime.base import MIMEBase
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.utils import parsedate_to_datetime

from google.oauth2.credentials import Credentials
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError
from googleapiclient.http import BatchHttpRequest

from apps.mail.utils import compare_iso_datetimes, html_to_text, text_to_html
from apps.user.utils import google_token_required

logger = logging.getLogger(__name__)


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

    def get_messages_batch(self, message_ids: list[str]) -> list[dict]:
        """
        Fetch multiple messages in a single batch HTTP request.

        Args:
            message_ids (list[str]): Gmail message IDs.

        Returns:
            list[dict]: List of parsed message dicts (same shape as get_message()).
        """

        results: list[dict] = []

        # callback will be called once per each sub-request added to the batch
        def _callback(request_id, response, exception):
            """
            request_id: internal ID we assign per subrequest
            response: raw Gmail message resource (if success)
            exception: HttpError (if failed)
            """
            if exception is not None:
                logger.warning(f"Failed to fetch message in batch [{request_id}]: {exception}")
                return

            try:
                parsed = self._parse_message(response)
                results.append(parsed)
            except Exception as e:
                logger.warning(f"Failed to parse message in batch [{request_id}]: {e}")

        batch = BatchHttpRequest(
            callback=_callback,
            batch_uri="https://gmail.googleapis.com/batch/gmail/v1",
        )

        for mid in message_ids:
            batch.add(
                self.service.users()
                .messages()
                .get(
                    userId="me",
                    id=mid,
                    format="full",  # full so we can parse headers/body like before
                ),
                request_id=mid,
            )

        # execute the whole batch in one HTTP roundtrip
        batch.execute()

        return results

    def list_messages(
        self,
        max_results: int = 20,
        page_token: str = None,
        label_ids: list = None,
        q: str | None = None,
    ):
        """
        List messages from Gmail

        Args:
            max_results: Maximum number of results
            page_token: Pagination token
            label_ids: Label filters (default: ['INBOX'])
            q: Gmail search query (ex: 'after:1730379248 label:INBOX')

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
                .list(
                    userId="me",
                    maxResults=max_results,
                    pageToken=page_token,
                    labelIds=label_ids,
                    q=q,
                )
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
            message = self.service.users().messages().get(userId="me", id=message_id, format="full").execute()
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

        attachments = self._get_attachments_meta(message["payload"])

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
            "attachments": attachments,
        }

    def _get_attachments_meta(self, payload: dict) -> list[dict]:
        results: list[dict] = []

        def _walk(part: dict):
            if part.get("parts"):
                for p in part["parts"]:
                    _walk(p)

            filename = part.get("filename")
            body = part.get("body", {})
            attachment_id = body.get("attachmentId")
            if filename and attachment_id:
                results.append(
                    {
                        "attachment_id": attachment_id,
                        "filename": filename,
                        "mime_type": part.get("mimeType", "application/octet-stream"),
                        "size": body.get("size"),
                    }
                )

        _walk(payload)
        return results

    def get_attachment(
        self,
        message_id: str,
        attachment_id: str,
        filename: str,
        mime_type: str,
    ) -> dict:
        try:
            att = self.service.users().messages().attachments().get(userId="me", messageId=message_id, id=attachment_id).execute()
        except Exception as e:
            logging.warning(f"Failed to fetch attachment {attachment_id} for message {message_id}: {e}")
            raise

        data_b64 = att.get("data")
        if not data_b64:
            raise ValueError("No attachment data returned from Gmail")

        file_bytes = base64.urlsafe_b64decode(data_b64.encode("utf-8"))

        final_mime = mime_type or "application/octet-stream"

        if filename and filename.strip():
            final_name = filename.strip()
        else:
            ts = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
            guessed_ext = mimetypes.guess_extension(final_mime) or ""
            final_name = f"{ts}{guessed_ext}"

        return {
            "data": file_bytes,
            "filename": final_name,
            "mime_type": final_mime,
            "size": len(file_bytes),
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
        if "parts" in payload:
            # Try to find text/html first
            for part in payload["parts"]:
                if part["mimeType"] == "text/html":
                    if "data" in part["body"]:
                        return self._decode_body(part["body"]["data"])
                elif part["mimeType"] == "multipart/alternative":
                    # Recursive search for HTML
                    body = self._get_body(part)
                    if body:
                        return body

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

    def send_message(
        self,
        to: list[str],
        subject: str,
        body: str,
        is_html: bool = True,
        cc: list[str] | None = None,
        bcc: list[str] | None = None,
        attachments: list[dict] | None = None,
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
            attachments = attachments or []

            if is_html:
                html_body = html.unescape(body)
                text_body = html_to_text(body)
            else:
                text_body = html.unescape(body)
                html_body = text_to_html(body)

            outer = MIMEMultipart("mixed")
            outer["Subject"] = str(Header(subject, "utf-8"))
            outer["To"] = ", ".join(to)
            if cc:
                outer["Cc"] = ", ".join(cc)
            if bcc:
                outer["Bcc"] = ", ".join(bcc)

            alt = MIMEMultipart("alternative")
            alt.attach(MIMEText(text_body, "plain", "utf-8"))
            alt.attach(MIMEText(html_body, "html", "utf-8"))

            outer.attach(alt)

            for att in attachments:
                filename = att.get("filename") or "attachment"
                mime_type = att.get("mime_type")
                content = att.get("content")

                if isinstance(content, str):
                    file_bytes = base64.b64decode(content)
                else:
                    file_bytes = content

                if not mime_type:
                    mime_type, _ = mimetypes.guess_type(filename)
                if not mime_type:
                    mime_type = "application/octet-stream"

                main_type, sub_type = mime_type.split("/", 1)

                part = MIMEBase(main_type, sub_type)
                part.set_payload(file_bytes)
                encoders.encode_base64(part)
                part.add_header("Content-Disposition", "attachment", filename=("utf-8", "", filename))
                outer.attach(part)

            raw = base64.urlsafe_b64encode(outer.as_bytes()).decode("utf-8")
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
            result = self.service.users().messages().modify(userId="me", id=message_id, body={"removeLabelIds": ["UNREAD"]}).execute()
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
            result = self.service.users().messages().modify(userId="me", id=message_id, body={"addLabelIds": ["UNREAD"]}).execute()
            return result
        except HttpError:
            raise


@google_token_required
def list_emails_logic(access_token, max_results, page_token, label_ids):
    """Helper function to list emails using Google access token"""
    gmail_service = GmailService(access_token)
    result = gmail_service.list_messages(max_results=max_results, page_token=page_token, label_ids=label_ids)

    # Fetch detailed info for each message
    msg_refs = result.get("messages", [])
    message_ids = [m["id"] for m in msg_refs if "id" in m]

    # 2. batch get full details
    if message_ids:
        messages = gmail_service.get_messages_batch(message_ids)
    else:
        messages = []

    return result, messages


@google_token_required
def list_newer_emails_logic(access_token, max_results, label_ids, since_date):
    """
    Incremental refresh mode:
    - since_date: tz-aware datetime (the newest email timestamp the client ALREADY has)
    - Fetch ALL emails after that timestamp (via `after:<epochSeconds>` query),
      across ALL pages.
    """
    gmail_service = GmailService(access_token)

    since_utc = since_date.astimezone(datetime.UTC)
    epoch_seconds = int(since_utc.timestamp())

    q = f"after:{epoch_seconds}"

    page_token = None
    collected_ids: list[str] = []
    all_full_msgs = []

    while True:
        collected_ids = []
        try:
            resp = gmail_service.list_messages(
                max_results=100,
                page_token=page_token,
                label_ids=label_ids,
                q=q,
            )
        except HttpError as e:
            logger.warning(f"Gmail list_messages failed: {e}")
            break

        raw_refs = resp.get("messages", [])
        if not raw_refs:
            break

        for ref in raw_refs:
            mid = ref.get("id")
            if mid:
                collected_ids.append(mid)

        all_full_msgs.extend(gmail_service.get_messages_batch(collected_ids))

        page_token = resp.get("nextPageToken")
        if not page_token:
            break

    newer_only = [m for m in all_full_msgs if m.get("date") and compare_iso_datetimes(m["date"], since_date) == 1]

    return newer_only


@google_token_required
def get_email_detail_logic(access_token, message_id):
    """Helper function to get email detail using Google access token"""
    gmail_service = GmailService(access_token)
    return gmail_service.get_message(message_id)


@google_token_required
def send_email_logic(access_token, to, subject, body, is_html=True, cc=None, bcc=None, attachments=None):
    """Helper function to send email using Google access token"""
    gmail_service = GmailService(access_token)
    return gmail_service.send_message(
        to=to,
        cc=cc or [],
        bcc=bcc or [],
        subject=subject,
        body=body,
        is_html=is_html,
        attachments=attachments or [],
    )


@google_token_required
def mark_read_logic(access_token, message_id, is_read):
    """Helper function to mark email as read/unread using Google access token"""
    gmail_service = GmailService(access_token)
    if is_read:
        return gmail_service.mark_as_read(message_id)
    else:
        return gmail_service.mark_as_unread(message_id)


@google_token_required
def get_attachment_logic(access_token, message_id: str, attachment_id: str, filename: str, mime_type: str):
    gmail_service = GmailService(access_token)
    return gmail_service.get_attachment(message_id, attachment_id, filename, mime_type)
