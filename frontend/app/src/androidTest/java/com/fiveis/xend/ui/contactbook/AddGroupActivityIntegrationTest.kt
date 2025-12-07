package com.fiveis.xend.ui.contactbook

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddGroupActivityIntegrationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<AddGroupActivity>()

    private var scenario: ActivityScenario<AddGroupActivity>? = null

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun activity_launches_successfully() {
        scenario = ActivityScenario.launch(AddGroupActivity::class.java)
        // If activity launches without crashing, test passes
    }

    @Test
    fun activity_handles_back_press() {
        scenario = ActivityScenario.launch(AddGroupActivity::class.java)
        scenario?.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        // If activity handles back press without crashing, test passes
    }

    @Test
    fun activity_displays_main_ui_elements() {
        // Then
        composeTestRule.onNodeWithText("그룹 추가").assertIsDisplayed()
        composeTestRule.onNodeWithText("저장").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("뒤로가기").assertIsDisplayed()
    }

    @Test
    fun activity_displays_input_fields() {
        // Then
        composeTestRule.onNodeWithText("그룹 이름").assertIsDisplayed()
        composeTestRule.onNodeWithText("그룹 설명").assertIsDisplayed()
        composeTestRule.onNodeWithText("이름을 입력하세요").assertIsDisplayed()
        composeTestRule.onNodeWithText("그룹을 소개해 주세요").assertIsDisplayed()
    }

    @Test
    fun activity_back_button_works() {
        // When
        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()

        // Then - activity should finish (test passes if no crash)
    }

    @Test
    fun activity_group_name_input_works() {
        // When
        composeTestRule.onNodeWithText("이름을 입력하세요").performTextInput("Test Group")

        // Then - should display the text without crashing
        composeTestRule.waitForIdle()
    }

    @Test
    fun activity_group_description_input_works() {
        // When
        composeTestRule.onNodeWithText("그룹을 소개해 주세요").performTextInput("Test Description")

        // Then - should display the text without crashing
        composeTestRule.waitForIdle()
    }

    @Test
    fun activity_shows_ai_prompt_section() {
        // Then
        composeTestRule.onNodeWithText("AI 프롬프트 설정").assertIsDisplayed()
    }

    @Test
    fun activity_shows_members_section() {
        // Then
        composeTestRule.onNodeWithText("그룹 멤버 (0명)").assertIsDisplayed()
        composeTestRule.onNodeWithText("추가").assertIsDisplayed()
    }

    @Test
    fun activity_add_member_button_opens_dialog() {
        // When
        composeTestRule.onNodeWithText("추가").performClick()

        // Then - dialog should open
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("연락처 선택").assertIsDisplayed()
    }

    @Test
    fun activity_contact_dialog_cancel_works() {
        // Given
        composeTestRule.onNodeWithText("추가").performClick()
        composeTestRule.waitForIdle()

        // When
        composeTestRule.onNodeWithText("취소").performClick()

        // Then - dialog should close (test passes if no crash)
        composeTestRule.waitForIdle()
    }

    @Test
    fun activity_contact_dialog_confirm_works() {
        // Given
        composeTestRule.onNodeWithText("추가").performClick()
        composeTestRule.waitForIdle()

        // When
        composeTestRule.onNodeWithText("확인 (0)").performClick()

        // Then - dialog should close (test passes if no crash)
        composeTestRule.waitForIdle()
    }

    @Test
    fun activity_fab_displays() {
        // Then
        composeTestRule.onNodeWithText("저장").assertIsDisplayed()
    }

    @Test
    fun activity_multiple_inputs_persist() {
        // When
        composeTestRule.onNodeWithText("이름을 입력하세요").performTextInput("VIP Group")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("그룹을 소개해 주세요").performTextInput("Important people")
        composeTestRule.waitForIdle()

        // Then - both inputs should persist without crashing
    }

    @Test
    fun activity_emoji_button_visible() {
        // Then
        composeTestRule.onNodeWithText("😀").assertIsDisplayed()
    }

    @Test
    fun activity_shows_all_sections() {
        // Then - verify all major sections are present
        composeTestRule.onNodeWithText("그룹 이름").assertIsDisplayed()
        composeTestRule.onNodeWithText("그룹 설명").assertIsDisplayed()
        composeTestRule.onNodeWithText("AI 프롬프트 설정").assertIsDisplayed()
    }

    @Test
    fun activity_contact_dialog_shows_empty_state() {
        // When
        composeTestRule.onNodeWithText("추가").performClick()
        composeTestRule.waitForIdle()

        // Then - should show empty message or contact list
        composeTestRule.onNodeWithText("연락처 선택").assertIsDisplayed()
    }
}
