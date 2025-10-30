package com.fiveis.xend.data.model

import androidx.compose.ui.graphics.Color
import org.junit.Assert.*
import org.junit.Test

class ContactTest {

    @Test
    fun create_contact_with_required_fields_only() {
        val contact = Contact(
            id = 1L,
            name = "John Doe",
            email = "john@example.com"
        )

        assertEquals(1L, contact.id)
        assertEquals("John Doe", contact.name)
        assertEquals("john@example.com", contact.email)
        assertNull(contact.group)
        assertNull(contact.context)
        assertNull(contact.createdAt)
        assertNull(contact.updatedAt)
        assertEquals(Color(0xFF5A7DFF), contact.color)
    }

    @Test
    fun create_contact_with_all_fields() {
        val group = Group(
            id = 10L,
            name = "Work"
        )

        val context = ContactContext(
            id = 100L,
            senderRole = "Manager",
            recipientRole = "Employee",
            relationshipDetails = "Direct report",
            personalPrompt = "Use formal language",
            languagePreference = "en"
        )

        val contact = Contact(
            id = 1L,
            group = group,
            name = "Jane Smith",
            email = "jane@example.com",
            context = context,
            createdAt = "2024-10-30T10:00:00Z",
            updatedAt = "2024-10-30T12:00:00Z",
            color = Color.Red
        )

        assertEquals(1L, contact.id)
        assertEquals(group, contact.group)
        assertEquals("Jane Smith", contact.name)
        assertEquals("jane@example.com", contact.email)
        assertEquals(context, contact.context)
        assertEquals("2024-10-30T10:00:00Z", contact.createdAt)
        assertEquals("2024-10-30T12:00:00Z", contact.updatedAt)
        assertEquals(Color.Red, contact.color)
    }

    @Test
    fun contact_copy_updates_specific_fields() {
        val original = Contact(
            id = 1L,
            name = "Original Name",
            email = "original@test.com"
        )

        val updated = original.copy(name = "Updated Name")

        assertEquals(1L, updated.id)
        assertEquals("Updated Name", updated.name)
        assertEquals("original@test.com", updated.email)
    }

    @Test
    fun contacts_with_same_values_are_equal() {
        val contact1 = Contact(
            id = 1L,
            name = "John Doe",
            email = "john@example.com",
            color = Color.Blue
        )

        val contact2 = Contact(
            id = 1L,
            name = "John Doe",
            email = "john@example.com",
            color = Color.Blue
        )

        assertEquals(contact1, contact2)
        assertEquals(contact1.hashCode(), contact2.hashCode())
    }

    @Test
    fun contacts_with_different_ids_are_not_equal() {
        val contact1 = Contact(
            id = 1L,
            name = "John Doe",
            email = "john@example.com"
        )

        val contact2 = contact1.copy(id = 2L)

        assertNotEquals(contact1, contact2)
    }
}

class ContactContextTest {

    @Test
    fun create_contact_context_with_required_field_only() {
        val context = ContactContext(id = 1L)

        assertEquals(1L, context.id)
        assertNull(context.senderRole)
        assertNull(context.recipientRole)
        assertNull(context.relationshipDetails)
        assertNull(context.personalPrompt)
        assertNull(context.languagePreference)
        assertNull(context.createdAt)
        assertNull(context.updatedAt)
    }

    @Test
    fun create_contact_context_with_all_fields() {
        val context = ContactContext(
            id = 1L,
            senderRole = "CEO",
            recipientRole = "Employee",
            relationshipDetails = "Direct supervisor",
            personalPrompt = "Use respectful tone",
            languagePreference = "en-US",
            createdAt = "2024-10-30T10:00:00Z",
            updatedAt = "2024-10-30T11:00:00Z"
        )

        assertEquals(1L, context.id)
        assertEquals("CEO", context.senderRole)
        assertEquals("Employee", context.recipientRole)
        assertEquals("Direct supervisor", context.relationshipDetails)
        assertEquals("Use respectful tone", context.personalPrompt)
        assertEquals("en-US", context.languagePreference)
        assertEquals("2024-10-30T10:00:00Z", context.createdAt)
        assertEquals("2024-10-30T11:00:00Z", context.updatedAt)
    }

    @Test
    fun contact_context_copy_updates_specific_fields() {
        val original = ContactContext(
            id = 1L,
            senderRole = "Manager"
        )

        val updated = original.copy(senderRole = "Director")

        assertEquals(1L, updated.id)
        assertEquals("Director", updated.senderRole)
    }

    @Test
    fun contact_contexts_with_same_values_are_equal() {
        val context1 = ContactContext(
            id = 1L,
            senderRole = "Manager",
            languagePreference = "en"
        )

        val context2 = ContactContext(
            id = 1L,
            senderRole = "Manager",
            languagePreference = "en"
        )

        assertEquals(context1, context2)
        assertEquals(context1.hashCode(), context2.hashCode())
    }
}
