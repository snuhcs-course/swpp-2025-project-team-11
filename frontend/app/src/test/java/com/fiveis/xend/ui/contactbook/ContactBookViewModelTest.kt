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
        repository = mockk()

        every { application.applicationContext } returns application

        mockkConstructor(ContactBookRepository::class)
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

        coEvery { anyConstructed<ContactBookRepository>().getAllGroups() } returns mockGroups

        viewModel = ContactBookViewModel(application)
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

        coEvery { anyConstructed<ContactBookRepository>().getAllGroups() } returns mockGroups
        coEvery { anyConstructed<ContactBookRepository>().getAllContacts() } returns mockContacts

        viewModel = ContactBookViewModel(application)
        advanceUntilIdle()

        viewModel.onTabSelected(ContactBookTab.Contacts)
        advanceUntilIdle()

        assertEquals(ContactBookTab.Contacts, viewModel.uiState.value.selectedTab)
        assertEquals(mockContacts, viewModel.uiState.value.contacts)
        assertEquals(emptyList<Group>(), viewModel.uiState.value.groups)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun load_groups_failure_sets_error() = runTest {
        coEvery { anyConstructed<ContactBookRepository>().getAllGroups() } throws Exception("Network error")

        viewModel = ContactBookViewModel(application)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun on_contact_delete_success_reloads_contacts() = runTest {
        val mockGroups = listOf(Group(id = 1L, name = "Group 1"))
        val mockContacts = listOf(Contact(id = 1L, name = "Contact 1", email = "contact1@example.com"))

        coEvery { anyConstructed<ContactBookRepository>().getAllGroups() } returns mockGroups
        coEvery { anyConstructed<ContactBookRepository>().getAllContacts() } returns mockContacts
        coEvery { anyConstructed<ContactBookRepository>().deleteContact(1L) } returns Unit

        viewModel = ContactBookViewModel(application)
        advanceUntilIdle()

        viewModel.onContactDelete(1L)
        advanceUntilIdle()

        coVerify { anyConstructed<ContactBookRepository>().deleteContact(1L) }
        coVerify(atLeast = 1) { anyConstructed<ContactBookRepository>().getAllContacts() }
    }

    @Test
    fun on_group_delete_success_reloads_groups() = runTest {
        val mockGroups = listOf(Group(id = 1L, name = "Group 1"))

        coEvery { anyConstructed<ContactBookRepository>().getAllGroups() } returns mockGroups
        coEvery { anyConstructed<ContactBookRepository>().deleteGroup(1L) } returns Unit

        viewModel = ContactBookViewModel(application)
        advanceUntilIdle()

        viewModel.onGroupDelete(1L)
        advanceUntilIdle()

        coVerify { anyConstructed<ContactBookRepository>().deleteGroup(1L) }
        coVerify(atLeast = 2) { anyConstructed<ContactBookRepository>().getAllGroups() }
    }
}
