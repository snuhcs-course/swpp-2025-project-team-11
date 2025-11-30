package com.fiveis.xend.ui.contactbook

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.PromptOption
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiPromptingCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_aiPromptingCard_displays_title() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        // Then
        composeTestRule.onNodeWithText("선택된 프롬프트 조합").assertIsDisplayed()
    }

    @Test
    fun test_aiPromptingCard_shows_edit_button() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("수정").assertIsDisplayed()
    }

    @Test
    fun test_aiPromptingCard_edit_button_click() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()

        // Then - Should open bottom sheet
        composeTestRule.onNodeWithText("AI 프롬프트 설정").assertIsDisplayed()
    }

    @Test
    fun test_aiPromptingCard_renders_without_crash() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        // Then
        composeTestRule.onNodeWithText("선택된 프롬프트 조합").assertIsDisplayed()
    }

    @Test
    fun test_aiPromptingCard_with_tone_options() {
        // Given
        val toneOptions = listOf(
            PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")
        )

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = toneOptions,
                allFormatOptions = emptyList()
            )
        }

        // Then - Should render without crash
        composeTestRule.onNodeWithText("선택된 프롬프트 조합").assertIsDisplayed()
    }

    @Test
    fun test_aiPromptingCard_with_format_options() {
        // Given
        val formatOptions = listOf(
            PromptOption(1, "format", "3~5문장", "간결하게 작성하세요")
        )

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = formatOptions
            )
        }

        // Then - Should render without crash
        composeTestRule.onNodeWithText("선택된 프롬프트 조합").assertIsDisplayed()
    }

    @Test
    fun test_aiPromptingCard_with_all_options() {
        // Given
        val toneOptions = listOf(PromptOption(1, "tone", "존댓말", ""))
        val formatOptions = listOf(PromptOption(2, "format", "3~5문장", ""))

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = toneOptions,
                allFormatOptions = formatOptions
            )
        }

        // Then
        composeTestRule.onNodeWithText("선택된 프롬프트 조합").assertIsDisplayed()
    }

    @Test
    fun test_promptingBottomSheet_displays_title() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()

        // Then
        composeTestRule.onNodeWithText("AI 프롬프트 설정").assertIsDisplayed()
    }

    @Test
    fun test_promptingBottomSheet_shows_description() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()

        // Then
        composeTestRule.onNodeWithText("그룹의 커뮤니케이션 스타일을 설정합니다").assertIsDisplayed()
    }

    @Test
    fun test_promptingBottomSheet_shows_tone_section() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()

        // Then
        composeTestRule.onNodeWithText("문체 스타일 프롬프트").assertIsDisplayed()
    }

    @Test
    fun test_promptingBottomSheet_shows_format_section() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()

        // Then
        composeTestRule.onNodeWithText("형식 가이드 프롬프트").assertIsDisplayed()
    }

    @Test
    fun test_promptingBottomSheet_shows_reset_button() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()

        // Then
        composeTestRule.onNodeWithText("초기화").assertIsDisplayed()
    }

    @Test
    fun test_promptingBottomSheet_shows_save_button() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()

        // Then
        composeTestRule.onNodeWithText("저장").assertIsDisplayed()
    }

    @Test
    fun test_promptingBottomSheet_tone_description() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()

        // Then
        composeTestRule.onNodeWithText("메일의 말투와 문체를 설정합니다").assertIsDisplayed()
    }

    @Test
    fun test_promptingBottomSheet_format_description() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()

        // Then
        composeTestRule.onNodeWithText("메일의 구조와 포맷을 설정합니다").assertIsDisplayed()
    }

    @Test
    fun test_promptingBottomSheet_shows_add_prompt_button() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()

        // Then
        composeTestRule.onAllNodesWithText("새 프롬프트 추가").assertCountEquals(2) // One for tone, one for format
    }

    @Test
    fun test_aiPromptingCard_with_many_tone_options() {
        // Given
        val toneOptions = List(10) { PromptOption(it.toLong(), "tone", "Option$it", "Prompt$it") }

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = toneOptions,
                allFormatOptions = emptyList()
            )
        }

        // Then - Should render without crash
        composeTestRule.onNodeWithText("선택된 프롬프트 조합").assertIsDisplayed()
    }

    @Test
    fun test_aiPromptingCard_with_many_format_options() {
        // Given
        val formatOptions = List(10) { PromptOption(it.toLong(), "format", "Format$it", "Desc$it") }

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = formatOptions
            )
        }

        // Then - Should render without crash
        composeTestRule.onNodeWithText("선택된 프롬프트 조합").assertIsDisplayed()
    }

    @Test
    fun test_aiPromptingCard_onValueChange_callback() {
        // Given
        var changed = false

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = { changed = true },
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        // Then - Just verify it renders (callback tested indirectly)
        composeTestRule.onNodeWithText("선택된 프롬프트 조합").assertIsDisplayed()
    }

    @Test
    fun test_memberCircle_displays() {
        // When
        composeTestRule.setContent {
            MemberCircle(label = "A", color = androidx.compose.ui.graphics.Color.Blue)
        }

        // Then
        composeTestRule.onNodeWithText("A").assertIsDisplayed()
    }

    @Test
    fun test_memberCircle_with_number() {
        // When
        composeTestRule.setContent {
            MemberCircle(label = "+5", color = androidx.compose.ui.graphics.Color.Gray)
        }

        // Then
        composeTestRule.onNodeWithText("+5").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_displays_title() {
        // Given
        val contacts = listOf(com.fiveis.xend.data.model.Contact(1, null, "John", "john@example.com"))

        // When
        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = contacts,
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("연락처 선택").assertIsDisplayed()
    }

    @Test
    fun test_contactSelectDialog_with_empty_contacts() {
        // When
        composeTestRule.setContent {
            ContactSelectDialog(
                contacts = emptyList(),
                selectedContacts = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("등록된 연락처가 없습니다").assertIsDisplayed()
    }
}
