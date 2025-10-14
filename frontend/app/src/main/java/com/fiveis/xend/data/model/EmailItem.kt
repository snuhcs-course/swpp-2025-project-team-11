package com.fiveis.xend.data.model

import com.google.gson.annotations.SerializedName

data class EmailItem(
    @SerializedName("id")
    val id: String,

    @SerializedName("thread_id")
    val threadId: String,

    @SerializedName("subject")
    val subject: String,

    @SerializedName("from_email")
    val fromEmail: String,

    @SerializedName("snippet")
    val snippet: String,

    @SerializedName("date")
    val date: String,

    @SerializedName("date_raw")
    val dateRaw: String,

    @SerializedName("is_unread")
    val isUnread: Boolean,

    @SerializedName("label_ids")
    val labelIds: List<String>
)
