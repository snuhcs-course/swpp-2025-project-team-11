package com.fiveis.xend.network

import com.fiveis.xend.data.model.MailDetailResponse
import com.fiveis.xend.data.model.MailListResponse
import com.fiveis.xend.data.model.MailSendRequest
import com.fiveis.xend.data.model.SendResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MailApiService {
    @POST("api/mail/emails/send/")
    suspend fun sendEmail(@Body payload: MailSendRequest): Response<SendResponse>

    @GET("api/mail/emails/")
    suspend fun getEmails(
        @Query("labels") labels: String? = "INBOX",
        @Query("max_results") maxResults: Int? = 20,
        @Query("page_token") pageToken: String? = null
    ): Response<MailListResponse>

    @GET("api/mail/emails/{message_id}/")
    suspend fun getMail(@Path("message_id") messageId: String): Response<MailDetailResponse>
}
