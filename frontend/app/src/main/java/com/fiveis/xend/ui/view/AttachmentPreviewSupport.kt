package com.fiveis.xend.ui.view

import com.fiveis.xend.data.model.Attachment
import java.util.Locale

enum class AttachmentPreviewType { TEXT, PDF, UNSUPPORTED }

fun Attachment.previewType(): AttachmentPreviewType {
    val mime = mimeType.lowercase(Locale.getDefault())
    val name = filename.lowercase(Locale.getDefault())
    return when {
        mime.contains("pdf") || name.endsWith(".pdf") -> AttachmentPreviewType.PDF
        mime.startsWith("text/") ||
            mime.contains("json") ||
            name.endsWith(".txt") ||
            name.endsWith(".md") ||
            name.endsWith(".csv") -> AttachmentPreviewType.TEXT
        else -> AttachmentPreviewType.UNSUPPORTED
    }
}

fun Attachment.supportsInAppPreview(): Boolean = previewType() != AttachmentPreviewType.UNSUPPORTED
