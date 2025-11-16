package com.fiveis.xend.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class PromptPreviewRequest(
    val to: List<String>
)

data class PromptPreviewResponse(
    val previewText: String?
)

interface AiApiService {
    @POST("api/ai/mail/prompts/preview/")
    suspend fun getPromptPreview(@Body request: PromptPreviewRequest): Response<PromptPreviewResponse>
}
