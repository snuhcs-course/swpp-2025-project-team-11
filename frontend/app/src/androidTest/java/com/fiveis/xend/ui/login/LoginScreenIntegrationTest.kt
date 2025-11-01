package com.fiveis.xend.ui.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.ui.theme.XendTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_displays_app_title() {
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Xend로 시작하세요")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_displays_subtitle() {
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("AI가 도와주는\n간편한 메일 작성")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_when_not_logged_in_shows_login_button() {
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoggedIn = false),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Google로 계속하기")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_when_logged_in_shows_logout_button() {
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(
                        isLoggedIn = true,
                        userEmail = "test@example.com"
                    ),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("로그아웃")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_when_logged_in_shows_user_email() {
        val testEmail = "test@example.com"

        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(
                        isLoggedIn = true,
                        userEmail = testEmail
                    ),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("로그인됨: $testEmail")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_login_button_click_triggers_callback() {
        var loginClicked = false

        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoggedIn = false),
                    onLoginClick = { loginClicked = true },
                    onLogoutClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Google로 계속하기")
            .performClick()

        assertTrue(loginClicked)
    }

    @Test
    fun loginScreen_logout_button_click_triggers_callback() {
        var logoutClicked = false

        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(
                        isLoggedIn = true,
                        userEmail = "test@example.com"
                    ),
                    onLoginClick = {},
                    onLogoutClick = { logoutClicked = true }
                )
            }
        }

        composeTestRule
            .onNodeWithText("로그아웃")
            .performClick()

        assertTrue(logoutClicked)
    }

    @Test
    fun loginScreen_displays_feature_descriptions() {
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("받는 사람과의 관계를 고려한 AI 작성")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("복잡한 메일도 몇 초만에 완성")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("나만의 톤과 스타일로 자동 작성")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_displays_special_features_title() {
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Xend만의 특별한 기능")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_displays_terms_and_conditions() {
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("계속 진행하면 Xend의 서비스 약관 및\n개인정보 처리방침에 동의하는 것입니다")
            .assertIsDisplayed()
    }
}
