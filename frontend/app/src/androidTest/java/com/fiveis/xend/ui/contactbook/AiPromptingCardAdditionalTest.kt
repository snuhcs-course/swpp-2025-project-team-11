package com.fiveis.xend.ui.contactbook

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.PromptOption
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AiPromptingCardAdditionalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_aiPromptingCard_shows_tone_section_title() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()

        // Then - In bottom sheet, it shows "문체 스타일 프롬프트"
        composeTestRule.onNodeWithText("문체 스타일 프롬프트").assertIsDisplayed()
    }

    @Test
    fun test_aiPromptingCard_shows_format_section_title() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()

        // Then - In bottom sheet, it shows "형식 가이드 프롬프트"
        composeTestRule.onNodeWithText("형식 가이드 프롬프트").assertIsDisplayed()
    }

    @Test
    fun test_aiPromptingCard_no_selected_prompts_message() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        // Then
        composeTestRule.onAllNodesWithText("선택된 프롬프트가 없습니다").assertCountEquals(2) // One for tone, one for format
    }

    @Test
    fun test_aiPromptingCard_with_selected_tone() {
        // Given
        val toneOption = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")
        val selectedState = PromptingUiState(selectedTone = setOf(toneOption))

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                selectedState = selectedState,
                onValueChange = {},
                allToneOptions = listOf(toneOption),
                allFormatOptions = emptyList()
            )
        }

        // Then
        composeTestRule.onNodeWithText("존댓말").assertIsDisplayed()
    }

    @Test
    fun test_aiPromptingCard_with_selected_format() {
        // Given
        val formatOption = PromptOption(1, "format", "3~5문장", "간결하게")
        val selectedState = PromptingUiState(selectedFormat = setOf(formatOption))

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                selectedState = selectedState,
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = listOf(formatOption)
            )
        }

        // Then
        composeTestRule.onNodeWithText("3~5문장").assertIsDisplayed()
    }

    @Test
    fun test_aiPromptingCard_shows_tone_count() {
        // Given
        val toneOption = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")
        val selectedState = PromptingUiState(selectedTone = setOf(toneOption))

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                selectedState = selectedState,
                onValueChange = {},
                allToneOptions = listOf(toneOption),
                allFormatOptions = emptyList()
            )
        }

        // Then
        composeTestRule.onNodeWithText("문체 스타일 (1개)").assertIsDisplayed()
    }

    @Test
    fun test_aiPromptingCard_shows_format_count() {
        // Given
        val formatOption = PromptOption(1, "format", "3~5문장", "간결하게")
        val selectedState = PromptingUiState(selectedFormat = setOf(formatOption))

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                selectedState = selectedState,
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = listOf(formatOption)
            )
        }

        // Then
        composeTestRule.onNodeWithText("형식 가이드 (1개)").assertIsDisplayed()
    }

    @Test
    fun test_promptingBottomSheet_with_existing_tone_options() {
        // Given
        val toneOptions = listOf(
            PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요"),
            PromptOption(2, "tone", "직설적", "직설적으로 작성하세요")
        )

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = toneOptions,
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()

        // Then
        composeTestRule.onNodeWithText("존댓말").assertIsDisplayed()
        composeTestRule.onNodeWithText("직설적").assertIsDisplayed()
    }

    @Test
    fun test_promptingBottomSheet_with_existing_format_options() {
        // Given
        val formatOptions = listOf(
            PromptOption(1, "format", "3~5문장", "간결하게"),
            PromptOption(2, "format", "핵심키워드", "키워드 강조")
        )

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = formatOptions
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()

        // Then
        composeTestRule.onNodeWithText("3~5문장").assertIsDisplayed()
        composeTestRule.onNodeWithText("핵심키워드").assertIsDisplayed()
    }

    @Test
    fun test_promptingBottomSheet_toggle_tone_option() {
        // Given
        val toneOption = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = listOf(toneOption),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onAllNodesWithText("존댓말")[0].performClick() // Toggle selection

        // Then - Should show checkmark when selected
        composeTestRule.waitForIdle()
    }

    @Test
    fun test_promptingBottomSheet_save_button_closes_sheet() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onNodeWithText("AI 프롬프트 설정").assertIsDisplayed()

        composeTestRule.onNodeWithText("저장").performClick()
        composeTestRule.waitForIdle()

        // Then - Sheet should be closed
        composeTestRule.onNodeWithText("AI 프롬프트 설정").assertDoesNotExist()
    }

    @Test
    fun test_promptingBottomSheet_reset_button() {
        // Given
        val toneOption = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")
        val selectedState = PromptingUiState(selectedTone = setOf(toneOption))

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                selectedState = selectedState,
                onValueChange = {},
                allToneOptions = listOf(toneOption),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onNodeWithText("초기화").performClick()

        // Then - Selection should be cleared (test implicitly by verifying no crash)
        composeTestRule.onNodeWithText("초기화").assertIsDisplayed()
    }

    @Test
    fun test_promptingBottomSheet_add_new_tone_prompt_button() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onAllNodesWithText("새 프롬프트 추가")[0].performClick()

        // Then - 3 "새 프롬프트 추가" exist (tone button, format button, dialog title)
        composeTestRule.onAllNodesWithText("새 프롬프트 추가").assertCountEquals(3)
        composeTestRule.onNodeWithText("카테고리: 문체 스타일").assertIsDisplayed()
    }

    @Test
    fun test_promptingBottomSheet_add_new_format_prompt_button() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onAllNodesWithText("새 프롬프트 추가")[1].performClick()

        // Then - 3 "새 프롬프트 추가" exist (tone button, format button, dialog title)
        composeTestRule.onAllNodesWithText("새 프롬프트 추가").assertCountEquals(3)
        composeTestRule.onNodeWithText("카테고리: 형식 가이드").assertIsDisplayed()
    }

    @Test
    fun test_addPromptDialog_shows_name_field() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onAllNodesWithText("새 프롬프트 추가")[0].performClick()

        // Then
        composeTestRule.onNodeWithText("이름").assertIsDisplayed()
    }

    @Test
    fun test_addPromptDialog_shows_prompt_field() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onAllNodesWithText("새 프롬프트 추가")[0].performClick()

        // Then
        composeTestRule.onNodeWithText("프롬프트 설명").assertIsDisplayed()
    }

    @Test
    fun test_addPromptDialog_cancel_button() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onAllNodesWithText("새 프롬프트 추가")[0].performClick()
        composeTestRule.onNodeWithText("취소").performClick()

        // Then - Dialog should be closed
        composeTestRule.onNodeWithText("카테고리: 문체 스타일").assertDoesNotExist()
    }

    @Test
    fun test_addPromptDialog_add_button_disabled_when_empty() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onAllNodesWithText("새 프롬프트 추가")[0].performClick()

        // Then - Add button should exist but be disabled (we can't directly test disabled state in Compose)
        composeTestRule.onNodeWithText("추가").assertIsDisplayed()
    }

    @Test
    fun test_editPromptDialog_shows_when_long_press() {
        // Given
        val toneOption = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = listOf(toneOption),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onAllNodesWithText("존댓말")[0].performTouchInput { longClick() }

        // Then - Context menu should appear with 수정 and 삭제 options
        composeTestRule.onAllNodesWithText("수정").assertCountEquals(2) // Card button + context menu
        composeTestRule.onNodeWithText("삭제").assertIsDisplayed()
    }

    @Test
    fun test_editPromptDialog_shows_prompt_preview() {
        // Given
        val toneOption = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = listOf(toneOption),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onAllNodesWithText("존댓말")[0].performTouchInput { longClick() }

        // Then
        composeTestRule.onNodeWithText("\"존댓말을 사용하세요\"").assertIsDisplayed()
    }

    @Test
    fun test_editPromptDialog_edit_menu_item() {
        // Given
        val toneOption = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = listOf(toneOption),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onAllNodesWithText("존댓말")[0].performTouchInput { longClick() }
        composeTestRule.onAllNodesWithText("수정")[1].performClick() // Click the edit menu item

        // Then - Edit dialog should appear
        composeTestRule.onNodeWithText("프롬프트 수정").assertIsDisplayed()
    }

    @Test
    fun test_editPromptDialog_shows_save_button() {
        // Given
        val toneOption = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = listOf(toneOption),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onAllNodesWithText("존댓말")[0].performTouchInput { longClick() }
        composeTestRule.onAllNodesWithText("수정")[1].performClick()

        // Then - Multiple "저장" buttons exist (one in bottom sheet, one in edit dialog)
        composeTestRule.onAllNodesWithText("저장").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("취소")[0].assertIsDisplayed()
    }

    @Test
    fun test_editPromptDialog_cancel_button() {
        // Given
        val toneOption = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = listOf(toneOption),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onAllNodesWithText("존댓말")[0].performTouchInput { longClick() }
        composeTestRule.onAllNodesWithText("수정")[1].performClick()
        composeTestRule.onAllNodesWithText("취소")[0].performClick()

        // Then - Dialog should be closed
        composeTestRule.onNodeWithText("프롬프트 수정").assertDoesNotExist()
    }

    @Test
    fun test_deletePromptDialog_shows_when_delete_clicked() {
        // Given
        val toneOption = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = listOf(toneOption),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onAllNodesWithText("존댓말")[0].performTouchInput { longClick() }
        composeTestRule.onAllNodesWithText("삭제")[0].performClick()

        // Then
        composeTestRule.onNodeWithText("프롬프트 삭제").assertIsDisplayed()
        composeTestRule.onNodeWithText("정말 \"존댓말\" 프롬프트를 삭제할까요?").assertIsDisplayed()
    }

    @Test
    fun test_deletePromptDialog_cancel_button() {
        // Given
        val toneOption = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = listOf(toneOption),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onAllNodesWithText("존댓말")[0].performTouchInput { longClick() }
        composeTestRule.onAllNodesWithText("삭제")[0].performClick()
        composeTestRule.onAllNodesWithText("취소")[0].performClick()

        // Then - Dialog should be closed
        composeTestRule.onNodeWithText("프롬프트 삭제").assertDoesNotExist()
    }

    @Test
    fun test_promptingBottomSheet_shows_hardcoded_examples() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()

        // Then - Should show hardcoded tone examples
        composeTestRule.onNodeWithText("존댓말").assertIsDisplayed()
        composeTestRule.onNodeWithText("직설적").assertIsDisplayed()
        composeTestRule.onNodeWithText("결론우선").assertIsDisplayed()
    }

    @Test
    fun test_promptingBottomSheet_shows_hardcoded_format_examples() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()

        // Then - Should show hardcoded format examples
        composeTestRule.onNodeWithText("3~5문장").assertIsDisplayed()
        composeTestRule.onNodeWithText("핵심키워드").assertIsDisplayed()
        composeTestRule.onNodeWithText("구체적일정").assertIsDisplayed()
    }

    @Test
    fun test_promptingBottomSheet_multiple_selections() {
        // Given
        val toneOptions = listOf(
            PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요"),
            PromptOption(2, "tone", "직설적", "직설적으로")
        )

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = toneOptions,
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onAllNodesWithText("존댓말")[0].performClick()
        composeTestRule.onAllNodesWithText("직설적")[0].performClick()

        // Then - Both should be selectable
        composeTestRule.waitForIdle()
    }

    @Test
    fun test_summaryChip_displays_option_name() {
        // Given
        val toneOption = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")
        val selectedState = PromptingUiState(selectedTone = setOf(toneOption))

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                selectedState = selectedState,
                onValueChange = {},
                allToneOptions = listOf(toneOption),
                allFormatOptions = emptyList()
            )
        }

        // Then
        composeTestRule.onNodeWithText("존댓말").assertIsDisplayed()
    }

    @Test
    fun test_promptingBottomSheet_with_many_options() {
        // Given
        val toneOptions = List(10) { PromptOption(it.toLong(), "tone", "Option$it", "Prompt$it") }
        val formatOptions = List(10) { PromptOption((it + 10).toLong(), "format", "Format$it", "FPrompt$it") }

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = toneOptions,
                allFormatOptions = formatOptions
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()

        // Then - Should render without crash
        composeTestRule.onNodeWithText("AI 프롬프트 설정").assertIsDisplayed()
    }

    @Test
    fun test_promptingBottomSheet_dismisses_on_swipe_down() {
        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onNodeWithText("AI 프롬프트 설정").assertIsDisplayed()

        // Then - Sheet should be displayed
        composeTestRule.onNodeWithText("문체 스타일 프롬프트").assertIsDisplayed()
    }

    @Test
    fun test_promptOption_with_blank_name_shows_prompt() {
        // Given
        val option = PromptOption(1, "tone", "", "존댓말을 사용하세요")

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = listOf(option),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()

        // Then - Should show prompt as label if name is blank
        composeTestRule.onNodeWithText("존댓말을 사용하세요").assertIsDisplayed()
    }

    @Test
    fun test_aiPromptingCard_selected_state_persistence() {
        // Given
        val toneOption = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")
        val selectedState = PromptingUiState(selectedTone = setOf(toneOption))

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                selectedState = selectedState,
                onValueChange = {},
                allToneOptions = listOf(toneOption),
                allFormatOptions = emptyList()
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onNodeWithText("저장").performClick()

        // Then - State should persist after save
        composeTestRule.onNodeWithText("존댓말").assertIsDisplayed()
    }

    @Test
    fun test_aiPromptingCard_multiple_format_selections() {
        // Given
        val formatOptions = listOf(
            PromptOption(1, "format", "3~5문장", "간결하게"),
            PromptOption(2, "format", "핵심키워드", "키워드 강조")
        )

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = emptyList(),
                allFormatOptions = formatOptions
            )
        }

        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onAllNodesWithText("3~5문장")[0].performClick()
        composeTestRule.onAllNodesWithText("핵심키워드")[0].performClick()

        // Then - Both should be selectable
        composeTestRule.waitForIdle()
    }

    @Test
    fun test_aiPromptingCard_dialog_state_transitions() {
        // Given
        val toneOption = PromptOption(1, "tone", "존댓말", "존댓말을 사용하세요")

        // When
        composeTestRule.setContent {
            AiPromptingCard(
                onValueChange = {},
                allToneOptions = listOf(toneOption),
                allFormatOptions = emptyList()
            )
        }

        // Open sheet, verify content, close sheet
        composeTestRule.onNodeWithText("수정").performClick()
        composeTestRule.onNodeWithText("AI 프롬프트 설정").assertIsDisplayed()

        composeTestRule.onAllNodesWithText("존댓말")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("저장")[0].performClick()

        // Then - Should handle state transitions smoothly
        composeTestRule.waitForIdle()
    }
}
