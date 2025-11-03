package com.fiveis.xend.ui.sent

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.model.MailListResponse
import com.fiveis.xend.data.repository.SentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class SentViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: SentRepository
    private lateinit var viewModel: SentViewModel

    private val mockEmail1 = EmailItem(
        id = "1",
        threadId = "t1",
        subject = "Test Email 1",
        fromEmail = "sender@test.com",
        snippet = "snippet1",
        date = "2025-01-01T10:00:00Z",
        dateRaw = "Wed, 1 Jan 2025 10:00:00 +0000",
        isUnread = false,
        labelIds = listOf("SENT")
    )

    private val mockEmail2 = EmailItem(
        id = "2",
        threadId = "t2",
        subject = "Test Email 2",
        fromEmail = "sender2@test.com",
        snippet = "snippet2",
        date = "2025-01-02T10:00:00Z",
        dateRaw = "Thu, 2 Jan 2025 10:00:00 +0000",
        isUnread = true,
        labelIds = listOf("SENT")
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_loads_cached_emails_from_repository() = runTest {
        val mockEmails = listOf(mockEmail1, mockEmail2)
        every { repository.getCachedEmails() } returns flowOf(mockEmails)
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success(null)

        viewModel = SentViewModel(repository)
        advanceUntilIdle()

        assertEquals(mockEmails, viewModel.uiState.value.emails)
        assertFalse(viewModel.uiState.value.isLoading)
        verify { repository.getCachedEmails() }
    }

    @Test
    fun init_performs_silent_refresh() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success(null)

        viewModel = SentViewModel(repository)
        advanceUntilIdle()

        coVerify { repository.refreshEmails(any(), any()) }
    }

    @Test
    fun refresh_emails_updates_loading_state() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success(null)

        viewModel = SentViewModel(repository)
        advanceUntilIdle()

        viewModel.refreshEmails()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun refresh_emails_success_updates_state() = runTest {
        every { repository.getCachedEmails() } returns flowOf(listOf(mockEmail1))
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success(null)

        viewModel = SentViewModel(repository)
        advanceUntilIdle()

        viewModel.refreshEmails()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRefreshing)
        assertNull(viewModel.uiState.value.error)
        coVerify(atLeast = 2) { repository.refreshEmails(any(), any()) }
    }

    @Test
    fun refresh_emails_failure_sets_error() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success(null)

        viewModel = SentViewModel(repository)
        advanceUntilIdle()

        coEvery { repository.refreshEmails(any(), any()) } returns Result.failure(Exception("Network error"))

        viewModel.refreshEmails()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("Network error") == true)
    }

    @Test
    fun load_more_emails_when_has_next_page() = runTest {
        val newEmails = listOf(mockEmail2)

        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success("nextToken")
        coEvery {
            repository.getMails(any(), any(), "nextToken", any())
        } returns Response.success(MailListResponse(newEmails, null, 0))
        coEvery { repository.saveEmailsToCache(any()) } returns Unit

        viewModel = SentViewModel(repository)
        advanceUntilIdle()

        assertEquals("nextToken", viewModel.uiState.value.loadMoreNextPageToken)

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        coVerify { repository.getMails(any(), any(), "nextToken", any()) }
        coVerify { repository.saveEmailsToCache(newEmails) }
    }

    @Test
    fun load_more_emails_when_no_next_page() = runTest {
        every { repository.getCachedEmails() } returns flowOf(listOf(mockEmail1))
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success(null)

        viewModel = SentViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        coVerify(exactly = 0) { repository.getMails(any(), any(), any(), any()) }
    }

    @Test
    fun load_more_emails_handles_api_error() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success("nextToken")
        coEvery {
            repository.getMails(any(), any(), "nextToken", any())
        } returns Response.error(500, "Server error".toResponseBody())

        viewModel = SentViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun load_more_emails_handles_exception() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success("nextToken")
        coEvery {
            repository.getMails(any(), any(), "nextToken", any())
        } throws RuntimeException("Network error")

        viewModel = SentViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("Network error") == true)
    }

    @Test
    fun load_more_emails_updates_next_page_token() = runTest {
        val batch1 = listOf(mockEmail2)

        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success("token1")
        coEvery {
            repository.getMails(any(), any(), "token1", any())
        } returns Response.success(MailListResponse(batch1, "token2", 0))
        coEvery { repository.saveEmailsToCache(any()) } returns Unit

        viewModel = SentViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        val batch2 = listOf(
            EmailItem(
                id = "3",
                threadId = "t3",
                subject = "Email 3",
                fromEmail = "sender3@test.com",
                snippet = "snippet3",
                date = "2025-01-03T10:00:00Z",
                dateRaw = "Fri, 3 Jan 2025 10:00:00 +0000",
                isUnread = false,
                labelIds = listOf("SENT")
            )
        )

        coEvery {
            repository.getMails(any(), any(), "token2", any())
        } returns Response.success(MailListResponse(batch2, null, 0))

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        coVerify { repository.getMails(any(), any(), "token2", any()) }
    }

    @Test
    fun ui_state_empty_list_by_default() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success(null)

        viewModel = SentViewModel(repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.emails.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun cached_emails_flow_updates_ui_state() = runTest {
        val initialEmails = listOf(mockEmail1)
        val updatedEmails = listOf(mockEmail1, mockEmail2)

        val emailsFlow = kotlinx.coroutines.flow.MutableStateFlow(initialEmails)
        every { repository.getCachedEmails() } returns emailsFlow
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success(null)

        viewModel = SentViewModel(repository)
        advanceUntilIdle()

        assertEquals(initialEmails, viewModel.uiState.value.emails)

        emailsFlow.value = updatedEmails
        advanceUntilIdle()

        assertEquals(updatedEmails, viewModel.uiState.value.emails)
    }

    @Test
    fun refresh_clears_previous_error() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success(null)

        viewModel = SentViewModel(repository)
        advanceUntilIdle()

        coEvery { repository.refreshEmails(any(), any()) } returns Result.failure(Exception("Error"))

        viewModel.refreshEmails()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)

        coEvery { repository.refreshEmails(any(), any()) } returns Result.success(null)

        viewModel.refreshEmails()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun multiple_refreshes_in_sequence() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success(null)

        viewModel = SentViewModel(repository)
        advanceUntilIdle()

        viewModel.refreshEmails()
        advanceUntilIdle()

        viewModel.refreshEmails()
        advanceUntilIdle()

        viewModel.refreshEmails()
        advanceUntilIdle()

        coVerify(exactly = 4) { repository.refreshEmails(any(), any()) }
    }

    @Test
    fun load_more_emails_handles_duplicate_emails() = runTest {
        val newEmailsWithDuplicate = listOf(
            mockEmail1,  // First
            mockEmail2   // Second
        )

        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success("nextToken")
        coEvery {
            repository.getMails(any(), any(), "nextToken", any())
        } returns Response.success(MailListResponse(newEmailsWithDuplicate, null, 0))
        coEvery { repository.saveEmailsToCache(any()) } returns Unit

        viewModel = SentViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        // Should save all emails received
        coVerify { repository.saveEmailsToCache(any()) }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun on_email_click_does_nothing() = runTest {
        every { repository.getCachedEmails() } returns flowOf(listOf(mockEmail1))
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success(null)

        viewModel = SentViewModel(repository)
        advanceUntilIdle()

        // Should not throw exception
        viewModel.onEmailClick(mockEmail1)
        advanceUntilIdle()

        // State should remain unchanged
        assertEquals(listOf(mockEmail1), viewModel.uiState.value.emails)
    }

    @Test
    fun load_more_emails_with_empty_response_body_updates_state() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success("nextToken")
        coEvery {
            repository.getMails(any(), any(), "nextToken", any())
        } returns Response.success(MailListResponse(emptyList(), null, 0))
        coEvery { repository.saveEmailsToCache(any()) } returns Unit

        viewModel = SentViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.loadMoreNextPageToken)
    }

    @Test
    fun refresh_emails_with_exception_in_catch_block() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success(null)

        viewModel = SentViewModel(repository)
        advanceUntilIdle()

        coEvery { repository.refreshEmails(any(), any()) } throws RuntimeException("Network failure")

        viewModel.refreshEmails()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("Network failure") == true)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun load_more_emails_keeps_existing_emails_in_state() = runTest {
        val initialEmails = listOf(mockEmail1)
        every { repository.getCachedEmails() } returns flowOf(initialEmails)
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success("token1")
        coEvery {
            repository.getMails(any(), any(), "token1", any())
        } returns Response.success(MailListResponse(listOf(mockEmail2), null, 0))
        coEvery { repository.saveEmailsToCache(any()) } returns Unit

        viewModel = SentViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        // After loadMore, initial emails should still be in cached flow
        assertEquals(initialEmails, viewModel.uiState.value.emails)
    }
}
