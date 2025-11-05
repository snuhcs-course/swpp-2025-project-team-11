package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.repository.ContactBookTab
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactBookViewModelIntegrationTest {

    private lateinit var application: Application
    private lateinit var viewModel: ContactBookViewModel

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = ContactBookViewModel(application)
    }

    @Test
    fun viewModel_initializes_with_groups_tab() = runBlocking {
        // Given - Fresh ViewModel
        val freshViewModel = ContactBookViewModel(application)
        Thread.sleep(500)

        // When
        val state = freshViewModel.uiState.first()

        // Then
        assertEquals(ContactBookTab.Groups, state.selectedTab)
        assertNotNull(state.groups)
        assertNotNull(state.contacts)
    }

    @Test
    fun onTabSelected_switches_to_contacts_tab() = runBlocking {
        // When
        viewModel.onTabSelected(ContactBookTab.Contacts)
        Thread.sleep(1000)

        // Then
        val state = viewModel.uiState.first()
        assertEquals(ContactBookTab.Contacts, state.selectedTab)
    }

    @Test
    fun onTabSelected_switches_between_tabs() = runBlocking {
        // Given
        viewModel.onTabSelected(ContactBookTab.Groups)
        Thread.sleep(500)

        // When
        viewModel.onTabSelected(ContactBookTab.Contacts)
        Thread.sleep(500)

        // Then
        val state = viewModel.uiState.first()
        assertEquals(ContactBookTab.Contacts, state.selectedTab)
    }

    @Test
    fun refreshAll_completes_without_error() = runBlocking {
        // When
        viewModel.refreshAll()
        Thread.sleep(2000)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun onContactDelete_completes_operation() = runBlocking {
        // When
        viewModel.onContactDelete(999L)
        Thread.sleep(1500)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun onGroupDelete_completes_operation() = runBlocking {
        // When
        viewModel.onGroupDelete(999L)
        Thread.sleep(1500)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun multiple_tab_switches_work() = runBlocking {
        // When
        viewModel.onTabSelected(ContactBookTab.Groups)
        Thread.sleep(500)
        viewModel.onTabSelected(ContactBookTab.Contacts)
        Thread.sleep(500)
        viewModel.onTabSelected(ContactBookTab.Groups)
        Thread.sleep(500)

        // Then
        val state = viewModel.uiState.first()
        assertEquals(ContactBookTab.Groups, state.selectedTab)
        assertFalse(state.isLoading)
    }

    @Test
    fun rapid_tab_switches_handled_correctly() = runBlocking {
        // When - Switch tabs rapidly
        viewModel.onTabSelected(ContactBookTab.Contacts)
        viewModel.onTabSelected(ContactBookTab.Groups)
        viewModel.onTabSelected(ContactBookTab.Contacts)
        Thread.sleep(1000)

        // Then
        val state = viewModel.uiState.first()
        assertEquals(ContactBookTab.Contacts, state.selectedTab)
        assertFalse(state.isLoading)
    }

    @Test
    fun multiple_refreshes_dont_cause_errors() = runBlocking {
        // When
        viewModel.refreshAll()
        viewModel.refreshAll()
        viewModel.refreshAll()
        Thread.sleep(2000)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun delete_nonexistent_contact_completes() = runBlocking {
        // When
        viewModel.onContactDelete(999999L)
        Thread.sleep(1500)

        // Then - Should not crash
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun delete_nonexistent_group_completes() = runBlocking {
        // When
        viewModel.onGroupDelete(999999L)
        Thread.sleep(1500)

        // Then - Should not crash
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun initial_state_has_empty_collections() = runBlocking {
        // Given - Fresh ViewModel
        val freshViewModel = ContactBookViewModel(application)
        Thread.sleep(500)

        // When
        val state = freshViewModel.uiState.first()

        // Then
        assertNotNull(state.groups)
        assertNotNull(state.contacts)
    }
}
