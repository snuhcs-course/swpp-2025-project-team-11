package com.fiveis.xend.data.database.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileEntityTest {

    @Test
    fun test_construction_withAllFields() {
        val profile = ProfileEntity(
            id = 0,
            displayName = "John Doe",
            info = "Software Engineer",
            languagePreference = "en"
        )

        assertEquals(0, profile.id)
        assertEquals("John Doe", profile.displayName)
        assertEquals("Software Engineer", profile.info)
        assertEquals("en", profile.languagePreference)
    }

    @Test
    fun test_construction_withNullFields() {
        val profile = ProfileEntity(
            id = 0,
            displayName = null,
            info = null,
            languagePreference = null
        )

        assertEquals(0, profile.id)
        assertNull(profile.displayName)
        assertNull(profile.info)
        assertNull(profile.languagePreference)
    }

    @Test
    fun test_construction_withDefaultValues() {
        val profile = ProfileEntity()

        assertEquals(0, profile.id)
        assertNull(profile.displayName)
        assertNull(profile.info)
        assertNull(profile.languagePreference)
    }

    @Test
    fun test_copy_changesDisplayName() {
        val original = ProfileEntity(
            id = 0,
            displayName = "Alice",
            info = "Designer",
            languagePreference = "ko"
        )

        val modified = original.copy(displayName = "Bob")

        assertEquals("Bob", modified.displayName)
        assertEquals("Alice", original.displayName)
        assertEquals("Designer", modified.info)
    }

    @Test
    fun test_equality() {
        val profile1 = ProfileEntity(
            id = 0,
            displayName = "Test",
            info = "Info",
            languagePreference = "en"
        )

        val profile2 = profile1.copy()

        assertEquals(profile1, profile2)
        assertEquals(profile1.hashCode(), profile2.hashCode())
    }
}
