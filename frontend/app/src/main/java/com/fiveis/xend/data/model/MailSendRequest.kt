package com.fiveis.xend.data.model

import com.google.gson.annotations.SerializedName

data class MailSendRequest(
    @SerializedName("to")
    val to: List<String>,
    @SerializedName("cc")
    val cc: List<String> = emptyList(),
    @SerializedName("bcc")
    val bcc: List<String> = emptyList(),
    @SerializedName("subject")
    val subject: String,
    @SerializedName("body")
    val body: String,
    @SerializedName("is_html")
    val isHtml: Boolean = true
)
