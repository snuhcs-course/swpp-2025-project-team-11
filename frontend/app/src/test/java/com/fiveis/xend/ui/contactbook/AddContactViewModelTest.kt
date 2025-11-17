package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.data.model.ContactResponse
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.repository.ContactBookRepository
import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddContactViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var application: Application
    private lateinit var repository: ContactBookRepository
    private lateinit var viewModel: AddContactViewModel

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
    fun add_contact_with_blank_name_sets_error() = runTest {
        viewModel = AddContactViewModel(application)

        viewModel.addContact("", "test@example.com", null, "Recipient", null, null)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("이름") == true)
    }

    @Test
    fun add_contact_with_blank_email_sets_error() = runTest {
        viewModel = AddContactViewModel(application)

        viewModel.addContact("John Doe", "", null, "Recipient", null, null)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("이메일") == true)
    }

    @Test
    fun add_contact_success_updates_state() = runTest {
        val mockResponse = ContactResponse(
            id = 1L,
            name = "John Doe",
            email = "john@example.com"
        )

        coEvery {
            anyConstructed<ContactBookRepository>().addContact(
                "John Doe",
                "john@example.com",
                null,
                "Sender",
                "Recipient",
                "Be formal"
            )
        } returns mockResponse

        viewModel = AddContactViewModel(application)

        viewModel.addContact(
            "John Doe",
            "john@example.com",
            "Sender",
            "Recipient",
            "Be formal",
            null
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.lastSuccessMsg)
        assertTrue(viewModel.uiState.value.lastSuccessMsg?.contains("1") == true)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun add_contact_with_group_success() = runTest {
        val group = Group(id = 5L, name = "VIP")
        val mockResponse = ContactResponse(
            id = 1L,
            name = "John Doe",
            email = "john@example.com"
        )

        coEvery {
            anyConstructed<ContactBookRepository>().addContact(
                "John Doe",
                "john@example.com",
                5L,
                null,
                "Recipient",
                null
            )
        } returns mockResponse

        viewModel = AddContactViewModel(application)

        viewModel.addContact(
            "John Doe",
            "john@example.com",
            null,
            "Recipient",
            null,
            group
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.lastSuccessMsg)
    }

    @Test
    fun add_contact_failure_sets_error() = runTest {
        coEvery {
            anyConstructed<ContactBookRepository>().addContact(any(), any(), any(), any(), any(), any())
        } throws Exception("Network error")

        viewModel = AddContactViewModel(application)

        viewModel.addContact(
            "John Doe",
            "john@example.com",
            null,
            "Recipient",
            null,
            null
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("Network error") == true)
    }

    @Test
    fun add_contact_with_long_email_address_succeeds() = runTest {
        val longEmail = "verylongemailaddressthatexceedsnormallength@verylongdomainname.com"
        val mockResponse = ContactResponse(
            id = 1L,
            name = "Test User",
            email = longEmail
        )

        coEvery {
            anyConstructed<ContactBookRepository>().addContact(
                "Test User",
                longEmail,
                null,
                null,
                "Recipient",
                null
            )
        } returns mockResponse

        viewModel = AddContactViewModel(application)

        viewModel.addContact(
            "Test User",
            longEmail,
            null,
            "Recipient",
            null,
            null
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.lastSuccessMsg)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun add_contact_multiple_times_updates_state_correctly() = runTest {
        val mockResponse1 = ContactResponse(id = 1L, name = "User1", email = "user1@test.com")
        val mockResponse2 = ContactResponse(id = 2L, name = "User2", email = "user2@test.com")

        coEvery {
            anyConstructed<ContactBookRepository>().addContact(
                "User1",
                "user1@test.com",
                null,
                null,
                "Recipient",
                null
            )
        } returns mockResponse1

        coEvery {
            anyConstructed<ContactBookRepository>().addContact(
                "User2",
                "user2@test.com",
                null,
                null,
                "Recipient",
                null
            )
        } returns mockResponse2

        viewModel = AddContactViewModel(application)

        // First add
        viewModel.addContact("User1", "user1@test.com", null, "Recipient", null, null)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.lastSuccessMsg)
        assertTrue(viewModel.uiState.value.lastSuccessMsg?.contains("1") == true)

        // Second add
        viewModel.addContact("User2", "user2@test.com", null, "Recipient", null, null)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.lastSuccessMsg)
        assertTrue(viewModel.uiState.value.lastSuccessMsg?.contains("2") == true)
    }

    @Test
    fun add_contact_with_whitespace_name_sets_error() = runTest {
        viewModel = AddContactViewModel(application)

        viewModel.addContact("   ", "test@example.com", null, "Recipient", null, null)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("이름") == true)
    }

    @Test
    fun add_contact_with_whitespace_email_sets_error() = runTest {
        viewModel = AddContactViewModel(application)

        viewModel.addContact("Test User", "   ", null, "Recipient", null, null)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.error)
        assertTrue(viewModel.uiState.value.error?.contains("이메일") == true)
    }

    @Test
    fun initial_state_is_correct() = runTest {
        viewModel = AddContactViewModel(application)

        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(null, state.error)
        assertEquals(null, state.lastSuccessMsg)
    }
}
