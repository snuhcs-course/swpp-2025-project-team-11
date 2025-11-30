package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.repository.ContactBookRepository
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactDetailViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var application: Application
    private lateinit var repository: ContactBookRepository
    private lateinit var viewModel: ContactDetailViewModel

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
    fun load_contact_success_updates_state() = runTest {
        val contactId = 1L
        val mockContact = Contact(
            id = contactId,
            name = "John Doe",
            email = "john@example.com"
        )

        every { repository.observeContact(contactId) } returns kotlinx.coroutines.flow.flowOf(mockContact)
        coEvery { repository.refreshContact(contactId) } returns Unit

        viewModel = ContactDetailViewModel(application, repository)

        viewModel.load(contactId)
        advanceUntilIdle()

        assertEquals(mockContact, viewModel.uiState.value.contact)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun load_contact_failure_sets_error() = runTest {
        val contactId = 1L

        every { repository.observeContact(contactId) } returns kotlinx.coroutines.flow.flowOf(null)
        coEvery { repository.refreshContact(contactId) } throws Exception("Network error")

        viewModel = ContactDetailViewModel(application, repository)

        viewModel.load(contactId)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun load_same_contact_without_force_does_nothing() = runTest {
        val contactId = 1L
        val mockContact = Contact(
            id = contactId,
            name = "John Doe",
            email = "john@example.com"
        )

        every { repository.observeContact(contactId) } returns kotlinx.coroutines.flow.flowOf(mockContact)
        coEvery { repository.refreshContact(contactId) } returns Unit

        viewModel = ContactDetailViewModel(application, repository)

        viewModel.load(contactId)
        advanceUntilIdle()

        viewModel.load(contactId, force = false)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.refreshContact(contactId) }
    }

    @Test
    fun load_same_contact_with_force_reloads() = runTest {
        val contactId = 1L
        val mockContact = Contact(
            id = contactId,
            name = "John Doe",
            email = "john@example.com"
        )

        every { repository.observeContact(contactId) } returns kotlinx.coroutines.flow.flowOf(mockContact)
        coEvery { repository.refreshContact(contactId) } returns Unit

        viewModel = ContactDetailViewModel(application, repository)

        viewModel.load(contactId)
        advanceUntilIdle()

        viewModel.load(contactId, force = true)
        advanceUntilIdle()

        coVerify(exactly = 2) { repository.refreshContact(contactId) }
    }

    @Test
    fun refresh_contact_success_updates_state() = runTest {
        val contactId = 1L
        val mockContact = Contact(id = contactId, name = "John", email = "john@test.com")

        every { repository.observeContact(contactId) } returns kotlinx.coroutines.flow.flowOf(mockContact)
        coEvery { repository.refreshContact(contactId) } returns Unit

        viewModel = ContactDetailViewModel(application, repository)
        viewModel.load(contactId)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
        coVerify(exactly = 2) { repository.refreshContact(contactId) }
    }

    @Test
    fun refresh_contact_failure_sets_error() = runTest {
        val contactId = 1L
        val mockContact = Contact(id = contactId, name = "John", email = "john@test.com")

        every { repository.observeContact(contactId) } returns kotlinx.coroutines.flow.flowOf(mockContact)
        coEvery { repository.refreshContact(contactId) } returns Unit andThenThrows Exception("Network error")

        viewModel = ContactDetailViewModel(application, repository)
        viewModel.load(contactId)
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun update_contact_success() = runTest {
        val contactId = 1L
        val mockContact = Contact(id = contactId, name = "John", email = "john@test.com")

        every { repository.observeContact(contactId) } returns kotlinx.coroutines.flow.flowOf(mockContact)
        coEvery { repository.refreshContact(contactId) } returns Unit
        coEvery {
            repository.updateContact(contactId, "Jane", "jane@test.com", "sender", "recipient", "prompt", null, null)
        } returns Unit

        viewModel = ContactDetailViewModel(application, repository)
        viewModel.load(contactId)
        advanceUntilIdle()

        viewModel.updateContact("Jane", "jane@test.com", "sender", "recipient", "prompt", null, null)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUpdating)
        assertNull(viewModel.uiState.value.updateError)
        coVerify { repository.updateContact(contactId, "Jane", "jane@test.com", "sender", "recipient", "prompt", null, null) }
    }

    @Test
    fun update_contact_failure_sets_error() = runTest {
        val contactId = 1L
        val mockContact = Contact(id = contactId, name = "John", email = "john@test.com")

        every { repository.observeContact(contactId) } returns kotlinx.coroutines.flow.flowOf(mockContact)
        coEvery { repository.refreshContact(contactId) } returns Unit
        coEvery {
            repository.updateContact(any(), any(), any(), any(), any(), any(), any(), any())
        } throws Exception("Update failed")

        viewModel = ContactDetailViewModel(application, repository)
        viewModel.load(contactId)
        advanceUntilIdle()

        viewModel.updateContact("Jane", "jane@test.com", null, null, null, null, null)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isUpdating)
        assertNotNull(viewModel.uiState.value.updateError)
    }

    @Test
    fun clear_update_error() = runTest {
        val contactId = 1L
        val mockContact = Contact(id = contactId, name = "John", email = "john@test.com")

        every { repository.observeContact(contactId) } returns kotlinx.coroutines.flow.flowOf(mockContact)
        coEvery { repository.refreshContact(contactId) } returns Unit
        coEvery {
            repository.updateContact(any(), any(), any(), any(), any(), any(), any(), any())
        } throws Exception("Update failed")

        viewModel = ContactDetailViewModel(application, repository)
        viewModel.load(contactId)
        advanceUntilIdle()

        viewModel.updateContact("Jane", "jane@test.com", null, null, null, null, null)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.updateError)

        viewModel.clearUpdateError()

        assertNull(viewModel.uiState.value.updateError)
    }
}
