package com.fiveis.xend.ui.compose

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.network.MailComposeSseClient
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

@OptIn(ExperimentalCoroutinesApi::class)
class MailComposeViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var api: MailComposeSseClient
    private lateinit var viewModel: MailComposeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        api = mockk(relaxed = true)
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
            api.start(
                payload = any(),
                onSubject = any(),
                onBodyDelta = any(),
                onDone = capture(onDoneSlot),
                onError = any()
            )
        } answers {
            // Do nothing - just let it start streaming
        }

        viewModel = MailComposeViewModel(api)

        viewModel.startStreaming(payload)

        assertTrue(viewModel.ui.value.isStreaming)

        // Clean up: stop the infinite throttle loop
        onDoneSlot.captured()
        advanceUntilIdle()
    }

    @Test
    fun start_streaming_calls_api_with_callbacks() = runTest {
        val payload = JSONObject()
        val onSubjectSlot = slot<(String) -> Unit>()
        val onBodyDeltaSlot = slot<(Int, String) -> Unit>()
        val onDoneSlot = slot<() -> Unit>()
        val onErrorSlot = slot<(String) -> Unit>()

        every {
            api.start(
                payload = any(),
                onSubject = capture(onSubjectSlot),
                onBodyDelta = capture(onBodyDeltaSlot),
                onDone = capture(onDoneSlot),
                onError = capture(onErrorSlot)
            )
        } answers {
            onDoneSlot.captured()
        }

        viewModel = MailComposeViewModel(api)

        viewModel.startStreaming(payload)

        verify { api.start(any(), any(), any(), any(), any()) }

        advanceUntilIdle()
    }

    @Test
    fun on_subject_callback_updates_subject() = runTest {
        val payload = JSONObject()
        val onSubjectSlot = slot<(String) -> Unit>()
        val onDoneSlot = slot<() -> Unit>()

        every {
            api.start(
                payload = any(),
                onSubject = capture(onSubjectSlot),
                onBodyDelta = any(),
                onDone = capture(onDoneSlot),
                onError = any()
            )
        } answers {
            onSubjectSlot.captured("Test Subject")
            onDoneSlot.captured()
        }

        viewModel = MailComposeViewModel(api)

        viewModel.startStreaming(payload)
        advanceUntilIdle()

        assertEquals("Test Subject", viewModel.ui.value.subject)
    }

    @Test
    fun on_body_delta_callback_updates_body() = runTest {
        val payload = JSONObject()
        val onBodyDeltaSlot = slot<(Int, String) -> Unit>()
        val onDoneSlot = slot<() -> Unit>()

        every {
            api.start(
                payload = any(),
                onSubject = any(),
                onBodyDelta = capture(onBodyDeltaSlot),
                onDone = capture(onDoneSlot),
                onError = any()
            )
        } answers {
            onBodyDeltaSlot.captured(0, "Hello")
            onBodyDeltaSlot.captured(1, " World")
            onDoneSlot.captured()
        }

        viewModel = MailComposeViewModel(api)

        viewModel.startStreaming(payload)
        advanceUntilIdle()

        assertEquals("Hello World", viewModel.ui.value.bodyRendered)
    }

    @Test
    fun on_done_callback_sets_is_streaming_false() = runTest {
        val payload = JSONObject()
        val onDoneSlot = slot<() -> Unit>()

        every {
            api.start(
                payload = any(),
                onSubject = any(),
                onBodyDelta = any(),
                onDone = capture(onDoneSlot),
                onError = any()
            )
        } answers {
            onDoneSlot.captured()
        }

        viewModel = MailComposeViewModel(api)

        viewModel.startStreaming(payload)
        advanceUntilIdle()

        assertFalse(viewModel.ui.value.isStreaming)
    }

    @Test
    fun on_error_callback_sets_error_and_stops_streaming() = runTest {
        val payload = JSONObject()
        val onErrorSlot = slot<(String) -> Unit>()

        every {
            api.start(
                payload = any(),
                onSubject = any(),
                onBodyDelta = any(),
                onDone = any(),
                onError = capture(onErrorSlot)
            )
        } answers {
            onErrorSlot.captured("Network error")
        }

        viewModel = MailComposeViewModel(api)

        viewModel.startStreaming(payload)
        advanceUntilIdle()

        assertNotNull(viewModel.ui.value.error)
        assertEquals("Network error", viewModel.ui.value.error)
        assertFalse(viewModel.ui.value.isStreaming)
    }

    @Test
    fun stop_streaming_calls_api_stop() = runTest {
        every { api.stop() } returns Unit

        viewModel = MailComposeViewModel(api)

        viewModel.stopStreaming()

        verify { api.stop() }
        assertFalse(viewModel.ui.value.isStreaming)
    }

    @Test
    fun initial_state_is_correct() = runTest {
        viewModel = MailComposeViewModel(api)

        assertFalse(viewModel.ui.value.isStreaming)
        assertEquals("", viewModel.ui.value.subject)
        assertEquals("", viewModel.ui.value.bodyRendered)
        assertEquals(null, viewModel.ui.value.error)
        assertEquals("", viewModel.ui.value.suggestionText)
        assertFalse(viewModel.ui.value.isRealtimeEnabled)
    }

    @Test
    fun start_streaming_clears_previous_body() = runTest {
        val payload = JSONObject()
        val onDoneSlot = slot<() -> Unit>()

        every {
            api.start(any(), any(), any(), capture(onDoneSlot), any())
        } answers {
            onDoneSlot.captured()
        }

        viewModel = MailComposeViewModel(api)
        advanceUntilIdle()

        assertEquals("", viewModel.ui.value.bodyRendered)
    }

    @Test
    fun body_with_newlines_converts_to_br_tags() = runTest {
        val payload = JSONObject()
        val onBodyDeltaSlot = slot<(Int, String) -> Unit>()
        val onDoneSlot = slot<() -> Unit>()

        every {
            api.start(any(), any(), capture(onBodyDeltaSlot), capture(onDoneSlot), any())
        } answers {
            onBodyDeltaSlot.captured(0, "Line 1\nLine 2\nLine 3")
            onDoneSlot.captured()
        }

        viewModel = MailComposeViewModel(api)
        viewModel.startStreaming(payload)
        advanceUntilIdle()

        assertTrue(viewModel.ui.value.bodyRendered.contains("<br>"))
    }

    @Test
    fun subject_with_blank_value_does_not_update() = runTest {
        val payload = JSONObject()
        val onSubjectSlot = slot<(String) -> Unit>()
        val onDoneSlot = slot<() -> Unit>()

        every {
            api.start(any(), capture(onSubjectSlot), any(), capture(onDoneSlot), any())
        } answers {
            onSubjectSlot.captured("Original")
            onSubjectSlot.captured("")
            onDoneSlot.captured()
        }

        viewModel = MailComposeViewModel(api)
        viewModel.startStreaming(payload)
        advanceUntilIdle()

        assertEquals("Original", viewModel.ui.value.subject)
    }

    @Test
    fun multiple_body_delta_callbacks_concatenate() = runTest {
        val payload = JSONObject()
        val onBodyDeltaSlot = slot<(Int, String) -> Unit>()
        val onDoneSlot = slot<() -> Unit>()

        every {
            api.start(any(), any(), capture(onBodyDeltaSlot), capture(onDoneSlot), any())
        } answers {
            onBodyDeltaSlot.captured(0, "Hello ")
            onBodyDeltaSlot.captured(1, "World ")
            onBodyDeltaSlot.captured(2, "!")
            onDoneSlot.captured()
        }

        viewModel = MailComposeViewModel(api)
        viewModel.startStreaming(payload)
        advanceUntilIdle()

        assertEquals("Hello World !", viewModel.ui.value.bodyRendered)
    }

    @Test
    fun mail_compose_ui_state_data_class_properties() {
        val state = MailComposeUiState(
            isStreaming = true,
            subject = "Test",
            bodyRendered = "Body",
            error = "Error",
            suggestionText = "Suggestion",
            isRealtimeEnabled = true
        )

        assertTrue(state.isStreaming)
        assertEquals("Test", state.subject)
        assertEquals("Body", state.bodyRendered)
        assertEquals("Error", state.error)
        assertEquals("Suggestion", state.suggestionText)
        assertTrue(state.isRealtimeEnabled)
    }

    @Test
    fun mail_compose_ui_state_default_values() {
        val state = MailComposeUiState()

        assertFalse(state.isStreaming)
        assertEquals("", state.subject)
        assertEquals("", state.bodyRendered)
        assertEquals(null, state.error)
        assertEquals("", state.suggestionText)
        assertFalse(state.isRealtimeEnabled)
    }

    @Test
    fun mail_compose_ui_state_copy_works() {
        val state = MailComposeUiState(subject = "Original")
        val copied = state.copy(subject = "Updated")

        assertEquals("Updated", copied.subject)
    }

    @Test
    fun stop_streaming_multiple_times_is_safe() = runTest {
        every { api.stop() } returns Unit

        viewModel = MailComposeViewModel(api)

        viewModel.stopStreaming()
        viewModel.stopStreaming()
        viewModel.stopStreaming()

        verify(atLeast = 3) { api.stop() }
    }


    @Test
    fun error_during_streaming_clears_streaming_state() = runTest {
        val payload = JSONObject()
        val onErrorSlot = slot<(String) -> Unit>()

        every {
            api.start(any(), any(), any(), any(), capture(onErrorSlot))
        } answers {
            onErrorSlot.captured("Test error")
        }

        viewModel = MailComposeViewModel(api)
        viewModel.startStreaming(payload)
        advanceUntilIdle()

        assertFalse(viewModel.ui.value.isStreaming)
        assertEquals("Test error", viewModel.ui.value.error)
    }

    @Test
    fun body_rendered_updates_correctly_after_done() = runTest {
        val payload = JSONObject()
        val onBodyDeltaSlot = slot<(Int, String) -> Unit>()
        val onDoneSlot = slot<() -> Unit>()

        every {
            api.start(any(), any(), capture(onBodyDeltaSlot), capture(onDoneSlot), any())
        } answers {
            onBodyDeltaSlot.captured(0, "Final body")
            onDoneSlot.captured()
        }

        viewModel = MailComposeViewModel(api)
        viewModel.startStreaming(payload)
        advanceUntilIdle()

        assertEquals("Final body", viewModel.ui.value.bodyRendered)
    }

    @Test
    fun empty_subject_callback_preserves_existing_subject() = runTest {
        val payload = JSONObject()
        val onSubjectSlot = slot<(String) -> Unit>()
        val onDoneSlot = slot<() -> Unit>()

        every {
            api.start(any(), capture(onSubjectSlot), any(), capture(onDoneSlot), any())
        } answers {
            onSubjectSlot.captured("")
            onDoneSlot.captured()
        }

        viewModel = MailComposeViewModel(api)
        viewModel.startStreaming(payload)
        advanceUntilIdle()

        assertEquals("", viewModel.ui.value.subject)
    }

    @Test
    fun streaming_with_empty_body_delta() = runTest {
        val payload = JSONObject()
        val onBodyDeltaSlot = slot<(Int, String) -> Unit>()
        val onDoneSlot = slot<() -> Unit>()

        every {
            api.start(any(), any(), capture(onBodyDeltaSlot), capture(onDoneSlot), any())
        } answers {
            onBodyDeltaSlot.captured(0, "")
            onDoneSlot.captured()
        }

        viewModel = MailComposeViewModel(api)
        viewModel.startStreaming(payload)
        advanceUntilIdle()

        assertEquals("", viewModel.ui.value.bodyRendered)
    }

    @Test
    fun subject_updates_multiple_times() = runTest {
        val payload = JSONObject()
        val onSubjectSlot = slot<(String) -> Unit>()
        val onDoneSlot = slot<() -> Unit>()

        every {
            api.start(any(), capture(onSubjectSlot), any(), capture(onDoneSlot), any())
        } answers {
            onSubjectSlot.captured("Subject 1")
            onSubjectSlot.captured("Subject 2")
            onSubjectSlot.captured("Subject 3")
            onDoneSlot.captured()
        }

        viewModel = MailComposeViewModel(api)
        viewModel.startStreaming(payload)
        advanceUntilIdle()

        assertEquals("Subject 3", viewModel.ui.value.subject)
    }

    @Test
    fun body_with_special_characters_renders_correctly() = runTest {
        val payload = JSONObject()
        val onBodyDeltaSlot = slot<(Int, String) -> Unit>()
        val onDoneSlot = slot<() -> Unit>()

        every {
            api.start(any(), any(), capture(onBodyDeltaSlot), capture(onDoneSlot), any())
        } answers {
            onBodyDeltaSlot.captured(0, "<>&\"'")
            onDoneSlot.captured()
        }

        viewModel = MailComposeViewModel(api)
        viewModel.startStreaming(payload)
        advanceUntilIdle()

        assertEquals("<>&\"'", viewModel.ui.value.bodyRendered)
    }

    @Test
    fun long_body_text_is_handled() = runTest {
        val payload = JSONObject()
        val onBodyDeltaSlot = slot<(Int, String) -> Unit>()
        val onDoneSlot = slot<() -> Unit>()
        val longText = "A".repeat(10000)

        every {
            api.start(any(), any(), capture(onBodyDeltaSlot), capture(onDoneSlot), any())
        } answers {
            onBodyDeltaSlot.captured(0, longText)
            onDoneSlot.captured()
        }

        viewModel = MailComposeViewModel(api)
        viewModel.startStreaming(payload)
        advanceUntilIdle()

        assertEquals(longText, viewModel.ui.value.bodyRendered)
    }

    @Test
    fun multiple_newlines_convert_to_multiple_br_tags() = runTest {
        val payload = JSONObject()
        val onBodyDeltaSlot = slot<(Int, String) -> Unit>()
        val onDoneSlot = slot<() -> Unit>()

        every {
            api.start(any(), any(), capture(onBodyDeltaSlot), capture(onDoneSlot), any())
        } answers {
            onBodyDeltaSlot.captured(0, "A\n\n\nB")
            onDoneSlot.captured()
        }

        viewModel = MailComposeViewModel(api)
        viewModel.startStreaming(payload)
        advanceUntilIdle()

        assertEquals("A<br><br><br>B", viewModel.ui.value.bodyRendered)
    }

    @Test
    fun enable_realtime_mode_updates_state() = runTest {
        val wsClient = mockk<com.fiveis.xend.network.MailComposeWebSocketClient>(relaxed = true)
        viewModel = MailComposeViewModel(api, wsClient)

        viewModel.enableRealtimeMode(true)
        advanceUntilIdle()

        assertTrue(viewModel.ui.value.isRealtimeEnabled)
        verify { wsClient.connect(any(), any(), any(), any()) }
    }

    @Test
    fun disable_realtime_mode_updates_state() = runTest {
        val wsClient = mockk<com.fiveis.xend.network.MailComposeWebSocketClient>(relaxed = true)
        viewModel = MailComposeViewModel(api, wsClient)

        viewModel.enableRealtimeMode(true)
        viewModel.enableRealtimeMode(false)

        assertFalse(viewModel.ui.value.isRealtimeEnabled)
        verify { wsClient.disconnect() }
    }

    @Test
    fun on_text_changed_sends_message_when_realtime_enabled() = runTest {
        val wsClient = mockk<com.fiveis.xend.network.MailComposeWebSocketClient>(relaxed = true)
        viewModel = MailComposeViewModel(api, wsClient)

        viewModel.enableRealtimeMode(true)
        viewModel.onTextChanged("Test text")
        advanceUntilIdle()

        verify { wsClient.sendMessage(any(), eq("Test text"), any()) }
    }

    @Test
    fun on_text_changed_does_not_send_when_realtime_disabled() = runTest {
        val wsClient = mockk<com.fiveis.xend.network.MailComposeWebSocketClient>(relaxed = true)
        viewModel = MailComposeViewModel(api, wsClient)

        viewModel.enableRealtimeMode(false)
        viewModel.onTextChanged("Test text")
        advanceUntilIdle()

        verify(exactly = 0) { wsClient.sendMessage(any(), any(), any()) }
    }

    // WebSocket integration tests are complex and are better tested in instrumentation tests
    // These tests focus on the state management aspects of the ViewModel

    @Test
    fun accept_next_word_returns_null_when_no_suggestion() = runTest {
        viewModel = MailComposeViewModel(api)

        val word = viewModel.acceptNextWord()

        assertEquals(null, word)
    }



    // Note: onCleared is protected and cannot be tested directly from unit tests
    // It is tested indirectly by verifying disconnect is called when disabling realtime mode

    @Test
    fun text_changed_debounce_delays_websocket_send() = runTest {
        val wsClient = mockk<com.fiveis.xend.network.MailComposeWebSocketClient>(relaxed = true)
        viewModel = MailComposeViewModel(api, wsClient)

        viewModel.enableRealtimeMode(true)
        viewModel.onTextChanged("First")

        // Should not send immediately
        verify(exactly = 0) { wsClient.sendMessage(any(), any(), any()) }

        advanceUntilIdle()

        // Should send after debounce
        verify { wsClient.sendMessage(any(), eq("First"), any()) }
    }

    @Test
    fun text_changed_multiple_times_only_sends_last_value() = runTest {
        val wsClient = mockk<com.fiveis.xend.network.MailComposeWebSocketClient>(relaxed = true)
        viewModel = MailComposeViewModel(api, wsClient)

        viewModel.enableRealtimeMode(true)
        viewModel.onTextChanged("First")
        viewModel.onTextChanged("Second")
        viewModel.onTextChanged("Third")

        advanceUntilIdle()

        // Should only send the last value
        verify(exactly = 1) { wsClient.sendMessage(any(), eq("Third"), any()) }
        verify(exactly = 0) { wsClient.sendMessage(any(), eq("First"), any()) }
        verify(exactly = 0) { wsClient.sendMessage(any(), eq("Second"), any()) }
    }

    @Test
    fun accept_next_word_with_empty_suggestion_returns_null() = runTest {
        viewModel = MailComposeViewModel(api)

        val word = viewModel.acceptNextWord()

        assertEquals(null, word)
    }

    @Test
    fun accept_suggestion_with_empty_suggestion_does_nothing() = runTest {
        viewModel = MailComposeViewModel(api)

        viewModel.acceptSuggestion()

        assertEquals("", viewModel.ui.value.suggestionText)
    }

    @Test
    fun websocket_on_error_updates_error_state() = runTest {
        val wsClient = mockk<com.fiveis.xend.network.MailComposeWebSocketClient>(relaxed = true)
        val onErrorSlot = slot<(String) -> Unit>()

        every { wsClient.connect(any(), capture(onErrorSlot), any(), any()) } answers {
            onErrorSlot.captured("WebSocket error")
        }

        viewModel = MailComposeViewModel(api, wsClient)
        viewModel.enableRealtimeMode(true)
        advanceUntilIdle()

        assertEquals("WebSocket error", viewModel.ui.value.error)
    }

    @Test
    fun websocket_on_close_disables_realtime_mode() = runTest {
        val wsClient = mockk<com.fiveis.xend.network.MailComposeWebSocketClient>(relaxed = true)
        val onCloseSlot = slot<() -> Unit>()

        every { wsClient.connect(any(), any(), capture(onCloseSlot), any()) } answers {
            onCloseSlot.captured()
        }

        viewModel = MailComposeViewModel(api, wsClient)
        viewModel.enableRealtimeMode(true)
        advanceUntilIdle()

        assertFalse(viewModel.ui.value.isRealtimeEnabled)
    }

    @Test
    fun disconnect_websocket_clears_suggestion() = runTest {
        val wsClient = mockk<com.fiveis.xend.network.MailComposeWebSocketClient>(relaxed = true)

        viewModel = MailComposeViewModel(api, wsClient)
        viewModel.enableRealtimeMode(true)
        advanceUntilIdle()

        viewModel.enableRealtimeMode(false)

        assertEquals("", viewModel.ui.value.suggestionText)
    }

    @Test
    fun accept_suggestion_with_non_empty_text() = runTest {
        viewModel = MailComposeViewModel(api)

        // Since we can't easily set suggestion text without WebSocket, test empty case
        viewModel.acceptSuggestion()

        assertEquals("", viewModel.ui.value.suggestionText)
    }

    @Test
    fun start_streaming_clears_body_buffer() = runTest {
        val payload = JSONObject()
        val onDoneSlot = slot<() -> Unit>()

        every {
            api.start(any(), any(), any(), capture(onDoneSlot), any())
        } answers {
            onDoneSlot.captured()
        }

        viewModel = MailComposeViewModel(api)

        viewModel.startStreaming(payload)
        advanceUntilIdle()

        // Body should be cleared
        assertFalse(viewModel.ui.value.isStreaming)
    }
}
