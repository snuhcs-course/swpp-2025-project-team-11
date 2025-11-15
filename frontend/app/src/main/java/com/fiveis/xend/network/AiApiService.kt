package com.fiveis.xend.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class PromptPreviewRequest(
    val to: List<String>
)

data class PromptPreviewResponse(
    val to_emails: List<String>,
    val relationship: String?,
    val sender_role: String?,
    val recipient_role: String?,
    val personal_prompt: String?,
    val situational_prompt: String?,
    val style_prompt: String?,
    val format_prompt: String?
)

interface AiApiService {
    @POST("api/ai/mail/prompts/preview/")
    suspend fun getPromptPreview(@Body request: PromptPreviewRequest): Response<PromptPreviewResponse>
}
