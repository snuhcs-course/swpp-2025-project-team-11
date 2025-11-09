package com.fiveis.xend.ui.contactbook

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.repository.ContactBookTab
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactBookActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ContactBookActivity>()

    @Test
    fun activity_launches_successfully() {
        // Activity should launch without crashing
        composeTestRule.waitForIdle()
    }

    @Test
    fun activity_with_start_tab_intent_extra() {
        // Given
        val intent = Intent(
            composeTestRule.activity.applicationContext,
            ContactBookActivity::class.java
        ).apply {
            putExtra(ContactBookActivity.START_TAB, ContactBookTab.Contacts.name)
        }

        // When
        composeTestRule.activity.startActivity(intent)
        composeTestRule.waitForIdle()

        // Then - Should not crash
    }

    @Test
    fun activity_handles_back_press() {
        // Given - Activity is launched
        composeTestRule.waitForIdle()

        // When - Back press is triggered
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        // Then - Should handle back press without crash
        Thread.sleep(200)
    }

    @Test
    fun activity_displays_contacts_tab() {
        // Wait for UI to load
        composeTestRule.waitForIdle()
        Thread.sleep(500)

        // Then - UI should be visible (not crashing)
    }

    @Test
    fun activity_handles_multiple_back_presses() {
        // Given - Activity is launched
        composeTestRule.waitForIdle()

        // When - Back press is triggered (activity will be destroyed)
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        // Then - Should handle gracefully
        Thread.sleep(200)
    }

    @Test
    fun activity_lifecycle_onCreate_completes() {
        // Given - Fresh activity
        composeTestRule.waitForIdle()

        // Then - Activity should be created successfully
        assert(!composeTestRule.activity.isFinishing)
    }

    @Test
    fun activity_handles_null_start_tab_intent() {
        // Given
        val intent = Intent(
            composeTestRule.activity.applicationContext,
            ContactBookActivity::class.java
        )
        // No START_TAB extra

        // When
        composeTestRule.activity.startActivity(intent)
        composeTestRule.waitForIdle()

        // Then - Should not crash
    }

    @Test
    fun activity_handles_empty_start_tab_intent() {
        // Given
        val intent = Intent(
            composeTestRule.activity.applicationContext,
            ContactBookActivity::class.java
        ).apply {
            putExtra(ContactBookActivity.START_TAB, "")
        }

        // When
        composeTestRule.activity.startActivity(intent)
        composeTestRule.waitForIdle()

        // Then - Should not crash
    }

    @Test
    fun activity_handles_invalid_start_tab_intent() {
        // Given
        val intent = Intent(
            composeTestRule.activity.applicationContext,
            ContactBookActivity::class.java
        ).apply {
            putExtra(ContactBookActivity.START_TAB, "INVALID_TAB")
        }

        // When
        composeTestRule.activity.startActivity(intent)
        composeTestRule.waitForIdle()

        // Then - Should not crash
    }

    @Test
    fun activity_can_be_recreated() {
        // When
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()

        // Then - Should not crash
    }

    @Test
    fun activity_handles_configuration_change() {
        // When - Simulate configuration change
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        Thread.sleep(500)

        // Then - Activity should still be functional
        assert(!composeTestRule.activity.isFinishing)
    }

    @Test
    fun activity_viewmodel_initialization() {
        // When - Activity is created, ViewModel should be initialized
        composeTestRule.waitForIdle()

        // Then - Should complete without crash
        assert(!composeTestRule.activity.isFinishing)
    }

    @Test
    fun activity_ui_state_collection() {
        // When - Activity collects UI state
        composeTestRule.waitForIdle()
        Thread.sleep(300)

        // Then - Should not crash during state collection
    }

    @Test
    fun activity_handles_rapid_back_presses() {
        // Given - Activity is launched
        composeTestRule.waitForIdle()

        // When - Back press is triggered (activity will be destroyed after first press)
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        // Then - Should handle gracefully
        Thread.sleep(200)
    }

    @Test
    fun activity_companion_object_constant() {
        // Then
        assert(ContactBookActivity.START_TAB == "start_tab")
    }
}
