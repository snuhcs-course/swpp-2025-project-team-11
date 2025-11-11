package com.fiveis.xend.ui.compose

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.repository.ContactBookRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactLookupViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var application: Application
    private lateinit var viewModel: ContactLookupViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        every { application.applicationContext } returns application
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun initial_state_has_empty_contact_map() = runTest {
        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(emptyList())

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        assertTrue(viewModel.byEmail.value.isEmpty())
    }

    @Test
    fun observes_contacts_and_indexes_by_email() = runTest {
        val contacts = listOf(
            Contact(1L, null, "John Doe", "john@example.com", null, null, null),
            Contact(2L, null, "Jane Smith", "jane@example.com", null, null, null)
        )

        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(contacts)

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        assertEquals(2, viewModel.byEmail.value.size)
        assertNotNull(viewModel.byEmail.value["john@example.com"])
        assertNotNull(viewModel.byEmail.value["jane@example.com"])
    }

    @Test
    fun lookup_returns_contact_by_email() = runTest {
        val contact = Contact(1L, null, "John Doe", "john@example.com", null, null, null)

        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(listOf(contact))

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        val result = viewModel.lookup("john@example.com")

        assertNotNull(result)
        assertEquals("John Doe", result?.name)
        assertEquals("john@example.com", result?.email)
    }

    @Test
    fun lookup_returns_null_for_non_existent_email() = runTest {
        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(emptyList())

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        val result = viewModel.lookup("nonexistent@example.com")

        assertNull(result)
    }

    @Test
    fun lookup_is_case_insensitive() = runTest {
        val contact = Contact(1L, null, "John Doe", "John@Example.COM", null, null, null)

        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(listOf(contact))

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        val result1 = viewModel.lookup("john@example.com")
        val result2 = viewModel.lookup("JOHN@EXAMPLE.COM")
        val result3 = viewModel.lookup("John@Example.COM")

        assertNotNull(result1)
        assertNotNull(result2)
        assertNotNull(result3)
        assertEquals("John Doe", result1?.name)
        assertEquals("John Doe", result2?.name)
        assertEquals("John Doe", result3?.name)
    }

    @Test
    fun lookup_trims_whitespace_from_email() = runTest {
        val contact = Contact(1L, null, "John Doe", "john@example.com", null, null, null)

        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(listOf(contact))

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        val result = viewModel.lookup("  john@example.com  ")

        assertNotNull(result)
        assertEquals("John Doe", result?.name)
    }

    @Test
    fun contact_map_updates_when_contacts_change() = runTest {
        val contacts1 = listOf(
            Contact(1L, null, "John Doe", "john@example.com", null, null, null)
        )
        val contacts2 = listOf(
            Contact(1L, null, "John Doe", "john@example.com", null, null, null),
            Contact(2L, null, "Jane Smith", "jane@example.com", null, null, null)
        )

        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returnsMany listOf(
            flowOf(contacts1),
            flowOf(contacts2)
        )

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        assertEquals(1, viewModel.byEmail.value.size)

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        assertEquals(2, viewModel.byEmail.value.size)
    }

    @Test
    fun handles_contacts_with_duplicate_emails() = runTest {
        val contacts = listOf(
            Contact(1L, null, "John Doe", "john@example.com", null, null, null),
            Contact(2L, null, "John Smith", "john@example.com", null, null, null)
        )

        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(contacts)

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        assertEquals(1, viewModel.byEmail.value.size)
        val contact = viewModel.lookup("john@example.com")
        assertNotNull(contact)
    }

    @Test
    fun handles_large_number_of_contacts() = runTest {
        val contacts = (1L..1000L).map {
            Contact(it, null, "Contact $it", "contact$it@example.com", null, null, null)
        }

        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(contacts)

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        assertEquals(1000, viewModel.byEmail.value.size)

        val result = viewModel.lookup("contact500@example.com")
        assertNotNull(result)
        assertEquals("Contact 500", result?.name)
    }

    @Test
    fun handles_contacts_with_special_characters_in_email() = runTest {
        val contact = Contact(1L, null, "Test User", "test+alias@example.com", null, null, null)

        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(listOf(contact))

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        val result = viewModel.lookup("test+alias@example.com")

        assertNotNull(result)
        assertEquals("Test User", result?.name)
    }

    @Test
    fun handles_contacts_with_unicode_names() = runTest {
        val contact = Contact(1L, null, "李明", "test@example.com", null, null, null)

        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(listOf(contact))

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        val result = viewModel.lookup("test@example.com")

        assertNotNull(result)
        assertEquals("李明", result?.name)
    }

    @Test
    fun lookup_with_empty_string_returns_null() = runTest {
        val contact = Contact(1L, null, "John Doe", "john@example.com", null, null, null)

        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(listOf(contact))

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        val result = viewModel.lookup("")

        assertNull(result)
    }

    @Test
    fun lookup_with_whitespace_only_returns_null() = runTest {
        val contact = Contact(1L, null, "John Doe", "john@example.com", null, null, null)

        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(listOf(contact))

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        val result = viewModel.lookup("   ")

        assertNull(result)
    }

    @Test
    fun contacts_with_same_email_different_case_are_treated_as_same() = runTest {
        val contacts = listOf(
            Contact(1L, null, "John Doe", "John@Example.Com", null, null, null),
            Contact(2L, null, "Jane Doe", "john@example.com", null, null, null)
        )

        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(contacts)

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        assertEquals(1, viewModel.byEmail.value.size)
    }

    @Test
    fun state_flow_emits_updated_values() = runTest {
        val contacts = listOf(
            Contact(1L, null, "John Doe", "john@example.com", null, null, null)
        )

        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(contacts)

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        assertNotNull(viewModel.byEmail.value)
        assertTrue(viewModel.byEmail.value.isNotEmpty())
    }

    @Test
    fun handles_contacts_with_null_fields() = runTest {
        val contact = Contact(1L, null, "John Doe", "john@example.com", null, null, null)

        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(listOf(contact))

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        val result = viewModel.lookup("john@example.com")

        assertNotNull(result)
        assertNull(result?.context)
        assertNull(result?.group)
    }

    @Test
    fun multiple_lookups_return_consistent_results() = runTest {
        val contact = Contact(1L, null, "John Doe", "john@example.com", null, null, null)

        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(listOf(contact))

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        val result1 = viewModel.lookup("john@example.com")
        val result2 = viewModel.lookup("john@example.com")
        val result3 = viewModel.lookup("john@example.com")

        assertEquals(result1, result2)
        assertEquals(result2, result3)
    }

    @Test
    fun by_email_state_flow_is_not_null() = runTest {
        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(emptyList())

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        assertNotNull(viewModel.byEmail)
        assertNotNull(viewModel.byEmail.value)
    }

    @Test
    fun normalizes_email_addresses_correctly() = runTest {
        val contacts = listOf(
            Contact(1L, null, "User 1", "  TEST@EXAMPLE.COM  ", null, null, null),
            Contact(2L, null, "User 2", "another@example.com", null, null, null)
        )

        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(contacts)

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        assertNotNull(viewModel.lookup("test@example.com"))
        assertNotNull(viewModel.lookup("ANOTHER@EXAMPLE.COM"))
    }

    @Test
    fun contacts_are_indexed_immediately_on_initialization() = runTest {
        val contact = Contact(1L, null, "John Doe", "john@example.com", null, null, null)

        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(listOf(contact))

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        assertTrue(viewModel.byEmail.value.isNotEmpty())
    }

    @Test
    fun empty_email_in_contact_is_handled() = runTest {
        val contacts = listOf(
            Contact(1L, null, "John Doe", "", null, null, null),
            Contact(2L, null, "Jane Doe", "jane@example.com", null, null, null)
        )

        mockkConstructor(ContactBookRepository::class)
        every { anyConstructed<ContactBookRepository>().observeContacts() } returns flowOf(contacts)

        viewModel = ContactLookupViewModel(application)
        advanceUntilIdle()

        assertEquals(2, viewModel.byEmail.value.size)
    }
}
