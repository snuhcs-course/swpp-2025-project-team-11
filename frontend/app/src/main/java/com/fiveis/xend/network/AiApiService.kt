package com.fiveis.xend.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class PromptPreviewRequest(
    val to: List<String>
)

data class PromptPreviewResponse(
    val previewText: String?
)

data class MailSuggestRequest(
    val subject: String? = null,
    val body: String? = null,
    @SerializedName("to_emails")
    val toEmails: List<String>,
    val target: String = "body",
    val cursor: Int? = null
)

data class MailSuggestResponse(
    val target: String,
    val suggestion: String
)

interface AiApiService {
    @POST("api/ai/mail/prompts/preview/")
    suspend fun getPromptPreview(@Body request: PromptPreviewRequest): Response<PromptPreviewResponse>

    @POST("api/ai/mail/suggest/")
    suspend fun suggestMail(@Body request: MailSuggestRequest): Response<MailSuggestResponse>
}
