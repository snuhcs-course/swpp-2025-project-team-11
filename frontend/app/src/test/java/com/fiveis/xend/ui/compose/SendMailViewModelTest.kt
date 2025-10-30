package com.fiveis.xend.ui.compose

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.data.model.SendResponse
import com.fiveis.xend.data.repository.MailSendRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
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
class SendMailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var application: Application
    private lateinit var repository: MailSendRepository
    private lateinit var viewModel: SendMailViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        repository = mockk()

        every { application.applicationContext } returns application

        mockkConstructor(MailSendRepository::class)
        every { anyConstructed<MailSendRepository>().equals(any()) } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun send_email_with_empty_recipients_sets_error() = runTest {
        viewModel = SendMailViewModel(application)
        advanceUntilIdle()

        viewModel.sendEmail(emptyList(), "Subject", "Body")
        advanceUntilIdle()

        assertFalse(viewModel.ui.value.isSending)
        assertNotNull(viewModel.ui.value.error)
        assertTrue(viewModel.ui.value.error?.contains("수신자") == true)
    }

    @Test
    fun send_email_success_updates_state() = runTest {
        val to = listOf("test@example.com")
        val subject = "Test Subject"
        val body = "Test Body"
        val mockResponse = SendResponse(
            id = "12345",
            threadId = "thread123",
            labelIds = listOf("SENT")
        )

        mockkConstructor(MailSendRepository::class)
        coEvery { anyConstructed<MailSendRepository>().sendEmail(to, subject, body) } returns mockResponse

        viewModel = SendMailViewModel(application)
        viewModel.sendEmail(to, subject, body)
        advanceUntilIdle()

        assertFalse(viewModel.ui.value.isSending)
        assertNotNull(viewModel.ui.value.lastSuccessMsg)
        assertTrue(viewModel.ui.value.lastSuccessMsg?.contains("12345") == true)
        assertEquals(null, viewModel.ui.value.error)
    }

    @Test
    fun send_email_failure_sets_error() = runTest {
        val to = listOf("test@example.com")
        val subject = "Test Subject"
        val body = "Test Body"

        mockkConstructor(MailSendRepository::class)
        coEvery { anyConstructed<MailSendRepository>().sendEmail(to, subject, body) } throws Exception("Network error")

        viewModel = SendMailViewModel(application)
        viewModel.sendEmail(to, subject, body)
        advanceUntilIdle()

        assertFalse(viewModel.ui.value.isSending)
        assertNotNull(viewModel.ui.value.error)
        assertTrue(viewModel.ui.value.error?.contains("Network error") == true)
    }
}
