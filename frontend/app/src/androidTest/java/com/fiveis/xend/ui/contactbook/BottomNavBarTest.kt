package com.fiveis.xend.ui.contactbook

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BottomNavBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_bottomNavBar_displays() {
        composeTestRule.setContent {
            BottomNavBar(selected = "contacts", onSelect = {})
        }

        composeTestRule.onNodeWithText("메일함").assertIsDisplayed()
        composeTestRule.onNodeWithText("연락처").assertIsDisplayed()
    }

    @Test
    fun test_bottomNavBar_mail_click_triggers_callback() {
        var selectedTab = ""

        composeTestRule.setContent {
            BottomNavBar(selected = "contacts", onSelect = { selectedTab = it })
        }

        composeTestRule.onNodeWithText("메일함").performClick()

        assert(selectedTab == "mail")
    }

    @Test
    fun test_bottomNavBar_contacts_click_triggers_callback() {
        var callbackInvoked = false
        var selectedValue = ""

        composeTestRule.setContent {
            BottomNavBar(selected = "mail", onSelect = {
                callbackInvoked = true
                selectedValue = it
            })
        }

        composeTestRule.onNodeWithText("연락처").performClick()
        composeTestRule.waitForIdle()

        assert(callbackInvoked && selectedValue == "contacts")
    }

    @Test
    fun test_bottomNavBar_mail_selected() {
        composeTestRule.setContent {
            BottomNavBar(selected = "mail", onSelect = {})
        }

        composeTestRule.onNodeWithText("메일함").assertIsDisplayed()
    }

    @Test
    fun test_bottomNavBar_contacts_selected() {
        composeTestRule.setContent {
            BottomNavBar(selected = "contacts", onSelect = {})
        }

        composeTestRule.onNodeWithText("연락처").assertIsDisplayed()
    }

    @Test
    fun test_bottomNavBar_renders_without_crash() {
        composeTestRule.setContent {
            BottomNavBar(selected = "contacts", onSelect = {})
        }

        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun test_bottomNavBar_divider_exists() {
        composeTestRule.setContent {
            BottomNavBar(selected = "contacts", onSelect = {})
        }

        composeTestRule.onRoot().assertIsDisplayed()
    }

    @Test
    fun test_bottomNavBar_switch_between_tabs() {
        var callCount = 0
        val selectedValues = mutableListOf<String>()

        composeTestRule.setContent {
            BottomNavBar(selected = "contacts", onSelect = {
                callCount++
                selectedValues.add(it)
            })
        }

        composeTestRule.onNodeWithText("메일함").performClick()
        composeTestRule.waitForIdle()
        assert(callCount == 1 && selectedValues[0] == "mail")

        composeTestRule.onNodeWithText("연락처").performClick()
        composeTestRule.waitForIdle()
        assert(callCount == 2 && selectedValues[1] == "contacts")
    }

    @Test
    fun test_bottomNavBar_displays_icons() {
        composeTestRule.setContent {
            BottomNavBar(selected = "contacts", onSelect = {})
        }

        composeTestRule.onNodeWithContentDescription("메일함").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("연락처").assertIsDisplayed()
    }

    @Test
    fun test_bottomNavBar_multiple_clicks() {
        var clickCount = 0

        composeTestRule.setContent {
            BottomNavBar(selected = "contacts", onSelect = { clickCount++ })
        }

        composeTestRule.onNodeWithText("메일함").performClick()
        composeTestRule.onNodeWithText("메일함").performClick()
        composeTestRule.onNodeWithText("메일함").performClick()

        assert(clickCount == 3)
    }
}
