package com.fiveis.xend.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileUiStateTest {

    @Test
    fun test_defaultState() {
        val state = ProfileUiState()

        assertEquals("", state.userEmail)
        assertEquals("", state.displayName)
        assertEquals("", state.info)
        assertFalse(state.isLoading)
        assertFalse(state.isEditing)
        assertFalse(state.isSaving)
        assertFalse(state.isLoggingOut)
        assertNull(state.logoutError)
        assertFalse(state.logoutSuccess)
        assertFalse(state.showLogoutFailureDialog)
        assertFalse(state.showLogoutSuccessToast)
        assertNull(state.profileError)
        assertFalse(state.saveSuccess)
        assertNull(state.originalDisplayName)
        assertNull(state.originalInfo)
    }

    @Test
    fun test_copy_userEmail() {
        val state = ProfileUiState()
        val newState = state.copy(userEmail = "user@example.com")

        assertEquals("user@example.com", newState.userEmail)
    }

    @Test
    fun test_copy_displayName() {
        val state = ProfileUiState()
        val newState = state.copy(displayName = "John Doe")

        assertEquals("John Doe", newState.displayName)
    }

    @Test
    fun test_copy_info() {
        val state = ProfileUiState()
        val newState = state.copy(info = "Software Engineer")

        assertEquals("Software Engineer", newState.info)
    }

    @Test
    fun test_copy_isLoading() {
        val state = ProfileUiState()
        val newState = state.copy(isLoading = true)

        assertTrue(newState.isLoading)
    }

    @Test
    fun test_copy_isEditing() {
        val state = ProfileUiState()
        val newState = state.copy(isEditing = true, originalDisplayName = "Original", originalInfo = "Original Info")

        assertTrue(newState.isEditing)
        assertEquals("Original", newState.originalDisplayName)
        assertEquals("Original Info", newState.originalInfo)
    }

    @Test
    fun test_copy_isSaving() {
        val state = ProfileUiState()
        val newState = state.copy(isSaving = true)

        assertTrue(newState.isSaving)
    }

    @Test
    fun test_copy_isLoggingOut() {
        val state = ProfileUiState()
        val newState = state.copy(isLoggingOut = true)

        assertTrue(newState.isLoggingOut)
    }

    @Test
    fun test_copy_logoutSuccess() {
        val state = ProfileUiState()
        val newState = state.copy(logoutSuccess = true, showLogoutSuccessToast = true)

        assertTrue(newState.logoutSuccess)
        assertTrue(newState.showLogoutSuccessToast)
    }

    @Test
    fun test_copy_logoutError() {
        val state = ProfileUiState()
        val newState = state.copy(logoutError = "Logout failed", showLogoutFailureDialog = true)

        assertEquals("Logout failed", newState.logoutError)
        assertTrue(newState.showLogoutFailureDialog)
    }

    @Test
    fun test_copy_profileError() {
        val state = ProfileUiState()
        val newState = state.copy(profileError = "Failed to load profile")

        assertEquals("Failed to load profile", newState.profileError)
    }

    @Test
    fun test_copy_saveSuccess() {
        val state = ProfileUiState()
        val newState = state.copy(saveSuccess = true, isEditing = false)

        assertTrue(newState.saveSuccess)
        assertFalse(newState.isEditing)
    }

    @Test
    fun test_copy_enterEditMode() {
        val state = ProfileUiState(displayName = "John", info = "Engineer")
        val newState = state.copy(
            isEditing = true,
            originalDisplayName = state.displayName,
            originalInfo = state.info
        )

        assertTrue(newState.isEditing)
        assertEquals("John", newState.originalDisplayName)
        assertEquals("Engineer", newState.originalInfo)
    }

    @Test
    fun test_copy_exitEditMode() {
        val state = ProfileUiState(isEditing = true, originalDisplayName = "John", originalInfo = "Engineer")
        val newState = state.copy(isEditing = false, originalDisplayName = null, originalInfo = null)

        assertFalse(newState.isEditing)
        assertNull(newState.originalDisplayName)
        assertNull(newState.originalInfo)
    }

    @Test
    fun test_copy_resetErrors() {
        val state = ProfileUiState(logoutError = "Error", profileError = "Error")
        val newState = state.copy(logoutError = null, profileError = null)

        assertNull(newState.logoutError)
        assertNull(newState.profileError)
    }

    @Test
    fun test_copy_multipleFields() {
        val state = ProfileUiState()
        val newState = state.copy(
            userEmail = "user@example.com",
            displayName = "John Doe",
            info = "Engineer",
            isLoading = false
        )

        assertEquals("user@example.com", newState.userEmail)
        assertEquals("John Doe", newState.displayName)
        assertEquals("Engineer", newState.info)
        assertFalse(newState.isLoading)
    }
}
