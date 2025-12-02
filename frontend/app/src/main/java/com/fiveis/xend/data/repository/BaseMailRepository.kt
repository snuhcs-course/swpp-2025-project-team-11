package com.fiveis.xend.data.repository

import android.util.Log
import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.data.model.AttachmentAnalysisRequest
import com.fiveis.xend.data.model.AttachmentAnalysisResponse
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.model.MailDetailResponse
import com.fiveis.xend.data.model.MailListResponse
import com.fiveis.xend.network.MailApiService
import kotlinx.coroutines.flow.Flow
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * Base repository class for mail operations.
 * Provides common functionality for both inbox and sent mail repositories.
 *
 * @param mailApiService API service for mail operations
 * @param emailDao DAO for local email database operations
 * @param label The label for this repository (e.g., "INBOX" or "SENT")
 * @param logTag Tag used for logging
 */
abstract class BaseMailRepository(
    protected val mailApiService: MailApiService,
    protected val emailDao: EmailDao,
    protected val label: String,
    protected val logTag: String
) {
    fun getCachedEmails(): Flow<List<EmailItem>> {
        return emailDao.getEmailsByLabel(label).also {
            Log.d(logTag, "getCachedEmails Flow created for $label")
        }
    }

    suspend fun getMails(
        labels: String? = label,
        maxResults: Int? = 20,
        pageToken: String? = null,
        sinceDate: String? = null
    ): Response<MailListResponse> {
        return mailApiService.getEmails(labels, maxResults, pageToken, sinceDate)
    }

    suspend fun refreshEmails(labels: String? = label, maxResults: Int? = 20): Result<String?> {
        return try {
            // 가장 최신 메일의 날짜 가져오기
            val latestDate = emailDao.getLatestEmailDate(label)

            if (latestDate == null) {
                // DB가 비어있으면 첫 페이지만 가져오기
                Log.d(logTag, "DB is empty, fetching first page")
                val response = mailApiService.getEmails(labels, maxResults, null, null)
                Log.d(logTag, "API response: isSuccessful=${response.isSuccessful}, code=${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null) {
                        Log.e(logTag, "Response body is null!")
                        return Result.failure(Exception("Response body is null"))
                    }

                    val messages = body.messages
                    Log.d(logTag, "Received ${messages.size} emails")

                    if (messages.isEmpty()) {
                        Log.w(logTag, "No emails received from API")
                        return Result.success(null)
                    }

                    emailDao.insertEmails(messages.withSourceLabel())
                    val count = emailDao.getEmailCount()
                    Log.d(logTag, "Successfully inserted ${messages.size} emails into DB")
                    Log.d(logTag, "Total emails in DB: $count")
                    Log.d(logTag, "nextPageToken: ${body.nextPageToken}")
                    return Result.success(body.nextPageToken)
                } else {
                    Log.e(logTag, "API request failed with code: ${response.code()}")
                    return Result.failure(Exception("Failed to fetch emails: ${response.code()}"))
                }
            }

            // DB에 메일이 있으면 since_date를 사용해서 최신 메일만 가져오기
            // 페이지네이션 루프로 모든 새 이메일 가져오기
            Log.d(logTag, "Fetching new emails since: $latestDate")
            var pageToken: String? = null
            var totalFetched = 0

            do {
                val response = mailApiService.getEmails(labels, maxResults, pageToken, latestDate)

                if (!response.isSuccessful) {
                    Log.e(logTag, "API request failed with code: ${response.code()}")
                    return Result.failure(Exception("Failed to fetch emails: ${response.code()}"))
                }

                val mailListResponse = response.body()
                if (mailListResponse == null) {
                    Log.w(logTag, "Response body is null")
                    return Result.failure(Exception("Response body is null"))
                }

                val newEmails = mailListResponse.messages
                totalFetched += newEmails.size
                Log.d(logTag, "Received ${newEmails.size} new emails (total: $totalFetched)")

                if (newEmails.isNotEmpty()) {
                    emailDao.insertEmails(newEmails.withSourceLabel())
                }

                val previousToken = pageToken
                val nextToken = mailListResponse.nextPageToken?.takeIf { it.isNotBlank() }
                if (nextToken != null && nextToken == previousToken) {
                    Log.d(logTag, "Received identical nextPageToken; stopping pagination to avoid loop")
                    break
                }

                pageToken = nextToken
                Log.d(logTag, "nextPageToken: $pageToken")
            } while (pageToken != null)

            val count = emailDao.getEmailCount()
            Log.d(logTag, "Successfully fetched $totalFetched new emails")
            Log.d(logTag, "Total emails in DB: $count")

            Result.success(null) // No more pages to fetch
        } catch (e: Exception) {
            Log.e(logTag, "Exception during refreshEmails", e)
            Result.failure(e)
        }
    }

    suspend fun getMail(messageId: String): Response<MailDetailResponse> {
        return mailApiService.getMail(messageId)
    }

    suspend fun downloadAttachment(
        messageId: String,
        attachmentId: String,
        filename: String,
        mimeType: String
    ): Response<ResponseBody> {
        return mailApiService.downloadAttachment(messageId, attachmentId, filename, mimeType)
    }

    suspend fun analyzeAttachment(
        messageId: String,
        attachmentId: String,
        filename: String,
        mimeType: String
    ): Response<AttachmentAnalysisResponse> {
        val request = AttachmentAnalysisRequest(
            messageId = messageId,
            attachmentId = attachmentId,
            filename = filename,
            mimeType = mimeType
        )
        return mailApiService.analyzeAttachment(request)
    }

    suspend fun updateReadStatus(emailId: String, isUnread: Boolean) {
        emailDao.updateReadStatus(emailId, isUnread)
    }

    suspend fun saveEmailsToCache(emails: List<EmailItem>) {
        Log.d(logTag, "saveEmailsToCache: saving ${emails.size} emails")
        emailDao.insertEmails(emails.withSourceLabel())
        val count = emailDao.getEmailCount()
        Log.d(logTag, "saveEmailsToCache: total emails in DB = $count")
    }

    private fun List<EmailItem>.withSourceLabel(): List<EmailItem> {
        return map { email ->
            if (email.sourceLabel == label) {
                email
            } else {
                email.copy(sourceLabel = label)
            }
        }
    }
}
