package com.fiveis.xend.ui.view

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.data.model.MailDetailResponse
import com.fiveis.xend.data.repository.InboxRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class MailDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var repository: InboxRepository
    private lateinit var viewModel: MailDetailViewModel

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
    fun init_loads_mail_successfully() = runTest {
        val messageId = "12345"
        val mockMail = MailDetailResponse(
            id = messageId,
            thread_id = "thread123",
            subject = "Test Subject",
            from_email = "sender@example.com",
            to = "receiver@example.com",
            date = "2025-01-01T00:00:00Z",
            date_raw = "Wed, 1 Jan 2025 00:00:00 +0000",
            body = "Test body",
            snippet = "Test snippet",
            is_unread = true,
            label_ids = listOf("INBOX")
        )

        coEvery { repository.getMail(messageId) } returns Response.success(mockMail)

        viewModel = MailDetailViewModel(repository, messageId)
        advanceUntilIdle()

        assertEquals(mockMail, viewModel.uiState.value.mail)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun init_handles_error_response() = runTest {
        val messageId = "12345"

        coEvery { repository.getMail(messageId) } returns Response.error(
            404,
            "Not found".toResponseBody()
        )

        viewModel = MailDetailViewModel(repository, messageId)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("Failed to load mail") == true)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun init_handles_exception() = runTest {
        val messageId = "12345"

        coEvery { repository.getMail(messageId) } throws Exception("Network error")

        viewModel = MailDetailViewModel(repository, messageId)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("Network error") == true)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
