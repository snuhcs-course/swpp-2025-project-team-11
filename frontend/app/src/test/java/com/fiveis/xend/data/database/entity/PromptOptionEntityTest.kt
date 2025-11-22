package com.fiveis.xend.data.database.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PromptOptionEntityTest {

    @Test
    fun test_createPromptOptionEntity_allFieldsSet() {
        val entity = PromptOptionEntity(
            id = 1L,
            key = "formal",
            name = "Formal",
            prompt = "Write in a formal tone",
            createdAt = "2025-01-01",
            updatedAt = "2025-01-02"
        )

        assertEquals(1L, entity.id)
        assertEquals("formal", entity.key)
        assertEquals("Formal", entity.name)
        assertEquals("Write in a formal tone", entity.prompt)
        assertEquals("2025-01-01", entity.createdAt)
        assertEquals("2025-01-02", entity.updatedAt)
    }

    @Test
    fun test_createPromptOptionEntity_nullableFieldsNull() {
        val entity = PromptOptionEntity(
            id = 2L,
            key = "casual",
            name = "Casual",
            prompt = "Write casually"
        )

        assertEquals(2L, entity.id)
        assertEquals("casual", entity.key)
        assertEquals("Casual", entity.name)
        assertEquals("Write casually", entity.prompt)
        assertEquals(null, entity.createdAt)
        assertEquals(null, entity.updatedAt)
    }

    @Test
    fun test_equality_sameValues() {
        val entity1 = PromptOptionEntity(
            id = 1L,
            key = "formal",
            name = "Formal",
            prompt = "Write in a formal tone",
            createdAt = "2025-01-01",
            updatedAt = "2025-01-02"
        )
        val entity2 = PromptOptionEntity(
            id = 1L,
            key = "formal",
            name = "Formal",
            prompt = "Write in a formal tone",
            createdAt = "2025-01-01",
            updatedAt = "2025-01-02"
        )

        assertEquals(entity1, entity2)
        assertEquals(entity1.hashCode(), entity2.hashCode())
    }

    @Test
    fun test_equality_differentIds() {
        val entity1 = PromptOptionEntity(
            id = 1L,
            key = "formal",
            name = "Formal",
            prompt = "Write in a formal tone"
        )
        val entity2 = PromptOptionEntity(
            id = 2L,
            key = "formal",
            name = "Formal",
            prompt = "Write in a formal tone"
        )

        assertNotEquals(entity1, entity2)
    }

    @Test
    fun test_copy_modifyField() {
        val original = PromptOptionEntity(
            id = 1L,
            key = "formal",
            name = "Formal",
            prompt = "Write in a formal tone"
        )
        val modified = original.copy(name = "Very Formal")

        assertEquals(1L, modified.id)
        assertEquals("formal", modified.key)
        assertEquals("Very Formal", modified.name)
        assertEquals("Write in a formal tone", modified.prompt)
    }

    @Test
    fun test_toString_containsAllFields() {
        val entity = PromptOptionEntity(
            id = 1L,
            key = "formal",
            name = "Formal",
            prompt = "Write in a formal tone",
            createdAt = "2025-01-01",
            updatedAt = "2025-01-02"
        )

        val string = entity.toString()
        assert(string.contains("id=1"))
        assert(string.contains("key=formal"))
        assert(string.contains("name=Formal"))
        assert(string.contains("prompt=Write in a formal tone"))
    }
}
