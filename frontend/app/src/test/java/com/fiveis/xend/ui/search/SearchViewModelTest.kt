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
}
