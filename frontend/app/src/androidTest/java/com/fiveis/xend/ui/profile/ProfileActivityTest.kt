package com.fiveis.xend.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ProfileActivity>()

    @Test
    fun test_profileActivity_displays() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("프로필").assertIsDisplayed()
    }
}
