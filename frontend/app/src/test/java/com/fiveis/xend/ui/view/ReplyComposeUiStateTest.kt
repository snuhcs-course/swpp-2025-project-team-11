package com.fiveis.xend.ui.view

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyComposeUiStateTest {

    @Test
    fun test_defaultState() {
        val state = ReplyComposeUiState()

        assertFalse(state.isLoading)
        assertFalse(state.isStreaming)
        assertEquals(emptyList<ReplyOptionState>(), state.options)
        assertNull(state.error)
    }

    @Test
    fun test_copy_isLoading() {
        val state = ReplyComposeUiState()
        val newState = state.copy(isLoading = true)

        assertTrue(newState.isLoading)
    }

    @Test
    fun test_copy_isStreaming() {
        val state = ReplyComposeUiState()
        val newState = state.copy(isStreaming = true)

        assertTrue(newState.isStreaming)
    }

    @Test
    fun test_copy_options() {
        val state = ReplyComposeUiState()
        val options = listOf(
            ReplyOptionState(id = 1, type = "formal", title = "Formal Reply"),
            ReplyOptionState(id = 2, type = "casual", title = "Casual Reply")
        )
        val newState = state.copy(options = options)

        assertEquals(2, newState.options.size)
        assertEquals("Formal Reply", newState.options[0].title)
        assertEquals("Casual Reply", newState.options[1].title)
    }

    @Test
    fun test_copy_error() {
        val state = ReplyComposeUiState()
        val newState = state.copy(error = "Failed to generate")

        assertEquals("Failed to generate", newState.error)
    }

    @Test
    fun test_replyOptionState_default() {
        val option = ReplyOptionState(id = 1, type = "formal", title = "Formal")

        assertEquals(1, option.id)
        assertEquals("formal", option.type)
        assertEquals("Formal", option.title)
        assertEquals("", option.body)
        assertFalse(option.isComplete)
        assertEquals(0, option.totalSeq)
    }

    @Test
    fun test_replyOptionState_withBody() {
        val option = ReplyOptionState(
            id = 1,
            type = "formal",
            title = "Formal Reply",
            body = "This is a formal response",
            isComplete = false,
            totalSeq = 5
        )

        assertEquals("This is a formal response", option.body)
        assertFalse(option.isComplete)
        assertEquals(5, option.totalSeq)
    }

    @Test
    fun test_replyOptionState_complete() {
        val option = ReplyOptionState(
            id = 1,
            type = "formal",
            title = "Formal",
            body = "Complete body",
            isComplete = true,
            totalSeq = 10
        )

        assertTrue(option.isComplete)
        assertEquals(10, option.totalSeq)
    }

    @Test
    fun test_replyOptionState_copy() {
        val original = ReplyOptionState(id = 1, type = "formal", title = "Formal")
        val modified = original.copy(body = "New body", isComplete = true)

        assertEquals("", original.body)
        assertFalse(original.isComplete)
        assertEquals("New body", modified.body)
        assertTrue(modified.isComplete)
    }

    @Test
    fun test_copy_multipleOptions() {
        val state = ReplyComposeUiState()
        val options = (1..5).map {
            ReplyOptionState(id = it, type = "type$it", title = "Title $it")
        }
        val newState = state.copy(options = options)

        assertEquals(5, newState.options.size)
        assertEquals("Title 1", newState.options[0].title)
        assertEquals("Title 5", newState.options[4].title)
    }

    @Test
    fun test_copy_streamingState() {
        val state = ReplyComposeUiState()
        val newState = state.copy(isLoading = true, isStreaming = true)

        assertTrue(newState.isLoading)
        assertTrue(newState.isStreaming)
    }

    @Test
    fun test_copy_errorState() {
        val state = ReplyComposeUiState(isLoading = true, isStreaming = true)
        val newState = state.copy(isLoading = false, isStreaming = false, error = "Connection failed")

        assertFalse(newState.isLoading)
        assertFalse(newState.isStreaming)
        assertEquals("Connection failed", newState.error)
    }

    @Test
    fun test_replyOptionState_longBody() {
        val longBody = "A".repeat(10000)
        val option = ReplyOptionState(
            id = 1,
            type = "formal",
            title = "Formal",
            body = longBody
        )

        assertEquals(10000, option.body.length)
    }

    @Test
    fun test_replyOptionState_specialCharactersInTitle() {
        val option = ReplyOptionState(
            id = 1,
            type = "formal",
            title = "한글 제목 🎉"
        )

        assertEquals("한글 제목 🎉", option.title)
    }
}
