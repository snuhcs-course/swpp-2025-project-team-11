package com.fiveis.xend.data.repository

import android.content.Context
import android.net.Uri
import com.fiveis.xend.data.model.MailSendRequest
import com.fiveis.xend.data.model.SendResponse
import com.fiveis.xend.data.model.toMultipartParts
import com.fiveis.xend.network.MailApiService
import com.fiveis.xend.network.RetrofitClient

class MailSendRepository(context: Context) {
    private val appContext = context.applicationContext
    private val mailApiService: MailApiService = RetrofitClient.getMailApiService(appContext)

    suspend fun sendEmail(
        to: List<String>,
        subject: String,
        body: String,
        attachmentUris: List<Uri> = emptyList()
    ): SendResponse {
        val request = MailSendRequest(to = to, subject = subject, body = body)

        val response = mailApiService.sendEmail(
            parts = request.toMultipartParts(
                context = appContext,
                attachmentUris = attachmentUris
            )
        )

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
}
