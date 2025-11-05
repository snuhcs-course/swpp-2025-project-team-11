package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Group
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddContactIntegrationTest {

    private lateinit var application: Application
    private lateinit var viewModel: AddContactViewModel

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = AddContactViewModel(application)
    }

    @Test
    fun addContact_with_blank_name_shows_error() = runBlocking {
        // Given
        val name = ""
        val email = "test@test.com"

        // When
        viewModel.addContact(name, email, null, "Recipient", null, null)
        Thread.sleep(200)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertEquals("이름을 입력해 주세요.", state.error)
        assertNull(state.lastSuccessMsg)
    }

    @Test
    fun addContact_with_blank_email_shows_error() = runBlocking {
        // Given
        val name = "Test User"
        val email = ""

        // When
        viewModel.addContact(name, email, null, "Recipient", null, null)
        Thread.sleep(200)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertEquals("이메일을 입력해 주세요.", state.error)
        assertNull(state.lastSuccessMsg)
    }

    @Test
    fun addContact_with_whitespace_name_shows_error() = runBlocking {
        // Given
        val name = "   "
        val email = "test@test.com"

        // When
        viewModel.addContact(name, email, null, "Recipient", null, null)
        Thread.sleep(200)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertEquals("이름을 입력해 주세요.", state.error)
    }

    @Test
    fun addContact_with_whitespace_email_shows_error() = runBlocking {
        // Given
        val name = "Test User"
        val email = "   "

        // When
        viewModel.addContact(name, email, null, "Recipient", null, null)
        Thread.sleep(200)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertEquals("이메일을 입력해 주세요.", state.error)
    }

    @Test
    fun addContact_initial_state_is_correct() = runBlocking {
        // Given - Fresh ViewModel
        val freshViewModel = AddContactViewModel(application)

        // When
        val state = freshViewModel.uiState.first()

        // Then
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertNull(state.lastSuccessMsg)
    }

    @Test
    fun addContact_with_special_characters_in_name() = runBlocking {
        // Given
        val name = "Test!@#$%"
        val email = "test@test.com"

        // When
        viewModel.addContact(name, email, null, "Recipient", null, null)
        Thread.sleep(1000)

        // Then - Should not crash
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun addContact_with_group_parameter() = runBlocking {
        // Given
        val name = "Test User"
        val email = "test@test.com"
        val group = Group(id = 1L, name = "Test Group", description = "Test", options = emptyList())

        // When
        viewModel.addContact(name, email, null, "Recipient", null, group)
        Thread.sleep(1000)

        // Then - Should not crash
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun addContact_with_all_optional_parameters() = runBlocking {
        // Given
        val name = "Test User"
        val email = "test@test.com"

        // When
        viewModel.addContact(name, email, "Manager", "Employee", "Personal prompt", null)
        Thread.sleep(1000)

        // Then - Should not crash
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun addContact_multiple_times_updates_state() = runBlocking {
        // When - Add contact twice
        viewModel.addContact("User1", "user1@test.com", null, "Recipient", null, null)
        Thread.sleep(1000)
        viewModel.addContact("User2", "user2@test.com", null, "Recipient", null, null)
        Thread.sleep(1000)

        // Then - Should complete without crash
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun addContact_with_long_email_address() = runBlocking {
        // Given
        val name = "Test User"
        val email = "verylongemailaddressthatexceedsnormallength@verylongdomainname.com"

        // When
        viewModel.addContact(name, email, null, "Recipient", null, null)
        Thread.sleep(1000)

        // Then - Should not crash
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
    }
}
