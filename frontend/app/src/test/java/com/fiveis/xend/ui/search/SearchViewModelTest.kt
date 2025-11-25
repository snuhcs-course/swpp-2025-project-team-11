package com.fiveis.xend.ui.search

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.data.model.EmailItem
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var emailDao: EmailDao
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        emailDao = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun on_query_change_updates_query_in_state() = runTest {
        every { emailDao.searchEmails(any()) } returns flowOf(emptyList())

        viewModel = SearchViewModel(emailDao)

        viewModel.onQueryChange("test")

        assertEquals("test", viewModel.uiState.value.query)
    }

    @Test
    fun on_query_change_with_blank_query_returns_empty_results() = runTest {
        every { emailDao.searchEmails(any()) } returns flowOf(emptyList())

        viewModel = SearchViewModel(emailDao)

        viewModel.onQueryChange("")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals(emptyList<EmailItem>(), viewModel.uiState.value.results)
    }

    @Test
    fun on_query_change_with_valid_query_returns_results() = runTest {
        val mockResults = listOf(
            EmailItem(
                id = "1",
                threadId = "thread1",
                subject = "Test Email",
                fromEmail = "sender@example.com",
                snippet = "Test snippet",
                date = "2025-01-01T00:00:00Z",
                dateRaw = "Wed, 1 Jan 2025 00:00:00 +0000",
                isUnread = true,
                labelIds = listOf("INBOX"),
                cachedAt = System.currentTimeMillis()
            )
        )

        every { emailDao.searchEmails("test") } returns flowOf(mockResults)

        viewModel = SearchViewModel(emailDao)

        viewModel.onQueryChange("test")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals(mockResults, viewModel.uiState.value.results)
    }

    @Test
    fun debounce_prevents_rapid_searches() = runTest {
        val mockResults = listOf(
            EmailItem(
                id = "1",
                threadId = "thread1",
                subject = "Test Email",
                fromEmail = "sender@example.com",
                snippet = "Test snippet",
                date = "2025-01-01T00:00:00Z",
                dateRaw = "Wed, 1 Jan 2025 00:00:00 +0000",
                isUnread = true,
                labelIds = listOf("INBOX"),
                cachedAt = System.currentTimeMillis()
            )
        )

        every { emailDao.searchEmails("test") } returns flowOf(mockResults)
        every { emailDao.searchEmails("te") } returns flowOf(emptyList())

        viewModel = SearchViewModel(emailDao)

        viewModel.onQueryChange("te")
        advanceTimeBy(100)
        viewModel.onQueryChange("tes")
        advanceTimeBy(100)
        viewModel.onQueryChange("test")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals(mockResults, viewModel.uiState.value.results)
    }

    @Test
    fun search_with_whitespace_only_returns_empty() = runTest {
        every { emailDao.searchEmails(any()) } returns flowOf(emptyList())

        viewModel = SearchViewModel(emailDao)

        viewModel.onQueryChange("   ")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals(emptyList<EmailItem>(), viewModel.uiState.value.results)
    }

    @Test
    fun search_with_multiple_results() = runTest {
        val mockResults = listOf(
            EmailItem("1", "t1", "Email 1", "a@test.com", "", "snippet", "2025-01-01T00:00:00Z", "raw", true, listOf()),
            EmailItem("2", "t2", "Email 2", "b@test.com", "", "snippet", "2025-01-01T00:00:00Z", "raw", true, listOf()),
            EmailItem("3", "t3", "Email 3", "c@test.com", "", "snippet", "2025-01-01T00:00:00Z", "raw", true, listOf())
        )

        every { emailDao.searchEmails("query") } returns flowOf(mockResults)

        viewModel = SearchViewModel(emailDao)
        viewModel.onQueryChange("query")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.results.size)
    }

    @Test
    fun search_updates_when_query_changes() = runTest {
        val results1 = listOf(EmailItem("1", "t1", "First", "a@test.com", "", "s", "2025-01-01T00:00:00Z", "r", true, listOf()))
        val results2 = listOf(EmailItem("2", "t2", "Second", "b@test.com", "", "s", "2025-01-01T00:00:00Z", "r", true, listOf()))

        every { emailDao.searchEmails("first") } returns flowOf(results1)
        every { emailDao.searchEmails("second") } returns flowOf(results2)

        viewModel = SearchViewModel(emailDao)

        viewModel.onQueryChange("first")
        advanceTimeBy(400)
        advanceUntilIdle()
        assertEquals("First", viewModel.uiState.value.results.first().subject)

        viewModel.onQueryChange("second")
        advanceTimeBy(400)
        advanceUntilIdle()
        assertEquals("Second", viewModel.uiState.value.results.first().subject)
    }

    @Test
    fun query_state_updates_immediately() = runTest {
        every { emailDao.searchEmails(any()) } returns flowOf(emptyList())

        viewModel = SearchViewModel(emailDao)
        viewModel.onQueryChange("immediate")

        assertEquals("immediate", viewModel.uiState.value.query)
    }

    @Test
    fun initial_state_is_empty() = runTest {
        every { emailDao.searchEmails(any()) } returns flowOf(emptyList())

        viewModel = SearchViewModel(emailDao)
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.query)
        assertEquals(emptyList<EmailItem>(), viewModel.uiState.value.results)
    }

    @Test
    fun distinct_until_changed_prevents_duplicate_searches() = runTest {
        val mockResults = listOf(EmailItem("1", "t1", "Test", "a@test.com", "", "s", "2025-01-01T00:00:00Z", "r", true, listOf()))

        every { emailDao.searchEmails("test") } returns flowOf(mockResults)

        viewModel = SearchViewModel(emailDao)

        viewModel.onQueryChange("test")
        advanceTimeBy(400)
        advanceUntilIdle()

        viewModel.onQueryChange("test")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals(mockResults, viewModel.uiState.value.results)
    }

    @Test
    fun search_with_special_characters() = runTest {
        val mockResults = listOf(EmailItem("1", "t1", "Special!", "a@test.com", "", "s", "2025-01-01T00:00:00Z", "r", true, listOf()))

        every { emailDao.searchEmails("test!@#") } returns flowOf(mockResults)

        viewModel = SearchViewModel(emailDao)
        viewModel.onQueryChange("test!@#")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals(mockResults, viewModel.uiState.value.results)
    }

    @Test
    fun search_with_unicode_characters() = runTest {
        val mockResults = listOf(EmailItem("1", "t1", "테스트", "a@test.com", "", "s", "2025-01-01T00:00:00Z", "r", true, listOf()))

        every { emailDao.searchEmails("테스트") } returns flowOf(mockResults)

        viewModel = SearchViewModel(emailDao)
        viewModel.onQueryChange("테스트")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals(mockResults, viewModel.uiState.value.results)
    }

    @Test
    fun search_with_very_long_query() = runTest {
        val longQuery = "a".repeat(1000)
        val mockResults = listOf(EmailItem("1", "t1", "Long", "a@test.com", "", "s", "2025-01-01T00:00:00Z", "r", true, listOf()))

        every { emailDao.searchEmails(longQuery) } returns flowOf(mockResults)

        viewModel = SearchViewModel(emailDao)
        viewModel.onQueryChange(longQuery)
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals(mockResults, viewModel.uiState.value.results)
    }

    @Test
    fun search_clears_results_when_query_becomes_blank() = runTest {
        val mockResults = listOf(EmailItem("1", "t1", "Test", "a@test.com", "", "s", "2025-01-01T00:00:00Z", "r", true, listOf()))

        every { emailDao.searchEmails("test") } returns flowOf(mockResults)

        viewModel = SearchViewModel(emailDao)
        viewModel.onQueryChange("test")
        advanceTimeBy(400)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.results.size)

        viewModel.onQueryChange("")
        advanceTimeBy(400)
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.results.size)
    }

    @Test
    fun search_handles_large_result_sets() = runTest {
        val largeResults = (1..1000).map {
            EmailItem("$it", "t$it", "Email $it", "user$it@test.com", "", "s", "2025-01-01T00:00:00Z", "r", true, listOf())
        }

        every { emailDao.searchEmails("large") } returns flowOf(largeResults)

        viewModel = SearchViewModel(emailDao)
        viewModel.onQueryChange("large")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals(1000, viewModel.uiState.value.results.size)
    }

    @Test
    fun search_with_mixed_case() = runTest {
        val mockResults = listOf(EmailItem("1", "t1", "Test", "a@test.com", "", "s", "2025-01-01T00:00:00Z", "r", true, listOf()))

        every { emailDao.searchEmails("TeSt") } returns flowOf(mockResults)

        viewModel = SearchViewModel(emailDao)
        viewModel.onQueryChange("TeSt")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals(mockResults, viewModel.uiState.value.results)
    }

    @Test
    fun search_with_leading_trailing_spaces() = runTest {
        val mockResults = listOf(EmailItem("1", "t1", "Test", "a@test.com", "", "s", "2025-01-01T00:00:00Z", "r", true, listOf()))

        every { emailDao.searchEmails("  test  ") } returns flowOf(mockResults)

        viewModel = SearchViewModel(emailDao)
        viewModel.onQueryChange("  test  ")
        advanceTimeBy(400)
        advanceUntilIdle()

        assertEquals(mockResults, viewModel.uiState.value.results)
    }
}
