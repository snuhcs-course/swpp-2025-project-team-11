package com.fiveis.xend.data.repository

import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.data.model.AttachmentAnalysisRequest
import com.fiveis.xend.data.model.AttachmentAnalysisResponse
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.model.MailDetailResponse
import com.fiveis.xend.data.model.MailListResponse
import com.fiveis.xend.network.MailApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.util.Date

class BaseMailRepositoryTest {

    private lateinit var mailApiService: MailApiService
    private lateinit var emailDao: EmailDao
    private lateinit var repository: TestMailRepository

    @Before
    fun setup() {
        mailApiService = mockk(relaxed = true)
        emailDao = mockk(relaxed = true)
        repository = TestMailRepository(mailApiService, emailDao, "INBOX", "TestRepo")
    }

    @Test
    fun test_getCachedEmails_returnsFlowFromDao() = runTest {
        val mockEmails = listOf(
            EmailItem(
                id = "1",
                threadId = "thread1",
                subject = "Test",
                fromEmail = "test@example.com",
                snippet = "Test email",
                date = "2025-11-19",
                dateRaw = "Wed, 19 Nov 2025 10:00:00 +0000",
                isUnread = true,
                labelIds = listOf("INBOX")
            )
        )
        every { emailDao.getEmailsByLabel("INBOX") } returns flowOf(mockEmails)

        val result = repository.getCachedEmails().first()

        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
    }

    @Test
    fun test_getMails_callsApiService() = runTest {
        val mockResponse = Response.success(
            MailListResponse(
                messages = emptyList(),
                nextPageToken = null,
                resultSizeEstimate = 0
            )
        )
        coEvery { mailApiService.getEmails(any(), any(), any(), any()) } returns mockResponse

        val result = repository.getMails(labels = "INBOX", maxResults = 20)

        assertTrue(result.isSuccessful)
        coVerify { mailApiService.getEmails("INBOX", 20, null, null) }
    }

    @Test
    fun test_refreshEmails_emptyDb_fetchesFirstPage() = runTest {
        val mockEmails = listOf(
            EmailItem(
                id = "1",
                threadId = "thread1",
                subject = "Test",
                fromEmail = "test@example.com",
                snippet = "Test",
                date = "2025-11-19",
                dateRaw = "Wed, 19 Nov 2025 10:00:00 +0000",
                isUnread = true,
                labelIds = listOf("INBOX")
            )
        )
        val mockResponse = Response.success(
            MailListResponse(
                messages = mockEmails,
                nextPageToken = "token123",
                resultSizeEstimate = 1
            )
        )

        coEvery { emailDao.getLatestEmailDate("INBOX") } returns null
        coEvery { mailApiService.getEmails(any(), any(), any(), any()) } returns mockResponse
        coEvery { emailDao.insertEmails(any()) } returns Unit
        coEvery { emailDao.getEmailCount() } returns 1

        val result = repository.refreshEmails()

        assertTrue(result.isSuccess)
        assertEquals("token123", result.getOrNull())
        coVerify {
            emailDao.insertEmails(
                match { inserted ->
                    inserted.size == mockEmails.size && inserted.all { it.sourceLabel == "INBOX" }
                }
            )
        }
    }

    @Test
    fun test_refreshEmails_withExistingData_usesSinceDate() = runTest {
        val latestDate = "2024-01-01"
        val mockEmails = listOf(
            EmailItem(
                id = "2",
                threadId = "thread2",
                subject = "New",
                fromEmail = "test@example.com",
                snippet = "New email",
                date = "2025-11-19",
                dateRaw = "Wed, 19 Nov 2025 10:00:00 +0000",
                isUnread = true,
                labelIds = listOf("INBOX")
            )
        )
        val mockResponse = Response.success(
            MailListResponse(
                messages = mockEmails,
                nextPageToken = null,
                resultSizeEstimate = 1
            )
        )

        coEvery { emailDao.getLatestEmailDate("INBOX") } returns latestDate
        coEvery { mailApiService.getEmails(any(), any(), any(), any()) } returns mockResponse
        coEvery { emailDao.insertEmails(any()) } returns Unit
        coEvery { emailDao.getEmailCount() } returns 2

        val result = repository.refreshEmails()

        assertTrue(result.isSuccess)
        coVerify { mailApiService.getEmails("INBOX", 20, null, latestDate) }
        coVerify {
            emailDao.insertEmails(
                match { inserted ->
                    inserted.size == mockEmails.size && inserted.all { it.sourceLabel == "INBOX" }
                }
            )
        }
    }

    @Test
    fun test_refreshEmails_apiFailure_returnsFailure() = runTest {
        val failureResponse = Response.error<MailListResponse>(
            404,
            ResponseBody.create(null, "Not found")
        )

        coEvery { emailDao.getLatestEmailDate("INBOX") } returns null
        coEvery { mailApiService.getEmails(any(), any(), any(), any()) } returns failureResponse

        val result = repository.refreshEmails()

        assertTrue(result.isFailure)
    }

