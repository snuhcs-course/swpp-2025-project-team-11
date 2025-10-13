package com.fiveis.xend.data.repository

import com.fiveis.xend.data.model.MailSendRequest
import com.fiveis.xend.data.model.SendResponse
import com.fiveis.xend.network.MailApiService
import com.fiveis.xend.network.RetrofitClient

class MailSendRepository(
    private val mailApiService: MailApiService = RetrofitClient.mailApiService
) {

    suspend fun sendEmail(
        endpointUrl: String,
        to: String,
        subject: String,
        body: String,
        accessToken: String?
    ): SendResponse {
        val request = MailSendRequest(to = to, subject = subject, body = body)
        val authHeader = if (!accessToken.isNullOrEmpty()) "Bearer $accessToken" else null

        val response = mailApiService.sendEmail(
            endpointUrl = endpointUrl,
            authorization = authHeader,
            payload = request
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
