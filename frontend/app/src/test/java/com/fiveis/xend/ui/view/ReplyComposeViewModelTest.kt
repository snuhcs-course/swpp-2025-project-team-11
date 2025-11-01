package com.fiveis.xend.ui.view

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.network.MailReplySseClient
import com.fiveis.xend.network.ReplyOptionInfo
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReplyComposeViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var api: MailReplySseClient
    private lateinit var viewModel: ReplyComposeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        api = mockk(relaxed = true)
        viewModel = ReplyComposeViewModel(api)
    }

    @Test
    fun initialState_isCorrect() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isStreaming)
        assertTrue(state.options.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun startReplyOptions_setsLoadingAndStreamingState() = runTest {
        val onReadySlot = slot<() -> Unit>()
        every {
            api.start(
                subject = any(),
                body = any(),
                toEmail = any(),
                onReady = capture(onReadySlot),
                onOptions = any(),
                onOptionDelta = any(),
                onOptionDone = any(),
                onOptionError = any(),
                onDone = any(),
                onError = any()
            )
        } just runs

        viewModel.startReplyOptions("Test Subject", "Test Body", "test@example.com")

        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isStreaming)

        onReadySlot.captured.invoke()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun startReplyOptions_callsApiStart() {
        val subject = "Test Subject"
        val body = "Test Body"
        val toEmail = "test@example.com"

        every {
            api.start(
                subject = any(),
                body = any(),
                toEmail = any(),
                onReady = any(),
                onOptions = any(),
                onOptionDelta = any(),
                onOptionDone = any(),
                onOptionError = any(),
                onDone = any(),
                onError = any()
            )
        } just runs

        viewModel.startReplyOptions(subject, body, toEmail)

        verify {
            api.start(
                subject = subject,
                body = body,
                toEmail = toEmail,
                onReady = any(),
                onOptions = any(),
                onOptionDelta = any(),
                onOptionDone = any(),
                onOptionError = any(),
                onDone = any(),
                onError = any()
            )
        }
    }

    @Test
    fun onOptions_updatesOptionsState() = runTest {
        val onOptionsSlot = slot<(List<ReplyOptionInfo>) -> Unit>()
        every {
            api.start(
                subject = any(),
                body = any(),
                toEmail = any(),
                onReady = any(),
                onOptions = capture(onOptionsSlot),
                onOptionDelta = any(),
                onOptionDone = any(),
                onOptionError = any(),
                onDone = any(),
                onError = any()
            )
        } just runs

        viewModel.startReplyOptions("Subject", "Body", "test@example.com")

        val options = listOf(
            ReplyOptionInfo(1, "formal", "Formal Reply"),
            ReplyOptionInfo(2, "casual", "Casual Reply")
        )
        onOptionsSlot.captured.invoke(options)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.options.size)
        assertEquals(1, state.options[0].id)
        assertEquals("formal", state.options[0].type)
        assertEquals("Formal Reply", state.options[0].title)
    }

    @Test
    fun onOptionDelta_appendsTextToOption() = runTest {
        val onOptionsSlot = slot<(List<ReplyOptionInfo>) -> Unit>()
        val onOptionDeltaSlot = slot<(Int, Int, String) -> Unit>()

        every {
            api.start(
                subject = any(),
                body = any(),
                toEmail = any(),
                onReady = any(),
                onOptions = capture(onOptionsSlot),
                onOptionDelta = capture(onOptionDeltaSlot),
                onOptionDone = any(),
                onOptionError = any(),
                onDone = any(),
                onError = any()
            )
        } just runs

        viewModel.startReplyOptions("Subject", "Body", "test@example.com")

        onOptionsSlot.captured.invoke(
            listOf(ReplyOptionInfo(1, "formal", "Formal Reply"))
        )
        testDispatcher.scheduler.advanceUntilIdle()

        onOptionDeltaSlot.captured.invoke(1, 0, "Hello ")
        onOptionDeltaSlot.captured.invoke(1, 1, "World")

        val state = viewModel.uiState.value
        assertEquals("Hello World", state.options[0].body)
    }

    @Test
    fun onOptionDone_marksOptionAsComplete() = runTest {
        val onOptionsSlot = slot<(List<ReplyOptionInfo>) -> Unit>()
        val onOptionDoneSlot = slot<(Int, Int) -> Unit>()

        every {
            api.start(
                subject = any(),
                body = any(),
                toEmail = any(),
                onReady = any(),
                onOptions = capture(onOptionsSlot),
                onOptionDelta = any(),
                onOptionDone = capture(onOptionDoneSlot),
                onOptionError = any(),
                onDone = any(),
                onError = any()
            )
        } just runs

        viewModel.startReplyOptions("Subject", "Body", "test@example.com")

        onOptionsSlot.captured.invoke(
            listOf(ReplyOptionInfo(1, "formal", "Formal Reply"))
        )
        testDispatcher.scheduler.advanceUntilIdle()

        onOptionDoneSlot.captured.invoke(1, 100)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.options[0].isComplete)
        assertEquals(100, state.options[0].totalSeq)
    }

    @Test
    fun onOptionError_setsErrorAndMarksComplete() = runTest {
        val onOptionsSlot = slot<(List<ReplyOptionInfo>) -> Unit>()
        val onOptionErrorSlot = slot<(Int, String) -> Unit>()

        every {
            api.start(
                subject = any(),
                body = any(),
                toEmail = any(),
                onReady = any(),
                onOptions = capture(onOptionsSlot),
                onOptionDelta = any(),
                onOptionDone = any(),
                onOptionError = capture(onOptionErrorSlot),
                onDone = any(),
                onError = any()
            )
        } just runs

        viewModel.startReplyOptions("Subject", "Body", "test@example.com")

        onOptionsSlot.captured.invoke(
            listOf(ReplyOptionInfo(1, "formal", "Formal Reply"))
        )
        testDispatcher.scheduler.advanceUntilIdle()

        onOptionErrorSlot.captured.invoke(1, "Network error")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("옵션 1 오류"))
        assertTrue(state.options[0].isComplete)
    }

    @Test
    fun onDone_stopsStreaming() = runTest {
        val onDoneSlot = slot<(String) -> Unit>()

        every {
            api.start(
                subject = any(),
                body = any(),
                toEmail = any(),
                onReady = any(),
                onOptions = any(),
                onOptionDelta = any(),
                onOptionDone = any(),
                onOptionError = any(),
                onDone = capture(onDoneSlot),
                onError = any()
            )
        } just runs

        viewModel.startReplyOptions("Subject", "Body", "test@example.com")
        assertTrue(viewModel.uiState.value.isStreaming)

        onDoneSlot.captured.invoke("complete")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isStreaming)
    }

    @Test
    fun onError_setsErrorStateAndStopsStreaming() = runTest {
        val onErrorSlot = slot<(String) -> Unit>()

        every {
            api.start(
                subject = any(),
                body = any(),
                toEmail = any(),
                onReady = any(),
                onOptions = any(),
                onOptionDelta = any(),
                onOptionDone = any(),
                onOptionError = any(),
                onDone = any(),
                onError = capture(onErrorSlot)
            )
        } just runs

        viewModel.startReplyOptions("Subject", "Body", "test@example.com")

        onErrorSlot.captured.invoke("Connection failed")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isStreaming)
        assertEquals("Connection failed", state.error)
    }

    @Test
    fun stopStreaming_callsApiStopAndUpdatesState() {
        every { api.stop() } just runs

        viewModel.startReplyOptions("Subject", "Body", "test@example.com")
        assertTrue(viewModel.uiState.value.isStreaming)

        viewModel.stopStreaming()

        verify { api.stop() }
        assertFalse(viewModel.uiState.value.isStreaming)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
