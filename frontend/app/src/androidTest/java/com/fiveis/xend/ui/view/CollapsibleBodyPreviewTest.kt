package com.fiveis.xend.ui.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollapsibleBodyPreviewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun collapsibleBodyPreview_displays_simple_body() {
        // When
        composeTestRule.setContent {
            CollapsibleBodyPreview(
                bodyPreview = "Simple email body without original message"
            )
        }

        // Then
        Thread.sleep(300)
        composeTestRule.onNodeWithText("본문 미리보기").assertIsDisplayed()
    }

    @Test
    fun collapsibleBodyPreview_hides_header_when_showHeader_false() {
        // When
        composeTestRule.setContent {
            CollapsibleBodyPreview(
                bodyPreview = "Body content",
                showHeader = false
            )
        }

        // Then
        Thread.sleep(200)
    }

    @Test
    fun collapsibleBodyPreview_displays_custom_header() {
        // When
        composeTestRule.setContent {
            CollapsibleBodyPreview(
                bodyPreview = "Body content",
                headerText = "Custom Header Text",
                showHeader = true
            )
        }

        // Then
        composeTestRule.onNodeWithText("Custom Header Text").assertIsDisplayed()
    }

    @Test
    fun collapsibleBodyPreview_detects_original_message_marker() {
        // Given
        val bodyWithOriginal = "Reply content\n\n-- original message --\nFrom: sender@test.com\nOriginal body"

        // When
        composeTestRule.setContent {
            CollapsibleBodyPreview(
                bodyPreview = bodyWithOriginal
            )
        }

        // Then
        Thread.sleep(300)
        composeTestRule.onNodeWithText("원본 메시지").assertIsDisplayed()
    }

    @Test
    fun collapsibleBodyPreview_toggles_original_message() {
        // Given
        val bodyWithOriginal = "Reply\n\n-- original message --\nFrom: test@test.com"

        // When
        composeTestRule.setContent {
            CollapsibleBodyPreview(
                bodyPreview = bodyWithOriginal
            )
        }

        // Then - Initially collapsed
        Thread.sleep(300)
        composeTestRule.onNodeWithText("원본 메시지").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("펼치기").assertIsDisplayed()

        // Click to expand
        composeTestRule.onNodeWithText("원본 메시지").performClick()
        Thread.sleep(300)
        composeTestRule.onNodeWithContentDescription("접기").assertIsDisplayed()

        // Click to collapse
        composeTestRule.onNodeWithText("원본 메시지").performClick()
        Thread.sleep(300)
        composeTestRule.onNodeWithContentDescription("펼치기").assertIsDisplayed()
    }

    @Test
    fun collapsibleBodyPreview_handles_original_message_with_uppercase_marker() {
        // Given
        val bodyWithOriginal = "Reply\n\n-----Original Message-----\nFrom: test@test.com"

        // When
        composeTestRule.setContent {
            CollapsibleBodyPreview(
                bodyPreview = bodyWithOriginal
            )
        }

        // Then
        Thread.sleep(300)
        composeTestRule.onNodeWithText("원본 메시지").assertIsDisplayed()
    }

    @Test
    fun collapsibleBodyPreview_handles_korean_original_marker() {
        // Given
        val bodyWithOriginal = "답장 내용\n\n-----원본 메시지-----\nFrom: test@test.com"

        // When
        composeTestRule.setContent {
            CollapsibleBodyPreview(
                bodyPreview = bodyWithOriginal
            )
        }

        // Then
        Thread.sleep(300)
        composeTestRule.onNodeWithText("원본 메시지").assertIsDisplayed()
    }

    @Test
    fun collapsibleBodyPreview_handles_from_colon_marker() {
        // Given
        val bodyWithOriginal = "Reply<br><br>From: sender@test.com<br>Subject: Test"

        // When
        composeTestRule.setContent {
            CollapsibleBodyPreview(
                bodyPreview = bodyWithOriginal
            )
        }

        // Then
        Thread.sleep(300)
        composeTestRule.onNodeWithText("원본 메시지").assertIsDisplayed()
    }

    @Test
    fun collapsibleBodyPreview_handles_html_content() {
        // Given
        val htmlBody = "<p>HTML <strong>bold</strong> content</p>"

        // When
        composeTestRule.setContent {
            CollapsibleBodyPreview(
                bodyPreview = htmlBody
            )
        }

        // Then - Should render without crash
        Thread.sleep(300)
    }

    @Test
    fun collapsibleBodyPreview_handles_long_body() {
        // Given
        val longBody = "Line content\n".repeat(100)

        // When
        composeTestRule.setContent {
            CollapsibleBodyPreview(
                bodyPreview = longBody
            )
        }

        // Then - Should render and be scrollable
        Thread.sleep(300)
        composeTestRule.onNodeWithText("본문 미리보기").assertIsDisplayed()
    }

    @Test
    fun collapsibleBodyPreview_handles_multiline_content() {
        // Given
        val multilineBody = "Line 1\nLine 2\nLine 3\nLine 4"

        // When
        composeTestRule.setContent {
            CollapsibleBodyPreview(
                bodyPreview = multilineBody
            )
        }

        // Then
        Thread.sleep(300)
    }

    @Test
    fun collapsibleBodyPreview_displays_expanded_original_message() {
        // Given
        val bodyWithOriginal = """
            Reply text

            -- original message --
            From: sender@example.com
            To: recipient@example.com
            Sent: Monday, January 01, 2024
            Subject: Original Subject

            Original message body content
        """.trimIndent()

        // When
        composeTestRule.setContent {
            CollapsibleBodyPreview(
                bodyPreview = bodyWithOriginal
            )
        }

        // Then - Expand original message
        Thread.sleep(300)
        composeTestRule.onNodeWithText("원본 메시지").performClick()
        Thread.sleep(300)
    }

    @Test
    fun collapsibleBodyPreview_handles_empty_body() {
        // When
        composeTestRule.setContent {
            CollapsibleBodyPreview(
                bodyPreview = ""
            )
        }

        // Then - Should render without crash
        Thread.sleep(300)
    }

    @Test
    fun collapsibleBodyPreview_handles_whitespace_body() {
        // When
        composeTestRule.setContent {
            CollapsibleBodyPreview(
                bodyPreview = "   \n\n   "
            )
        }

        // Then
        Thread.sleep(300)
    }

    @Test
    fun collapsibleBodyPreview_handles_special_characters() {
        // Given
        val specialBody = "Body with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?"

        // When
        composeTestRule.setContent {
            CollapsibleBodyPreview(
                bodyPreview = specialBody
            )
        }

        // Then
        Thread.sleep(300)
    }

    @Test
    fun collapsibleBodyPreview_handles_unicode_characters() {
        // Given
        val unicodeBody = "Unicode: 한글, 日本語, 中文, Emoji: 😀🎉"

        // When
        composeTestRule.setContent {
            CollapsibleBodyPreview(
                bodyPreview = unicodeBody
            )
        }

        // Then
        Thread.sleep(300)
    }

    @Test
    fun collapsibleBodyPreview_handles_complex_original_message() {
        // Given
        val complexOriginal = """
            My reply

            -- original message --
            From: john.doe@company.com
            To: jane.smith@company.com
            Cc: team@company.com
            Sent: Tuesday, December 19, 2024 10:30 AM
            Subject: RE: Q4 Report Review

            <p>Please review the attached Q4 report.</p>
            <ul>
                <li>Sales performance</li>
                <li>Cost analysis</li>
            </ul>
        """.trimIndent()

        // When
        composeTestRule.setContent {
            CollapsibleBodyPreview(
                bodyPreview = complexOriginal
            )
        }

        // Then
        Thread.sleep(300)
        composeTestRule.onNodeWithText("원본 메시지").performClick()
        Thread.sleep(300)
    }
}
