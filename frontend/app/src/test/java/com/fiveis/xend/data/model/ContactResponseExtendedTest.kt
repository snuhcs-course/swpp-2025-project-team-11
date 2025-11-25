package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ContactResponseExtendedTest {

    @Test
    fun test_contactResponse_basic() {
        val response = ContactResponse(
            id = 1L,
            name = "John Doe",
            email = "john@example.com"
        )

        assertEquals(1L, response.id)
        assertEquals("John Doe", response.name)
        assertEquals("john@example.com", response.email)
        assertNull(response.group)
        assertNull(response.context)
    }

    @Test
    fun test_contactResponse_withGroup() {
        val group = GroupResponse(
            id = 5L,
            name = "Team Alpha",
            description = "Main team"
        )
        val response = ContactResponse(
            id = 1L,
            group = group,
            name = "John Doe",
            email = "john@example.com"
        )

        assertNotNull(response.group)
        assertEquals("Team Alpha", response.group?.name)
    }

    @Test
    fun test_contactResponse_withContext() {
        val context = ContactResponseContext(
            id = 10L,
            senderRole = "Manager",
            recipientRole = "Employee"
        )
        val response = ContactResponse(
            id = 1L,
            name = "Jane Doe",
            email = "jane@example.com",
            context = context
        )

        assertNotNull(response.context)
        assertEquals("Manager", response.context?.senderRole)
        assertEquals("Employee", response.context?.recipientRole)
    }

    @Test
    fun test_contactResponse_toDomain() {
        val response = ContactResponse(
            id = 1L,
            name = "Test User",
            email = "test@example.com"
        )

        val domain = response.toDomain()

        assertEquals(1L, domain.id)
        assertEquals("Test User", domain.name)
        assertEquals("test@example.com", domain.email)
    }

    @Test
    fun test_contactResponse_toDomain_withGroupAndContext() {
        val group = GroupResponse(
            id = 5L,
            name = "Team",
            description = "Desc"
        )
        val context = ContactResponseContext(
            id = 10L,
            senderRole = "Boss",
            recipientRole = "Worker"
        )
        val response = ContactResponse(
            id = 1L,
            group = group,
            name = "User",
            email = "user@example.com",
            context = context
        )

        val domain = response.toDomain()

        assertEquals(1L, domain.id)
        assertNotNull(domain.group)
        assertNotNull(domain.context)
        assertEquals("Boss", domain.context?.senderRole)
    }

    @Test
    fun test_contactResponseContext_toDomain() {
        val responseContext = ContactResponseContext(
            id = 10L,
            senderRole = "Teacher",
            recipientRole = "Student",
            relationshipDetails = "Class mentor",
            personalPrompt = "Be encouraging",
            languagePreference = "en"
        )

        val domainContext = responseContext.toDomain()

        assertEquals(10L, domainContext.id)
        assertEquals("Teacher", domainContext.senderRole)
        assertEquals("Student", domainContext.recipientRole)
        assertEquals("Class mentor", domainContext.relationshipDetails)
        assertEquals("Be encouraging", domainContext.personalPrompt)
        assertEquals("en", domainContext.languagePreference)
    }
}
