package com.fiveis.xend.data.repository

import android.util.Log
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
        return emailDao.getAllEmails().also {
            Log.d("InboxRepository", "getCachedEmails Flow created")
        }
    }

    suspend fun getMails(
        labels: String? = "INBOX",
        maxResults: Int? = 20,
        pageToken: String? = null
    ): Response<MailListResponse> {
        return mailApiService.getEmails(labels, maxResults, pageToken)
    }

    suspend fun refreshEmails(labels: String? = "INBOX", maxResults: Int? = 20): Result<String?> {
        return try {
            val existingEmails = emailDao.getAllEmails().firstOrNull() ?: emptyList()
            val existingEmailIds = existingEmails.map { it.id }.toSet()

            // DB가 비어있으면 첫 페이지만 가져오기
            if (existingEmailIds.isEmpty()) {
                Log.d("InboxRepository", "DB is empty, fetching first page")
                val response = mailApiService.getEmails(labels, maxResults, null)
                Log.d("InboxRepository", "API response: isSuccessful=${response.isSuccessful}, code=${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null) {
                        Log.e("InboxRepository", "Response body is null!")
                        return Result.failure(Exception("Response body is null"))
                    }

                    val messages = body.messages
                    Log.d("InboxRepository", "Received ${messages.size} emails")

                    if (messages.isEmpty()) {
                        Log.w("InboxRepository", "No emails received from API")
                        return Result.success(null)
                    }

                    emailDao.insertEmails(messages)
                    val count = emailDao.getEmailCount()
                    Log.d("InboxRepository", "Successfully inserted ${messages.size} emails into DB")
                    Log.d("InboxRepository", "Total emails in DB: $count")
                    Log.d("InboxRepository", "nextPageToken: ${body.nextPageToken}")
                    return Result.success(body.nextPageToken)
                } else {
                    Log.e("InboxRepository", "API request failed with code: ${response.code()}")
                    return Result.failure(Exception("Failed to fetch emails: ${response.code()}"))
                }
            }

            // DB에 메일이 있으면 겹치는 메일이 나올 때까지 가져오기
            Log.d("InboxRepository", "DB has ${existingEmailIds.size} existing emails, fetching new ones")
            val allNewEmails = mutableListOf<EmailItem>()
            var pageToken: String? = null
            var lastNextPageToken: String? = null
            var hasOverlap = false

            while (!hasOverlap) {
                val response = mailApiService.getEmails(labels, maxResults, pageToken)
                if (!response.isSuccessful) {
                    Log.e("InboxRepository", "API request failed with code: ${response.code()}")
                    return Result.failure(Exception("Failed to fetch emails: ${response.code()}"))
                }

                val mailListResponse = response.body()
                if (mailListResponse == null) {
                    Log.w("InboxRepository", "Response body is null, breaking loop")
                    break
                }

                val fetchedEmails = mailListResponse.messages

                if (fetchedEmails.isEmpty()) {
                    Log.d("InboxRepository", "No more emails to fetch")
                    break
                }

                val newEmailsInBatch = fetchedEmails.filter { it.id !in existingEmailIds }
                allNewEmails.addAll(newEmailsInBatch)
                Log.d("InboxRepository", "Found ${newEmailsInBatch.size} new emails in this batch")

                if (newEmailsInBatch.size < fetchedEmails.size) {
                    hasOverlap = true
                }

                lastNextPageToken = mailListResponse.nextPageToken
                pageToken = lastNextPageToken
                if (pageToken == null) {
                    Log.d("InboxRepository", "No more pages to fetch")
                    break
                }
            }

            if (allNewEmails.isNotEmpty()) {
                emailDao.insertEmails(allNewEmails)
                val count = emailDao.getEmailCount()
                Log.d("InboxRepository", "Successfully inserted ${allNewEmails.size} new emails into DB")
                Log.d("InboxRepository", "Total emails in DB: $count")
            } else {
                Log.d("InboxRepository", "No new emails to insert")
            }

            Log.d("InboxRepository", "nextPageToken after refresh: $lastNextPageToken")
            Result.success(lastNextPageToken)
        } catch (e: Exception) {
            Log.e("InboxRepository", "Exception during refreshEmails", e)
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
        Log.d("InboxRepository", "saveEmailsToCache: saving ${emails.size} emails")
        emailDao.insertEmails(emails)
        val count = emailDao.getEmailCount()
        Log.d("InboxRepository", "saveEmailsToCache: total emails in DB = $count")
    }
}
