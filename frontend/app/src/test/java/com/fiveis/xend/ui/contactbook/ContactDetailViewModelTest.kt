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
    fun load_contact_success_updates_state() = runTest {
        val contactId = 1L
        val mockContact = Contact(
            id = contactId,
            name = "John Doe",
            email = "john@example.com"
        )

        coEvery { anyConstructed<ContactBookRepository>().getContact(contactId) } returns mockContact

        viewModel = ContactDetailViewModel(application)

        viewModel.load(contactId)
        advanceUntilIdle()

        assertEquals(mockContact, viewModel.uiState.value.contact)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun load_contact_failure_sets_error() = runTest {
        val contactId = 1L

        coEvery { anyConstructed<ContactBookRepository>().getContact(contactId) } throws Exception("Network error")

        viewModel = ContactDetailViewModel(application)

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

        coEvery { anyConstructed<ContactBookRepository>().getContact(contactId) } returns mockContact

        viewModel = ContactDetailViewModel(application)

        viewModel.load(contactId)
        advanceUntilIdle()

        viewModel.load(contactId, force = false)
        advanceUntilIdle()

        coVerify(exactly = 1) { anyConstructed<ContactBookRepository>().getContact(contactId) }
    }

    @Test
    fun load_same_contact_with_force_reloads() = runTest {
        val contactId = 1L
        val mockContact = Contact(
            id = contactId,
            name = "John Doe",
            email = "john@example.com"
        )

        coEvery { anyConstructed<ContactBookRepository>().getContact(contactId) } returns mockContact

        viewModel = ContactDetailViewModel(application)

        viewModel.load(contactId)
        advanceUntilIdle()

        viewModel.load(contactId, force = true)
        advanceUntilIdle()

        coVerify(exactly = 2) { anyConstructed<ContactBookRepository>().getContact(contactId) }
    }
}
