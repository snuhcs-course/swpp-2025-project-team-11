package com.fiveis.xend.ui.compose

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemplateSelectionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        // Reset templates to a known state
        TemplateData.templates.clear()
        TemplateData.templates.addAll(
            listOf(
                EmailTemplate(
                    id = "1",
                    category = TemplateCategory.WORK,
                    title = "업무 협조 요청",
                    description = "동료나 타 부서에 업무 협조를 요청할 때",
                    subject = "업무 협조 요청 드립니다",
                    body = "업무 협조 본문"
                ),
                EmailTemplate(
                    id = "2",
                    category = TemplateCategory.SCHOOL,
                    title = "교수님께 질문",
                    description = "수업 내용이나 과제에 대해 질문할 때",
                    subject = "수업 질문",
                    body = "질문 본문"
                ),
                EmailTemplate(
                    id = "3",
                    category = TemplateCategory.PERSONAL,
                    title = "감사 인사",
                    description = "도움을 받았을 때 고마움을 표현",
                    subject = "감사합니다",
                    body = "감사 본문"
                )
            )
        )
    }

    @Test
    fun template_selection_screen_displays_title() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithText("템플릿 선택").assertIsDisplayed()
    }

    @Test
    fun template_selection_screen_displays_back_button() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("뒤로가기").assertIsDisplayed()
    }

    @Test
    fun back_button_triggers_callback() {
        var backClicked = false

        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = { backClicked = true },
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()
        assertTrue(backClicked)
    }

    @Test
    fun search_button_displays() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("검색").assertIsDisplayed()
    }

    @Test
    fun all_category_tabs_display() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithText("전체").assertIsDisplayed()
        composeTestRule.onNodeWithText("업무").assertIsDisplayed()
        composeTestRule.onNodeWithText("학업").assertIsDisplayed()
        composeTestRule.onNodeWithText("개인").assertIsDisplayed()
    }

    @Test
    fun category_tab_can_be_selected() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithText("업무").performClick()
        composeTestRule.waitForIdle()

        // Should only show work templates
        composeTestRule.onNodeWithText("업무 협조 요청").assertIsDisplayed()
    }

    @Test
    fun template_cards_display_all_templates_in_all_category() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithText("업무 협조 요청").assertIsDisplayed()
        composeTestRule.onNodeWithText("교수님께 질문").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("감사 인사").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun template_card_displays_description() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithText("동료나 타 부서에 업무 협조를 요청할 때").assertIsDisplayed()
    }

    @Test
    fun template_card_use_button_triggers_callback() {
        var selectedTemplate: EmailTemplate? = null

        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = { selectedTemplate = it }
            )
        }

        // Find and click the "사용" button
        composeTestRule.onAllNodesWithText("사용")[0].performClick()

        assertNotNull(selectedTemplate)
        assertEquals("1", selectedTemplate?.id)
    }

    @Test
    fun template_card_delete_button_removes_template() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        val initialCount = TemplateData.templates.size

        // Click delete button on first template
        composeTestRule.onAllNodesWithContentDescription("삭제")[0].performClick()

        composeTestRule.waitForIdle()

        assertEquals(initialCount - 1, TemplateData.templates.size)
    }

    @Test
    fun new_template_button_displays() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithText("새 템플릿 만들기").assertExists()
    }

    @Test
    fun new_template_button_opens_dialog() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithText("새 템플릿 만들기").performClick()

        composeTestRule.waitForIdle()

        // Verify dialog opened by checking for unique dialog elements
        composeTestRule.onNodeWithText("카테고리").assertIsDisplayed()
        composeTestRule.onNodeWithText("저장").assertIsDisplayed()
        composeTestRule.onNodeWithText("취소").assertIsDisplayed()
    }

    @Test
    fun new_template_dialog_displays_all_fields() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithText("새 템플릿 만들기").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("카테고리").assertIsDisplayed()
        composeTestRule.onNodeWithText("템플릿 제목").assertIsDisplayed()
        composeTestRule.onNodeWithText("템플릿 설명").assertIsDisplayed()
        composeTestRule.onNodeWithText("메일 제목").assertIsDisplayed()
        composeTestRule.onNodeWithText("메일 본문").assertIsDisplayed()
    }

    @Test
    fun new_template_dialog_can_be_cancelled() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithText("새 템플릿 만들기").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("취소").performClick()
        composeTestRule.waitForIdle()

        // Dialog should be closed
        composeTestRule.onNodeWithText("카테고리").assertDoesNotExist()
    }

    @Test
    fun new_template_dialog_save_creates_template() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithText("새 템플릿 만들기").performClick()
        composeTestRule.waitForIdle()

        // Fill in all fields
        composeTestRule.onNodeWithText("예: 업무 협조 요청").performTextInput("Test Template")
        composeTestRule.onNodeWithText("예: 동료나 타 부서에 업무 협조를 요청할 때").performTextInput("Test Description")
        composeTestRule.onNodeWithText("예: 업무 협조 요청 드립니다").performTextInput("Test Subject")
        composeTestRule.onNodeWithText("메일 본문 내용을 입력하세요").performTextInput("Test Body")

        val initialCount = TemplateData.templates.size

        composeTestRule.onNodeWithText("저장").performClick()
        composeTestRule.waitForIdle()

        assertEquals(initialCount + 1, TemplateData.templates.size)
        assertEquals("Test Template", TemplateData.templates[0].title)
    }

    @Test
    fun template_card_click_opens_detail_dialog() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        // Click on template card (not the use button)
        composeTestRule.onNodeWithText("업무 협조 요청").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("템플릿 상세").assertIsDisplayed()
    }

    @Test
    fun template_detail_dialog_shows_existing_values() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithText("업무 협조 요청").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("템플릿 상세").assertIsDisplayed()
        // Multiple nodes might contain this text (in dialog fields), so just check it exists
        composeTestRule.onAllNodesWithText("업무 협조 요청", substring = true).assertCountEquals(3)
    }

    @Test
    fun work_category_filters_templates() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithText("업무").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("업무 협조 요청").assertIsDisplayed()
        composeTestRule.onNodeWithText("교수님께 질문").assertDoesNotExist()
        composeTestRule.onNodeWithText("감사 인사").assertDoesNotExist()
    }

    @Test
    fun school_category_filters_templates() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithText("학업").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("교수님께 질문").assertIsDisplayed()
        composeTestRule.onNodeWithText("업무 협조 요청").assertDoesNotExist()
        composeTestRule.onNodeWithText("감사 인사").assertDoesNotExist()
    }

    @Test
    fun personal_category_filters_templates() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithText("개인").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("감사 인사").assertIsDisplayed()
        composeTestRule.onNodeWithText("업무 협조 요청").assertDoesNotExist()
        composeTestRule.onNodeWithText("교수님께 질문").assertDoesNotExist()
    }

    @Test
    fun template_list_header_displays() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithText("템플릿").assertIsDisplayed()
    }

    @Test
    fun all_use_buttons_are_displayed() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        // Should have 3 "사용" buttons for 3 templates
        composeTestRule.onAllNodesWithText("사용").assertCountEquals(3)
    }

    @Test
    fun new_template_dialog_category_selection_works() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithText("새 템플릿 만들기").performClick()
        composeTestRule.waitForIdle()

        // The dialog shows category chips excluding ALL
        composeTestRule.onAllNodesWithText("업무").assertCountEquals(2) // One in tab, one in dialog
    }

    @Test
    fun empty_template_list_shows_no_templates() {
        TemplateData.templates.clear()

        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithText("새 템플릿 만들기").assertExists()
        composeTestRule.onAllNodesWithText("사용").assertCountEquals(0)
    }

    @Test
    fun switching_categories_updates_template_list() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        // Start with ALL
        composeTestRule.onAllNodesWithText("사용").assertCountEquals(3)

        // Switch to WORK
        composeTestRule.onNodeWithText("업무").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("사용").assertCountEquals(1)

        // Switch back to ALL
        composeTestRule.onNodeWithText("전체").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("사용").assertCountEquals(3)
    }

    @Test
    fun template_card_displays_all_information() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithText("업무 협조 요청").assertIsDisplayed()
        composeTestRule.onNodeWithText("동료나 타 부서에 업무 협조를 요청할 때").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("사용").assertCountEquals(3)
        composeTestRule.onAllNodesWithContentDescription("삭제").assertCountEquals(3)
    }

    @Test
    fun multiple_templates_can_be_deleted() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        val initialCount = TemplateData.templates.size

        // Delete first template
        composeTestRule.onAllNodesWithContentDescription("삭제")[0].performClick()
        composeTestRule.waitForIdle()

        // Delete another template (now at index 0)
        composeTestRule.onAllNodesWithContentDescription("삭제")[0].performClick()
        composeTestRule.waitForIdle()

        assertEquals(initialCount - 2, TemplateData.templates.size)
    }

    @Test
    fun template_selection_returns_correct_template() {
        var selectedTemplate: EmailTemplate? = null

        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = { selectedTemplate = it }
            )
        }

        composeTestRule.onAllNodesWithText("사용")[1].performClick()

        assertNotNull(selectedTemplate)
        assertEquals("2", selectedTemplate?.id)
        assertEquals("교수님께 질문", selectedTemplate?.title)
    }

    @Test
    fun new_template_button_is_always_visible() {
        composeTestRule.setContent {
            TemplateSelectionScreen(
                onBack = {},
                onTemplateSelected = {}
            )
        }

        composeTestRule.onNodeWithText("새 템플릿 만들기").assertExists()

        // Switch category
        composeTestRule.onNodeWithText("업무").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("새 템플릿 만들기").assertExists()
    }
}
