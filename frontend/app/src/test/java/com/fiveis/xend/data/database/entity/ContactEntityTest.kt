package com.fiveis.xend.data.database.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactEntityTest {

    @Test
    fun test_construction_withAllFields() {
        val contact = ContactEntity(
            id = 1L,
            groupId = 10L,
            name = "John Doe",
            email = "john@example.com",
            createdAt = "2024-01-01",
            updatedAt = "2024-01-02"
        )

        assertEquals(1L, contact.id)
        assertEquals(10L, contact.groupId)
        assertEquals("John Doe", contact.name)
        assertEquals("john@example.com", contact.email)
        assertEquals("2024-01-01", contact.createdAt)
        assertEquals("2024-01-02", contact.updatedAt)
    }

    @Test
    fun test_construction_withNullGroupId() {
        val contact = ContactEntity(
            id = 1L,
            groupId = null,
            name = "Jane Doe",
            email = "jane@example.com"
        )

        assertNull(contact.groupId)
        assertEquals("Jane Doe", contact.name)
    }

    @Test
    fun test_construction_withDefaultTimestamps() {
        val contact = ContactEntity(
            id = 1L,
            name = "Test User",
            email = "test@example.com"
        )

        assertNull(contact.groupId)
        assertNull(contact.createdAt)
        assertNull(contact.updatedAt)
    }

    @Test
    fun test_copy_changesName() {
        val original = ContactEntity(1L, null, "Original", "original@example.com")
        val modified = original.copy(name = "Updated")

        assertEquals("Updated", modified.name)
        assertEquals("original@example.com", modified.email)
        assertEquals(1L, modified.id)
    }

    @Test
    fun test_copy_changesGroupId() {
        val original = ContactEntity(1L, null, "User", "user@example.com")
        val modified = original.copy(groupId = 5L)

        assertEquals(5L, modified.groupId)
        assertEquals("User", modified.name)
    }

    @Test
    fun test_equals_sameValues_returnsTrue() {
        val contact1 = ContactEntity(1L, 10L, "Name", "email@example.com", "2024-01-01", "2024-01-02")
        val contact2 = ContactEntity(1L, 10L, "Name", "email@example.com", "2024-01-01", "2024-01-02")

        assertEquals(contact1, contact2)
    }
}
