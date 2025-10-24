import logging

from apps.mail.services import GmailService
from apps.user.utils import google_token_required

logger = logging.getLogger(__name__)


@google_token_required
def list_emails_logic(access_token, max_results, page_token, label_ids):
    """Helper function to list emails using Google access token"""
    gmail_service = GmailService(access_token)
    result = gmail_service.list_messages(
        max_results=max_results, page_token=page_token, label_ids=label_ids
    )

    # Fetch detailed info for each message
    messages = []
    for msg in result.get("messages", []):
        try:
            message_detail = gmail_service.get_message(msg["id"])
            messages.append(message_detail)
        except Exception as e:
            # Skip individual message fetch failures
            logger.warning(f"Failed to fetch message {msg['id']}: {str(e)}")
            continue

    return result, messages


@google_token_required
def get_email_detail_logic(access_token, message_id):
    """Helper function to get email detail using Google access token"""
    gmail_service = GmailService(access_token)
    return gmail_service.get_message(message_id)


@google_token_required
def send_email_logic(access_token, to, subject, body, is_html=True, cc=None, bcc=None):
    """Helper function to send email using Google access token"""
    gmail_service = GmailService(access_token)
    return gmail_service.send_message(
        to=to,
        cc=cc or [],
        bcc=bcc or [],
        subject=subject,
        body=body,
        is_html=is_html,
    )


@google_token_required
def mark_read_logic(access_token, message_id, is_read):
    """Helper function to mark email as read/unread using Google access token"""
    gmail_service = GmailService(access_token)
    if is_read:
        return gmail_service.mark_as_read(message_id)
    else:
        return gmail_service.mark_as_unread(message_id)
