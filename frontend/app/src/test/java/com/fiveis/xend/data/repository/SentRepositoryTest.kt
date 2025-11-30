package com.fiveis.xend.data.repository

import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.model.MailListResponse
import com.fiveis.xend.data.model.ReadStatusUpdateRequest
import com.fiveis.xend.data.model.ReadStatusUpdateResponse
import com.fiveis.xend.network.MailApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class SentRepositoryTest {

    private lateinit var mailApiService: MailApiService
    private lateinit var emailDao: EmailDao
    private lateinit var repository: SentRepository

    @Before
    fun setup() {
        mailApiService = mockk()
        emailDao = mockk(relaxed = true)
        repository = SentRepository(mailApiService, emailDao)
    }

    @Test
    fun get_cached_emails_returns_flow_from_dao() = runTest {
        val mockEmails = listOf(
            EmailItem(
                id = "1",
                threadId = "t1",
                subject = "Test 1",
                fromEmail = "sender@test.com",
                snippet = "snippet",
                date = "2025-01-01",
                dateRaw = "raw",
                isUnread = false,
                labelIds = listOf("SENT")
            )
        )

        every { emailDao.getEmailsByLabel("SENT") } returns flowOf(mockEmails)

        val flow = repository.getCachedEmails()
        var result: List<EmailItem>? = null
        flow.collect { result = it }

        assertEquals(1, result?.size)
        assertEquals("1", result?.get(0)?.id)
        verify { emailDao.getEmailsByLabel("SENT") }
    }

    @Test
    fun get_mails_calls_api_with_sent_label() = runTest {
        val mockResponse = Response.success(MailListResponse(emptyList(), null, 0))
        coEvery { mailApiService.getEmails("SENT", 20, null, null) } returns mockResponse

        val response = repository.getMails()

        assertTrue(response.isSuccessful)
        coVerify { mailApiService.getEmails("SENT", 20, null, null) }
    }

    @Test
    fun get_mails_with_custom_parameters() = runTest {
        val mockResponse = Response.success(MailListResponse(emptyList(), null, 0))
        coEvery { mailApiService.getEmails("SENT", 50, "token123", "2025-01-01") } returns mockResponse

        val response = repository.getMails(
            labels = "SENT",
            maxResults = 50,
            pageToken = "token123",
            sinceDate = "2025-01-01"
        )

        assertTrue(response.isSuccessful)
        coVerify { mailApiService.getEmails("SENT", 50, "token123", "2025-01-01") }
    }

    @Test
    fun refresh_emails_with_empty_db_fetches_first_page_successfully() = runTest {
        val mockEmails = listOf(
            EmailItem(
                id = "1",
                threadId = "t1",
                subject = "Test",
                fromEmail = "sender@test.com",
                snippet = "snippet",
                date = "2025-01-01",
                dateRaw = "Wed, 1 Jan 2025 00:00:00 +0000",
                isUnread = false,
                labelIds = listOf("SENT")
            )
        )

        coEvery { emailDao.getLatestEmailDate() } returns null
        coEvery { mailApiService.getEmails("SENT", 20, null, null) } returns Response.success(
            MailListResponse(mockEmails, "nextToken", 0)
        )
        coEvery { emailDao.insertEmails(any()) } returns Unit
        coEvery { emailDao.getEmailCount() } returns 1

        val result = repository.refreshEmails()

        assertTrue(result.isSuccess)
        assertEquals("nextToken", result.getOrNull())
        coVerify { emailDao.insertEmails(any()) }
    }

    @Test
    fun refresh_emails_with_empty_db_returns_null_when_no_messages() = runTest {
        coEvery { emailDao.getLatestEmailDate() } returns null
        coEvery { mailApiService.getEmails("SENT", 20, null, null) } returns Response.success(
            MailListResponse(emptyList(), null, 0)
        )

        val result = repository.refreshEmails()

        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrNull())
        coVerify(exactly = 0) { emailDao.insertEmails(any()) }
    }

    @Test
    fun refresh_emails_returns_failure_when_response_body_is_null() = runTest {
        coEvery { emailDao.getLatestEmailDate() } returns null
        coEvery { mailApiService.getEmails("SENT", 20, null, null) } returns Response.success(null)

        val result = repository.refreshEmails()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Response body is null") == true)
    }

    @Test
    fun refresh_emails_with_existing_emails_fetches_new_ones_until_overlap() = runTest {
        val newEmail1 = EmailItem(
            id = "new1",
            threadId = "t1",
            subject = "New Email 1",
            fromEmail = "sender@test.com",
            snippet = "snippet",
            date = "2025-01-02",
            dateRaw = "Thu, 2 Jan 2025 00:00:00 +0000",
            isUnread = true,
            labelIds = listOf("SENT")
        )

        val newEmail2 = EmailItem(
            id = "new2",
            threadId = "t2",
            subject = "New Email 2",
            fromEmail = "sender2@test.com",
            snippet = "snippet2",
            date = "2025-01-03",
            dateRaw = "Fri, 3 Jan 2025 00:00:00 +0000",
            isUnread = true,
            labelIds = listOf("SENT")
        )

        coEvery { emailDao.getLatestEmailDate() } returns "2025-01-01"
        coEvery {
            mailApiService.getEmails("SENT", 20, null, "2025-01-01")
        } returns Response.success(MailListResponse(listOf(newEmail1), "token1", 0))

        coEvery {
            mailApiService.getEmails("SENT", 20, "token1", "2025-01-01")
        } returns Response.success(MailListResponse(listOf(newEmail2), null, 0))

        coEvery { emailDao.insertEmails(any()) } returns Unit
        coEvery { emailDao.getEmailCount() } returns 3

        val result = repository.refreshEmails()

        assertTrue(result.isSuccess)
        coVerify(exactly = 2) { emailDao.insertEmails(any()) }
    }

    @Test
    fun refresh_emails_stops_fetching_when_no_more_pages() = runTest {
        val newEmail = EmailItem(
            id = "new1",
            threadId = "t1",
            subject = "New Email",
            fromEmail = "sender@test.com",
            snippet = "snippet",
            date = "2025-01-02",
            dateRaw = "Thu, 2 Jan 2025 00:00:00 +0000",
            isUnread = true,
            labelIds = listOf("SENT")
        )

        coEvery { emailDao.getLatestEmailDate() } returns "2025-01-01"
        coEvery {
            mailApiService.getEmails("SENT", 20, null, "2025-01-01")
        } returns Response.success(MailListResponse(listOf(newEmail), null, 0))

        coEvery { emailDao.insertEmails(any()) } returns Unit
        coEvery { emailDao.getEmailCount() } returns 2

        val result = repository.refreshEmails()

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { mailApiService.getEmails(any(), any(), any(), any()) }
        coVerify { emailDao.insertEmails(any()) }
    }

    @Test
    fun refresh_emails_returns_failure_on_api_error() = runTest {
        coEvery { emailDao.getLatestEmailDate() } returns null
        coEvery {
            mailApiService.getEmails("SENT", 20, null, null)
        } returns Response.error(500, "Server error".toResponseBody())

        val result = repository.refreshEmails()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Failed to fetch emails: 500") == true)
    }

    @Test
    fun refresh_emails_with_existing_emails_returns_failure_on_api_error() = runTest {
        coEvery { emailDao.getLatestEmailDate() } returns "2025-01-01"
        coEvery {
            mailApiService.getEmails("SENT", 20, null, "2025-01-01")
        } returns Response.error(404, "Not found".toResponseBody())

        val result = repository.refreshEmails()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Failed to fetch emails: 404") == true)
    }

    @Test
    fun refresh_emails_with_exception_returns_failure() = runTest {
        coEvery { emailDao.getLatestEmailDate() } throws RuntimeException("Database error")

        val result = repository.refreshEmails()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
        assertTrue(result.exceptionOrNull()?.message?.contains("Database error") == true)
    }

    @Test
    fun get_mail_calls_api_service() = runTest {
        val messageId = "msg123"
        val mockResponse = Response.success(mockk<com.fiveis.xend.data.model.MailDetailResponse>())

        coEvery { mailApiService.getMail(messageId) } returns mockResponse

        val response = repository.getMail(messageId)

        assertTrue(response.isSuccessful)
        coVerify { mailApiService.getMail(messageId) }
    }

    @Test
    fun update_read_status_updates_dao() = runTest {
        val emailId = "email123"

        coEvery {
            mailApiService.updateReadStatus(emailId, ReadStatusUpdateRequest(isRead = true))
        } returns Response.success(ReadStatusUpdateResponse(id = emailId, labelIds = emptyList()))
        coEvery { emailDao.updateReadStatus(emailId, false) } returns Unit

        repository.updateReadStatus(emailId, false)

        coVerify { mailApiService.updateReadStatus(emailId, ReadStatusUpdateRequest(isRead = true)) }
        coVerify { emailDao.updateReadStatus(emailId, false) }
    }

    @Test
    fun update_read_status_with_unread_true() = runTest {
        val emailId = "email456"

        coEvery {
            mailApiService.updateReadStatus(emailId, ReadStatusUpdateRequest(isRead = false))
        } returns Response.success(ReadStatusUpdateResponse(id = emailId, labelIds = emptyList()))
        coEvery { emailDao.updateReadStatus(emailId, true) } returns Unit

        repository.updateReadStatus(emailId, true)

        coVerify { mailApiService.updateReadStatus(emailId, ReadStatusUpdateRequest(isRead = false)) }
        coVerify { emailDao.updateReadStatus(emailId, true) }
    }

    @Test
    fun save_emails_to_cache_inserts_into_dao() = runTest {
        val emails = listOf(
            EmailItem(
                id = "1",
                threadId = "t1",
                subject = "Test",
                fromEmail = "sender@test.com",
                snippet = "snippet",
                date = "2025-01-01",
                dateRaw = "Wed, 1 Jan 2025 00:00:00 +0000",
                isUnread = false,
                labelIds = listOf("SENT")
            ),
            EmailItem(
                id = "2",
                threadId = "t2",
                subject = "Test 2",
                fromEmail = "sender2@test.com",
                snippet = "snippet2",
                date = "2025-01-02",
                dateRaw = "Thu, 2 Jan 2025 00:00:00 +0000",
                isUnread = true,
                labelIds = listOf("SENT")
            )
        )

        coEvery { emailDao.insertEmails(any()) } returns Unit
        coEvery { emailDao.getEmailCount() } returns 2

        repository.saveEmailsToCache(emails)

        coVerify { emailDao.insertEmails(any()) }
        coVerify { emailDao.getEmailCount() }
    }

    @Test
    fun save_emails_to_cache_with_empty_list() = runTest {
        coEvery { emailDao.getEmailCount() } returns 0

        repository.saveEmailsToCache(emptyList())

        coVerify { emailDao.insertEmails(emptyList()) }
    }

    @Test
    fun refresh_emails_with_custom_labels() = runTest {
        val mockEmails = listOf(
            EmailItem(
                id = "1",
                threadId = "t1",
                subject = "Test",
                fromEmail = "sender@test.com",
                snippet = "snippet",
                date = "2025-01-01",
                dateRaw = "raw",
                isUnread = false,
                labelIds = listOf("CUSTOM_LABEL")
            )
        )

        coEvery { emailDao.getLatestEmailDate() } returns null
        coEvery { mailApiService.getEmails("CUSTOM_LABEL", 50, null, null) } returns Response.success(
            MailListResponse(mockEmails, null, 0)
        )
        coEvery { emailDao.getEmailCount() } returns 1

        val result = repository.refreshEmails(labels = "CUSTOM_LABEL", maxResults = 50)

        assertTrue(result.isSuccess)
        coVerify { mailApiService.getEmails("CUSTOM_LABEL", 50, null, null) }
    }

    @Test
    fun refresh_emails_handles_pagination_correctly() = runTest {
        val batch1 = listOf(
            EmailItem(
                id = "1",
                threadId = "t1",
                subject = "Email 1",
                fromEmail = "sender@test.com",
                snippet = "snippet1",
                date = "2025-01-02",
                dateRaw = "Thu, 2 Jan 2025 00:00:00 +0000",
                isUnread = true,
                labelIds = listOf("SENT")
            )
        )

        val batch2 = listOf(
            EmailItem(
                id = "2",
                threadId = "t2",
                subject = "Email 2",
                fromEmail = "sender@test.com",
                snippet = "snippet2",
                date = "2025-01-03",
                dateRaw = "Fri, 3 Jan 2025 00:00:00 +0000",
                isUnread = true,
                labelIds = listOf("SENT")
            )
        )

        val batch3 = listOf(
            EmailItem(
                id = "3",
                threadId = "t3",
                subject = "Email 3",
                fromEmail = "sender@test.com",
                snippet = "snippet3",
                date = "2025-01-04",
                dateRaw = "Sat, 4 Jan 2025 00:00:00 +0000",
                isUnread = true,
                labelIds = listOf("SENT")
            )
        )

        coEvery { emailDao.getLatestEmailDate() } returns "2025-01-01"
        coEvery {
            mailApiService.getEmails("SENT", 20, null, "2025-01-01")
        } returns Response.success(MailListResponse(batch1, "token1", 0))

        coEvery {
            mailApiService.getEmails("SENT", 20, "token1", "2025-01-01")
        } returns Response.success(MailListResponse(batch2, "token2", 0))

        coEvery {
            mailApiService.getEmails("SENT", 20, "token2", "2025-01-01")
        } returns Response.success(MailListResponse(batch3, null, 0))

        coEvery { emailDao.insertEmails(any()) } returns Unit
        coEvery { emailDao.getEmailCount() } returns 4

        val result = repository.refreshEmails()

        assertTrue(result.isSuccess)
        coVerify(exactly = 3) { mailApiService.getEmails(any(), any(), any(), any()) }
        coVerify(exactly = 3) { emailDao.insertEmails(any()) }
    }
}
