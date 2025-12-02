package com.fiveis.xend.data.repository

import android.util.Log
import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.data.model.AttachmentAnalysisRequest
import com.fiveis.xend.data.model.AttachmentAnalysisResponse
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.model.MailDetailResponse
import com.fiveis.xend.data.model.MailListResponse
import com.fiveis.xend.data.model.ReadStatusUpdateRequest
import com.fiveis.xend.network.MailApiService
import com.fiveis.xend.utils.EmailUtils
import com.fiveis.xend.utils.mergeSourceLabels
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.ResponseBody
import retrofit2.Response

class SentRepository(
    private val mailApiService: MailApiService,
    private val emailDao: EmailDao
) {
    /**
     * Add dateTimestamp to emails for proper chronological sorting
     */
    private fun List<EmailItem>.withParsedTimestamps(): List<EmailItem> {
        return map { email ->
            val timestamp = EmailUtils.parseDateToTimestamp(email.dateRaw)
            val displayDate = EmailUtils.formatDisplayDate(timestamp, email.date)
            val displaySenderName = EmailUtils.extractSenderName(email.fromEmail)
            email.copy(
                dateTimestamp = timestamp,
                displayDate = displayDate,
                displaySenderName = displaySenderName,
                sourceLabel = "SENT"
            )
        }
    }

    private fun EmailItem.ensureDisplayFields(): EmailItem {
        val needsDate = displayDate.isBlank()
        val needsName = displaySenderName.isBlank()
        if (!needsDate && !needsName) return this
        val resolvedDate = if (needsDate) {
            EmailUtils.formatDisplayDate(dateTimestamp, date)
        } else {
            displayDate
        }
        val resolvedName = if (needsName) {
            EmailUtils.extractSenderName(fromEmail)
        } else {
            displaySenderName
        }
        return copy(displayDate = resolvedDate, displaySenderName = resolvedName)
    }

    fun getCachedEmails(): Flow<List<EmailItem>> {
        return emailDao.getEmailsByLabel("SENT").map { emails ->
            emails.map { it.ensureDisplayFields() }
        }.also {
            Log.d("SentRepository", "getCachedEmails Flow created for SENT")
        }
    }

    suspend fun getMails(
        labels: String? = "SENT",
        maxResults: Int? = 20,
        pageToken: String? = null,
        sinceDate: String? = null
    ): Response<MailListResponse> {
        return mailApiService.getEmails(labels, maxResults, pageToken, sinceDate)
    }

    suspend fun refreshEmails(labels: String? = "SENT", maxResults: Int? = 20): Result<String?> {
        return try {
            // 가장 최신 메일의 날짜 가져오기
            val latestDate = emailDao.getLatestEmailDate("SENT")

            if (latestDate == null) {
                // DB가 비어있으면 첫 페이지만 가져오기
                Log.d("SentRepository", "DB is empty, fetching first page")
                val response = mailApiService.getEmails(labels, maxResults, null, null)
                Log.d("SentRepository", "API response: isSuccessful=${response.isSuccessful}, code=${response.code()}")

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body == null) {
                        Log.e("SentRepository", "Response body is null!")
                        return Result.failure(Exception("Response body is null"))
                    }

                    val messages = body.messages
                    Log.d("SentRepository", "Received ${messages.size} emails")

                    if (messages.isEmpty()) {
                        Log.w("SentRepository", "No emails received from API")
                        return Result.success(null)
                    }

                    val processed = messages.withParsedTimestamps().withSourceLabel()
                    emailDao.insertEmails(processed)
                    val count = emailDao.getEmailCount()
                    Log.d("SentRepository", "Successfully inserted ${messages.size} emails into DB")
                    Log.d("SentRepository", "Total emails in DB: $count")
                    Log.d("SentRepository", "nextPageToken: ${body.nextPageToken}")
                    return Result.success(body.nextPageToken)
                } else {
                    Log.e("SentRepository", "API request failed with code: ${response.code()}")
                    return Result.failure(Exception("Failed to fetch emails: ${response.code()}"))
                }
            }

            // DB에 메일이 있으면 since_date를 사용해서 최신 메일만 가져오기
            // 페이지네이션 루프로 모든 새 이메일 가져오기
            Log.d("SentRepository", "Fetching new emails since: $latestDate")
            var pageToken: String? = null
            var totalFetched = 0

            do {
                val response = mailApiService.getEmails(labels, maxResults, pageToken, latestDate)

                if (!response.isSuccessful) {
                    Log.e("SentRepository", "API request failed with code: ${response.code()}")
                    return Result.failure(Exception("Failed to fetch emails: ${response.code()}"))
                }

                val mailListResponse = response.body()
                if (mailListResponse == null) {
                    Log.w("SentRepository", "Response body is null")
                    return Result.failure(Exception("Response body is null"))
                }

                val newEmails = mailListResponse.messages
                totalFetched += newEmails.size
                Log.d("SentRepository", "Received ${newEmails.size} new emails (total: $totalFetched)")

                if (newEmails.isNotEmpty()) {
                    val processed = newEmails.withParsedTimestamps().withSourceLabel()
                    emailDao.insertEmails(processed)
                }

                val previousToken = pageToken
                val nextToken = mailListResponse.nextPageToken
                if (nextToken != null && nextToken == previousToken) {
                    Log.d("SentRepository", "Received identical nextPageToken; stopping pagination to avoid loop")
                    break
                }

                pageToken = nextToken
                Log.d("SentRepository", "nextPageToken: $pageToken")
            } while (pageToken != null)

            val count = emailDao.getEmailCount()
            Log.d("SentRepository", "Successfully fetched $totalFetched new emails")
            Log.d("SentRepository", "Total emails in DB: $count")

            Result.success(null) // No more pages to fetch
        } catch (e: Exception) {
            Log.e("SentRepository", "Exception during refreshEmails", e)
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
        try {
            val response = mailApiService.updateReadStatus(
                messageId = emailId,
                request = ReadStatusUpdateRequest(isRead = !isUnread)
            )

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                Log.e(
                    "SentRepository",
                    "Failed to sync read status (code=${response.code()} body=$errorBody)"
                )
                throw Exception("Failed to update read status: ${response.code()}")
            }

            emailDao.updateReadStatus(emailId, isUnread)
        } catch (e: Exception) {
            Log.e("SentRepository", "Error updating read status for $emailId", e)
            throw e
        }
    }

    suspend fun saveEmailsToCache(emails: List<EmailItem>) {
        Log.d("SentRepository", "saveEmailsToCache: saving ${emails.size} emails")
        val processed = emails.withParsedTimestamps().withSourceLabel()
        emailDao.insertEmails(processed)
        val count = emailDao.getEmailCount()
        Log.d("SentRepository", "saveEmailsToCache: total emails in DB = $count")
    }

    private suspend fun List<EmailItem>.withSourceLabel(): List<EmailItem> {
        if (isEmpty()) return this
        val existing = emailDao.getEmailsByIds(map { it.id }).associateBy { it.id }
        return map { email ->
            val merged = mergeSourceLabels(existing[email.id]?.sourceLabel, "SENT")
            email.copy(sourceLabel = merged)
        }
    }
}
