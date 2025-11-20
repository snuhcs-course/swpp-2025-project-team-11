package com.fiveis.xend.data.database

import com.fiveis.xend.data.database.entity.ContactContextEntity
import com.fiveis.xend.data.database.entity.ContactEntity
import com.fiveis.xend.data.database.entity.GroupEntity
import com.fiveis.xend.data.database.entity.PromptOptionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RelationsTest {

    @Test
    fun test_contactWithContext_withContext() {
        val contact = ContactEntity(
            id = 1L,
            name = "John Doe",
            email = "john@example.com",
            groupId = null
        )
        val context = ContactContextEntity(
            contactId = 1L,
            relationshipDetails = "Friend from university"
        )
        val withContext = ContactWithContext(contact, context)

        assertEquals(contact, withContext.contact)
        assertEquals(context, withContext.context)
        assertEquals("John Doe", withContext.contact.name)
        assertEquals("Friend from university", withContext.context?.relationshipDetails)
    }

    @Test
    fun test_contactWithContext_nullContext() {
        val contact = ContactEntity(
            id = 1L,
            name = "Jane Doe",
            email = "jane@example.com",
            groupId = null
        )
        val withContext = ContactWithContext(contact, null)

        assertEquals(contact, withContext.contact)
        assertNull(withContext.context)
    }

    @Test
    fun test_contactWithGroupAndContext_allFields() {
        val contact = ContactEntity(
            id = 1L,
            name = "John Doe",
            email = "john@example.com",
            groupId = 5L
        )
        val context = ContactContextEntity(
            contactId = 1L,
            relationshipDetails = "Colleague"
        )
        val group = GroupEntity(
            id = 5L,
            name = "Work Team",
            emoji = "💼",
            createdAt = "2025-01-01"
        )
        val withGroupAndContext = ContactWithGroupAndContext(contact, context, group)

        assertEquals(contact, withGroupAndContext.contact)
        assertEquals(context, withGroupAndContext.context)
        assertEquals(group, withGroupAndContext.group)
        assertEquals("Work Team", withGroupAndContext.group?.name)
        assertEquals("💼", withGroupAndContext.group?.emoji)
    }

    @Test
    fun test_contactWithGroupAndContext_nullFields() {
        val contact = ContactEntity(
            id = 1L,
            name = "Solo Contact",
            email = "solo@example.com",
            groupId = null
        )
        val withGroupAndContext = ContactWithGroupAndContext(contact, null, null)

        assertEquals(contact, withGroupAndContext.contact)
        assertNull(withGroupAndContext.context)
        assertNull(withGroupAndContext.group)
    }

    @Test
    fun test_groupWithMembersAndOptions_complete() {
        val group = GroupEntity(
            id = 1L,
            name = "Family",
            emoji = "👨‍👩‍👧",
            createdAt = "2025-01-01"
        )
        val contact1 = ContactEntity(
            id = 1L,
            name = "Alice",
            email = "alice@example.com",
            groupId = 1L
        )
        val contact2 = ContactEntity(
            id = 2L,
            name = "Bob",
            email = "bob@example.com",
            groupId = 1L
        )
        val context1 = ContactContextEntity(
            contactId = 1L,
            relationshipDetails = "Sister"
        )
        val members = listOf(
            ContactWithContext(contact1, context1),
            ContactWithContext(contact2, null)
        )
        val option1 = PromptOptionEntity(
            id = 1L,
            key = "casual",
            name = "Casual",
            prompt = "Write casually"
        )
        val option2 = PromptOptionEntity(
            id = 2L,
            key = "formal",
            name = "Formal",
            prompt = "Write formally"
        )
        val options = listOf(option1, option2)

        val groupWithData = GroupWithMembersAndOptions(group, members, options)

        assertEquals(group, groupWithData.group)
        assertEquals(2, groupWithData.members.size)
        assertEquals(2, groupWithData.options.size)
        assertEquals("Alice", groupWithData.members[0].contact.name)
        assertEquals("Bob", groupWithData.members[1].contact.name)
        assertEquals("Casual", groupWithData.options[0].name)
        assertEquals("Formal", groupWithData.options[1].name)
    }

    @Test
    fun test_groupWithMembersAndOptions_emptyLists() {
        val group = GroupEntity(
            id = 1L,
            name = "Empty Group",
            emoji = "📦",
            createdAt = "2025-01-01"
        )
        val groupWithData = GroupWithMembersAndOptions(group, emptyList(), emptyList())

        assertEquals(group, groupWithData.group)
        assertEquals(0, groupWithData.members.size)
        assertEquals(0, groupWithData.options.size)
    }

    @Test
    fun test_contactWithContext_equality() {
        val contact = ContactEntity(1L, null, "John", "john@example.com")
        val context = ContactContextEntity(
            contactId = 1L,
            relationshipDetails = "Friend"
        )
        val wc1 = ContactWithContext(contact, context)
        val wc2 = ContactWithContext(contact, context)

        assertEquals(wc1, wc2)
    }

    @Test
    fun test_contactWithContext_inequality_differentContact() {
        val contact1 = ContactEntity(1L, null, "John", "john@example.com")
        val contact2 = ContactEntity(2L, null, "Jane", "jane@example.com")
        val context = ContactContextEntity(
            contactId = 1L,
            relationshipDetails = "Friend"
        )
        val wc1 = ContactWithContext(contact1, context)
        val wc2 = ContactWithContext(contact2, context)

        assertNotEquals(wc1, wc2)
    }

    @Test
    fun test_groupWithMembersAndOptions_singleMember() {
        val group = GroupEntity(1L, "Solo", "👤", "2025-01-01")
        val contact = ContactEntity(1L, 1L, "Only Member", "only@example.com")
        val members = listOf(ContactWithContext(contact, null))
        val groupWithData = GroupWithMembersAndOptions(group, members, emptyList())

        assertEquals(1, groupWithData.members.size)
        assertEquals("Only Member", groupWithData.members[0].contact.name)
    }
}
