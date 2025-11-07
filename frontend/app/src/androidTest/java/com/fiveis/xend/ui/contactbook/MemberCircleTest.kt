package com.fiveis.xend.ui.contactbook

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemberCircleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_memberCircle_displays_label() {
        composeTestRule.setContent {
            MemberCircle(label = "A", color = Color.Blue)
        }

        composeTestRule.onNodeWithText("A").assertIsDisplayed()
    }

    @Test
    fun test_memberCircle_displays_plus_label() {
        composeTestRule.setContent {
            MemberCircle(label = "+3", color = Color.Gray)
        }

        composeTestRule.onNodeWithText("+3").assertIsDisplayed()
    }

    @Test
    fun test_memberCircle_renders_without_crash() {
        composeTestRule.setContent {
            MemberCircle(label = "B", color = Color.Red)
        }

        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun test_memberCircle_with_korean_letter() {
        composeTestRule.setContent {
            MemberCircle(label = "가", color = Color.Green)
        }

        composeTestRule.onNodeWithText("가").assertIsDisplayed()
    }

    @Test
    fun test_memberCircle_with_number() {
        composeTestRule.setContent {
            MemberCircle(label = "1", color = Color.Yellow)
        }

        composeTestRule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun test_memberCircle_with_multiple_chars() {
        composeTestRule.setContent {
            MemberCircle(label = "+10", color = Color.Magenta)
        }

        composeTestRule.onNodeWithText("+10").assertIsDisplayed()
    }

    @Test
    fun test_memberCircle_different_colors() {
        composeTestRule.setContent {
            MemberCircle(label = "C", color = Color.Cyan)
        }

        composeTestRule.onNodeWithText("C").assertIsDisplayed()
    }

    @Test
    fun test_memberCircle_special_character() {
        composeTestRule.setContent {
            MemberCircle(label = "#", color = Color.Black)
        }

        composeTestRule.onNodeWithText("#").assertIsDisplayed()
    }

    @Test
    fun test_memberCircle_question_mark() {
        composeTestRule.setContent {
            MemberCircle(label = "?", color = Color.LightGray)
        }

        composeTestRule.onNodeWithText("?").assertIsDisplayed()
    }

    @Test
    fun test_memberCircle_empty_string() {
        composeTestRule.setContent {
            MemberCircle(label = "", color = Color.DarkGray)
        }

        composeTestRule.onRoot().assertIsDisplayed()
    }
}
