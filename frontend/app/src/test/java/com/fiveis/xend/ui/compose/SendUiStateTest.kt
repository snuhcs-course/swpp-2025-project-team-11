package com.fiveis.xend.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SendUiStateTest {

    @Test
    fun test_sendUiState_default() {
        val state = SendUiState()

        assertFalse(state.isSending)
        assertNull(state.lastSuccessMsg)
        assertNull(state.error)
    }

    @Test
    fun test_sendUiState_copy_isSending() {
        val state = SendUiState()
        val newState = state.copy(isSending = true)

        assertFalse(state.isSending)
        assertTrue(newState.isSending)
    }

    @Test
    fun test_sendUiState_copy_lastSuccessMsg() {
        val state = SendUiState()
        val newState = state.copy(lastSuccessMsg = "Email sent successfully")

        assertNull(state.lastSuccessMsg)
        assertEquals("Email sent successfully", newState.lastSuccessMsg)
    }

    @Test
    fun test_sendUiState_copy_error() {
        val state = SendUiState()
        val newState = state.copy(error = "Failed to send")

        assertNull(state.error)
        assertEquals("Failed to send", newState.error)
    }

    @Test
    fun test_sendUiState_sendingState() {
        val state = SendUiState(isSending = true)

        assertTrue(state.isSending)
        assertNull(state.lastSuccessMsg)
        assertNull(state.error)
    }

    @Test
    fun test_sendUiState_successState() {
        val state = SendUiState(
            isSending = false,
            lastSuccessMsg = "전송 완료: msg_123",
            error = null
        )

        assertFalse(state.isSending)
        assertEquals("전송 완료: msg_123", state.lastSuccessMsg)
        assertNull(state.error)
    }

    @Test
    fun test_sendUiState_errorState() {
        val state = SendUiState(
            isSending = false,
            lastSuccessMsg = null,
            error = "Network error"
        )

        assertFalse(state.isSending)
        assertNull(state.lastSuccessMsg)
        assertEquals("Network error", state.error)
    }

    @Test
    fun test_sendUiState_equality() {
        val state1 = SendUiState(isSending = true, error = "Error")
        val state2 = SendUiState(isSending = true, error = "Error")

        assertEquals(state1, state2)
        assertEquals(state1.hashCode(), state2.hashCode())
    }

    @Test
    fun test_sendUiState_inequality() {
        val state1 = SendUiState(isSending = true)
        val state2 = SendUiState(isSending = false)

        assertNotEquals(state1, state2)
    }

    @Test
    fun test_sendUiState_resetError() {
        val state = SendUiState(error = "Previous error")
        val newState = state.copy(error = null)

        assertEquals("Previous error", state.error)
        assertNull(newState.error)
    }

    @Test
    fun test_sendUiState_resetSuccessMsg() {
        val state = SendUiState(lastSuccessMsg = "Previous success")
        val newState = state.copy(lastSuccessMsg = null)

        assertEquals("Previous success", state.lastSuccessMsg)
        assertNull(newState.lastSuccessMsg)
    }

    @Test
    fun test_sendUiState_longSuccessMessage() {
        val longMsg = "전송 완료: " + "A".repeat(1000)
        val state = SendUiState(lastSuccessMsg = longMsg)

        assert(state.lastSuccessMsg!!.length > 1000)
    }

    @Test
    fun test_sendUiState_specialCharactersInError() {
        val state = SendUiState(error = "오류 발생: 네트워크 연결 실패 ❌")

        assertEquals("오류 발생: 네트워크 연결 실패 ❌", state.error)
    }
}
