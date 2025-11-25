package com.fiveis.xend.network

import com.fiveis.xend.data.model.AttachmentAnalysisRequest
import com.fiveis.xend.data.model.AttachmentAnalysisResponse
import com.fiveis.xend.data.model.MailDetailResponse
import com.fiveis.xend.data.model.MailListResponse
import com.fiveis.xend.data.model.ReadStatusUpdateRequest
import com.fiveis.xend.data.model.ReadStatusUpdateResponse
import com.fiveis.xend.data.model.SendResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface MailApiService {
    @Multipart
    @POST("api/mail/emails/send/")
    suspend fun sendEmail(@Part parts: List<MultipartBody.Part>): Response<SendResponse>

    @GET("api/mail/emails/")
    suspend fun getEmails(
        @Query("labels") labels: String? = "INBOX",
        @Query("max_results") maxResults: Int? = 20,
        @Query("page_token") pageToken: String? = null,
        @Query("since_date") sinceDate: String? = null
    ): Response<MailListResponse>

    @GET("api/mail/emails/{message_id}/")
    suspend fun getMail(@Path("message_id") messageId: String): Response<MailDetailResponse>

    @GET("api/mail/emails/{message_id}/attachments/{attachment_id}/")
    suspend fun downloadAttachment(
        @Path("message_id") messageId: String,
        @Path("attachment_id") attachmentId: String,
        @Query("filename") filename: String,
        @Query("mime_type") mimeType: String
    ): Response<ResponseBody>

    @POST("api/ai/mail/attachments/analyze/")
    suspend fun analyzeAttachment(@Body request: AttachmentAnalysisRequest): Response<AttachmentAnalysisResponse>

    @PATCH("api/mail/emails/{message_id}/read/")
    suspend fun updateReadStatus(
        @Path("message_id") messageId: String,
        @Body request: ReadStatusUpdateRequest
    ): Response<ReadStatusUpdateResponse>
}
