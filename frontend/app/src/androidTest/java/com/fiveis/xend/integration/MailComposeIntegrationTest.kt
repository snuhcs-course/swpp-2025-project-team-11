package com.fiveis.xend.integration

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.MailSendRequest
import com.fiveis.xend.data.model.SendResponse
import com.fiveis.xend.data.model.toMultipartParts
import com.fiveis.xend.data.repository.MailSendRepository
import com.fiveis.xend.network.AiApiService
import com.fiveis.xend.network.MailApiService
import com.fiveis.xend.network.MailComposeSseClient
import com.fiveis.xend.network.MailSuggestResponse
import com.fiveis.xend.ui.compose.MailComposeViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@RunWith(AndroidJUnit4::class)
class MailComposeIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var context: Context
    private lateinit var mailApiService: MailApiService
    private lateinit var repository: MailSendRepository
    private lateinit var sseClient: MailComposeSseClient
    private lateinit var aiApiService: AiApiService
    private lateinit var viewModel: MailComposeViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mailApiService = mockk()
        sseClient = mockk(relaxed = true)
        aiApiService = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        // Clean up
    }

    @Test
    fun repository_sends_email_successfully() = runTest {
        repository = MailSendRepository(context)

        val mockResponse = SendResponse(
            id = "msg123",
            threadId = "thread123",
            labelIds = listOf("SENT")
        )

        coEvery {
            mailApiService.sendEmail(parts = any())
        } returns Response.success(201, mockResponse)

        val request = MailSendRequest(
            to = listOf("test@example.com"),
            subject = "Test Subject",
            body = "Test Body"
        )

        val result = mailApiService.sendEmail(request.toMultipartParts(context, emptyList()))

        assertTrue(result.isSuccessful)
        assertEquals(201, result.code())
        assertEquals("msg123", result.body()?.id)
    }

    @Test(timeout = 10000)
    fun viewModel_starts_streaming_successfully() {
        viewModel = MailComposeViewModel(sseClient, aiApiService)

        val payload = JSONObject().apply {
            put("subject", "Test")
            put("body", "Test body")
        }

        viewModel.startStreaming(payload)
        Thread.sleep(200)

        // isStreaming should be true when streaming starts
        assertTrue(viewModel.ui.value.isStreaming)
        verify { sseClient.start(any(), any(), any(), any(), any()) }
    }

    @Test(timeout = 5000)
    fun viewModel_stops_streaming_successfully() {
        viewModel = MailComposeViewModel(sseClient, aiApiService)

        val payload = JSONObject().apply {
            put("subject", "Test")
        }

        viewModel.startStreaming(payload)
        Thread.sleep(100)

        viewModel.stopStreaming()
        Thread.sleep(100)

        assertFalse(viewModel.ui.value.isStreaming)
        verify { sseClient.stop() }
    }

    @Test(timeout = 5000)
    fun viewModel_handles_text_changed_with_realtime_enabled() {
        coEvery { aiApiService.suggestMail(any()) } returns Response.success(
            MailSuggestResponse(target = "body", suggestion = "Hello there.")
        )
        viewModel = MailComposeViewModel(sseClient, aiApiService)

        viewModel.enableRealtimeMode(true)
        viewModel.onTextChanged(
            currentText = "This is a long enough message for realtime.",
            subject = "Subject",
            toEmails = listOf("user@example.com"),
            cursorPosition = 5
        )
        Thread.sleep(500) // wait for debounce

        coVerify { aiApiService.suggestMail(any()) }
        assertEquals("Hello there.", viewModel.ui.value.suggestionText)
    }

    @Test(timeout = 5000)
    fun viewModel_ignores_text_changed_with_realtime_disabled() {
        viewModel = MailComposeViewModel(sseClient, aiApiService)

        viewModel.enableRealtimeMode(false)
        viewModel.onTextChanged(
            currentText = "This is ignored because realtime is off.",
            subject = "Subject",
            toEmails = listOf("user@example.com"),
            cursorPosition = 0
        )
        Thread.sleep(600)  // Wait for debounce timeout

        coVerify(exactly = 0) { aiApiService.suggestMail(any()) }
    }

    @Test(timeout = 5000)
    fun viewModel_accepts_suggestion_clears_text() {
        coEvery { aiApiService.suggestMail(any()) } returns Response.success(
            MailSuggestResponse(target = "body", suggestion = "Hi!")
        )
        viewModel = MailComposeViewModel(sseClient, aiApiService)

        viewModel.enableRealtimeMode(true)
        viewModel.requestImmediateSuggestion(
            currentText = "This text is long enough to trigger the API.",
            subject = "Subject",
            toEmails = listOf("user@example.com"),
            cursorPosition = 5,
            force = true
        )
        Thread.sleep(200)

        assertEquals("Hi!", viewModel.ui.value.suggestionText)

        viewModel.acceptSuggestion()
        Thread.sleep(100)

        assertEquals("", viewModel.ui.value.suggestionText)
    }

    @Test
    fun mail_send_request_creates_correctly() {
        val request = MailSendRequest(
            to = listOf("test1@example.com", "test2@example.com"),
            subject = "Test Subject",
            body = "Test Body Content"
        )

        assertEquals(2, request.to.size)
        assertEquals("Test Subject", request.subject)
        assertEquals("Test Body Content", request.body)
    }

    @Test
    fun send_response_parses_correctly() {
        val response = SendResponse(
            id = "msg456",
            threadId = "thread456",
            labelIds = listOf("SENT", "INBOX")
        )

        assertEquals("msg456", response.id)
        assertEquals("thread456", response.threadId)
        assertEquals(2, response.labelIds.size)
    }

    @Test
    fun viewModel_ui_state_initial_values_are_correct() {
        viewModel = MailComposeViewModel(sseClient, aiApiService)

        val state = viewModel.ui.value

        assertFalse(state.isStreaming)
        assertEquals("", state.subject)
        assertEquals("", state.bodyRendered)
        assertEquals(null, state.error)
        assertEquals("", state.suggestionText)
        assertFalse(state.isRealtimeEnabled)
    }

    @Test
    fun repository_handles_send_failure_correctly() = runTest {
        repository = MailSendRepository(context)

        coEvery {
            mailApiService.sendEmail(parts = any())
        } returns Response.error(400, mockk(relaxed = true))

        val request = MailSendRequest(
            to = listOf("test@example.com"),
            subject = "Test",
            body = "Test"
        )

        val result = mailApiService.sendEmail(request.toMultipartParts(context, emptyList()))

        assertFalse(result.isSuccessful)
        assertEquals(400, result.code())
    }

    @Test(timeout = 5000)
    fun viewModel_handles_multiple_start_stop_cycles() {
        viewModel = MailComposeViewModel(sseClient, aiApiService)

        val payload = JSONObject().apply {
            put("subject", "Test")
        }

        viewModel.startStreaming(payload)
        Thread.sleep(100)
        viewModel.stopStreaming()
        Thread.sleep(100)

        viewModel.startStreaming(payload)
        Thread.sleep(100)
        viewModel.stopStreaming()
        Thread.sleep(100)

        verify(atLeast = 2) { sseClient.start(any(), any(), any(), any(), any()) }
        verify(atLeast = 2) { sseClient.stop() }
    }

    @Test(timeout = 5000)
    fun viewModel_handles_realtime_toggle_multiple_times() {
        viewModel = MailComposeViewModel(sseClient, aiApiService)

        viewModel.enableRealtimeMode(true)
        Thread.sleep(50)
        assertTrue(viewModel.ui.value.isRealtimeEnabled)

        viewModel.enableRealtimeMode(false)
        Thread.sleep(50)
        assertFalse(viewModel.ui.value.isRealtimeEnabled)

        viewModel.enableRealtimeMode(true)
        Thread.sleep(50)
        assertTrue(viewModel.ui.value.isRealtimeEnabled)
    }

    @Test(timeout = 5000)
    fun viewModel_handles_empty_payload() {
        viewModel = MailComposeViewModel(sseClient, aiApiService)

        val emptyPayload = JSONObject()

        viewModel.startStreaming(emptyPayload)
        Thread.sleep(100)

        assertTrue(viewModel.ui.value.isStreaming)
    }

    @Test
    fun repository_sends_email_with_multiple_recipients() = runTest {
        repository = MailSendRepository(context)

        val mockResponse = SendResponse(
            id = "msg789",
            threadId = "thread789",
            labelIds = listOf("SENT")
        )

        coEvery {
            mailApiService.sendEmail(parts = any())
        } returns Response.success(201, mockResponse)

        val request = MailSendRequest(
            to = listOf("test1@example.com", "test2@example.com", "test3@example.com"),
            subject = "Test Subject",
            body = "Test Body"
        )

        val result = mailApiService.sendEmail(request.toMultipartParts(context, emptyList()))

        assertTrue(result.isSuccessful)
        assertEquals("msg789", result.body()?.id)
    }
}