    @Test
    fun test_getMail_callsApiService() = runTest {
        val mockResponse = Response.success(
            MailDetailResponse(
                id = "1",
                threadId = "thread1",
                subject = "Test",
                fromEmail = "test@example.com",
                toEmail = "user@example.com",
                to = "user@example.com",
                date = "2024-01-01",
                dateRaw = "2024-01-01",
                body = "Test body",
                snippet = "Test",
                isUnread = true,
                labelIds = emptyList(),
                attachments = emptyList()
            )
        )
        coEvery { mailApiService.getMail("1") } returns mockResponse

        val result = repository.getMail("1")

        assertTrue(result.isSuccessful)
        coVerify { mailApiService.getMail("1") }
    }

    @Test
    fun test_downloadAttachment_callsApiService() = runTest {
        val mockResponse = Response.success(ResponseBody.create(null, "file content"))
        coEvery {
            mailApiService.downloadAttachment(any(), any(), any(), any())
        } returns mockResponse

        val result = repository.downloadAttachment("msg1", "att1", "file.pdf", "application/pdf")

        assertTrue(result.isSuccessful)
        coVerify { mailApiService.downloadAttachment("msg1", "att1", "file.pdf", "application/pdf") }
    }

    @Test
    fun test_analyzeAttachment_callsApiService() = runTest {
        val mockResponse = Response.success(
            AttachmentAnalysisResponse(
                summary = "Safe file",
                insights = "No threats found.",
                mailGuide = "This email is safe to open."
            )
        )
        coEvery { mailApiService.analyzeAttachment(any()) } returns mockResponse

        val result = repository.analyzeAttachment("msg1", "att1", "file.pdf", "application/pdf")

        assertTrue(result.isSuccessful)
        coVerify {
            mailApiService.analyzeAttachment(
                match {
                    it.messageId == "msg1" &&
                        it.attachmentId == "att1" &&
                        it.filename == "file.pdf" &&
                        it.mimeType == "application/pdf"
                }
            )
        }
    }

    @Test
    fun test_updateReadStatus_callsDao() = runTest {
        coEvery { emailDao.updateReadStatus(any(), any()) } returns Unit

        repository.updateReadStatus("1", false)

        coVerify { emailDao.updateReadStatus("1", false) }
    }

    @Test
    fun test_saveEmailsToCache_insertsToDao() = runTest {
        val emails = listOf(
            EmailItem(
                id = "1",
                threadId = "thread1",
                subject = "Test",
                fromEmail = "test@example.com",
                snippet = "Test",
                date = "2025-11-19",
                dateRaw = "Wed, 19 Nov 2025 10:00:00 +0000",
                isUnread = true,
                labelIds = listOf("INBOX")
            )
        )
        coEvery { emailDao.insertEmails(any()) } returns Unit
        coEvery { emailDao.getEmailCount() } returns 1

        repository.saveEmailsToCache(emails)

        coVerify {
            emailDao.insertEmails(
                match { inserted ->
                    inserted.size == emails.size && inserted.all { it.sourceLabel == "INBOX" }
                }
            )
        }
    }

    @Test
    fun test_refreshEmails_paginationLoop_stopsOnDuplicateToken() = runTest {
        val latestDate = "2024-01-01"
        val mockEmails = listOf(
            EmailItem(
                id = "2",
                threadId = "thread2",
                subject = "New",
                fromEmail = "test@example.com",
                snippet = "New email",
                date = "2025-11-19",
                dateRaw = "Wed, 19 Nov 2025 10:00:00 +0000",
                isUnread = true,
                labelIds = listOf("INBOX")
            )
        )

        // First response with nextPageToken
        val firstResponse = Response.success(
            MailListResponse(
                messages = mockEmails,
                nextPageToken = "sameToken",
                resultSizeEstimate = 1
            )
        )

        // Second response with same nextPageToken (should break loop)
        val secondResponse = Response.success(
            MailListResponse(
                messages = emptyList(),
                nextPageToken = "sameToken",
                resultSizeEstimate = 0
            )
        )

        coEvery { emailDao.getLatestEmailDate("INBOX") } returns latestDate
        coEvery { emailDao.insertEmails(any()) } returns Unit
        coEvery { emailDao.getEmailCount() } returns 1
        coEvery {
            mailApiService.getEmails("INBOX", 20, null, latestDate)
        } returns firstResponse
        coEvery {
            mailApiService.getEmails("INBOX", 20, "sameToken", latestDate)
        } returns secondResponse

        val result = repository.refreshEmails()

        assertTrue(result.isSuccess)
        coVerify(exactly = 2) { mailApiService.getEmails(any(), any(), any(), any()) }
    }

    // Concrete implementation for testing
    private class TestMailRepository(
        mailApiService: MailApiService,
        emailDao: EmailDao,
        label: String,
        logTag: String
    ) : BaseMailRepository(mailApiService, emailDao, label, logTag)
}
