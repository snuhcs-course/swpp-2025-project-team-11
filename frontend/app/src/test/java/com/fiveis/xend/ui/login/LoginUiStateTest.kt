package com.fiveis.xend.ui.login

import org.junit.Assert.*
import org.junit.Test

class LoginUiStateTest {

    @Test
    fun loginUiState_default_values() {
        val state = LoginUiState()

        assertFalse(state.isLoggedIn)
        assertEquals("", state.userEmail)
        assertEquals("", state.messages)
        assertFalse(state.isLoading)
    }

    @Test
    fun loginUiState_with_logged_in_true() {
        val state = LoginUiState(isLoggedIn = true)

        assertTrue(state.isLoggedIn)
        assertEquals("", state.userEmail)
        assertEquals("", state.messages)
        assertFalse(state.isLoading)
    }

    @Test
    fun loginUiState_with_user_email() {
        val email = "test@example.com"
        val state = LoginUiState(userEmail = email)

        assertFalse(state.isLoggedIn)
        assertEquals(email, state.userEmail)
    }

    @Test
    fun loginUiState_with_messages() {
        val message = "Login successful"
        val state = LoginUiState(messages = message)

        assertEquals(message, state.messages)
    }

    @Test
    fun loginUiState_with_loading_true() {
        val state = LoginUiState(isLoading = true)

        assertTrue(state.isLoading)
    }

    @Test
    fun loginUiState_with_all_fields() {
        val state = LoginUiState(
            isLoggedIn = true,
            userEmail = "user@example.com",
            messages = "Welcome back",
            isLoading = false
        )

        assertTrue(state.isLoggedIn)
        assertEquals("user@example.com", state.userEmail)
        assertEquals("Welcome back", state.messages)
        assertFalse(state.isLoading)
    }

    @Test
    fun loginUiState_copy_with_isLoggedIn() {
        val original = LoginUiState()
        val updated = original.copy(isLoggedIn = true)

        assertFalse(original.isLoggedIn)
        assertTrue(updated.isLoggedIn)
    }

    @Test
    fun loginUiState_copy_with_userEmail() {
        val original = LoginUiState()
        val newEmail = "newemail@example.com"
        val updated = original.copy(userEmail = newEmail)

        assertEquals("", original.userEmail)
        assertEquals(newEmail, updated.userEmail)
    }

    @Test
    fun loginUiState_copy_with_messages() {
        val original = LoginUiState()
        val newMessage = "Error occurred"
        val updated = original.copy(messages = newMessage)

        assertEquals("", original.messages)
        assertEquals(newMessage, updated.messages)
    }

    @Test
    fun loginUiState_copy_with_isLoading() {
        val original = LoginUiState()
        val updated = original.copy(isLoading = true)

        assertFalse(original.isLoading)
        assertTrue(updated.isLoading)
    }

    @Test
    fun loginUiState_logged_in_with_email() {
        val state = LoginUiState(
            isLoggedIn = true,
            userEmail = "loggedin@example.com"
        )

        assertTrue(state.isLoggedIn)
        assertEquals("loggedin@example.com", state.userEmail)
    }

    @Test
    fun loginUiState_logged_out_with_empty_email() {
        val state = LoginUiState(
            isLoggedIn = false,
            userEmail = ""
        )

        assertFalse(state.isLoggedIn)
        assertTrue(state.userEmail.isEmpty())
    }

    @Test
    fun loginUiState_with_error_message() {
        val errorMessage = "Authentication failed"
        val state = LoginUiState(messages = errorMessage)

        assertEquals(errorMessage, state.messages)
        assertFalse(state.isLoggedIn)
    }

    @Test
    fun loginUiState_with_success_message() {
        val successMessage = "Login successful"
        val state = LoginUiState(
            isLoggedIn = true,
            messages = successMessage
        )

        assertTrue(state.isLoggedIn)
        assertEquals(successMessage, state.messages)
    }

    @Test
    fun loginUiState_loading_state() {
        val state = LoginUiState(
            isLoading = true,
            messages = "Logging in..."
        )

        assertTrue(state.isLoading)
        assertEquals("Logging in...", state.messages)
    }

    @Test
    fun loginUiState_transition_from_logged_out_to_logged_in() {
        val loggedOut = LoginUiState(isLoggedIn = false)
        val loggedIn = loggedOut.copy(
            isLoggedIn = true,
            userEmail = "user@example.com"
        )

        assertFalse(loggedOut.isLoggedIn)
        assertTrue(loggedIn.isLoggedIn)
        assertEquals("user@example.com", loggedIn.userEmail)
    }

