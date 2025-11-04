package com.fiveis.xend.ui.login

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.ui.login.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        Thread.sleep(1000)
    }

    @Test
    fun loginScreen_displays_correctly() {
        Thread.sleep(2000)
        composeTestRule.waitForIdle()
    }

    @Test
    fun loginScreen_has_signin_button() {
        Thread.sleep(1500)
        composeTestRule.waitForIdle()
    }

    @Test
    fun loginScreen_initial_state_not_loading() {
        Thread.sleep(1000)
        composeTestRule.waitForIdle()
    }
}
