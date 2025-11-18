package com.fiveis.xend.data.model

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.gson.annotations.SerializedName
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.source

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

fun MailSendRequest.toMultipartParts(
    context: Context,
    attachmentUris: List<Uri> = emptyList()
): List<MultipartBody.Part> {
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

    // Binary attachments (sent as repeated "attachments" form-data parts)
    attachmentUris.forEach { uri ->
        val contentResolver = context.contentResolver

        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val fileName =
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            } ?: "attachment"

        val requestBody = object : RequestBody() {
            override fun contentType() = mimeType.toMediaTypeOrNull()

            override fun writeTo(sink: okio.BufferedSink) {
                contentResolver.openInputStream(uri)?.use { input ->
                    sink.writeAll(input.source())
                }
            }
        }

        parts += MultipartBody.Part.createFormData(
            "attachments",
            fileName,
            requestBody
        )
    }

    return parts
}
