package com.fiveis.xend.data.model

import com.google.gson.annotations.SerializedName

/**
 * Lightweight metadata for an email attachment.
 * The binary payload is fetched on demand via the attachment download endpoint,
 * so Room only persists this reference info.
 */
data class Attachment(
    @SerializedName("attachment_id")
    val attachmentId: String,

    @SerializedName("filename")
    val filename: String,

    @SerializedName("mime_type")
    val mimeType: String,

    @SerializedName("size")
    val size: Long
)
