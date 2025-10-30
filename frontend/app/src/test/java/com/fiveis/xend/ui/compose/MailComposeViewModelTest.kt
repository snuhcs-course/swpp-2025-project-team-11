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
import kotlinx.coroutines.test.advanceTimeBy
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
}
