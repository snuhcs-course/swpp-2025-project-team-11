package com.fiveis.xend.data.repository

import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.model.MailDetailResponse
import com.fiveis.xend.data.model.MailListResponse
import com.fiveis.xend.network.MailApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import retrofit2.Response

class InboxRepository(
    private val mailApiService: MailApiService,
    private val emailDao: EmailDao
) {
    fun getCachedEmails(): Flow<List<EmailItem>> {
        return emailDao.getAllEmails()
    }

    suspend fun getMails(
        labels: String? = "INBOX",
        maxResults: Int? = 20,
        pageToken: String? = null
    ): Response<MailListResponse> {
        return mailApiService.getEmails(labels, maxResults, pageToken)
    }

    suspend fun refreshEmails(labels: String? = "INBOX", maxResults: Int? = 20): Result<Unit> {
        return try {
            val existingEmails = emailDao.getAllEmails().firstOrNull() ?: emptyList()
            val existingEmailIds = existingEmails.map { it.id }.toSet()

            // DB가 비어있으면 첫 페이지만 가져오기
            if (existingEmailIds.isEmpty()) {
                val response = mailApiService.getEmails(labels, maxResults, null)
                if (response.isSuccessful) {
                    response.body()?.let { mailListResponse ->
                        emailDao.insertEmails(mailListResponse.messages)
                    }
                    return Result.success(Unit)
                } else {
                    return Result.failure(Exception("Failed to fetch emails: ${response.code()}"))
                }
            }

            // DB에 메일이 있으면 겹치는 메일이 나올 때까지 가져오기
            val allNewEmails = mutableListOf<EmailItem>()
            var pageToken: String? = null
            var hasOverlap = false

            while (!hasOverlap) {
                val response = mailApiService.getEmails(labels, maxResults, pageToken)
                if (!response.isSuccessful) {
                    return Result.failure(Exception("Failed to fetch emails: ${response.code()}"))
                }

                val mailListResponse = response.body() ?: break
                val fetchedEmails = mailListResponse.messages

                if (fetchedEmails.isEmpty()) {
                    break
                }

                val newEmailsInBatch = fetchedEmails.filter { it.id !in existingEmailIds }
                allNewEmails.addAll(newEmailsInBatch)

                if (newEmailsInBatch.size < fetchedEmails.size) {
                    hasOverlap = true
                }

                pageToken = mailListResponse.nextPageToken
                if (pageToken == null) {
                    break
                }
            }

            if (allNewEmails.isNotEmpty()) {
                emailDao.insertEmails(allNewEmails)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMail(messageId: String): Response<MailDetailResponse> {
        return mailApiService.getMail(messageId)
    }

    suspend fun updateReadStatus(emailId: String, isUnread: Boolean) {
        emailDao.updateReadStatus(emailId, isUnread)
    }

    suspend fun saveEmailsToCache(emails: List<EmailItem>) {
        emailDao.insertEmails(emails)
    }
}
