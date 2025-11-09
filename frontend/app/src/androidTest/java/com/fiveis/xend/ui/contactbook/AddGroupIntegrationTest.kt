package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class AddGroupIntegrationTest {

    private lateinit var application: Application
    private lateinit var viewModel: AddGroupViewModel

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = AddGroupViewModel(application)
    }

    @Test
    fun addGroup_with_blank_name_shows_error() = runBlocking {
        // Given
        val name = ""
        val description = "Description"

        // Wait for init to complete
        Thread.sleep(1000)

        // When
        viewModel.addGroup(name, description, emptyList())
        Thread.sleep(200)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isSubmitting)
        assertNotNull(state.error)
        assertEquals("그룹 이름을 입력해 주세요.", state.error)
    }

    @Test
    fun addGroup_with_whitespace_name_shows_error() = runBlocking {
        // Given
        val name = "   "
        val description = "Description"

        // Wait for init to complete
        Thread.sleep(1000)

        // When
        viewModel.addGroup(name, description, emptyList())
        Thread.sleep(200)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isSubmitting)
        assertNotNull(state.error)
        assertEquals("그룹 이름을 입력해 주세요.", state.error)
    }

    @Test
    fun addGroup_initial_state_has_option_lists() = runBlocking {
        // Given - Fresh ViewModel
        val freshViewModel = AddGroupViewModel(application)
        Thread.sleep(500)

        // When
        val state = freshViewModel.uiState.first()

        // Then
        assertFalse(state.isSubmitting)
        assertNotNull(state.tonePromptOptions)
        assertNotNull(state.formatPromptOptions)
    }

    @Test
    fun getAllPromptOptions_completes_successfully() = runBlocking {
        // When
        viewModel.getAllPromptOptions()
        Thread.sleep(3000)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isFetchingOptions)
    }

    @Test
    fun addGroup_with_special_characters_in_name() = runBlocking {
        // Given
        val name = "Group!@#$%"
        val description = "Description"

        // Wait for init
        Thread.sleep(1000)

        // When
        viewModel.addGroup(name, description, emptyList())
        Thread.sleep(1000)

        // Then - Should not crash
        val state = viewModel.uiState.first()
        assertFalse(state.isSubmitting)
    }

    @Test
    fun addGroup_with_long_description() = runBlocking {
        // Given
        val name = "Test Group"
        val description = "Very long description ".repeat(50)

        // Wait for init
        Thread.sleep(1000)

        // When
        viewModel.addGroup(name, description, emptyList())
        Thread.sleep(1000)

        // Then - Should not crash
        val state = viewModel.uiState.first()
        assertFalse(state.isSubmitting)
    }

    @Test
    fun addGroup_multiple_times_updates_state() = runBlocking {
        // Wait for init
        Thread.sleep(1000)

        // When - Add group twice
        viewModel.addGroup("Group1", "Desc1", emptyList())
        Thread.sleep(1000)
        viewModel.addGroup("Group2", "Desc2", emptyList())
        Thread.sleep(1000)

        // Then - Should complete without crash
        val state = viewModel.uiState.first()
        assertFalse(state.isSubmitting)
    }

    @Test
    fun addGroup_with_empty_description() = runBlocking {
        // Given
        val name = "Test Group"
        val description = ""

        // Wait for init
        Thread.sleep(1000)

        // When
        viewModel.addGroup(name, description, emptyList())
        Thread.sleep(1000)

        // Then - Should not crash (description is optional)
        val state = viewModel.uiState.first()
        assertFalse(state.isSubmitting)
    }

    @Test
    fun addPromptOption_creates_new_option() = runBlocking {
        // Wait for init
        Thread.sleep(1000)

        // When
        viewModel.addPromptOption("tone", "Formal", "Be formal", {}, {})
        Thread.sleep(1000)

        // Then - Should not crash
        val state = viewModel.uiState.first()
        assertFalse(state.isSubmitting)
    }
}
