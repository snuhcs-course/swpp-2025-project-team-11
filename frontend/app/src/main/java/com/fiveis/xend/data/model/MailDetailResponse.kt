package com.fiveis.xend.data.model

data class MailDetailResponse(
    val id: String,
    val thread_id: String,
    val subject: String,
    val from_email: String,
    val to: String,
    val date: String,
    val date_raw: String,
    val body: String,
    val snippet: String,
    val is_unread: Boolean,
    val label_ids: List<String>
)
