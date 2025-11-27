package com.fiveis.xend.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ModelCompanionTest {

    private val gson = Gson()

    @Test
    fun promptOption_serialization_and_deserialization() {
        val original = PromptOption(
            id = 1L,
            key = "tone",
            name = "Formal",
            prompt = "Be formal"
        )

        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, PromptOption::class.java)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.key, deserialized.key)
        assertEquals(original.name, deserialized.name)
        assertEquals(original.prompt, deserialized.prompt)
    }

    @Test
    fun contactResponse_serialization_and_deserialization() {
        val original = ContactResponse(
            id = 1L,
            name = "John",
            email = "john@example.com",
            group = null,
            context = null
        )

        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, ContactResponse::class.java)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.name, deserialized.name)
        assertEquals(original.email, deserialized.email)
    }

    @Test
    fun addContactRequestContext_serialization_and_deserialization() {
        val original = AddContactRequestContext(
            senderRole = "Manager",
            recipientRole = "Employee",
            relationshipDetails = "Direct report",
            personalPrompt = "Be professional",
            languagePreference = "en"
        )

        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, AddContactRequestContext::class.java)

        assertEquals(original.senderRole, deserialized.senderRole)
        assertEquals(original.recipientRole, deserialized.recipientRole)
    }

    @Test
    fun contactResponseContext_serialization_and_deserialization() {
        val original = ContactResponseContext(
            id = 1L,
            senderRole = "Manager",
            recipientRole = "Employee",
            relationshipDetails = "Direct report",
            personalPrompt = "Be professional",
            languagePreference = "en"
        )

        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, ContactResponseContext::class.java)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.senderRole, deserialized.senderRole)
    }

    @Test
    fun addGroupRequest_serialization_and_deserialization() {
        val original = AddGroupRequest(
            name = "Dev Team",
            description = "Development team",
            emoji = "=�",
            optionIds = listOf(1L, 2L)
        )

        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, AddGroupRequest::class.java)

        assertEquals(original.name, deserialized.name)
        assertEquals(original.description, deserialized.description)
        assertEquals(original.emoji, deserialized.emoji)
        assertEquals(original.optionIds, deserialized.optionIds)
    }

    @Test
    fun promptOptionRequest_serialization_and_deserialization() {
        val original = PromptOptionRequest(
            key = "tone",
            name = "Formal",
            prompt = "Be formal"
        )

        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, PromptOptionRequest::class.java)

        assertEquals(original.key, deserialized.key)
        assertEquals(original.name, deserialized.name)
        assertEquals(original.prompt, deserialized.prompt)
    }

    @Test
    fun groupResponse_serialization_and_deserialization() {
        val original = GroupResponse(
            id = 1L,
            name = "Team",
            description = "Description",
            emoji = "=%",
            options = emptyList()
        )

        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, GroupResponse::class.java)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.name, deserialized.name)
        assertEquals(original.description, deserialized.description)
        assertEquals(original.emoji, deserialized.emoji)
    }

    @Test
    fun addContactRequest_serialization_and_deserialization() {
        val original = AddContactRequest(
            name = "Jane",
            email = "jane@example.com",
            groupId = 1L,
            context = null
        )

        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, AddContactRequest::class.java)

        assertEquals(original.name, deserialized.name)
        assertEquals(original.email, deserialized.email)
        assertEquals(original.groupId, deserialized.groupId)
    }

    @Test
    fun promptOptionUpdateRequest_serialization_and_deserialization() {
        val original = PromptOptionUpdateRequest(
            id = 1L,
            name = "Casual",
            prompt = "Be casual"
        )

        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, PromptOptionUpdateRequest::class.java)

        assertEquals(original.id, deserialized.id)
        assertEquals(original.name, deserialized.name)
        assertEquals(original.prompt, deserialized.prompt)
    }

    @Test
    fun all_companion_objects_exist() {
        assertNotNull(PromptOption.Companion)
        assertNotNull(ContactResponse.Companion)
        assertNotNull(AddContactRequestContext.Companion)
        assertNotNull(ContactResponseContext.Companion)
        assertNotNull(AddGroupRequest.Companion)
        assertNotNull(PromptOptionRequest.Companion)
        assertNotNull(GroupResponse.Companion)
        assertNotNull(AddContactRequest.Companion)
        assertNotNull(PromptOptionUpdateRequest.Companion)
    }
}
