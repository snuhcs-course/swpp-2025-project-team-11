package com.fiveis.xend.ui.compose

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.network.AiApiService
import com.fiveis.xend.network.MailComposeSseClient
import com.fiveis.xend.network.MailSuggestResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
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
class MailComposeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var sseClient: MailComposeSseClient
    private lateinit var aiApiService: AiApiService
    private lateinit var viewModel: MailComposeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sseClient = mockk(relaxed = true)
        aiApiService = mockk(relaxed = true)
        viewModel = MailComposeViewModel(sseClient, aiApiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun start_streaming_sets_is_streaming_true() = runTest {
        val payload = JSONObject()
        val onDoneSlot = slot<() -> Unit>()

        every {
            sseClient.start(any(), any(), any(), capture(onDoneSlot), any())
        } answers {}

        viewModel.startStreaming(payload)

        assertTrue(viewModel.ui.value.isStreaming)

        onDoneSlot.captured()
        advanceUntilIdle()
    }

    @Test
    fun start_streaming_handles_callbacks() = runTest {
        val payload = JSONObject()
        val onSubjectSlot = slot<(String) -> Unit>()
        val onBodySlot = slot<(Int, String) -> Unit>()
        val onDoneSlot = slot<() -> Unit>()

        every {
            sseClient.start(
                payload = any(),
                onSubject = capture(onSubjectSlot),
                onBodyDelta = capture(onBodySlot),
                onDone = capture(onDoneSlot),
                onError = any()
            )
        } answers {
            onSubjectSlot.captured("Subject")
            onBodySlot.captured(0, "Body")
            onDoneSlot.captured()
        }

        viewModel.startStreaming(payload)
        advanceUntilIdle()

        assertEquals("Subject", viewModel.ui.value.subject)
        assertEquals("Body", viewModel.ui.value.bodyRendered)
        verify { sseClient.start(any(), any(), any(), any(), any()) }
    }

    @Test
    fun enable_and_disable_realtime_mode_updates_state() = runTest {
        viewModel.enableRealtimeMode(true)
        assertTrue(viewModel.ui.value.isRealtimeEnabled)

        viewModel.enableRealtimeMode(false)
        assertFalse(viewModel.ui.value.isRealtimeEnabled)
        assertEquals(RealtimeConnectionStatus.IDLE, viewModel.ui.value.realtimeStatus)
    }

    @Test
    fun on_text_changed_triggers_http_request_after_debounce() = runTest {
        coEvery { aiApiService.suggestMail(any()) } returns Response.success(
            MailSuggestResponse(target = "body", suggestion = "Hello world.")
        )

        viewModel.enableRealtimeMode(true)
        viewModel.onTextChanged(
            currentText = "<p>${"a".repeat(25)}</p>",
            subject = "Subject",
            toEmails = listOf("user@example.com"),
            cursorPosition = 10
        )

        advanceUntilIdle()

        coVerify(exactly = 1) { aiApiService.suggestMail(any()) }
        assertEquals("Hello world.", viewModel.ui.value.suggestionText)
        assertEquals(RealtimeConnectionStatus.CONNECTED, viewModel.ui.value.realtimeStatus)
    }

    @Test
    fun duplicate_html_event_does_not_clear_suggestion() = runTest {
        val suggestion = "Hello world."
        coEvery { aiApiService.suggestMail(any()) } returns Response.success(
            MailSuggestResponse(target = "body", suggestion = suggestion)
        )

        viewModel.enableRealtimeMode(true)
        val html = "<p>${"a".repeat(30)}</p>"

        viewModel.onTextChanged(
            currentText = html,
            subject = "Subject",
            toEmails = listOf("user@example.com"),
            cursorPosition = 5
        )
        advanceUntilIdle()
        assertEquals(suggestion, viewModel.ui.value.suggestionText)

        viewModel.onTextChanged(
            currentText = html,
            subject = "Subject",
            toEmails = listOf("user@example.com"),
            cursorPosition = 5
        )
        advanceUntilIdle()

        assertEquals(suggestion, viewModel.ui.value.suggestionText)
        coVerify(exactly = 1) { aiApiService.suggestMail(any()) }
    }

    @Test
    fun on_text_changed_does_not_request_when_short() = runTest {
        viewModel.enableRealtimeMode(true)

        viewModel.onTextChanged(
            currentText = "short text",
            subject = "Subject",
            toEmails = listOf("user@example.com"),
            cursorPosition = 5
        )

        advanceUntilIdle()

        coVerify(exactly = 0) { aiApiService.suggestMail(any()) }
        assertEquals("", viewModel.ui.value.suggestionText)
    }

    @Test
    fun request_immediate_suggestion_honors_force_flag() = runTest {
        coEvery { aiApiService.suggestMail(any()) } returns Response.success(
            MailSuggestResponse(target = "body", suggestion = "Hi there!")
        )

        viewModel.requestImmediateSuggestion(
            currentText = "short",
            subject = "Subject",
            toEmails = emptyList(),
            cursorPosition = 3,
            force = true
        )

        advanceUntilIdle()

        coVerify { aiApiService.suggestMail(any()) }
        assertEquals("Hi there!", viewModel.ui.value.suggestionText)
    }

    @Test
    fun accept_suggestion_clears_text() = runTest {
        coEvery { aiApiService.suggestMail(any()) } returns Response.success(
            MailSuggestResponse(target = "body", suggestion = "Hi again.")
        )

        viewModel.enableRealtimeMode(true)
        viewModel.requestImmediateSuggestion(
            currentText = "This is a long enough text to trigger.",
            subject = "Subject",
            toEmails = listOf("user@example.com"),
            cursorPosition = 12,
            force = true
        )
        advanceUntilIdle()

        assertEquals("Hi again.", viewModel.ui.value.suggestionText)

        viewModel.acceptSuggestion()
        assertEquals("", viewModel.ui.value.suggestionText)
    }
}
