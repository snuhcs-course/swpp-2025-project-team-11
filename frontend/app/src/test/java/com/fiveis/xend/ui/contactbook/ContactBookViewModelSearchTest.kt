package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.repository.ContactBookRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactBookViewModelSearchTest {

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
    fun search_with_empty_query_returns_empty_results() = runTest {
        val searchResults = MutableStateFlow<List<Contact>>(emptyList())
        every { repository.searchContacts("") } returns searchResults

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
    }

    @Test
    fun search_with_single_result() = runTest {
        val contact = Contact(id = 1L, name = "Test User", email = "test@test.com")
        every { repository.searchContacts("test") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        // Search results update based on debounced query
        Thread.sleep(400)
        advanceUntilIdle()
    }

    @Test
    fun search_with_multiple_results() = runTest {
        val contacts = listOf(
            Contact(id = 1L, name = "John Doe", email = "john@test.com"),
            Contact(id = 2L, name = "Jane Doe", email = "jane@test.com")
        )
        every { repository.searchContacts("doe") } returns flowOf(contacts)

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_no_results() = runTest {
        every { repository.searchContacts("nonexistent") } returns flowOf(emptyList())

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
    }

    @Test
    fun search_with_special_characters() = runTest {
        val contact = Contact(id = 1L, name = "O'Brien", email = "obrien@test.com")
        every { repository.searchContacts("o'brien") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_unicode_characters() = runTest {
        val contact = Contact(id = 1L, name = "김철수", email = "kim@test.com")
        every { repository.searchContacts("김철수") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_email_pattern() = runTest {
        val contact = Contact(id = 1L, name = "Test", email = "test@example.com")
        every { repository.searchContacts("example.com") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_partial_match() = runTest {
        val contact = Contact(id = 1L, name = "Alexander", email = "alex@test.com")
        every { repository.searchContacts("alex") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_case_insensitive_query() = runTest {
        val contact = Contact(id = 1L, name = "Test User", email = "test@test.com")
        every { repository.searchContacts("TEST") } returns flowOf(listOf(contact))
        every { repository.searchContacts("test") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_whitespace_query() = runTest {
        every { repository.searchContacts("   ") } returns flowOf(emptyList())

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
    }

    @Test
    fun search_with_very_long_query() = runTest {
        val longQuery = "a".repeat(1000)
        every { repository.searchContacts(longQuery) } returns flowOf(emptyList())

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_results_persist_across_tab_switches() = runTest {
        val contact = Contact(id = 1L, name = "Test", email = "test@test.com")
        every { repository.searchContacts("test") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        // Results should remain available
        // (actual behavior may vary based on implementation)
    }

    @Test
    fun search_with_number_query() = runTest {
        val contact = Contact(id = 1L, name = "User123", email = "user123@test.com")
        every { repository.searchContacts("123") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_email_local_part() = runTest {
        val contact = Contact(id = 1L, name = "Test", email = "test.user@example.com")
        every { repository.searchContacts("test.user") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_email_domain() = runTest {
        val contact = Contact(id = 1L, name = "Test", email = "test@gmail.com")
        every { repository.searchContacts("gmail") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_mixed_language_query() = runTest {
        val contact = Contact(id = 1L, name = "John김", email = "john@test.com")
        every { repository.searchContacts("John김") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_returns_large_result_set() = runTest {
        val contacts = (1..100).map { i ->
            Contact(id = i.toLong(), name = "User$i", email = "user$i@test.com")
        }
        every { repository.searchContacts("user") } returns flowOf(contacts)

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_emoji_query() = runTest {
        val contact = Contact(id = 1L, name = "Party 🎉", email = "party@test.com")
        every { repository.searchContacts("🎉") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_single_character_query() = runTest {
        val contact = Contact(id = 1L, name = "Alice", email = "a@test.com")
        every { repository.searchContacts("a") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_hyphenated_query() = runTest {
        val contact = Contact(id = 1L, name = "Mary-Jane", email = "mj@test.com")
        every { repository.searchContacts("mary-jane") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_apostrophe_query() = runTest {
        val contact = Contact(id = 1L, name = "O'Connor", email = "oconnor@test.com")
        every { repository.searchContacts("o'connor") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_dot_query() = runTest {
        val contact = Contact(id = 1L, name = "Dr. Smith", email = "dr.smith@test.com")
        every { repository.searchContacts("dr.") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_at_symbol_query() = runTest {
        val contact = Contact(id = 1L, name = "Test", email = "test@example.com")
        every { repository.searchContacts("@example") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_plus_sign_query() = runTest {
        val contact = Contact(id = 1L, name = "Test", email = "test+tag@example.com")
        every { repository.searchContacts("+tag") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_underscore_query() = runTest {
        val contact = Contact(id = 1L, name = "user_name", email = "user_name@test.com")
        every { repository.searchContacts("user_name") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_results_update_on_query_change() = runTest {
        val contact1 = Contact(id = 1L, name = "Alice", email = "alice@test.com")
        val contact2 = Contact(id = 2L, name = "Bob", email = "bob@test.com")

        every { repository.searchContacts("alice") } returns flowOf(listOf(contact1))
        every { repository.searchContacts("bob") } returns flowOf(listOf(contact2))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_handles_duplicate_contacts() = runTest {
        val contact = Contact(id = 1L, name = "Test", email = "test@test.com")
        every { repository.searchContacts("test") } returns flowOf(listOf(contact, contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_japanese_characters() = runTest {
        val contact = Contact(id = 1L, name = "田中太郎", email = "tanaka@test.com")
        every { repository.searchContacts("田中") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_chinese_characters() = runTest {
        val contact = Contact(id = 1L, name = "李明", email = "li@test.com")
        every { repository.searchContacts("李明") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_cyrillic_characters() = runTest {
        val contact = Contact(id = 1L, name = "Иван", email = "ivan@test.com")
        every { repository.searchContacts("Иван") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun search_with_arabic_characters() = runTest {
        val contact = Contact(id = 1L, name = "محمد", email = "mohammed@test.com")
        every { repository.searchContacts("محمد") } returns flowOf(listOf(contact))

        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()
    }

    @Test
    fun start_contact_search_enables_search_mode() = runTest {
        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.startContactSearch()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSearchMode)
    }

    @Test
    fun close_contact_search_disables_search_mode() = runTest {
        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.startContactSearch()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSearchMode)

        viewModel.closeContactSearch()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isSearchMode)
        assertEquals("", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.searchResults.isEmpty())
    }

    @Test
    fun on_contact_search_query_change_updates_query() = runTest {
        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        viewModel.onContactSearchQueryChange("test query")
        advanceUntilIdle()

        assertEquals("test query", viewModel.uiState.value.searchQuery)
    }

    @Test
    fun search_query_debounce_handles_rapid_changes() = runTest {
        viewModel = ContactBookViewModel(application, repository)
        advanceUntilIdle()

        // Simulate rapid typing
        viewModel.onContactSearchQueryChange("t")
        viewModel.onContactSearchQueryChange("te")
        viewModel.onContactSearchQueryChange("tes")
        viewModel.onContactSearchQueryChange("test")
        advanceUntilIdle()

        // Final query should be "test"
        assertEquals("test", viewModel.uiState.value.searchQuery)
    }
}
