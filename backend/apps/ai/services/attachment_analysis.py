from apps.ai.services.chains import attachment_analysis_chain
from apps.ai.services.utils import extract_text_from_bytes, hash_bytes
from apps.mail.models import AttachmentAnalysis
from apps.mail.services import get_attachment_logic


def analyze_gmail_attachment(
    user,
    message_id: str,
    attachment_id: str,
    filename: str,
    mime_type: str,
):
    cached = AttachmentAnalysis.get_recent_by_attachment(user, attachment_id)
    if cached:
        return {
            "summary": cached.summary,
            "insights": cached.insights,
            "mail_guide": cached.mail_guide,
        }

    att = get_attachment_logic(
        user,
        message_id=message_id,
        attachment_id=attachment_id,
        filename=filename,
        mime_type=mime_type,
    )
    data = att["data"]
    real_filename = att["filename"]
    real_mime = att["mime_type"]

    text = extract_text_from_bytes(data, real_mime, real_filename)

    result = attachment_analysis_chain.invoke(
        {
            "text": text,
            "filename": real_filename,
        }
    )

    AttachmentAnalysis.objects.create(
        user=user,
        message_id=message_id,
        attachment_id=attachment_id,
        filename=real_filename,
        mime_type=real_mime,
        summary=result.summary,
        insights=result.insights,
        mail_guide=result.mail_guide,
    )

    return result.model_dump()


def analyze_uploaded_file(user, file_obj):
    data = file_obj.read()
    filename = file_obj.name
    mime_type = getattr(file_obj, "content_type", "") or "application/octet-stream"
    content_key = hash_bytes(data)

    cached = AttachmentAnalysis.get_recent_by_content_key(user, content_key)
    if cached:
        return {
            "summary": cached.summary,
            "insights": cached.insights,
            "mail_guide": cached.mail_guide,
        }

    text = extract_text_from_bytes(data, mime_type, filename)

    result = attachment_analysis_chain.invoke(
        {
            "text": text,
            "filename": filename,
        }
    )

    AttachmentAnalysis.objects.create(
        user=user,
        content_key=content_key,
        filename=filename,
        mime_type=mime_type,
        summary=result.summary,
        insights=result.insights,
        mail_guide=result.mail_guide,
    )

    return result.model_dump()
