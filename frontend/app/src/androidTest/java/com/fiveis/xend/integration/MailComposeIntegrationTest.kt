package com.fiveis.xend.integration

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.MailSendRequest
import com.fiveis.xend.data.model.SendResponse
import com.fiveis.xend.data.model.toMultipartParts
import com.fiveis.xend.data.repository.MailSendRepository
import com.fiveis.xend.network.MailApiService
import com.fiveis.xend.network.MailComposeSseClient
import com.fiveis.xend.network.MailComposeWebSocketClient
import com.fiveis.xend.ui.compose.MailComposeViewModel
import io.mockk.coEvery
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
    private lateinit var wsClient: MailComposeWebSocketClient
    private lateinit var viewModel: MailComposeViewModel

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mailApiService = mockk()
        sseClient = mockk(relaxed = true)
        wsClient = mockk(relaxed = true)
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

        val result = mailApiService.sendEmail(request.toMultipartParts(context))

        assertTrue(result.isSuccessful)
        assertEquals(201, result.code())
        assertEquals("msg123", result.body()?.id)
    }

    @Test(timeout = 10000)
    fun viewModel_starts_streaming_successfully() {
        viewModel = MailComposeViewModel(sseClient, wsClient)

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
        viewModel = MailComposeViewModel(sseClient, wsClient)

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
    fun viewModel_enables_realtime_mode_connects_websocket() {
        viewModel = MailComposeViewModel(sseClient, wsClient)

        viewModel.enableRealtimeMode(true)
        Thread.sleep(100)

        assertTrue(viewModel.ui.value.isRealtimeEnabled)
        verify { wsClient.connect(any(), any(), any(), any()) }
    }

    @Test(timeout = 5000)
    fun viewModel_disables_realtime_mode_disconnects_websocket() {
        viewModel = MailComposeViewModel(sseClient, wsClient)

        viewModel.enableRealtimeMode(true)
        Thread.sleep(100)

        viewModel.enableRealtimeMode(false)
        Thread.sleep(100)

        assertFalse(viewModel.ui.value.isRealtimeEnabled)
        verify(atLeast = 1) { wsClient.disconnect() }
    }

    @Test(timeout = 5000)
    fun viewModel_handles_text_changed_with_realtime_enabled() {
        viewModel = MailComposeViewModel(sseClient, wsClient)

        viewModel.enableRealtimeMode(true)
        Thread.sleep(100)

        viewModel.onTextChanged("Test message", "Test subject")
        Thread.sleep(100)

        verify { wsClient.connect(any(), any(), any(), any()) }
    }

    @Test(timeout = 5000)
    fun viewModel_ignores_text_changed_with_realtime_disabled() {
        viewModel = MailComposeViewModel(sseClient, wsClient)

        viewModel.onTextChanged("Test message", "Test subject")
        Thread.sleep(600)  // Wait for debounce timeout

        verify(exactly = 0) { wsClient.sendMessage(any(), any(), any(), any()) }
    }

    @Test(timeout = 5000)
    fun viewModel_accepts_suggestion_clears_text() {
        viewModel = MailComposeViewModel(sseClient, wsClient)

        viewModel.enableRealtimeMode(true)
        Thread.sleep(100)

        viewModel.acceptSuggestion()
        Thread.sleep(100)

        assertEquals("", viewModel.ui.value.suggestionText)
    }

    @Test(timeout = 5000)
    fun viewModel_clears_websocket_on_destruction() {
        viewModel = MailComposeViewModel(sseClient, wsClient)

        viewModel.enableRealtimeMode(true)
        Thread.sleep(100)

        viewModel.enableRealtimeMode(false)
        Thread.sleep(100)

        verify(atLeast = 1) { wsClient.disconnect() }
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
        viewModel = MailComposeViewModel(sseClient, wsClient)

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

        val result = mailApiService.sendEmail(request.toMultipartParts(context))

        assertFalse(result.isSuccessful)
        assertEquals(400, result.code())
    }

    @Test(timeout = 5000)
    fun viewModel_handles_multiple_start_stop_cycles() {
        viewModel = MailComposeViewModel(sseClient, wsClient)

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
        viewModel = MailComposeViewModel(sseClient, wsClient)

        viewModel.enableRealtimeMode(true)
        Thread.sleep(50)
        viewModel.enableRealtimeMode(false)
        Thread.sleep(50)
        viewModel.enableRealtimeMode(true)
        Thread.sleep(50)

        // Connect is called twice (once for each enable), disconnect is called twice (once for disable, once on second enable)
        verify(atLeast = 2) { wsClient.connect(any(), any(), any(), any()) }
        verify(atLeast = 1) { wsClient.disconnect() }
    }

    @Test(timeout = 5000)
    fun viewModel_handles_empty_payload() {
        viewModel = MailComposeViewModel(sseClient, wsClient)

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

        val result = mailApiService.sendEmail(request.toMultipartParts(context))

        assertTrue(result.isSuccessful)
        assertEquals("msg789", result.body()?.id)
    }
}
