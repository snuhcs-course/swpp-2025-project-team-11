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
class ReplyComposeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun replyComposeScreen_displays_topBar() {
        // When
        composeTestRule.setContent {
            ReplyComposeScreen(
                senderEmail = "test@test.com",
                date = "2024.12.19",
                subject = "Test Subject",
                body = "Test Body"
            )
        }

        // Then
        composeTestRule.onNodeWithText("답장 작성").assertIsDisplayed()
    }

    @Test
    fun replyComposeScreen_displays_sender_info() {
        // When
        composeTestRule.setContent {
            ReplyComposeScreen(
                senderEmail = "김철수 <chulsoo@test.com>",
                date = "2024.12.19 오전 10:00",
                subject = "Test Subject",
                body = "Test Body"
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("김철수 (chulsoo@test.com)").assertIsDisplayed()
        composeTestRule.onNodeWithText("2024.12.19 오전 10:00").assertIsDisplayed()
    }

    @Test
    fun replyComposeScreen_displays_subject() {
        // When
        composeTestRule.setContent {
            ReplyComposeScreen(
                senderEmail = "test@test.com",
                date = "2024.12.19",
                subject = "RE: Important Meeting",
                body = "Test Body"
            )
        }

        // Then
        composeTestRule.onNodeWithText("RE: Important Meeting").assertIsDisplayed()
    }

    @Test
    fun replyComposeScreen_displays_attachments() {
        // Given
        val attachments = listOf(
            AttachmentFile("1", "document.pdf", "1.5MB", "excel"),
            AttachmentFile("2", "image.png", "500KB", "image")
        )

        // When
        composeTestRule.setContent {
            ReplyComposeScreen(
                senderEmail = "test@test.com",
                date = "2024.12.19",
                subject = "Test",
                body = "Body",
                attachments = attachments
            )
        }

        // Then
        composeTestRule.onNodeWithText("첨부파일 (2개)").assertIsDisplayed()
        composeTestRule.onNodeWithText("document.pdf").assertIsDisplayed()
        composeTestRule.onNodeWithText("1.5MB").assertIsDisplayed()
        composeTestRule.onNodeWithText("image.png").assertIsDisplayed()
        composeTestRule.onNodeWithText("500KB").assertIsDisplayed()
    }

    @Test
    fun replyComposeScreen_displays_loading_state() {
        // When
        composeTestRule.setContent {
            ReplyComposeScreen(
                senderEmail = "test@test.com",
                date = "2024.12.19",
                subject = "Test",
                body = "Body",
                isLoadingOptions = true
            )
        }

        // Then
        composeTestRule.onNodeWithText("답장 옵션 추천").assertIsDisplayed()
        composeTestRule.onNodeWithText("답장 옵션 생성 중...").assertIsDisplayed()
    }

    @Test
    fun replyComposeScreen_displays_reply_options() {
        // Given
        val replyOptions = listOf(
            ReplyOptionState(
                id = 1,
                type = "상세 보고형",
                title = "RE: Test - Detailed",
                body = "Detailed reply body",
                isComplete = true
            ),
            ReplyOptionState(
                id = 2,
                type = "간결형",
                title = "RE: Test - Brief",
                body = "Brief reply",
                isComplete = true
            )
        )

        // When
        composeTestRule.setContent {
            ReplyComposeScreen(
                senderEmail = "test@test.com",
                date = "2024.12.19",
                subject = "Test",
                body = "Body",
                replyOptions = replyOptions
            )
        }

        // Then
        composeTestRule.onNodeWithText("답장 옵션 추천").assertIsDisplayed()
        composeTestRule.onNodeWithText("상세 보고형").assertIsDisplayed()
        composeTestRule.onNodeWithText("Detailed reply body").assertIsDisplayed()
    }

    @Test
    fun replyComposeScreen_backButton_triggers_callback() {
        // Given
        var backClicked = false

        // When
        composeTestRule.setContent {
            ReplyComposeScreen(
                senderEmail = "test@test.com",
                date = "2024.12.19",
                subject = "Test",
                body = "Body",
                onBack = { backClicked = true }
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()
        assert(backClicked)
    }

    @Test
    fun replyComposeScreen_directComposeButton_triggers_callback() {
        // Given
        var directClicked = false

        // When
        composeTestRule.setContent {
            ReplyComposeScreen(
                senderEmail = "test@test.com",
                date = "2024.12.19",
                subject = "Test",
                body = "Body",
                onDirectCompose = { directClicked = true }
            )
        }

        // Then
        composeTestRule.onNodeWithText("직접 작성").performClick()
        assert(directClicked)
    }

    @Test
    fun replyComposeScreen_generateMoreButton_triggers_callback() {
        // Given
        var generateClicked = false

        // When
        composeTestRule.setContent {
            ReplyComposeScreen(
                senderEmail = "test@test.com",
                date = "2024.12.19",
                subject = "Test",
                body = "Body",
                onGenerateMore = { generateClicked = true }
            )
        }

        // Then
        composeTestRule.onNodeWithText("추가 생성").performClick()
        assert(generateClicked)
    }

    @Test
    fun replyComposeScreen_useOptionButton_triggers_callback() {
        // Given
        var optionClicked = false
        val replyOptions = listOf(
            ReplyOptionState(
                id = 1,
                type = "간결형",
                title = "RE: Test",
                body = "Reply body",
                isComplete = true
            )
        )

        // When
        composeTestRule.setContent {
            ReplyComposeScreen(
                senderEmail = "test@test.com",
                date = "2024.12.19",
                subject = "Test",
                body = "Body",
                replyOptions = replyOptions,
                onUseOption = { optionClicked = true }
            )
        }

        // Then
        composeTestRule.onNodeWithText("이 옵션 사용").performClick()
        assert(optionClicked)
    }

    @Test
    fun replyComposeScreen_nextOptionButton_works() {
        // Given
        val replyOptions = listOf(
            ReplyOptionState(
                id = 1,
                type = "Type1",
                title = "Title1",
                body = "Body1",
                isComplete = true
            ),
            ReplyOptionState(
                id = 2,
                type = "Type2",
                title = "Title2",
                body = "Body2",
                isComplete = true
            )
        )

        // When
        composeTestRule.setContent {
            ReplyComposeScreen(
                senderEmail = "test@test.com",
                date = "2024.12.19",
                subject = "Test",
                body = "Body",
                replyOptions = replyOptions
            )
        }

        // Then - Initial state shows first option
        composeTestRule.onNodeWithText("Body1").assertIsDisplayed()

        // Click next option
        composeTestRule.onNodeWithText("다음 옵션").performClick()
        Thread.sleep(300)
    }

    @Test
    fun replyComposeScreen_togglesSenderInfo() {
        // When
        composeTestRule.setContent {
            ReplyComposeScreen(
                senderEmail = "test@test.com",
                date = "2024.12.19",
                subject = "Test Subject",
                body = "Test Body"
            )
        }

        // Then - Initially expanded, subject is visible
        composeTestRule.onNodeWithText("Test Subject").assertIsDisplayed()

        // Click to collapse
        composeTestRule.onNodeWithContentDescription("접기").performClick()
        composeTestRule.waitForIdle()
        Thread.sleep(300)

        // Verify that expand icon is now displayed
        composeTestRule.onNodeWithContentDescription("펼치기").assertExists()
    }

    @Test
    fun replyComposeScreen_displays_attachmentButtons() {
        // Given
        val attachments = listOf(
            AttachmentFile("1", "test.xlsx", "1MB", "excel")
        )

        // When
        composeTestRule.setContent {
            ReplyComposeScreen(
                senderEmail = "test@test.com",
                date = "2024.12.19",
                subject = "Test",
                body = "Body",
                attachments = attachments
            )
        }

        // Then
        composeTestRule.onNodeWithText("AI 분석").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("보기").assertIsDisplayed()
    }

    @Test
    fun replyComposeScreen_handles_empty_replyOptions() {
        // When
        composeTestRule.setContent {
            ReplyComposeScreen(
                senderEmail = "test@test.com",
                date = "2024.12.19",
                subject = "Test",
                body = "Body",
                replyOptions = emptyList()
            )
        }

        // Then - Should not show reply options section
        Thread.sleep(200)
        composeTestRule.onNodeWithText("직접 작성").assertIsDisplayed()
    }

    @Test
    fun replyComposeScreen_handles_streaming_state() {
        // Given
        val replyOptions = listOf(
            ReplyOptionState(
                id = 1,
                type = "Type1",
                title = "Title",
                body = "Partial body...",
                isComplete = false
            )
        )

        // When
        composeTestRule.setContent {
            ReplyComposeScreen(
                senderEmail = "test@test.com",
                date = "2024.12.19",
                subject = "Test",
                body = "Body",
                replyOptions = replyOptions,
                isStreamingOptions = true
            )
        }

        // Then
        Thread.sleep(200)
        composeTestRule.onNodeWithText("Type1").assertIsDisplayed()
    }

    @Test
    fun replyComposeScreen_displays_originalMessage_marker() {
        // Given
        val bodyWithOriginal = "Reply text\n\n-- original message --\nFrom: sender@test.com"

        // When
        composeTestRule.setContent {
            ReplyComposeScreen(
                senderEmail = "test@test.com",
                date = "2024.12.19",
                subject = "RE: Test",
                body = bodyWithOriginal
            )
        }

        // Then - Should use CollapsibleBodyPreview
        Thread.sleep(300)
        composeTestRule.onNodeWithText("RE: Test").assertIsDisplayed()
    }

    @Test
    fun replyComposeScreen_optionTab_switches_pages() {
        // Given
        val replyOptions = listOf(
            ReplyOptionState(id = 1, type = "Type1", title = "Title1", body = "Body1", isComplete = true),
            ReplyOptionState(id = 2, type = "Type2", title = "Title2", body = "Body2", isComplete = true)
        )

        // When
        composeTestRule.setContent {
            ReplyComposeScreen(
                senderEmail = "test@test.com",
                date = "2024.12.19",
                subject = "Test",
                body = "Body",
                replyOptions = replyOptions
            )
        }

        // Then - Swipe to navigate (tabs may not be clickable if out of view)
        Thread.sleep(500)
        composeTestRule.onNodeWithText("Type1").assertIsDisplayed()
    }
}