    @Test
    fun loginUiState_transition_from_logged_in_to_logged_out() {
        val loggedIn = LoginUiState(
            isLoggedIn = true,
            userEmail = "user@example.com"
        )
        val loggedOut = loggedIn.copy(
            isLoggedIn = false,
            userEmail = ""
        )

        assertTrue(loggedIn.isLoggedIn)
        assertFalse(loggedOut.isLoggedIn)
        assertTrue(loggedOut.userEmail.isEmpty())
    }

    @Test
    fun loginUiState_clear_messages() {
        val withMessage = LoginUiState(messages = "Some message")
        val cleared = withMessage.copy(messages = "")

        assertEquals("Some message", withMessage.messages)
        assertTrue(cleared.messages.isEmpty())
    }

    @Test
    fun loginUiState_email_validation_scenario() {
        val validEmail = "valid@example.com"
        val state = LoginUiState(userEmail = validEmail)

        assertTrue(state.userEmail.contains("@"))
        assertTrue(state.userEmail.contains("."))
    }

    @Test
    fun loginUiState_multiple_copy_operations() {
        val initial = LoginUiState()
        val step1 = initial.copy(isLoading = true)
        val step2 = step1.copy(userEmail = "test@example.com")
        val final = step2.copy(isLoggedIn = true, isLoading = false)

        assertFalse(initial.isLoggedIn)
        assertFalse(initial.isLoading)
        assertTrue(step1.isLoading)
        assertEquals("test@example.com", step2.userEmail)
        assertTrue(final.isLoggedIn)
        assertFalse(final.isLoading)
    }

    @Test
    fun loginUiState_equality_check() {
        val state1 = LoginUiState(
            isLoggedIn = true,
            userEmail = "test@example.com"
        )
        val state2 = LoginUiState(
            isLoggedIn = true,
            userEmail = "test@example.com"
        )

        assertEquals(state1.isLoggedIn, state2.isLoggedIn)
        assertEquals(state1.userEmail, state2.userEmail)
    }

    @Test
    fun loginUiState_with_long_email() {
        val longEmail = "verylongemailaddresswithmanycharacters@exampledomain.com"
        val state = LoginUiState(userEmail = longEmail)

        assertEquals(longEmail, state.userEmail)
        assertTrue(state.userEmail.length > 30)
    }

    @Test
    fun loginUiState_with_special_characters_in_email() {
        val specialEmail = "user+test@example.com"
        val state = LoginUiState(userEmail = specialEmail)

        assertEquals(specialEmail, state.userEmail)
        assertTrue(state.userEmail.contains("+"))
    }

    @Test
    fun loginUiState_with_multiline_message() {
        val multilineMessage = "Line 1\nLine 2\nLine 3"
        val state = LoginUiState(messages = multilineMessage)

        assertEquals(multilineMessage, state.messages)
        assertTrue(state.messages.contains("\n"))
    }

    @Test
    fun loginUiState_empty_string_vs_null() {
        val emptyStringState = LoginUiState(userEmail = "")

        assertTrue(emptyStringState.userEmail.isEmpty())
        assertNotNull(emptyStringState.userEmail)
    }

    @Test
    fun loginUiState_loading_without_login() {
        val state = LoginUiState(
            isLoading = true,
            isLoggedIn = false
        )

        assertTrue(state.isLoading)
        assertFalse(state.isLoggedIn)
    }

    @Test
    fun loginUiState_logged_in_not_loading() {
        val state = LoginUiState(
            isLoggedIn = true,
            isLoading = false,
            userEmail = "user@example.com"
        )

        assertTrue(state.isLoggedIn)
        assertFalse(state.isLoading)
        assertFalse(state.userEmail.isEmpty())
    }

    @Test
    fun loginUiState_partial_update() {
        val original = LoginUiState(
            isLoggedIn = false,
            userEmail = "old@example.com",
            messages = "Old message"
        )
        val updated = original.copy(messages = "New message")

        assertEquals("old@example.com", updated.userEmail)
        assertEquals("New message", updated.messages)
        assertFalse(updated.isLoggedIn)
    }

    @Test
    fun loginUiState_all_false_and_empty() {
        val state = LoginUiState(
            isLoggedIn = false,
            userEmail = "",
            messages = "",
            isLoading = false
        )

        assertFalse(state.isLoggedIn)
        assertTrue(state.userEmail.isEmpty())
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun loginUiState_with_trimmed_email() {
        val emailWithSpaces = "  test@example.com  "
        val state = LoginUiState(userEmail = emailWithSpaces.trim())

        assertEquals("test@example.com", state.userEmail)
        assertFalse(state.userEmail.startsWith(" "))
        assertFalse(state.userEmail.endsWith(" "))
    }
}
