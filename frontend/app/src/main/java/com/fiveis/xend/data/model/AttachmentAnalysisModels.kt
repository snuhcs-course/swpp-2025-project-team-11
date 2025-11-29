package com.fiveis.xend.data.model

import com.google.gson.annotations.SerializedName

data class AttachmentAnalysisRequest(
    @SerializedName("message_id")
    val messageId: String,
    @SerializedName("attachment_id")
    val attachmentId: String,
    @SerializedName("filename")
    val filename: String,
    @SerializedName("mime_type")
    val mimeType: String
)

data class AttachmentAnalysisResponse(
    @SerializedName("summary")
    val summary: String,
    @SerializedName("insights")
    val insights: String,
    @SerializedName("mail_guide")
    val mailGuide: String,
    @SerializedName("content_key")
    val contentKey: String? = null
)
