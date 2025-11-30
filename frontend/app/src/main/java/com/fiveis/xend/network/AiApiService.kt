package com.fiveis.xend.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class MailGenerateRequestDto(
    val subject: String?,
    val body: String?,
    val toEmails: List<String>,
    val attachmentContentKeys: List<String> = emptyList()
)

data class MailGenerateVariantDto(
    val subject: String,
    val body: String
)

data class MailGenerateTestResponse(
    val analysis: com.google.gson.JsonElement?,
    val fewshots: com.google.gson.JsonElement?,
    val withoutAnalysis: MailGenerateVariantDto,
    val withAnalysis: MailGenerateVariantDto,
    val withFewshots: MailGenerateVariantDto
)

data class PromptPreviewRequest(
    val to: List<String>
)

data class PromptPreviewResponse(
    val previewText: String?
)

interface AiApiService {
    @POST("api/ai/mail/prompts/preview/")
    suspend fun getPromptPreview(@Body request: PromptPreviewRequest): Response<PromptPreviewResponse>

    @POST("api/ai/mail/generate/test/")
    suspend fun runMailGenerateTest(@Body request: MailGenerateRequestDto): Response<MailGenerateTestResponse>
}
