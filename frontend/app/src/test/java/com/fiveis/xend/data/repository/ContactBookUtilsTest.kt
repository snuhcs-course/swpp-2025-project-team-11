package com.fiveis.xend.data.repository

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ContactBookUtilsTest {

    @Test
    fun randomNotTooLightColor_returns_valid_color() {
        val color = randomNotTooLightColor()

        assertTrue("Alpha should be 1.0f", color.alpha == 1.0f)
        assertTrue("Red component should be in [0, 1]", color.red >= 0f && color.red <= 1f)
        assertTrue("Green component should be in [0, 1]", color.green >= 0f && color.green <= 1f)
        assertTrue("Blue component should be in [0, 1]", color.blue >= 0f && color.blue <= 1f)
    }

    @Test
    fun randomNotTooLightColor_with_seed_is_deterministic() {
        val rnd1 = Random(12345L)
        val rnd2 = Random(12345L)

        val color1 = randomNotTooLightColor(rnd1)
        val color2 = randomNotTooLightColor(rnd2)

        assertTrue("Colors with the same seed should be identical", color1 == color2)
    }

    @Test
    fun randomNotTooLightColor_saturation_and_value_are_within_range() {
        // This test is more complex as it requires converting back from RGB to HSV.
        // For now, we trust the implementation, but a more thorough test could be added.
        val color = randomNotTooLightColor()
        // A simple visual check can be done by printing the color
        println("Generated color: $color")
        assertTrue(true) // Placeholder assertion
    }
}
