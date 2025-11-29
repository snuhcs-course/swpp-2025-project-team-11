package com.fiveis.xend.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.fiveis.xend.data.model.AttachmentAnalysisResponse
import com.fiveis.xend.data.model.MailSendRequest
import com.fiveis.xend.data.model.SendResponse
import com.fiveis.xend.data.model.toMultipartParts
import com.fiveis.xend.network.MailApiService
import com.fiveis.xend.network.RetrofitClient
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.source

class MailSendRepository(
    context: Context,
    private val mailApiService: MailApiService = RetrofitClient.getMailApiService(context.applicationContext)
) {
    private val appContext = context.applicationContext

    suspend fun sendEmail(
        to: List<String>,
        subject: String,
        body: String,
        attachmentUris: List<Uri> = emptyList()
    ): SendResponse {
        val request = MailSendRequest(to = to, subject = subject, body = body)

        val parts = try {
            request.toMultipartParts(
                context = appContext,
                attachmentUris = attachmentUris
            )
        } catch (ioe: IOException) {
            Log.e("MailSendRepository", "Failed to prepare attachment streams for URIs=$attachmentUris", ioe)
            throw AttachmentException("Attachment preparation failed", ioe)
        }

        val response = mailApiService.sendEmail(parts = parts)

        // 응답 코드가 201이고, 요청이 성공했는지 확인
        if (response.isSuccessful && response.code() == 201) {
            return response.body()
                ?: throw IllegalStateException("Success response but body is null")
        } else {
            // 요청 실패 시 예외 발생
            val errorBody = response.errorBody()?.string()?.take(500) ?: "Unknown error"
            throw IllegalStateException(
                "Send failed: HTTP ${response.code()} ${response.message()} | body=$errorBody"
            )
        }
    }

    suspend fun analyzeAttachmentUpload(uri: Uri): AttachmentAnalysisResponse {
        val resolver = appContext.contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val fileName =
            resolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            } ?: "attachment"

        val requestBody = object : RequestBody() {
            override fun contentType() = mimeType.toMediaType()

            override fun writeTo(sink: okio.BufferedSink) {
                val inputStream = resolver.openInputStream(uri)
                    ?: throw IOException("Unable to open stream for analysis uri=$uri")
                inputStream.use { input ->
                    sink.writeAll(input.source())
                }
            }
        }

        val attachmentPart = MultipartBody.Part.createFormData(
            "file",
            fileName,
            requestBody
        )
        val response = mailApiService.analyzeAttachmentUpload(file = attachmentPart)

        if (response.isSuccessful) {
            return response.body()
                ?: throw IllegalStateException("AI 분석 결과가 비어 있습니다.")
        } else {
            val errorBody = response.errorBody()?.string()?.take(500) ?: "Unknown error"
            throw IllegalStateException(
                "AI 분석 실패: HTTP ${response.code()} ${response.message()} | body=$errorBody"
            )
        }
    }
}

class AttachmentException(message: String, cause: Throwable? = null) : IOException(message, cause)
