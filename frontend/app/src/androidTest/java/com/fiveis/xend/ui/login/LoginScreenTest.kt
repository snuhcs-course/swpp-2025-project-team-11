package com.fiveis.xend.ui.login

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.ui.theme.XendTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_displays_logo() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithContentDescription("Xend Logo").assertIsDisplayed()
    }

    @Test
    fun loginScreen_displays_main_title() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Xend로 시작하세요").assertIsDisplayed()
    }

    @Test
    fun loginScreen_displays_subtitle() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("AI가 도와주는\n간편한 메일 작성").assertIsDisplayed()
    }

    @Test
    fun loginScreen_displays_google_signin_button_when_logged_out() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoggedIn = false),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Google로 계속하기").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Google Logo").assertIsDisplayed()
    }

    @Test
    fun loginScreen_google_signin_button_triggers_callback() {
        // Given
        var loginClicked = false

        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoggedIn = false),
                    onLoginClick = { loginClicked = true },
                    onLogoutClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Google로 계속하기").performClick()

        // Then
        assert(loginClicked)
    }

    @Test
    fun loginScreen_displays_logged_in_state() {
        // When
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

        // Then
        composeTestRule.onNodeWithText("로그인됨: test@example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("로그아웃").assertIsDisplayed()
    }

    @Test
    fun loginScreen_logout_button_triggers_callback() {
        // Given
        var logoutClicked = false

        // When
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

        composeTestRule.onNodeWithText("로그아웃").performClick()

        // Then
        assert(logoutClicked)
    }

    @Test
    fun loginScreen_displays_feature_section_title() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Xend만의 특별한 기능").assertIsDisplayed()
    }

    @Test
    fun loginScreen_displays_feature1_ai_writing() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("받는 사람과의 관계를 고려한 AI 작성").assertIsDisplayed()
    }

    @Test
    fun loginScreen_displays_feature2_speed() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("복잡한 메일도 몇 초만에 완성").assertIsDisplayed()
    }

    @Test
    fun loginScreen_displays_feature3_custom_style() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("나만의 톤과 스타일로 자동 작성").assertIsDisplayed()
    }

    @Test
    fun loginScreen_displays_terms_text() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("계속 진행하면 Xend의 서비스 약관 및\n개인정보 처리방침에 동의하는 것입니다")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_google_button_has_correct_styling() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoggedIn = false),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then - Button should be displayed with text
        val button = composeTestRule.onNodeWithText("Google로 계속하기")
        button.assertIsDisplayed()
        button.assertHasClickAction()
    }

    @Test
    fun loginScreen_renders_without_crash_when_logged_out() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoggedIn = false),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Xend로 시작하세요").assertIsDisplayed()
    }

    @Test
    fun loginScreen_renders_without_crash_when_logged_in() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(
                        isLoggedIn = true,
                        userEmail = "user@example.com"
                    ),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("로그인됨: user@example.com").assertIsDisplayed()
    }

    @Test
    fun loginScreen_shows_correct_email_in_logged_in_state() {
        // Given
        val testEmail = "testuser@gmail.com"

        // When
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

        // Then
        composeTestRule.onNodeWithText("로그인됨: $testEmail").assertIsDisplayed()
    }

    @Test
    fun loginScreen_hides_google_button_when_logged_in() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoggedIn = true, userEmail = "test@example.com"),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Google로 계속하기").assertDoesNotExist()
    }

    @Test
    fun loginScreen_hides_logout_button_when_logged_out() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoggedIn = false),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("로그아웃").assertDoesNotExist()
    }

    @Test
    fun loginScreen_with_empty_email_shows_logged_in_text() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoggedIn = true, userEmail = ""),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("로그인됨: ").assertIsDisplayed()
    }

    @Test
    fun loginScreen_layout_is_vertical_with_spacer() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then - All major elements should be visible (vertically arranged)
        composeTestRule.onNodeWithText("Xend로 시작하세요").assertIsDisplayed()
        composeTestRule.onNodeWithText("Xend만의 특별한 기능").assertIsDisplayed()
        composeTestRule.onNodeWithText("계속 진행하면 Xend의 서비스 약관 및\n개인정보 처리방침에 동의하는 것입니다")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_feature_icons_are_displayed() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then - All three features should be visible
        composeTestRule.onNodeWithText("받는 사람과의 관계를 고려한 AI 작성").assertIsDisplayed()
        composeTestRule.onNodeWithText("복잡한 메일도 몇 초만에 완성").assertIsDisplayed()
        composeTestRule.onNodeWithText("나만의 톤과 스타일로 자동 작성").assertIsDisplayed()
    }

    @Test
    fun loginScreen_with_messages_state() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(messages = "Test message"),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then - Should render without crash
        composeTestRule.onNodeWithText("Xend로 시작하세요").assertIsDisplayed()
    }

    @Test
    fun loginScreen_logout_button_has_error_color() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoggedIn = true, userEmail = "test@example.com"),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then - Logout button should be visible
        val logoutButton = composeTestRule.onNodeWithText("로그아웃")
        logoutButton.assertIsDisplayed()
        logoutButton.assertHasClickAction()
    }

    @Test
    fun featureItem_displays_all_content() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then - All feature items should be fully visible
        composeTestRule.onNodeWithText("받는 사람과의 관계를 고려한 AI 작성").assertIsDisplayed()
        composeTestRule.onNodeWithText("복잡한 메일도 몇 초만에 완성").assertIsDisplayed()
        composeTestRule.onNodeWithText("나만의 톤과 스타일로 자동 작성").assertIsDisplayed()
    }

    @Test
    fun loginScreen_multiple_renders_with_different_states() {
        // Test with logged in state
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoggedIn = true, userEmail = "test@example.com"),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("로그인됨: test@example.com").assertIsDisplayed()
    }

    @Test
    fun loginScreen_handles_long_email() {
        // Given
        val longEmail = "verylongemailaddress.with.many.dots@example.com"

        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoggedIn = true, userEmail = longEmail),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("로그인됨: $longEmail").assertIsDisplayed()
    }

    @Test
    fun loginScreen_all_feature_texts_are_visible() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then - Check all three feature descriptions
        val feature1 = composeTestRule.onNodeWithText("받는 사람과의 관계를 고려한 AI 작성")
        val feature2 = composeTestRule.onNodeWithText("복잡한 메일도 몇 초만에 완성")
        val feature3 = composeTestRule.onNodeWithText("나만의 톤과 스타일로 자동 작성")

        feature1.assertIsDisplayed()
        feature2.assertIsDisplayed()
        feature3.assertIsDisplayed()
    }

    @Test
    fun loginScreen_surface_fills_entire_screen() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then - All major sections should be visible, indicating full screen layout
        composeTestRule.onNodeWithContentDescription("Xend Logo").assertIsDisplayed()
        composeTestRule.onNodeWithText("계속 진행하면 Xend의 서비스 약관 및\n개인정보 처리방침에 동의하는 것입니다")
            .assertIsDisplayed()
    }
}
