package com.fiveis.xend.data.model

import com.google.gson.annotations.SerializedName

data class MailDetailResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("thread_id")
    val threadId: String,

    @SerializedName("subject")
    val subject: String,

    @SerializedName("from_email")
    val fromEmail: String,

    @SerializedName("to_email")
    val toEmail: String,

    @SerializedName("to")
    val to: String,

    @SerializedName("date")
    val date: String,

    @SerializedName("date_raw")
    val dateRaw: String,

    @SerializedName("body")
    val body: String,

    @SerializedName("snippet")
    val snippet: String,

    @SerializedName("is_unread")
    val isUnread: Boolean,

    @SerializedName("label_ids")
    val labelIds: List<String>,

    @SerializedName("attachments")
    val attachments: List<Attachment> = emptyList()
)
