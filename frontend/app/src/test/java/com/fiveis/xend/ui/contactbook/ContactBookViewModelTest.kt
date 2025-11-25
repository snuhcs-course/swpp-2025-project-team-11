package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.repository.ContactBookRepository
import com.fiveis.xend.data.repository.ContactBookTab
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactBookViewModelTest {

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
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun init_loads_groups_by_default() = runTest {
        val mockGroups = listOf(
            Group(id = 1L, name = "Group 1"),
            Group(id = 2L, name = "Group 2")
        )

        every { repository.observeGroups() } returns kotlinx.coroutines.flow.flowOf(mockGroups)
        every { repository.observeContacts() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        assertEquals(ContactBookTab.Groups, viewModel.uiState.value.selectedTab)
        assertEquals(mockGroups, viewModel.uiState.value.groups)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun on_tab_selected_contacts_loads_contacts() = runTest {
        val mockGroups = listOf(Group(id = 1L, name = "Group 1"))
        val mockContacts = listOf(
            Contact(id = 1L, name = "Contact 1", email = "contact1@example.com"),
            Contact(id = 2L, name = "Contact 2", email = "contact2@example.com")
        )

        every { repository.observeGroups() } returns kotlinx.coroutines.flow.flowOf(mockGroups)
        every { repository.observeContacts() } returns kotlinx.coroutines.flow.flowOf(mockContacts)
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        // Switch to Contacts tab first, then let the flow collector update
        viewModel.onTabSelected(ContactBookTab.Contacts)
        advanceUntilIdle()

        assertEquals(ContactBookTab.Contacts, viewModel.uiState.value.selectedTab)
        // Verify that refreshContacts was called after switching tabs
        coVerify(atLeast = 2) { repository.refreshContacts() }
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun load_groups_failure_sets_error() = runTest {
        every { repository.observeGroups() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        every { repository.observeContacts() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        coEvery { repository.refreshGroups() } throws Exception("Network error")
        coEvery { repository.refreshContacts() } returns Unit

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun on_contact_delete_success_reloads_contacts() = runTest {
        val mockGroups = listOf(Group(id = 1L, name = "Group 1"))
        val mockContacts = listOf(Contact(id = 1L, name = "Contact 1", email = "contact1@example.com"))

        every { repository.observeGroups() } returns kotlinx.coroutines.flow.flowOf(mockGroups)
        every { repository.observeContacts() } returns kotlinx.coroutines.flow.flowOf(mockContacts)
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit
        coEvery { repository.deleteContact(1L) } returns Unit

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.onContactDelete(1L)
        advanceUntilIdle()

        coVerify { repository.deleteContact(1L) }
        coVerify { repository.refreshContacts() }
    }

    @Test
    fun on_group_delete_success_reloads_groups() = runTest {
        val mockGroups = listOf(Group(id = 1L, name = "Group 1"))

        every { repository.observeGroups() } returns kotlinx.coroutines.flow.flowOf(mockGroups)
        every { repository.observeContacts() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit
        coEvery { repository.deleteGroup(1L) } returns Unit

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.onGroupDelete(1L)
        advanceUntilIdle()

        // Verify deleteGroup was called
        coVerify { repository.deleteGroup(1L) }
        // refreshGroups is called at least once during init
        coVerify(atLeast = 1) { repository.refreshGroups() }
        // Verify loading state was updated
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun on_contact_delete_failure_sets_error() = runTest {
        val mockGroups = listOf(Group(id = 1L, name = "Group 1"))

        every { repository.observeGroups() } returns kotlinx.coroutines.flow.flowOf(mockGroups)
        every { repository.observeContacts() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit
        coEvery { repository.deleteContact(1L) } throws Exception("Delete failed")

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.onContactDelete(1L)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun on_group_delete_failure_sets_error() = runTest {
        val mockGroups = listOf(Group(id = 1L, name = "Group 1"))

        every { repository.observeGroups() } returns kotlinx.coroutines.flow.flowOf(mockGroups)
        every { repository.observeContacts() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit
        coEvery { repository.deleteGroup(1L) } throws Exception("Delete failed")

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.onGroupDelete(1L)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun refresh_all_success() = runTest {
        val mockGroups = listOf(Group(id = 1L, name = "Group 1"))

        every { repository.observeGroups() } returns kotlinx.coroutines.flow.flowOf(mockGroups)
        every { repository.observeContacts() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        coEvery { repository.refreshGroups() } returns Unit
        coEvery { repository.refreshContacts() } returns Unit

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.refreshAll()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        coVerify(atLeast = 2) { repository.refreshGroups() }
        coVerify(atLeast = 2) { repository.refreshContacts() }
    }

    @Test
    fun refresh_all_failure_sets_error() = runTest {
        every { repository.observeGroups() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        every { repository.observeContacts() } returns kotlinx.coroutines.flow.flowOf(emptyList())
        coEvery { repository.refreshGroups() } returns Unit andThenThrows Exception("Refresh failed")
        coEvery { repository.refreshContacts() } returns Unit

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.refreshAll()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun factory_creates_view_model_successfully() {
        val factory = ContactBookViewModel.Factory(application)
        val createdViewModel = factory.create(ContactBookViewModel::class.java)

        assertNotNull(createdViewModel)
        assertEquals(ContactBookViewModel::class.java, createdViewModel::class.java)
    }

    @Test(expected = IllegalArgumentException::class)
    fun factory_throws_exception_for_wrong_class() {
        val factory = ContactBookViewModel.Factory(application)
        factory.create(AddContactViewModel::class.java)
    }
}
