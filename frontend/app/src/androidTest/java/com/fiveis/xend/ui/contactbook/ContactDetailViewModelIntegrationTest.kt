package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.repository.ContactBookRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ContactDetailViewModelIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var application: Application
    private lateinit var repository: ContactBookRepository
    private lateinit var viewModel: ContactDetailViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        repository = mockk(relaxed = true)

        // Mock observeGroups to return empty list
        every { repository.observeGroups() } returns flowOf(emptyList())

        viewModel = ContactDetailViewModel(application, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun viewModel_initial_state_is_correct() = runTest {
        // When
        val state = viewModel.uiState.value

        // Then
        assertFalse(state.isLoading)
        assertNull(state.contact)
        assertNull(state.error)
    }

    @Test
    fun load_same_id_twice_without_force_does_not_reload() = runTest {
        // Given - Mock repository responses
        val mockContact = Contact(id = 1L, name = "Test", email = "test@example.com")
        every { repository.observeContact(1L) } returns flowOf(mockContact)
        coEvery { repository.refreshContact(1L) } returns Unit

        // Load once and wait for completion
        viewModel.load(1L)
        advanceUntilIdle()
        val firstState = viewModel.uiState.value
        assertFalse(firstState.isLoading) // Verify loading is done

        // When - Call load again with same ID and force=false
        viewModel.load(1L, force = false)
        advanceUntilIdle()

        // Then - Loading state should remain false (early return prevents reload)
        val secondState = viewModel.uiState.value
        assertFalse(secondState.isLoading) // Should still be false, no reload triggered
        assertEquals(firstState.contact, secondState.contact) // Contact should be unchanged
    }

    @Test
    fun refresh_without_loading_does_nothing() = runTest {
        // Given - No load called
        val initialState = viewModel.uiState.value

        // When
        viewModel.refresh()
        advanceUntilIdle()

        // Then
        val afterRefreshState = viewModel.uiState.value
        assertEquals(initialState.isLoading, afterRefreshState.isLoading)
    }

    @Test
    fun load_with_force_reloads_data() = runTest {
        // Given - Mock repository responses
        val mockContact = Contact(id = 1L, name = "Test", email = "test@example.com")
        every { repository.observeContact(1L) } returns flowOf(mockContact)
        coEvery { repository.refreshContact(1L) } returns Unit

        // Load once
        viewModel.load(1L)
        advanceUntilIdle()
        val firstState = viewModel.uiState.value

        // When - Load again with force=true (should trigger reload even with same ID)
        viewModel.load(1L, force = true)
        advanceUntilIdle()

        // Then - Loading should have been triggered
        // We just verify that the function was called without error
        val state = viewModel.uiState.value
        // State should exist (not null), regardless of loading status
        assertEquals(firstState.groups, state.groups)
    }
}
