package com.fiveis.xend.data.model

import org.junit.Assert.*
import org.junit.Test

class PromptOptionTest {

    @Test
    fun create_prompt_option_with_required_fields() {
        val option = PromptOption(
            id = 1L,
            key = "formal",
            name = "Formal",
            prompt = "Use formal language"
        )

        assertEquals(1L, option.id)
        assertEquals("formal", option.key)
        assertEquals("Formal", option.name)
        assertEquals("Use formal language", option.prompt)
        assertNull(option.createdAt)
        assertNull(option.updatedAt)
    }

    @Test
    fun create_prompt_option_with_all_fields() {
        val option = PromptOption(
            id = 1L,
            key = "professional",
            name = "Professional Tone",
            prompt = "Use a professional and respectful tone",
            createdAt = "2024-10-30T10:00:00Z",
            updatedAt = "2024-10-30T12:00:00Z"
        )

        assertEquals(1L, option.id)
        assertEquals("professional", option.key)
        assertEquals("Professional Tone", option.name)
        assertEquals("Use a professional and respectful tone", option.prompt)
        assertEquals("2024-10-30T10:00:00Z", option.createdAt)
        assertEquals("2024-10-30T12:00:00Z", option.updatedAt)
    }

    @Test
    fun prompt_option_copy_updates_specific_fields() {
        val original = PromptOption(
            id = 1L,
            key = "casual",
            name = "Casual",
            prompt = "Be casual"
        )

        val updated = original.copy(prompt = "Be friendly and casual")

        assertEquals(1L, updated.id)
        assertEquals("casual", updated.key)
        assertEquals("Casual", updated.name)
        assertEquals("Be friendly and casual", updated.prompt)
    }

    @Test
    fun prompt_options_with_same_values_are_equal() {
        val option1 = PromptOption(
            id = 1L,
            key = "formal",
            name = "Formal",
            prompt = "Use formal language"
        )

        val option2 = PromptOption(
            id = 1L,
            key = "formal",
            name = "Formal",
            prompt = "Use formal language"
        )

        assertEquals(option1, option2)
        assertEquals(option1.hashCode(), option2.hashCode())
    }

    @Test
    fun prompt_options_with_different_keys_are_not_equal() {
        val option1 = PromptOption(
            id = 1L,
            key = "formal",
            name = "Formal",
            prompt = "Use formal language"
        )

        val option2 = option1.copy(key = "casual")

        assertNotEquals(option1, option2)
    }
}

class PromptOptionRequestTest {

    @Test
    fun create_prompt_option_request_with_all_fields() {
        val request = PromptOptionRequest(
            key = "friendly",
            name = "Friendly Tone",
            prompt = "Use a friendly and warm tone"
        )

        assertEquals("friendly", request.key)
        assertEquals("Friendly Tone", request.name)
        assertEquals("Use a friendly and warm tone", request.prompt)
    }

    @Test
    fun prompt_option_request_copy_updates_fields() {
        val original = PromptOptionRequest(
            key = "formal",
            name = "Formal",
            prompt = "Be formal"
        )

        val updated = original.copy(name = "Very Formal", prompt = "Be very formal")

        assertEquals("formal", updated.key)
        assertEquals("Very Formal", updated.name)
        assertEquals("Be very formal", updated.prompt)
    }

    @Test
    fun prompt_option_requests_with_same_values_are_equal() {
        val request1 = PromptOptionRequest(
            key = "brief",
            name = "Brief",
            prompt = "Keep it brief"
        )

        val request2 = PromptOptionRequest(
            key = "brief",
            name = "Brief",
            prompt = "Keep it brief"
        )

        assertEquals(request1, request2)
        assertEquals(request1.hashCode(), request2.hashCode())
    }
}
