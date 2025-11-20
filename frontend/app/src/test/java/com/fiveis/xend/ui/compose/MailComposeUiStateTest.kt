package com.fiveis.xend.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MailComposeUiStateTest {

    @Test
    fun test_defaultState() {
        val state = MailComposeUiState()

        assertFalse(state.isStreaming)
        assertEquals("", state.subject)
        assertEquals("", state.bodyRendered)
        assertNull(state.error)
        assertEquals("", state.suggestionText)
        assertFalse(state.isRealtimeEnabled)
    }

    @Test
    fun test_copy_isStreaming() {
        val state = MailComposeUiState()
        val newState = state.copy(isStreaming = true)

        assertFalse(state.isStreaming)
        assertTrue(newState.isStreaming)
    }

    @Test
    fun test_copy_subject() {
        val state = MailComposeUiState()
        val newState = state.copy(subject = "Meeting Tomorrow")

        assertEquals("", state.subject)
        assertEquals("Meeting Tomorrow", newState.subject)
    }

    @Test
    fun test_copy_bodyRendered() {
        val state = MailComposeUiState()
        val newState = state.copy(bodyRendered = "<p>Hello World</p>")

        assertEquals("", state.bodyRendered)
        assertEquals("<p>Hello World</p>", newState.bodyRendered)
    }

    @Test
    fun test_copy_error() {
        val state = MailComposeUiState()
        val newState = state.copy(error = "Failed to generate")

        assertNull(state.error)
        assertEquals("Failed to generate", newState.error)
    }

    @Test
    fun test_copy_suggestionText() {
        val state = MailComposeUiState()
        val newState = state.copy(suggestionText = "Would you like to...")

        assertEquals("", state.suggestionText)
        assertEquals("Would you like to...", newState.suggestionText)
    }

    @Test
    fun test_copy_isRealtimeEnabled() {
        val state = MailComposeUiState()
        val newState = state.copy(isRealtimeEnabled = true)

        assertFalse(state.isRealtimeEnabled)
        assertTrue(newState.isRealtimeEnabled)
    }

    @Test
    fun test_copy_multipleFields() {
        val state = MailComposeUiState()
        val newState = state.copy(
            isStreaming = true,
            subject = "Test Subject",
            bodyRendered = "<p>Body</p>",
            suggestionText = "Suggestion"
        )

        assertTrue(newState.isStreaming)
        assertEquals("Test Subject", newState.subject)
        assertEquals("<p>Body</p>", newState.bodyRendered)
        assertEquals("Suggestion", newState.suggestionText)
    }

    @Test
    fun test_copy_resetError() {
        val state = MailComposeUiState(error = "Previous error")
        val newState = state.copy(error = null)

        assertEquals("Previous error", state.error)
        assertNull(newState.error)
    }

    @Test
    fun test_copy_clearSuggestion() {
        val state = MailComposeUiState(suggestionText = "Old suggestion")
        val newState = state.copy(suggestionText = "")

        assertEquals("Old suggestion", state.suggestionText)
        assertEquals("", newState.suggestionText)
    }

    @Test
    fun test_equality_sameValues() {
        val state1 = MailComposeUiState(subject = "Test", isStreaming = true)
        val state2 = MailComposeUiState(subject = "Test", isStreaming = true)

        assertEquals(state1, state2)
    }

    @Test
    fun test_inequality_differentSubject() {
        val state1 = MailComposeUiState(subject = "Test 1")
        val state2 = MailComposeUiState(subject = "Test 2")

        assertNotEquals(state1, state2)
    }

    @Test
    fun test_copy_longBodyRendered() {
        val state = MailComposeUiState()
        val longBody = "<p>" + "A".repeat(10000) + "</p>"
        val newState = state.copy(bodyRendered = longBody)

        assertTrue(newState.bodyRendered.length > 10000)
    }

    @Test
    fun test_copy_specialCharactersInSubject() {
        val state = MailComposeUiState()
        val newState = state.copy(subject = "한글 제목 & Special 💌")

        assertEquals("한글 제목 & Special 💌", newState.subject)
    }

    @Test
    fun test_copy_htmlInBodyRendered() {
        val state = MailComposeUiState()
        val html = "<div><h1>Title</h1><p>Paragraph</p><ul><li>Item</li></ul></div>"
        val newState = state.copy(bodyRendered = html)

        assertEquals(html, newState.bodyRendered)
    }

    @Test
    fun test_copy_streamingState() {
        val state = MailComposeUiState()
        val streamingState = state.copy(
            isStreaming = true,
            bodyRendered = "Generating...",
            error = null
        )

        assertTrue(streamingState.isStreaming)
        assertEquals("Generating...", streamingState.bodyRendered)
        assertNull(streamingState.error)
    }

    @Test
    fun test_copy_errorState() {
        val state = MailComposeUiState(isStreaming = true)
        val errorState = state.copy(
            isStreaming = false,
            error = "Connection failed"
        )

        assertFalse(errorState.isStreaming)
        assertEquals("Connection failed", errorState.error)
    }
}
