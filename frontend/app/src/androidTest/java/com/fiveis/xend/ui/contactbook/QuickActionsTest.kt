package com.fiveis.xend.ui.contactbook

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickActionsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_quickActions_displays_new_group_button() {
        composeTestRule.setContent {
            QuickActions(onAddGroupClick = {})
        }

        composeTestRule.onNodeWithText(" 새 그룹").assertIsDisplayed()
    }

    @Test
    fun test_quickActions_new_group_click_triggers_callback() {
        var clicked = false

        composeTestRule.setContent {
            QuickActions(onAddGroupClick = { clicked = true })
        }

        composeTestRule.onNodeWithText(" 새 그룹").performClick()

        assert(clicked)
    }

    @Test
    fun test_quickActions_renders_without_crash() {
        composeTestRule.setContent {
            QuickActions(onAddGroupClick = {})
        }

        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun test_quickActions_button_is_clickable() {
        var clickCount = 0

        composeTestRule.setContent {
            QuickActions(onAddGroupClick = { clickCount++ })
        }

        composeTestRule.onNodeWithText(" 새 그룹").performClick()
        composeTestRule.onNodeWithText(" 새 그룹").performClick()

        assert(clickCount == 2)
    }

    @Test
    fun test_quickActions_multiple_clicks() {
        var clickCount = 0

        composeTestRule.setContent {
            QuickActions(onAddGroupClick = { clickCount++ })
        }

        repeat(5) {
            composeTestRule.onNodeWithText(" 새 그룹").performClick()
        }

        assert(clickCount == 5)
    }
}
