package com.fiveis.xend.data.model

import com.google.gson.annotations.SerializedName

data class MailSendRequest(
    @SerializedName("to")
    val to: String,
    @SerializedName("subject")
    val subject: String,
    @SerializedName("body")
    val body: String
)
