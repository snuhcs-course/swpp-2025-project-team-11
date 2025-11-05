package com.fiveis.xend.ui.view

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.data.model.EmailItem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MailDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var emailDao: EmailDao
    private lateinit var viewModel: MailDetailViewModel

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
    fun init_loads_mail_successfully() = runTest {
        val messageId = "12345"
        val mockMail = EmailItem(
            id = messageId,
            threadId = "thread123",
            subject = "Test Subject",
            fromEmail = "sender@example.com",
            snippet = "Test snippet",
            date = "2025-01-01T00:00:00Z",
            dateRaw = "Wed, 1 Jan 2025 00:00:00 +0000",
            isUnread = true,
            labelIds = listOf("INBOX"),
            body = "Test body"
        )

        coEvery { emailDao.getEmailById(messageId) } returns mockMail

        viewModel = MailDetailViewModel(emailDao, messageId)
        advanceUntilIdle()

        assertEquals(mockMail, viewModel.uiState.value.mail)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun init_handles_not_found() = runTest {
        val messageId = "12345"

        coEvery { emailDao.getEmailById(messageId) } returns null

        viewModel = MailDetailViewModel(emailDao, messageId)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("not found") == true)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun init_handles_exception() = runTest {
        val messageId = "12345"

        coEvery { emailDao.getEmailById(messageId) } throws Exception("Database error")

        viewModel = MailDetailViewModel(emailDao, messageId)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("Database error") == true)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
