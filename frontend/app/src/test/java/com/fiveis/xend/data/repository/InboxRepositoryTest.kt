package com.fiveis.xend.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.model.MailListResponse
import com.fiveis.xend.network.MailApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

class InboxRepositoryTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var mailApiService: MailApiService
    private lateinit var emailDao: EmailDao
    private lateinit var repository: InboxRepository

    @Before
    fun setup() {
        mailApiService = mockk()
        emailDao = mockk(relaxed = true)
        repository = InboxRepository(mailApiService, emailDao)
    }

    @Test
    fun get_cached_emails_returns_flow_from_dao() = runTest {
        val mockEmails = listOf(
            createMockEmailItem("1"),
            createMockEmailItem("2")
        )
        every { emailDao.getEmailsByLabel("INBOX") } returns flowOf(mockEmails)

        val result = repository.getCachedEmails()

        coVerify { emailDao.getEmailsByLabel("INBOX") }
    }

    @Test
    fun get_mails_calls_api_service_with_correct_parameters() = runTest {
        val mockResponse = Response.success(
            MailListResponse(
                messages = emptyList(),
                nextPageToken = null,
                resultSizeEstimate = 0
            )
        )
        coEvery { mailApiService.getEmails("INBOX", 20, null) } returns mockResponse

        val result = repository.getMails("INBOX", 20, null)

        assertEquals(mockResponse, result)
        coVerify { mailApiService.getEmails("INBOX", 20, null) }
    }

    @Test
    fun refresh_emails_with_empty_db_fetches_first_page_successfully() = runTest {
        val mockEmails = listOf(
            createMockEmailItem("1"),
            createMockEmailItem("2")
        )
        val mockResponse = Response.success(
            MailListResponse(
                messages = mockEmails,
                nextPageToken = "token123",
                resultSizeEstimate = 2
            )
        )

        coEvery { emailDao.getLatestEmailDate() } returns null
        coEvery { mailApiService.getEmails("INBOX", 20, null, null) } returns mockResponse
        coEvery { emailDao.insertEmails(any()) } returns Unit
        coEvery { emailDao.getEmailCount() } returns 2

        val result = repository.refreshEmails("INBOX", 20)

        assertTrue(result.isSuccess)
        assertEquals("token123", result.getOrNull())
        coVerify { emailDao.insertEmails(mockEmails) }
    }

    @Test
    fun refresh_emails_with_empty_db_returns_null_when_no_messages() = runTest {
        val mockResponse = Response.success(
            MailListResponse(
                messages = emptyList(),
                nextPageToken = null,
                resultSizeEstimate = 0
            )
        )

        coEvery { emailDao.getLatestEmailDate() } returns null
        coEvery { mailApiService.getEmails("INBOX", 20, null, null) } returns mockResponse

        val result = repository.refreshEmails("INBOX", 20)

        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrNull())
        coVerify(exactly = 0) { emailDao.insertEmails(any()) }
    }

    @Test
    fun refresh_emails_returns_failure_when_api_call_fails() = runTest {
        val mockResponse = Response.error<MailListResponse>(
            500,
            "Server error".toResponseBody()
        )

        coEvery { emailDao.getLatestEmailDate() } returns null
        coEvery { mailApiService.getEmails("INBOX", 20, null, null) } returns mockResponse

        val result = repository.refreshEmails("INBOX", 20)

        assertTrue(result.isFailure)
    }

    @Test
    fun refresh_emails_returns_failure_when_response_body_is_null() = runTest {
        val mockResponse = Response.success<MailListResponse>(null)

        coEvery { emailDao.getLatestEmailDate() } returns null
        coEvery { mailApiService.getEmails("INBOX", 20, null, null) } returns mockResponse

        val result = repository.refreshEmails("INBOX", 20)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Response body is null") == true)
    }

    @Test
    fun refresh_emails_with_existing_emails_fetches_new_ones_until_overlap() = runTest {
        val latestDate = "2025-01-01T00:00:00Z"
        val newEmails = listOf(
            createMockEmailItem("1"),
            createMockEmailItem("2")
        )

        val firstResponse = Response.success(
            MailListResponse(
                messages = newEmails,
                nextPageToken = "token1",
                resultSizeEstimate = 2
            )
        )
        val secondResponse = Response.success(
            MailListResponse(
                messages = emptyList(),
                nextPageToken = null,
                resultSizeEstimate = 0
            )
        )

        coEvery { emailDao.getLatestEmailDate() } returns latestDate
        coEvery { mailApiService.getEmails("INBOX", 20, null, latestDate) } returns firstResponse
        coEvery { mailApiService.getEmails("INBOX", 20, "token1", latestDate) } returns secondResponse
        coEvery { emailDao.insertEmails(any()) } returns Unit
        coEvery { emailDao.getEmailCount() } returns 2

        val result = repository.refreshEmails("INBOX", 20)

        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrNull())
        coVerify(atLeast = 1) { emailDao.insertEmails(newEmails) }
    }

    @Test
    fun refresh_emails_stops_fetching_when_no_more_pages() = runTest {
        val latestDate = "2025-01-01T00:00:00Z"
        val newEmails = listOf(createMockEmailItem("1"))

        val response = Response.success(
            MailListResponse(
                messages = newEmails,
                nextPageToken = null,
                resultSizeEstimate = 1
            )
        )

        coEvery { emailDao.getLatestEmailDate() } returns latestDate
        coEvery { mailApiService.getEmails("INBOX", 20, null, latestDate) } returns response
        coEvery { emailDao.insertEmails(any()) } returns Unit
        coEvery { emailDao.getEmailCount() } returns 1

        val result = repository.refreshEmails("INBOX", 20)

        assertTrue(result.isSuccess)
        assertEquals(null, result.getOrNull())
        coVerify(exactly = 1) { mailApiService.getEmails(any(), any(), any(), any()) }
    }

    @Test
    fun update_read_status_calls_dao_with_correct_parameters() = runTest {
        coEvery { emailDao.updateReadStatus("1", false) } returns Unit

        repository.updateReadStatus("1", false)

        coVerify { emailDao.updateReadStatus("1", false) }
    }

    @Test
    fun save_emails_to_cache_inserts_emails_to_dao() = runTest {
        val mockEmails = listOf(
            createMockEmailItem("1"),
            createMockEmailItem("2")
        )
        coEvery { emailDao.insertEmails(mockEmails) } returns Unit
        coEvery { emailDao.getEmailCount() } returns 2

        repository.saveEmailsToCache(mockEmails)

        coVerify { emailDao.insertEmails(mockEmails) }
        coVerify { emailDao.getEmailCount() }
    }

    private fun createMockEmailItem(id: String) = EmailItem(
        id = id,
        threadId = "thread_$id",
        subject = "Subject $id",
        fromEmail = "sender$id@example.com",
        snippet = "Snippet $id",
        date = "2025-01-01T00:00:00Z",
        dateRaw = "Wed, 1 Jan 2025 00:00:00 +0000",
        isUnread = true,
        labelIds = listOf("INBOX"),
        cachedAt = System.currentTimeMillis()
    )
}
