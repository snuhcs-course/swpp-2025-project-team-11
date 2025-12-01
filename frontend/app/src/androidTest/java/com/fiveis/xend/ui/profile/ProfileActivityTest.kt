package com.fiveis.xend.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.ui.theme.XendTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileActivityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_profileActivity_displays() {
        composeTestRule.setContent {
            XendTheme {
                ProfileScreen(
                    uiState = ProfileUiState(
                        userEmail = "test@example.com",
                        displayName = "Test User"
                    ),
                    onLogout = {},
                    onBack = {},
                    onForceLogout = {},
                    onDismissLogoutFailureDialog = {},
                    onToggleEditMode = {},
                    onUpdateDisplayName = {},
                    onUpdateInfo = {},
                    onUpdateLanguagePreference = {},
                    onSaveProfile = {},
                    onDismissProfileError = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("프로필").assertIsDisplayed()
    }
}
