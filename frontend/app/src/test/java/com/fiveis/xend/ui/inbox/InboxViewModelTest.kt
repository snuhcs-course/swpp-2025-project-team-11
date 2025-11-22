package com.fiveis.xend.ui.inbox

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.model.MailListResponse
import com.fiveis.xend.data.repository.ContactBookRepository
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
    private lateinit var contactRepository: ContactBookRepository
    private lateinit var viewModel: InboxViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        contactRepository = mockk()
        every { contactRepository.observeGroups() } returns flowOf(emptyList())
        every { contactRepository.observeContacts() } returns flowOf(emptyList())
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

        viewModel = InboxViewModel(repository, contactRepository)
        advanceUntilIdle()

        assertEquals(mockEmails, viewModel.uiState.value.emails)
    }

    @Test
    fun refresh_emails_success_updates_state() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails() } returns Result.success("token123")

        viewModel = InboxViewModel(repository, contactRepository)
        advanceUntilIdle()

        assertEquals("token123", viewModel.uiState.value.loadMoreNextPageToken)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun refresh_emails_failure_sets_error() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails() } returns Result.failure(Exception("Network error"))

        viewModel = InboxViewModel(repository, contactRepository)
        advanceUntilIdle()

        // Silent refresh in init doesn't set error (showLoading=false)
        // So we need to explicitly call refreshEmails() to test error handling
        viewModel.refreshEmails()
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

        viewModel = InboxViewModel(repository, contactRepository)
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

        viewModel = InboxViewModel(repository, contactRepository)
        advanceUntilIdle()

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.getMails(any(), any(), any()) }
    }

    @Test
    fun load_more_emails_failure_sets_error() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails() } returns Result.success("token123")
        coEvery { repository.getMails(pageToken = "token123") } throws Exception("Load more failed")

        viewModel = InboxViewModel(repository, contactRepository)
        advanceUntilIdle()

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun initial_state_is_correct() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails() } returns Result.success(null)

        viewModel = InboxViewModel(repository, contactRepository)

        assertTrue(viewModel.uiState.value.emails.isEmpty())
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun refresh_emails_sets_refreshing_state() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails() } returns Result.success(null)

        viewModel = InboxViewModel(repository, contactRepository)
        advanceUntilIdle()

        viewModel.refreshEmails()

        // After completion
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun load_more_emails_with_empty_response() = runTest {
        val mockResponse = Response.success(
            MailListResponse(
                messages = emptyList(),
                nextPageToken = null,
                resultSizeEstimate = 0
            )
        )

        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails() } returns Result.success("token123")
        coEvery { repository.getMails(pageToken = "token123") } returns mockResponse

        viewModel = InboxViewModel(repository, contactRepository)
        advanceUntilIdle()

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.loadMoreNextPageToken)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun multiple_refresh_calls_handled_correctly() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails() } returns Result.success(null)

        viewModel = InboxViewModel(repository, contactRepository)
        advanceUntilIdle()

        viewModel.refreshEmails()
        viewModel.refreshEmails()
        viewModel.refreshEmails()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRefreshing)
        coVerify(atLeast = 3) { repository.refreshEmails() }
    }

    @Test
    fun cached_emails_update_triggers_ui_update() = runTest {
        val mockEmails1 = listOf(createMockEmailItem("1"))
        val mockEmails2 = listOf(createMockEmailItem("1"), createMockEmailItem("2"))

        every { repository.getCachedEmails() } returns flowOf(mockEmails1, mockEmails2)
        coEvery { repository.refreshEmails() } returns Result.success(null)

        viewModel = InboxViewModel(repository, contactRepository)
        advanceUntilIdle()

        // Should have received the flow updates
        assertTrue(viewModel.uiState.value.emails.isNotEmpty())
    }

    @Test
    fun load_more_emails_updates_page_token() = runTest {
        val newEmails = listOf(createMockEmailItem("3"))
        val mockResponse = Response.success(
            MailListResponse(
                messages = newEmails,
                nextPageToken = "newToken",
                resultSizeEstimate = 1
            )
        )

        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails() } returns Result.success("token123")
        coEvery { repository.getMails(pageToken = "token123") } returns mockResponse
        coEvery { repository.saveEmailsToCache(newEmails) } returns Unit

        viewModel = InboxViewModel(repository, contactRepository)
        advanceUntilIdle()

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        assertEquals("newToken", viewModel.uiState.value.loadMoreNextPageToken)
    }

    @Test
    fun error_state_cleared_on_successful_refresh() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails() } returns Result.success(null)

        viewModel = InboxViewModel(repository, contactRepository)
        advanceUntilIdle()

        // First, set an error
        coEvery { repository.refreshEmails() } returns Result.failure(Exception("Error"))
        viewModel.refreshEmails()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)

        // Then refresh successfully
        coEvery { repository.refreshEmails() } returns Result.success(null)
        viewModel.refreshEmails()
        advanceUntilIdle()

        // Error should be null after successful refresh
        assertNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun load_more_emails_null_response_handled() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails() } returns Result.success("token123")
        coEvery { repository.getMails(pageToken = "token123") } returns Response.success(null)

        viewModel = InboxViewModel(repository, contactRepository)
        advanceUntilIdle()

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun refresh_emails_with_token_returned() = runTest {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails() } returns Result.success("nextToken")

        viewModel = InboxViewModel(repository, contactRepository)
        advanceUntilIdle()

        assertEquals("nextToken", viewModel.uiState.value.loadMoreNextPageToken)
    }

    @Test
    fun load_more_with_unsuccessful_response() = runTest {
        val mockResponse = Response.error<MailListResponse>(
            404,
            okhttp3.ResponseBody.create(null, "Not found")
        )

        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails() } returns Result.success("token123")
        coEvery { repository.getMails(pageToken = "token123") } returns mockResponse

        viewModel = InboxViewModel(repository, contactRepository)
        advanceUntilIdle()

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun contacts_and_groups_observed() = runTest {
        val mockContacts = listOf(
            com.fiveis.xend.data.model.Contact(
                id = 1L,
                name = "John",
                email = "john@test.com"
            )
        )
        val mockGroups = listOf(
            com.fiveis.xend.data.model.Group(
                id = 1L,
                name = "VIP"
            )
        )

        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails() } returns Result.success(null)
        every { contactRepository.observeContacts() } returns flowOf(mockContacts)
        every { contactRepository.observeGroups() } returns flowOf(mockGroups)

        viewModel = InboxViewModel(repository, contactRepository)
        advanceUntilIdle()

        assertEquals(setOf("john@test.com"), viewModel.uiState.value.contactEmails)
        assertEquals(mapOf("john@test.com" to "John"), viewModel.uiState.value.contactsByEmail)
        assertEquals(mockGroups, viewModel.uiState.value.groups)
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
