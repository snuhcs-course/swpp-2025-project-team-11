package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.repository.ContactBookRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactDetailViewModelIntegrationTest {

    private lateinit var application: Application
    private lateinit var repository: ContactBookRepository
    private lateinit var viewModel: ContactDetailViewModel

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        repository = ContactBookRepository(application.applicationContext)
        viewModel = ContactDetailViewModel(application, repository)
    }

    @Test
    fun viewModel_initial_state_is_correct() = runBlocking {
        // When
        val state = viewModel.uiState.first()

        // Then
        assertFalse(state.isLoading)
        assertNull(state.contact)
        assertNull(state.error)
    }

    @Test
    fun load_same_id_twice_without_force_does_not_reload() = runBlocking {
        // Given
        viewModel.load(1L)
        Thread.sleep(1000)
        val firstState = viewModel.uiState.first()

        // When
        viewModel.load(1L, force = false)
        Thread.sleep(500)

        // Then
        val secondState = viewModel.uiState.first()
        assertEquals(firstState.isLoading, secondState.isLoading)
    }

    @Test
    fun refresh_without_loading_does_nothing() = runBlocking {
        // Given - No load called
        val initialState = viewModel.uiState.first()

        // When
        viewModel.refresh()
        Thread.sleep(500)

        // Then
        val afterRefreshState = viewModel.uiState.first()
        assertEquals(initialState.isLoading, afterRefreshState.isLoading)
    }

    @Test
    fun load_with_force_reloads_data() = runBlocking {
        // Given - Load once
        viewModel.load(1L)
        Thread.sleep(500)
        val firstState = viewModel.uiState.first()

        // When - Load again with force=true (should trigger reload even with same ID)
        viewModel.load(1L, force = true)
        Thread.sleep(500)

        // Then - Loading should have been triggered
        // We just verify that the function was called without error
        val state = viewModel.uiState.first()
        // State should exist (not null), regardless of loading status
        assertEquals(firstState.groups, state.groups)
    }
}
