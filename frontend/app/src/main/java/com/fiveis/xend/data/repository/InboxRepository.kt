package com.fiveis.xend.data.repository

import android.util.Log
import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.model.MailDetailResponse
import com.fiveis.xend.data.model.MailListResponse
import com.fiveis.xend.network.MailApiService
import kotlinx.coroutines.flow.Flow
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
        pageToken: String? = null,
        sinceDate: String? = null
    ): Response<MailListResponse> {
        return mailApiService.getEmails(labels, maxResults, pageToken, sinceDate)
    }

    suspend fun refreshEmails(labels: String? = "INBOX", maxResults: Int? = 20): Result<String?> {
        return try {
            // 가장 최신 메일의 날짜 가져오기
            val latestDate = emailDao.getLatestEmailDate()

            if (latestDate == null) {
                // DB가 비어있으면 첫 페이지만 가져오기
                Log.d("InboxRepository", "DB is empty, fetching first page")
                val response = mailApiService.getEmails(labels, maxResults, null, null)
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

            // DB에 메일이 있으면 since_date를 사용해서 최신 메일만 가져오기
            Log.d("InboxRepository", "Fetching new emails since: $latestDate")
            val response = mailApiService.getEmails(labels, maxResults, null, latestDate)

            if (!response.isSuccessful) {
                Log.e("InboxRepository", "API request failed with code: ${response.code()}")
                return Result.failure(Exception("Failed to fetch emails: ${response.code()}"))
            }

            val mailListResponse = response.body()
            if (mailListResponse == null) {
                Log.w("InboxRepository", "Response body is null")
                return Result.failure(Exception("Response body is null"))
            }

            val newEmails = mailListResponse.messages
            Log.d("InboxRepository", "Received ${newEmails.size} new emails")

            if (newEmails.isNotEmpty()) {
                emailDao.insertEmails(newEmails)
                val count = emailDao.getEmailCount()
                Log.d("InboxRepository", "Successfully inserted ${newEmails.size} new emails into DB")
                Log.d("InboxRepository", "Total emails in DB: $count")
            } else {
                Log.d("InboxRepository", "No new emails to insert")
            }

            Log.d("InboxRepository", "nextPageToken after refresh: ${mailListResponse.nextPageToken}")
            Result.success(mailListResponse.nextPageToken)
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
