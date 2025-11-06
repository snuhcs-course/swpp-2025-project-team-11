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
}
