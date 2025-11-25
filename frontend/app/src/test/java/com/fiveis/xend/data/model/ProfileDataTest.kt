package com.fiveis.xend.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileDataTest {

    @Test
    fun test_construction_withAllFields() {
        val profile = ProfileData(
            displayName = "John Doe",
            info = "Software Developer"
        )

        assertEquals("John Doe", profile.displayName)
        assertEquals("Software Developer", profile.info)
    }

    @Test
    fun test_construction_withNullFields() {
        val profile = ProfileData(
            displayName = null,
            info = null
        )

        assertNull(profile.displayName)
        assertNull(profile.info)
    }

    @Test
    fun test_construction_withDefaultValues() {
        val profile = ProfileData()

        assertNull(profile.displayName)
        assertNull(profile.info)
    }

    @Test
    fun test_copy_changesFields() {
        val original = ProfileData("Alice", "Manager")
        val modified = original.copy(displayName = "Bob")

        assertEquals("Bob", modified.displayName)
        assertEquals("Manager", modified.info)
    }

    @Test
    fun test_equals_sameValues_returnsTrue() {
        val profile1 = ProfileData("Name", "Info")
        val profile2 = ProfileData("Name", "Info")

        assertEquals(profile1, profile2)
    }
}
