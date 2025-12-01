package com.fiveis.xend.ui.view

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.repository.InboxRepository
import io.mockk.coEvery
import io.mockk.every
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

    private lateinit var context: Context
    private lateinit var emailDao: EmailDao
    private lateinit var inboxRepository: InboxRepository
    private lateinit var viewModel: MailDetailViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = mockk(relaxed = true)
        emailDao = mockk()
        inboxRepository = mockk(relaxed = true)
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
            body = "Test body",
            cachedAt = System.currentTimeMillis()
        )

        coEvery { emailDao.getEmailById(messageId) } returns mockMail
        coEvery { emailDao.insertEmail(any()) } returns Unit
        coEvery { inboxRepository.getMail(messageId) } returns mockk(relaxed = true) {
            every { isSuccessful } returns false
            every { code() } returns 500
        }

        viewModel = MailDetailViewModel(context, emailDao, inboxRepository, messageId, testDispatcher)
        advanceUntilIdle()

        val resultMail = viewModel.uiState.value.mail
        assertEquals(mockMail.id, resultMail?.id)
        assertEquals(mockMail.subject, resultMail?.subject)
        assertEquals(mockMail.fromEmail, resultMail?.fromEmail)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun init_handles_not_found() = runTest {
        val messageId = "12345"
        val mockResponse = mockk<retrofit2.Response<com.fiveis.xend.data.model.MailDetailResponse>>(relaxed = true)

        coEvery { emailDao.getEmailById(messageId) } returns null
        coEvery { mockResponse.isSuccessful } returns false
        coEvery { mockResponse.code() } returns 404
        coEvery { inboxRepository.getMail(messageId) } returns mockResponse

        viewModel = MailDetailViewModel(context, emailDao, inboxRepository, messageId, testDispatcher)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun init_handles_exception() = runTest {
        val messageId = "12345"
        val mockResponse = mockk<retrofit2.Response<com.fiveis.xend.data.model.MailDetailResponse>>(relaxed = true)

        coEvery { emailDao.getEmailById(messageId) } throws Exception("Database error")
        coEvery { mockResponse.isSuccessful } returns false
        coEvery { mockResponse.code() } returns 500
        coEvery { inboxRepository.getMail(messageId) } returns mockResponse

        viewModel = MailDetailViewModel(context, emailDao, inboxRepository, messageId, testDispatcher)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
