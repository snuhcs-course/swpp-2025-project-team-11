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

    @Test
    fun initial_state_is_correct() = runTest {
        viewModel = SendMailViewModel(application)
        advanceUntilIdle()

        assertFalse(viewModel.ui.value.isSending)
        assertEquals(null, viewModel.ui.value.lastSuccessMsg)
        assertEquals(null, viewModel.ui.value.error)
    }

    @Test
    fun send_email_sets_is_sending_to_true() = runTest {
        val to = listOf("test@example.com")

        mockkConstructor(MailSendRepository::class)
        coEvery { anyConstructed<MailSendRepository>().sendEmail(any(), any(), any()) } coAnswers {
            kotlinx.coroutines.delay(1000)
            SendResponse("123", "thread", listOf("SENT"))
        }

        viewModel = SendMailViewModel(application)
        viewModel.sendEmail(to, "Subject", "Body")

        assertTrue(viewModel.ui.value.isSending)
    }

    @Test
    fun send_email_with_multiple_recipients_success() = runTest {
        val to = listOf("test1@example.com", "test2@example.com", "test3@example.com")
        val subject = "Test Subject"
        val body = "Test Body"
        val mockResponse = SendResponse("12345", "thread123", listOf("SENT"))

        mockkConstructor(MailSendRepository::class)
        coEvery { anyConstructed<MailSendRepository>().sendEmail(to, subject, body) } returns mockResponse

        viewModel = SendMailViewModel(application)
        viewModel.sendEmail(to, subject, body)
        advanceUntilIdle()

        assertFalse(viewModel.ui.value.isSending)
        assertNotNull(viewModel.ui.value.lastSuccessMsg)
        assertEquals(null, viewModel.ui.value.error)
    }

    @Test
    fun send_email_with_empty_subject_success() = runTest {
        val to = listOf("test@example.com")
        val subject = ""
        val body = "Test Body"
        val mockResponse = SendResponse("12345", "thread123", listOf("SENT"))

        mockkConstructor(MailSendRepository::class)
        coEvery { anyConstructed<MailSendRepository>().sendEmail(to, subject, body) } returns mockResponse

        viewModel = SendMailViewModel(application)
        viewModel.sendEmail(to, subject, body)
        advanceUntilIdle()

        assertFalse(viewModel.ui.value.isSending)
        assertNotNull(viewModel.ui.value.lastSuccessMsg)
    }

    @Test
    fun send_email_with_empty_body_success() = runTest {
        val to = listOf("test@example.com")
        val subject = "Test Subject"
        val body = ""
        val mockResponse = SendResponse("12345", "thread123", listOf("SENT"))

        mockkConstructor(MailSendRepository::class)
        coEvery { anyConstructed<MailSendRepository>().sendEmail(to, subject, body) } returns mockResponse

        viewModel = SendMailViewModel(application)
        viewModel.sendEmail(to, subject, body)
        advanceUntilIdle()

        assertFalse(viewModel.ui.value.isSending)
        assertNotNull(viewModel.ui.value.lastSuccessMsg)
    }

    @Test
    fun send_email_with_special_characters_in_subject() = runTest {
        val to = listOf("test@example.com")
        val subject = "Test Subject !@#$%^&*()"
        val body = "Test Body"
        val mockResponse = SendResponse("12345", "thread123", listOf("SENT"))

        mockkConstructor(MailSendRepository::class)
        coEvery { anyConstructed<MailSendRepository>().sendEmail(to, subject, body) } returns mockResponse

        viewModel = SendMailViewModel(application)
        viewModel.sendEmail(to, subject, body)
        advanceUntilIdle()

        assertFalse(viewModel.ui.value.isSending)
        assertNotNull(viewModel.ui.value.lastSuccessMsg)
    }

    @Test
    fun send_email_with_long_body() = runTest {
        val to = listOf("test@example.com")
        val subject = "Test Subject"
        val body = "A".repeat(10000)
        val mockResponse = SendResponse("12345", "thread123", listOf("SENT"))

        mockkConstructor(MailSendRepository::class)
        coEvery { anyConstructed<MailSendRepository>().sendEmail(to, subject, body) } returns mockResponse

        viewModel = SendMailViewModel(application)
        viewModel.sendEmail(to, subject, body)
        advanceUntilIdle()

        assertFalse(viewModel.ui.value.isSending)
        assertNotNull(viewModel.ui.value.lastSuccessMsg)
    }

    @Test
    fun send_email_failure_with_null_message() = runTest {
        val to = listOf("test@example.com")

        mockkConstructor(MailSendRepository::class)
        coEvery { anyConstructed<MailSendRepository>().sendEmail(any(), any(), any()) } throws Exception()

        viewModel = SendMailViewModel(application)
        viewModel.sendEmail(to, "Subject", "Body")
        advanceUntilIdle()

        assertFalse(viewModel.ui.value.isSending)
        assertNotNull(viewModel.ui.value.error)
        assertTrue(viewModel.ui.value.error?.contains("알 수 없는 오류") == true)
    }

    @Test
    fun send_email_multiple_times_updates_state_correctly() = runTest {
        val to = listOf("test@example.com")
        val mockResponse1 = SendResponse("id1", "thread1", listOf("SENT"))
        val mockResponse2 = SendResponse("id2", "thread2", listOf("SENT"))

        mockkConstructor(MailSendRepository::class)
        coEvery { anyConstructed<MailSendRepository>().sendEmail(any(), any(), any()) } returnsMany listOf(mockResponse1, mockResponse2)

        viewModel = SendMailViewModel(application)

        viewModel.sendEmail(to, "Subject 1", "Body 1")
        advanceUntilIdle()
        assertTrue(viewModel.ui.value.lastSuccessMsg?.contains("id1") == true)

        viewModel.sendEmail(to, "Subject 2", "Body 2")
        advanceUntilIdle()
        assertTrue(viewModel.ui.value.lastSuccessMsg?.contains("id2") == true)
    }

    @Test
    fun send_email_with_unicode_characters() = runTest {
        val to = listOf("test@example.com")
        val subject = "안녕하세요"
        val body = "テスト 测试 🎉"
        val mockResponse = SendResponse("12345", "thread123", listOf("SENT"))

        mockkConstructor(MailSendRepository::class)
        coEvery { anyConstructed<MailSendRepository>().sendEmail(to, subject, body) } returns mockResponse

        viewModel = SendMailViewModel(application)
        viewModel.sendEmail(to, subject, body)
        advanceUntilIdle()

        assertFalse(viewModel.ui.value.isSending)
        assertNotNull(viewModel.ui.value.lastSuccessMsg)
    }

    @Test
    fun send_email_error_clears_previous_success() = runTest {
        val to = listOf("test@example.com")
        val mockResponse = SendResponse("12345", "thread123", listOf("SENT"))

        mockkConstructor(MailSendRepository::class)
        coEvery { anyConstructed<MailSendRepository>().sendEmail(any(), any(), any()) } returns mockResponse andThenThrows Exception("Network error")

        viewModel = SendMailViewModel(application)

        viewModel.sendEmail(to, "Subject 1", "Body 1")
        advanceUntilIdle()
        assertNotNull(viewModel.ui.value.lastSuccessMsg)

        viewModel.sendEmail(to, "Subject 2", "Body 2")
        advanceUntilIdle()
        assertNotNull(viewModel.ui.value.error)
    }

    @Test
    fun send_email_with_html_content_in_body() = runTest {
        val to = listOf("test@example.com")
        val subject = "Test"
        val body = "<html><body><h1>Hello</h1></body></html>"
        val mockResponse = SendResponse("12345", "thread123", listOf("SENT"))

        mockkConstructor(MailSendRepository::class)
        coEvery { anyConstructed<MailSendRepository>().sendEmail(to, subject, body) } returns mockResponse

        viewModel = SendMailViewModel(application)
        viewModel.sendEmail(to, subject, body)
        advanceUntilIdle()

        assertFalse(viewModel.ui.value.isSending)
        assertNotNull(viewModel.ui.value.lastSuccessMsg)
    }

    @Test
    fun send_email_with_newlines_in_body() = runTest {
        val to = listOf("test@example.com")
        val subject = "Test"
        val body = "Line 1\nLine 2\nLine 3"
        val mockResponse = SendResponse("12345", "thread123", listOf("SENT"))

        mockkConstructor(MailSendRepository::class)
        coEvery { anyConstructed<MailSendRepository>().sendEmail(to, subject, body) } returns mockResponse

        viewModel = SendMailViewModel(application)
        viewModel.sendEmail(to, subject, body)
        advanceUntilIdle()

        assertFalse(viewModel.ui.value.isSending)
        assertNotNull(viewModel.ui.value.lastSuccessMsg)
    }

    @Test
    fun factory_creates_viewmodel_successfully() {
        val factory = SendMailViewModel.Factory(application)
        val vm = factory.create(SendMailViewModel::class.java)

        assertNotNull(vm)
        assertTrue(vm is SendMailViewModel)
    }


    @Test
    fun send_ui_state_data_class_properties() {
        val state = SendUiState(
            isSending = true,
            lastSuccessMsg = "Success",
            error = "Error"
        )

        assertTrue(state.isSending)
        assertEquals("Success", state.lastSuccessMsg)
        assertEquals("Error", state.error)
    }

    @Test
    fun send_ui_state_default_values() {
        val state = SendUiState()

        assertFalse(state.isSending)
        assertEquals(null, state.lastSuccessMsg)
        assertEquals(null, state.error)
    }

    @Test
    fun send_ui_state_copy_works_correctly() {
        val state = SendUiState(isSending = true)
        val copied = state.copy(error = "Error")

        assertTrue(copied.isSending)
        assertEquals("Error", copied.error)
    }

    @Test
    fun send_ui_state_equals_works() {
        val state1 = SendUiState(isSending = true, error = "Error")
        val state2 = SendUiState(isSending = true, error = "Error")

        assertEquals(state1, state2)
    }
}
