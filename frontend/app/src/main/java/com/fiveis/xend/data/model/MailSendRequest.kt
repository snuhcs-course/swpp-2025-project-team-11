package com.fiveis.xend.data.model

import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody

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

fun MailSendRequest.toMultipartParts(): List<MultipartBody.Part> {
    val parts = mutableListOf<MultipartBody.Part>()

    fun add(name: String, value: String) {
        parts += MultipartBody.Part.createFormData(name, value)
    }

    to.forEach { recipient ->
        add("to", recipient)
    }
    cc.forEach { recipient ->
        add("cc", recipient)
    }
    bcc.forEach { recipient ->
        add("bcc", recipient)
    }

    add("subject", subject)
    add("body", body)
    add("is_html", isHtml.toString())

    return parts
}
