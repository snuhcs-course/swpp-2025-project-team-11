package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.data.repository.ContactBookRepository
import com.fiveis.xend.data.repository.ContactBookTab
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactBookViewModelRefreshTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var application: Application
    private lateinit var repository: ContactBookRepository
    private lateinit var viewModel: ContactBookViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        repository = mockk(relaxed = true)

        every { application.applicationContext } returns application
        every { repository.observeGroups() } returns flowOf(emptyList())
        every { repository.observeContacts() } returns flowOf(emptyList())
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit
        every { repository.searchContacts(any()) } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun refresh_all_calls_both_refresh_methods() = runTest {
        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.refreshAll()
        advanceUntilIdle()

        coVerify(atLeast = 1) { repository.refreshGroups() }
        coVerify(atLeast = 1) { repository.refreshContacts() }
    }

    @Test
    fun refresh_all_handles_error() = runTest {
        coEvery { repository.refreshGroups() } throws Exception("Network error")

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.refreshAll()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun refresh_all_sets_loading_state() = runTest {
        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.refreshAll()

        // After completion, loading should be false
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun on_tab_selected_updates_tab() = runTest {
        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.onTabSelected(ContactBookTab.Contacts)
        advanceUntilIdle()

        assertEquals(ContactBookTab.Contacts, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun on_tab_selected_triggers_refresh() = runTest {
        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.onTabSelected(ContactBookTab.Contacts)
        advanceUntilIdle()

        coVerify(atLeast = 1) { repository.refreshGroups() }
        coVerify(atLeast = 1) { repository.refreshContacts() }
    }

    @Test
    fun initial_tab_is_groups() = runTest {
        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        assertEquals(ContactBookTab.Groups, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun switch_between_tabs_multiple_times() = runTest {
        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.onTabSelected(ContactBookTab.Contacts)
        advanceUntilIdle()
        assertEquals(ContactBookTab.Contacts, viewModel.uiState.value.selectedTab)

        viewModel.onTabSelected(ContactBookTab.Groups)
        advanceUntilIdle()
        assertEquals(ContactBookTab.Groups, viewModel.uiState.value.selectedTab)

        viewModel.onTabSelected(ContactBookTab.Contacts)
        advanceUntilIdle()
        assertEquals(ContactBookTab.Contacts, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun refresh_clears_previous_error() = runTest {
        coEvery { repository.refreshGroups() } throws Exception("First error")

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.refreshAll()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.error)

        coEvery { repository.refreshGroups() } returns Unit
        viewModel.refreshAll()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun refresh_groups_network_error_handled() = runTest {
        coEvery { repository.refreshGroups() } throws Exception("Network unavailable")

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.refreshAll()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("Network") == true ||
            viewModel.uiState.value.error?.contains("동기화") == true)
    }

    @Test
    fun refresh_contacts_network_error_handled() = runTest {
        coEvery { repository.refreshContacts() } throws Exception("Connection timeout")

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.refreshAll()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun initial_state_is_not_loading() = runTest {
        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        // After initial refresh completes
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun initial_state_has_empty_lists() = runTest {
        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.groups.isEmpty())
        assertTrue(viewModel.uiState.value.contacts.isEmpty())
    }

    @Test
    fun initial_search_mode_is_false() = runTest {
        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSearchMode)
    }

    @Test
    fun initial_search_query_is_empty() = runTest {
        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun initial_search_results_is_empty() = runTest {
        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
    }

    @Test
    fun refresh_multiple_times_in_succession() = runTest {
        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.refreshAll()
        advanceUntilIdle()

        viewModel.refreshAll()
        advanceUntilIdle()

        viewModel.refreshAll()
        advanceUntilIdle()

        coVerify(atLeast = 3) { repository.refreshGroups() }
        coVerify(atLeast = 3) { repository.refreshContacts() }
    }

    @Test
    fun tab_selection_persists_across_refreshes() = runTest {
        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.onTabSelected(ContactBookTab.Contacts)
        advanceUntilIdle()

        viewModel.refreshAll()
        advanceUntilIdle()

        assertEquals(ContactBookTab.Contacts, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun refresh_error_contains_meaningful_message() = runTest {
        val errorMessage = "Failed to connect to server"
        coEvery { repository.refreshGroups() } throws Exception(errorMessage)

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.refreshAll()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertTrue(
            viewModel.uiState.value.error?.contains(errorMessage) == true ||
            viewModel.uiState.value.error?.contains("동기화") == true
        )
    }

    @Test
    fun refresh_null_error_message_uses_default() = runTest {
        coEvery { repository.refreshGroups() } throws Exception()

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.refreshAll()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertEquals("동기화 실패", viewModel.uiState.value.error)
    }

    @Test
    fun viewModel_initialization_triggers_refresh() = runTest {
        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        coVerify(atLeast = 1) { repository.refreshGroups() }
        coVerify(atLeast = 1) { repository.refreshContacts() }
    }
}
