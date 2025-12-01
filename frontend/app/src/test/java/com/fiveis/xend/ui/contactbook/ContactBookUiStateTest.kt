package com.fiveis.xend.ui.contactbook

import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.repository.ContactBookTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactBookUiStateTest {

    private fun createContact(id: Long, name: String) = Contact(
        id = id,
        name = name,
        email = "$name@example.com",
        group = null,
        context = null
    )

    private fun createGroup(id: Long, name: String) = Group(
        id = id,
        name = name,
        emoji = "😊",
        members = emptyList(),
        options = emptyList()
    )

    @Test
    fun test_defaultState() {
        val state = ContactBookUiState()

        assertEquals(ContactBookTab.Groups, state.selectedTab)
        assertEquals(emptyList<Group>(), state.groups)
        assertEquals(emptyList<Contact>(), state.contacts)
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertFalse(state.isSearchMode)
        assertEquals("", state.searchQuery)
        assertEquals(emptyList<Contact>(), state.searchResults)
    }

    @Test
    fun test_copy_selectedTab() {
        val state = ContactBookUiState()
        val newState = state.copy(selectedTab = ContactBookTab.Contacts)

        assertEquals(ContactBookTab.Groups, state.selectedTab)
        assertEquals(ContactBookTab.Contacts, newState.selectedTab)
    }

    @Test
    fun test_copy_groups() {
        val state = ContactBookUiState()
        val groups = listOf(
            createGroup(1L, "Friends"),
            createGroup(2L, "Work")
        )
        val newState = state.copy(groups = groups)

        assertEquals(0, state.groups.size)
        assertEquals(2, newState.groups.size)
        assertEquals("Friends", newState.groups[0].name)
        assertEquals("Work", newState.groups[1].name)
    }

    @Test
    fun test_copy_contacts() {
        val state = ContactBookUiState()
        val contacts = listOf(
            createContact(1L, "Alice"),
            createContact(2L, "Bob"),
            createContact(3L, "Charlie")
        )
        val newState = state.copy(contacts = contacts)

        assertEquals(0, state.contacts.size)
        assertEquals(3, newState.contacts.size)
        assertEquals("Alice", newState.contacts[0].name)
    }

    @Test
    fun test_copy_isLoading() {
        val state = ContactBookUiState()
        val newState = state.copy(isLoading = true)

        assertFalse(state.isLoading)
        assertTrue(newState.isLoading)
    }

    @Test
    fun test_copy_error() {
        val state = ContactBookUiState()
        val newState = state.copy(error = "Failed to load")

        assertNull(state.error)
        assertEquals("Failed to load", newState.error)
    }

    @Test
    fun test_copy_isSearchMode() {
        val state = ContactBookUiState()
        val newState = state.copy(isSearchMode = true, searchQuery = "test")

        assertFalse(state.isSearchMode)
        assertTrue(newState.isSearchMode)
        assertEquals("test", newState.searchQuery)
    }

    @Test
    fun test_copy_searchResults() {
        val state = ContactBookUiState()
        val results = listOf(createContact(1L, "SearchResult"))
        val newState = state.copy(searchResults = results)

        assertEquals(0, state.searchResults.size)
        assertEquals(1, newState.searchResults.size)
    }

    @Test
    fun test_copy_switchToContactsTab() {
        val state = ContactBookUiState(
            selectedTab = ContactBookTab.Groups,
            groups = listOf(createGroup(1L, "Group1"))
        )
        val contacts = listOf(createContact(1L, "Contact1"))
        val newState = state.copy(selectedTab = ContactBookTab.Contacts, contacts = contacts)

        assertEquals(ContactBookTab.Contacts, newState.selectedTab)
        assertEquals(1, newState.contacts.size)
    }

    @Test
    fun test_copy_multipleFields() {
        val state = ContactBookUiState()
        val groups = listOf(createGroup(1L, "Team"))
        val newState = state.copy(
            selectedTab = ContactBookTab.Groups,
            groups = groups,
            isLoading = false,
            error = null
        )

        assertEquals(ContactBookTab.Groups, newState.selectedTab)
        assertEquals(1, newState.groups.size)
        assertFalse(newState.isLoading)
        assertNull(newState.error)
    }

    @Test
    fun test_copy_enterSearchMode() {
        val state = ContactBookUiState()
        val newState = state.copy(isSearchMode = true, searchQuery = "alice")

        assertTrue(newState.isSearchMode)
        assertEquals("alice", newState.searchQuery)
    }

    @Test
    fun test_copy_exitSearchMode() {
        val state = ContactBookUiState(isSearchMode = true, searchQuery = "search", searchResults = listOf(createContact(1L, "Result")))
        val newState = state.copy(isSearchMode = false, searchQuery = "", searchResults = emptyList())

        assertFalse(newState.isSearchMode)
        assertEquals("", newState.searchQuery)
        assertEquals(0, newState.searchResults.size)
    }

    @Test
    fun test_copy_loadingState() {
        val state = ContactBookUiState()
        val newState = state.copy(isLoading = true, error = null)

        assertTrue(newState.isLoading)
        assertNull(newState.error)
    }

    @Test
    fun test_copy_errorState() {
        val state = ContactBookUiState(isLoading = true)
        val newState = state.copy(isLoading = false, error = "Network error")

        assertFalse(newState.isLoading)
        assertEquals("Network error", newState.error)
    }

    @Test
    fun test_copy_manyGroups() {
        val state = ContactBookUiState()
        val groups = (1..50).map { createGroup(it.toLong(), "Group$it") }
        val newState = state.copy(groups = groups)

        assertEquals(50, newState.groups.size)
    }

    @Test
    fun test_copy_manyContacts() {
        val state = ContactBookUiState()
        val contacts = (1..100).map { createContact(it.toLong(), "Contact$it") }
        val newState = state.copy(contacts = contacts)

        assertEquals(100, newState.contacts.size)
    }

    @Test
    fun test_copy_resetError() {
        val state = ContactBookUiState(error = "Previous error")
        val newState = state.copy(error = null)

        assertEquals("Previous error", state.error)
        assertNull(newState.error)
    }
}
