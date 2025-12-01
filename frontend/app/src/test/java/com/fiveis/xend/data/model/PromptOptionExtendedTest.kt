package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PromptOptionExtendedTest {

    @Test
    fun test_createPromptOption_allFields() {
        val option = PromptOption(
            id = 1L,
            key = "formal",
            name = "Formal Tone",
            prompt = "Write in a formal and professional manner",
            createdAt = "2025-01-01T10:00:00",
            updatedAt = "2025-01-02T10:00:00"
        )

        assertEquals(1L, option.id)
        assertEquals("formal", option.key)
        assertEquals("Formal Tone", option.name)
        assertEquals("Write in a formal and professional manner", option.prompt)
        assertEquals("2025-01-01T10:00:00", option.createdAt)
        assertEquals("2025-01-02T10:00:00", option.updatedAt)
    }

    @Test
    fun test_createPromptOption_nullableDates() {
        val option = PromptOption(
            id = 1L,
            key = "casual",
            name = "Casual",
            prompt = "Be casual",
            createdAt = null,
            updatedAt = null
        )

        assertNull(option.createdAt)
        assertNull(option.updatedAt)
    }

    @Test
    fun test_copy_name() {
        val original = PromptOption(
            id = 1L,
            key = "formal",
            name = "Formal",
            prompt = "Be formal"
        )
        val modified = original.copy(name = "Very Formal")

        assertEquals("Formal", original.name)
        assertEquals("Very Formal", modified.name)
    }

    @Test
    fun test_copy_prompt() {
        val original = PromptOption(
            id = 1L,
            key = "formal",
            name = "Formal",
            prompt = "Be formal"
        )
        val modified = original.copy(prompt = "Write in a very formal and professional tone")

        assertEquals("Be formal", original.prompt)
        assertEquals("Write in a very formal and professional tone", modified.prompt)
    }

    @Test
    fun test_equality_sameValues() {
        val option1 = PromptOption(
            id = 1L,
            key = "formal",
            name = "Formal",
            prompt = "Be formal"
        )
        val option2 = PromptOption(
            id = 1L,
            key = "formal",
            name = "Formal",
            prompt = "Be formal"
        )

        assertEquals(option1, option2)
        assertEquals(option1.hashCode(), option2.hashCode())
    }

    @Test
    fun test_inequality_differentId() {
        val option1 = PromptOption(id = 1L, key = "formal", name = "Formal", prompt = "Be formal")
        val option2 = PromptOption(id = 2L, key = "formal", name = "Formal", prompt = "Be formal")

        assertNotEquals(option1, option2)
    }

    @Test
    fun test_inequality_differentKey() {
        val option1 = PromptOption(id = 1L, key = "formal", name = "Formal", prompt = "Be formal")
        val option2 = PromptOption(id = 1L, key = "casual", name = "Formal", prompt = "Be formal")

        assertNotEquals(option1, option2)
    }

    @Test
    fun test_longPromptText() {
        val longPrompt = "Write in a professional manner. ".repeat(100)
        val option = PromptOption(
            id = 1L,
            key = "detailed",
            name = "Detailed Instructions",
            prompt = longPrompt
        )

        assertEquals(longPrompt, option.prompt)
        assert(option.prompt.length > 3000)
    }

    @Test
    fun test_specialCharactersInName() {
        val option = PromptOption(
            id = 1L,
            key = "korean",
            name = "한글 톤 & 스타일 💌",
            prompt = "한글로 작성하세요"
        )

        assertEquals("한글 톤 & 스타일 💌", option.name)
        assertEquals("한글로 작성하세요", option.prompt)
    }

    @Test
    fun test_multipleLanguagesInPrompt() {
        val option = PromptOption(
            id = 1L,
            key = "multilingual",
            name = "Multilingual",
            prompt = "Write in English, 한글, 日本語, and include emojis 🌍"
        )

        assertEquals("Write in English, 한글, 日本語, and include emojis 🌍", option.prompt)
    }

    @Test
    fun test_keyVariations() {
        val options = listOf(
            PromptOption(1L, "formal", "Formal", "Be formal"),
            PromptOption(2L, "casual", "Casual", "Be casual"),
            PromptOption(3L, "friendly", "Friendly", "Be friendly"),
            PromptOption(4L, "professional", "Professional", "Be professional"),
            PromptOption(5L, "concise", "Concise", "Be brief")
        )

        assertEquals("formal", options[0].key)
        assertEquals("casual", options[1].key)
        assertEquals("friendly", options[2].key)
        assertEquals("professional", options[3].key)
        assertEquals("concise", options[4].key)
    }

    @Test
    fun test_copy_updatedAt() {
        val original = PromptOption(
            id = 1L,
            key = "formal",
            name = "Formal",
            prompt = "Be formal",
            createdAt = "2025-01-01",
            updatedAt = "2025-01-01"
        )
        val modified = original.copy(updatedAt = "2025-01-15")

        assertEquals("2025-01-01", original.updatedAt)
        assertEquals("2025-01-15", modified.updatedAt)
    }
}
