package com.fiveis.xend.network

import com.fiveis.xend.data.model.MailSendRequest
import com.fiveis.xend.data.model.SendResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface MailApiService {
    @POST
    suspend fun sendEmail(
        @Url endpointUrl: String,
        @Header("Authorization") authorization: String?,
        @Body payload: MailSendRequest
    ): Response<SendResponse>
}
