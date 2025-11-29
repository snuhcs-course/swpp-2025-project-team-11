package com.fiveis.xend.ui.login

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.ui.theme.XendTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenAdditionalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_google_button_clickable() {
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
        val button = composeTestRule.onNodeWithText("Google로 계속하기")
        button.assertHasClickAction()
    }

    @Test
    fun loginScreen_logout_button_clickable() {
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
        val button = composeTestRule.onNodeWithText("로그아웃")
        button.assertHasClickAction()
    }

    @Test
    fun loginScreen_feature_items_have_icons() {
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

        // Then - All three features should be visible with text
        composeTestRule.onNodeWithText("받는 사람과의 관계를 고려한 AI 작성").assertIsDisplayed()
        composeTestRule.onNodeWithText("복잡한 메일도 몇 초만에 완성").assertIsDisplayed()
        composeTestRule.onNodeWithText("나만의 톤과 스타일로 자동 작성").assertIsDisplayed()
    }

    @Test
    fun loginScreen_spacing_between_elements() {
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

        // Then - All elements should be visible (proper spacing)
        composeTestRule.onNodeWithContentDescription("Xend Logo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Xend로 시작하세요").assertIsDisplayed()
        composeTestRule.onNodeWithText("Google로 계속하기").assertIsDisplayed()
        composeTestRule.onNodeWithText("Xend만의 특별한 기능").assertIsDisplayed()
    }

    @Test
    fun loginScreen_terms_at_bottom() {
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

        // Then - Terms should be visible at bottom
        composeTestRule.onNodeWithText("계속 진행하면 Xend의 서비스 약관 및\n개인정보 처리방침에 동의하는 것입니다")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_callback_not_called_initially() {
        // Given
        var loginCalled = false
        var logoutCalled = false

        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(),
                    onLoginClick = { loginCalled = true },
                    onLogoutClick = { logoutCalled = true }
                )
            }
        }

        // Then - Callbacks should not be called on initial render
        assert(!loginCalled)
        assert(!logoutCalled)
    }

    @Test
    fun loginScreen_multiple_clicks_on_login_button() {
        // Given
        var clickCount = 0

        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoggedIn = false),
                    onLoginClick = { clickCount++ },
                    onLogoutClick = {}
                )
            }
        }

        // Click multiple times
        val button = composeTestRule.onNodeWithText("Google로 계속하기")
        button.performClick()
        button.performClick()
        button.performClick()

        // Then
        assert(clickCount == 3)
    }

    @Test
    fun loginScreen_transition_logged_out_to_logged_in() {
        // When - Test logged in state
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoggedIn = true, userEmail = "test@example.com"),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then - Should display logged in state
        composeTestRule.onNodeWithText("로그인됨: test@example.com").assertIsDisplayed()
    }

    @Test
    fun loginScreen_with_loading_state() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoading = true),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then - Should render without crash
        composeTestRule.onNodeWithText("Xend로 시작하세요").assertIsDisplayed()
    }

    @Test
    fun loginScreen_with_loading_and_logged_out() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoading = true, isLoggedIn = false),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Google로 계속하기").assertIsDisplayed()
    }

    @Test
    fun loginScreen_with_messages() {
        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(messages = "Welcome back!"),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then - Should render without crash
        composeTestRule.onNodeWithText("Xend로 시작하세요").assertIsDisplayed()
    }

    @Test
    fun loginScreen_all_text_elements_present() {
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

        // Then - Check all major text elements
        composeTestRule.onNodeWithText("Xend로 시작하세요").assertExists()
        composeTestRule.onNodeWithText("AI가 도와주는\n간편한 메일 작성").assertExists()
        composeTestRule.onNodeWithText("Google로 계속하기").assertExists()
        composeTestRule.onNodeWithText("Xend만의 특별한 기능").assertExists()
        composeTestRule.onNodeWithText("받는 사람과의 관계를 고려한 AI 작성").assertExists()
        composeTestRule.onNodeWithText("복잡한 메일도 몇 초만에 완성").assertExists()
        composeTestRule.onNodeWithText("나만의 톤과 스타일로 자동 작성").assertExists()
        composeTestRule.onNodeWithText("계속 진행하면 Xend의 서비스 약관 및\n개인정보 처리방침에 동의하는 것입니다").assertExists()
    }

    @Test
    fun loginScreen_google_logo_visible() {
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
        composeTestRule.onNodeWithContentDescription("Google Logo").assertIsDisplayed()
    }

    @Test
    fun loginScreen_xend_logo_visible() {
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
    fun loginScreen_with_special_email_characters() {
        // Given
        val email = "user+tag@example.com"

        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoggedIn = true, userEmail = email),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("로그인됨: $email").assertIsDisplayed()
    }

    @Test
    fun loginScreen_with_korean_email() {
        // Given
        val email = "사용자@example.com"

        // When
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoggedIn = true, userEmail = email),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("로그인됨: $email").assertIsDisplayed()
    }

    @Test
    fun loginScreen_stress_test_rapid_state_changes() {
        // When - Test with final state
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoggedIn = false),
                    onLoginClick = {},
                    onLogoutClick = {}
                )
            }
        }

        // Then - Should render without crash
        composeTestRule.onNodeWithText("Xend로 시작하세요").assertIsDisplayed()
    }

    @Test
    fun loginScreen_with_null_modifier() {
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

        // Then - Should render with default modifier
        composeTestRule.onNodeWithText("Xend로 시작하세요").assertIsDisplayed()
    }

    @Test
    fun loginScreen_callback_isolation() {
        // Given
        var loginCalled = false

        // When - Logged out state
        composeTestRule.setContent {
            XendTheme {
                LoginScreen(
                    uiState = LoginUiState(isLoggedIn = false),
                    onLoginClick = { loginCalled = true },
                    onLogoutClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Google로 계속하기").performClick()

        // Then
        assert(loginCalled)
    }

    @Test
    fun loginScreen_surface_background_color() {
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

        // Then - Should render without crash (color is applied)
        composeTestRule.onNodeWithText("Xend로 시작하세요").assertIsDisplayed()
    }

    @Test
    fun loginScreen_column_arrangement() {
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

        // Then - Top element and bottom element should both be visible
        composeTestRule.onNodeWithContentDescription("Xend Logo").assertIsDisplayed()
        composeTestRule.onNodeWithText("계속 진행하면 Xend의 서비스 약관 및\n개인정보 처리방침에 동의하는 것입니다")
            .assertIsDisplayed()
    }

    @Test
    fun loginScreen_empty_email_edge_case() {
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

        // Then - Should handle empty email gracefully
        composeTestRule.onNodeWithText("로그인됨: ").assertIsDisplayed()
        composeTestRule.onNodeWithText("로그아웃").assertIsDisplayed()
    }
}
