package com.fiveis.xend.data.repository

import com.fiveis.xend.data.model.MailDetailResponse
import com.fiveis.xend.data.model.MailListResponse
import com.fiveis.xend.network.MailApiService
import retrofit2.Response

class InboxRepository(private val mailApiService: MailApiService) {
    suspend fun getMails(
        labels: String? = "INBOX",
        maxResults: Int? = 20,
        pageToken: String? = null
    ): Response<MailListResponse> {
        return mailApiService.getEmails(labels, maxResults, pageToken)
    }

    suspend fun getMail(messageId: String): Response<MailDetailResponse> {
        return mailApiService.getMail(messageId)
    }
}
