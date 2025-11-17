package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.repository.ContactBookRepository
import com.fiveis.xend.data.repository.ContactBookTab
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
    private lateinit var repository: ContactBookRepository
    private lateinit var viewModel: ContactBookViewModel

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        repository = mockk(relaxed = true)
    }

    @Test
    fun viewModel_initializes_with_groups_tab() = runBlocking {
        // Given
        every { repository.observeGroups() } returns flowOf(emptyList())
        every { repository.observeContacts() } returns flowOf(emptyList())
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit

        // When
        val freshViewModel = ContactBookViewModel(application, repository)
        Thread.sleep(500)
        val state = freshViewModel.uiState.first()

        // Then
        assertEquals(ContactBookTab.Groups, state.selectedTab)
        assertNotNull(state.groups)
        assertNotNull(state.contacts)
    }

    @Test
    fun onTabSelected_switches_to_contacts_tab() = runBlocking {
        // Given
        every { repository.observeGroups() } returns flowOf(emptyList())
        every { repository.observeContacts() } returns flowOf(emptyList())
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit
        viewModel = ContactBookViewModel(application, repository)
        Thread.sleep(300)

        // When
        viewModel.onTabSelected(ContactBookTab.Contacts)
        Thread.sleep(500)

        // Then
        val state = viewModel.uiState.first()
        assertEquals(ContactBookTab.Contacts, state.selectedTab)
    }

    @Test
    fun onTabSelected_switches_between_tabs() = runBlocking {
        // Given
        every { repository.observeGroups() } returns flowOf(emptyList())
        every { repository.observeContacts() } returns flowOf(emptyList())
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit
        viewModel = ContactBookViewModel(application, repository)
        Thread.sleep(300)

        viewModel.onTabSelected(ContactBookTab.Groups)
        Thread.sleep(300)

        // When
        viewModel.onTabSelected(ContactBookTab.Contacts)
        Thread.sleep(300)

        // Then
        val state = viewModel.uiState.first()
        assertEquals(ContactBookTab.Contacts, state.selectedTab)
    }

    @Test
    fun refreshAll_completes_without_error() = runBlocking {
        // Given
        every { repository.observeGroups() } returns flowOf(emptyList())
        every { repository.observeContacts() } returns flowOf(emptyList())
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit
        viewModel = ContactBookViewModel(application, repository)
        Thread.sleep(300)

        // When
        viewModel.refreshAll()
        Thread.sleep(500)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun onContactDelete_completes_operation() = runBlocking {
        // Given
        every { repository.observeGroups() } returns flowOf(emptyList())
        every { repository.observeContacts() } returns flowOf(emptyList())
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit
        coEvery { repository.deleteContact(999L) } returns Unit
        viewModel = ContactBookViewModel(application, repository)
        Thread.sleep(300)

        // When
        viewModel.onContactDelete(999L)
        Thread.sleep(500)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun onGroupDelete_completes_operation() = runBlocking {
        // Given
        every { repository.observeGroups() } returns flowOf(emptyList())
        every { repository.observeContacts() } returns flowOf(emptyList())
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit
        coEvery { repository.deleteGroup(999L) } returns Unit
        viewModel = ContactBookViewModel(application, repository)
        Thread.sleep(300)

        // When
        viewModel.onGroupDelete(999L)
        Thread.sleep(500)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun multiple_tab_switches_work() = runBlocking {
        // Given
        every { repository.observeGroups() } returns flowOf(emptyList())
        every { repository.observeContacts() } returns flowOf(emptyList())
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit
        viewModel = ContactBookViewModel(application, repository)
        Thread.sleep(300)

        // When
        viewModel.onTabSelected(ContactBookTab.Groups)
        Thread.sleep(300)
        viewModel.onTabSelected(ContactBookTab.Contacts)
        Thread.sleep(300)
        viewModel.onTabSelected(ContactBookTab.Groups)
        Thread.sleep(300)

        // Then
        val state = viewModel.uiState.first()
        assertEquals(ContactBookTab.Groups, state.selectedTab)
        assertFalse(state.isLoading)
    }

    @Test
    fun rapid_tab_switches_handled_correctly() = runBlocking {
        // Given
        every { repository.observeGroups() } returns flowOf(emptyList())
        every { repository.observeContacts() } returns flowOf(emptyList())
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit
        viewModel = ContactBookViewModel(application, repository)
        Thread.sleep(300)

        // When - Switch tabs rapidly
        viewModel.onTabSelected(ContactBookTab.Contacts)
        viewModel.onTabSelected(ContactBookTab.Groups)
        viewModel.onTabSelected(ContactBookTab.Contacts)
        Thread.sleep(500)

        // Then
        val state = viewModel.uiState.first()
        assertEquals(ContactBookTab.Contacts, state.selectedTab)
        assertFalse(state.isLoading)
    }

    @Test
    fun multiple_refreshes_dont_cause_errors() = runBlocking {
        // Given
        every { repository.observeGroups() } returns flowOf(emptyList())
        every { repository.observeContacts() } returns flowOf(emptyList())
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit
        viewModel = ContactBookViewModel(application, repository)
        Thread.sleep(300)

        // When
        viewModel.refreshAll()
        viewModel.refreshAll()
        viewModel.refreshAll()
        Thread.sleep(500)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
    }

    @Test
    fun delete_nonexistent_contact_sets_error() = runBlocking {
        // Given
        every { repository.observeGroups() } returns flowOf(emptyList())
        every { repository.observeContacts() } returns flowOf(emptyList())
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit
        coEvery { repository.deleteContact(999999L) } throws Exception("Contact not found")
        viewModel = ContactBookViewModel(application, repository)
        Thread.sleep(300)

        // When
        viewModel.onContactDelete(999999L)
        Thread.sleep(500)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun delete_nonexistent_group_sets_error() = runBlocking {
        // Given
        every { repository.observeGroups() } returns flowOf(emptyList())
        every { repository.observeContacts() } returns flowOf(emptyList())
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit
        coEvery { repository.deleteGroup(999999L) } throws Exception("Group not found")
        viewModel = ContactBookViewModel(application, repository)
        Thread.sleep(300)

        // When
        viewModel.onGroupDelete(999999L)
        Thread.sleep(500)

        // Then
        val state = viewModel.uiState.first()
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun initial_state_has_empty_collections() = runBlocking {
        // Given
        every { repository.observeGroups() } returns flowOf(emptyList())
        every { repository.observeContacts() } returns flowOf(emptyList())
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit
        val freshViewModel = ContactBookViewModel(application, repository)
        Thread.sleep(300)

        // When
        val state = freshViewModel.uiState.first()

        // Then
        assertNotNull(state.groups)
        assertNotNull(state.contacts)
    }
}
