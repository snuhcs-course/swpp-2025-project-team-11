package com.fiveis.xend.ui.inbox

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.model.MailListResponse
import com.fiveis.xend.data.repository.InboxRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
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
class InboxViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: InboxRepository
    private lateinit var viewModel: InboxViewModel

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
    fun init_loads_cached_emails() = runTest {
        val mockEmails = listOf(
            createMockEmailItem("1"),
            createMockEmailItem("2")
        )
        every { repository.getCachedEmails() } returns flowOf(mockEmails)
        coEvery { repository.refreshEmails() } returns Result.success(null)

        viewModel = InboxViewModel(repository)
        advanceUntilIdle()

        assertEquals(mockEmails, viewModel.uiState.value.emails)
    }

    @Test
    fun refresh_emails_success_updates_state() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails() } returns Result.success("token123")

        viewModel = InboxViewModel(repository)
        advanceUntilIdle()

        assertEquals("token123", viewModel.uiState.value.loadMoreNextPageToken)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun refresh_emails_failure_sets_error() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails() } returns Result.failure(Exception("Network error"))

        viewModel = InboxViewModel(repository)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("Network error") == true)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun load_more_emails_success_saves_to_cache() = runTest {
        val newEmails = listOf(createMockEmailItem("3"))
        val mockResponse = Response.success(
            MailListResponse(
                messages = newEmails,
                nextPageToken = "token456",
                resultSizeEstimate = 1
            )
        )

        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails() } returns Result.success("token123")
        coEvery { repository.getMails(pageToken = "token123") } returns mockResponse
        coEvery { repository.saveEmailsToCache(newEmails) } returns Unit

        viewModel = InboxViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        coVerify { repository.saveEmailsToCache(newEmails) }
        assertEquals("token456", viewModel.uiState.value.loadMoreNextPageToken)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun load_more_emails_without_token_does_nothing() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails() } returns Result.success(null)

        viewModel = InboxViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.getMails(any(), any(), any()) }
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
