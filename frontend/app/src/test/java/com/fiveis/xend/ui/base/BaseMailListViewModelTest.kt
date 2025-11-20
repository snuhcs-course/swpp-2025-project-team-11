package com.fiveis.xend.ui.base

import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.model.MailListResponse
import com.fiveis.xend.data.repository.BaseMailRepository
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
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class BaseMailListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: BaseMailRepository
    private lateinit var viewModel: TestMailListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun test_init_loadsCachedEmails() = runTest(testDispatcher) {
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
        every { repository.getCachedEmails() } returns flowOf(mockEmails)
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success(null)

        viewModel = TestMailListViewModel(repository)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.emails.size)
        assertEquals("1", viewModel.uiState.value.emails[0].id)
    }

    @Test
    fun test_refreshEmails_setsRefreshingState() = runTest(testDispatcher) {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success("token123")
        viewModel = TestMailListViewModel(repository)
        advanceUntilIdle()

        viewModel.refreshEmails()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRefreshing)
        coVerify(atLeast = 1) { repository.refreshEmails(any(), any()) }
    }

    @Test
    fun test_refreshEmails_failure_setsError() = runTest(testDispatcher) {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.failure(Exception("Network error"))
        viewModel = TestMailListViewModel(repository)
        advanceUntilIdle()

        viewModel.refreshEmails()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isRefreshing)
        assertEquals("Network error", viewModel.uiState.value.error)
    }

    @Test
    fun test_refreshEmails_emptyDb_setsNextPageToken() = runTest(testDispatcher) {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success("nextToken")
        viewModel = TestMailListViewModel(repository)
        advanceUntilIdle()

        viewModel.refreshEmails()
        advanceUntilIdle()

        assertEquals("nextToken", viewModel.uiState.value.loadMoreNextPageToken)
    }

    @Test
    fun test_refreshEmails_nonEmptyDb_keepsExistingToken() = runTest(testDispatcher) {
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
        every { repository.getCachedEmails() } returns flowOf(mockEmails)
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success("newToken")
        viewModel = TestMailListViewModel(repository)
        advanceUntilIdle()

        // Set an existing token
        viewModel.setLoadMoreToken("existingToken")

        viewModel.refreshEmails()
        advanceUntilIdle()

        assertEquals("existingToken", viewModel.uiState.value.loadMoreNextPageToken)
    }

    @Test
    fun test_loadMoreEmails_skipsIfAlreadyLoading() = runTest(testDispatcher) {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success("token")
        viewModel = TestMailListViewModel(repository)
        advanceUntilIdle()

        viewModel.setLoadMoreToken("token123")
        viewModel.setLoading(true)

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.getMails(any(), any(), any(), any()) }
    }

    @Test
    fun test_loadMoreEmails_skipsIfNoToken() = runTest(testDispatcher) {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success(null)
        viewModel = TestMailListViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMoreEmails()
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.getMails(any(), any(), any(), any()) }
    }

    @Test
    fun test_loadMoreEmails_success_savesEmailsAndUpdatesToken() = runTest(testDispatcher) {
        val existingEmails = listOf(
            EmailItem(
                id = "1",
                threadId = "thread1",
                subject = "Existing",
                fromEmail = "test@example.com",
                snippet = "Existing",
                date = "2025-11-19",
                dateRaw = "Wed, 19 Nov 2025 10:00:00 +0000",
                isUnread = true,
                labelIds = listOf("INBOX")
            )
        )
        val newEmails = listOf(
            EmailItem(
                id = "2",
                threadId = "thread2",
                subject = "New",
                fromEmail = "test@example.com",
                snippet = "New",
                date = "2025-11-19",
                dateRaw = "Wed, 19 Nov 2025 10:00:00 +0000",
                isUnread = true,
                labelIds = listOf("INBOX")
            )
        )
        val mockResponse = Response.success(
            MailListResponse(
                messages = newEmails,
                nextPageToken = "newToken",
                resultSizeEstimate = 1
            )
        )

        every { repository.getCachedEmails() } returns flowOf(existingEmails)
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success("token123")
        coEvery { repository.getMails(any(), any(), any(), any()) } returns mockResponse
        coEvery { repository.saveEmailsToCache(any()) } returns Unit

        viewModel = TestMailListViewModel(repository)
        advanceUntilIdle()

        viewModel.setLoadMoreToken("token123")
        viewModel.loadMoreEmails()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("newToken", viewModel.uiState.value.loadMoreNextPageToken)
        coVerify { repository.saveEmailsToCache(newEmails) }
    }

    @Test
    fun test_loadMoreEmails_filtersDuplicates() = runTest(testDispatcher) {
        val existingEmails = listOf(
            EmailItem(
                id = "1",
                threadId = "thread1",
                subject = "Existing",
                fromEmail = "test@example.com",
                snippet = "Existing",
                date = "2025-11-19",
                dateRaw = "Wed, 19 Nov 2025 10:00:00 +0000",
                isUnread = true,
                labelIds = listOf("INBOX")
            )
        )
        val newEmailsWithDuplicate = listOf(
            EmailItem(
                id = "1",
                threadId = "thread1",
                subject = "Duplicate",
                fromEmail = "test@example.com",
                snippet = "Duplicate",
                date = "2025-11-19",
                dateRaw = "Wed, 19 Nov 2025 10:00:00 +0000",
                isUnread = true,
                labelIds = listOf("INBOX")
            ),
            EmailItem(
                id = "2",
                threadId = "thread2",
                subject = "New",
                fromEmail = "test@example.com",
                snippet = "New",
                date = "2025-11-19",
                dateRaw = "Wed, 19 Nov 2025 10:00:00 +0000",
                isUnread = true,
                labelIds = listOf("INBOX")
            )
        )
        val mockResponse = Response.success(
            MailListResponse(
                messages = newEmailsWithDuplicate,
                nextPageToken = null,
                resultSizeEstimate = 2
            )
        )

        every { repository.getCachedEmails() } returns flowOf(existingEmails)
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success("token123")
        coEvery { repository.getMails(any(), any(), any(), any()) } returns mockResponse
        coEvery { repository.saveEmailsToCache(any()) } returns Unit

        viewModel = TestMailListViewModel(repository)
        advanceUntilIdle()

        viewModel.setLoadMoreToken("token123")
        viewModel.loadMoreEmails()
        advanceUntilIdle()

        // Should still save all emails to cache, duplicate filtering is just for logging
        coVerify { repository.saveEmailsToCache(newEmailsWithDuplicate) }
    }

    @Test
    fun test_loadMoreEmails_apiFailure_setsError() = runTest(testDispatcher) {
        val mockResponse = Response.error<MailListResponse>(
            500,
            ResponseBody.create(null, "Server error")
        )

        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success("token123")
        coEvery { repository.getMails(any(), any(), any(), any()) } returns mockResponse

        viewModel = TestMailListViewModel(repository)
        advanceUntilIdle()

        viewModel.setLoadMoreToken("token123")
        viewModel.loadMoreEmails()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Failed to load more emails", viewModel.uiState.value.error)
    }

    @Test
    fun test_loadMoreEmails_exception_setsError() = runTest(testDispatcher) {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.success("token123")
        coEvery {
            repository.getMails(any(), any(), any(), any())
        } throws Exception("Network failure")

        viewModel = TestMailListViewModel(repository)
        advanceUntilIdle()

        viewModel.setLoadMoreToken("token123")
        viewModel.loadMoreEmails()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Network failure", viewModel.uiState.value.error)
    }

    @Test
    fun test_silentRefresh_noError_doesNotSetErrorInUi() = runTest(testDispatcher) {
        every { repository.getCachedEmails() } returns flowOf(emptyList())
        coEvery { repository.refreshEmails(any(), any()) } returns Result.failure(Exception("Silent error"))

        viewModel = TestMailListViewModel(repository)
        advanceUntilIdle()

        // Silent refresh failure should not set error in UI
        assertNull(viewModel.uiState.value.error)
    }

    // Test implementation classes
    private data class TestUiState(
        override val emails: List<EmailItem> = emptyList(),
        override val isLoading: Boolean = false,
        override val error: String? = null,
        override val loadMoreNextPageToken: String? = null,
        override val isRefreshing: Boolean = false
    ) : BaseMailListUiState {
        override fun copyWith(
            emails: List<EmailItem>,
            isLoading: Boolean,
            error: String?,
            loadMoreNextPageToken: String?,
            isRefreshing: Boolean
        ): BaseMailListUiState {
            return copy(
                emails = emails,
                isLoading = isLoading,
                error = error,
                loadMoreNextPageToken = loadMoreNextPageToken,
                isRefreshing = isRefreshing
            )
        }
    }

    private class TestMailListViewModel(
        repository: BaseMailRepository
    ) : BaseMailListViewModel<TestUiState, BaseMailRepository>(
        repository = repository,
        uiStateFlow = kotlinx.coroutines.flow.MutableStateFlow(TestUiState()),
        logTag = "TestViewModel"
    ) {
        val uiState = uiStateFlow

        fun setLoadMoreToken(token: String?) {
            updateUiState { it.copyWith(loadMoreNextPageToken = token) }
        }

        fun setLoading(loading: Boolean) {
            updateUiState { it.copyWith(isLoading = loading) }
        }
    }
}
