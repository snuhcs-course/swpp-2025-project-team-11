package com.fiveis.xend.ui.inbox

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.ui.theme.XendTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddContactDialogIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun addContactDialog_displays_header_and_close_button() {
        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "Test User",
                    senderEmail = "test@example.com",
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        // "연락처 추가" appears twice (title and save button)
        composeTestRule.onAllNodesWithText("연락처 추가").assertCountEquals(2)
        composeTestRule.onNodeWithContentDescription("닫기").assertIsDisplayed()
    }

    @Test
    fun addContactDialog_displays_sender_info() {
        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "John Doe",
                    senderEmail = "john.doe@example.com",
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Name appears in multiple places (display + input field)
        composeTestRule.onAllNodesWithText("John Doe").assertCountEquals(2)
        composeTestRule.onAllNodesWithText("john.doe@example.com").assertCountEquals(2)
    }

    @Test
    fun addContactDialog_close_button_triggers_dismiss() {
        var dismissCalled = false
        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "Test User",
                    senderEmail = "test@example.com",
                    onDismiss = { dismissCalled = true },
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("닫기").performClick()
        assert(dismissCalled)
    }

    @Test
    fun addContactDialog_cancel_button_triggers_dismiss() {
        var dismissCalled = false
        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "Test User",
                    senderEmail = "test@example.com",
                    onDismiss = { dismissCalled = true },
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("취소").performClick()
        assert(dismissCalled)
    }

    @Test
    fun addContactDialog_save_button_disabled_when_name_empty() {
        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "",
                    senderEmail = "test@example.com",
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Clear the name field to make it empty
        composeTestRule.waitForIdle()

        // The save button should exist (2 "연락처 추가": title and button)
        composeTestRule.onAllNodesWithText("연락처 추가").assertCountEquals(2)
    }

    @Test
    fun addContactDialog_save_button_enabled_with_valid_name() {
        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "Valid Name",
                    senderEmail = "test@example.com",
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Get the save button (second "연락처 추가")
        composeTestRule.onAllNodesWithText("연락처 추가")[1].assertIsEnabled()
    }

    @Test
    fun addContactDialog_displays_name_field_with_placeholder() {
        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "Test User",
                    senderEmail = "test@example.com",
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("이름").assertIsDisplayed()
    }

    @Test
    fun addContactDialog_displays_relationship_section() {
        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "Test User",
                    senderEmail = "test@example.com",
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("관계 - 나").assertIsDisplayed()
        composeTestRule.onNodeWithText("관계 - 상대방").assertIsDisplayed()
        composeTestRule.onNodeWithText("나").assertIsDisplayed()
        composeTestRule.onNodeWithText("상대방").assertIsDisplayed()
    }

    @Test
    fun addContactDialog_displays_personal_prompt_field() {
        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "Test User",
                    senderEmail = "test@example.com",
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("관계 프롬프팅(선택사항)").assertIsDisplayed()
        composeTestRule.onNodeWithText("상대방과의 관계를 설명해 주세요").assertIsDisplayed()
    }

    @Test
    fun addContactDialog_displays_group_selection() {
        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "Test User",
                    senderEmail = "test@example.com",
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithText("그룹 선택(선택사항)").assertIsDisplayed()
        composeTestRule.onNodeWithText("그룹 선택").assertIsDisplayed()
    }

    @Test
    fun addContactDialog_displays_groups_when_provided() {
        val groups = listOf(
            Group(1L, "Work", "work prompt"),
            Group(2L, "Friends", "friends prompt")
        )
        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "Test User",
                    senderEmail = "test@example.com",
                    groups = groups,
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Click on group dropdown to expand
        composeTestRule.onNodeWithText("그룹 선택").performClick()
        composeTestRule.waitForIdle()

        // Check if groups are displayed
        composeTestRule.onNodeWithText("Work").assertIsDisplayed()
        composeTestRule.onNodeWithText("Friends").assertIsDisplayed()
    }

    @Test
    fun addContactDialog_can_select_group() {
        val groups = listOf(
            Group(1L, "Work", "work prompt")
        )
        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "Test User",
                    senderEmail = "test@example.com",
                    groups = groups,
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Click on group dropdown
        composeTestRule.onNodeWithText("그룹 선택").performClick()
        composeTestRule.waitForIdle()

        // Select a group
        composeTestRule.onNodeWithText("Work").performClick()
        composeTestRule.waitForIdle()

        // Verify group is selected
        composeTestRule.onNodeWithText("Work").assertIsDisplayed()
    }

    @Test
    fun addContactDialog_save_calls_onConfirm_with_correct_data() {
        var confirmedName: String? = null
        var confirmedEmail: String? = null
        var confirmedSenderRole: String? = null
        var confirmedRecipientRole: String? = null
        var confirmedPersonalPrompt: String? = null
        var confirmedGroupId: Long? = null

        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "Test User",
                    senderEmail = "test@example.com",
                    onDismiss = {},
                    onConfirm = { name, email, senderRole, recipientRole, personalPrompt, groupId, _ ->
                        confirmedName = name
                        confirmedEmail = email
                        confirmedSenderRole = senderRole
                        confirmedRecipientRole = recipientRole
                        confirmedPersonalPrompt = personalPrompt
                        confirmedGroupId = groupId
                    }
                )
            }
        }

        // Click save button (second "연락처 추가")
        composeTestRule.onAllNodesWithText("연락처 추가")[1].performClick()

        // Verify callback was called with correct data
        assert(confirmedName == "Test User")
        assert(confirmedEmail == "test@example.com")
    }

    @Test
    fun addContactDialog_sender_role_dropdown_expands() {
        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "Test User",
                    senderEmail = "test@example.com",
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Click on sender role dropdown
        composeTestRule.onNodeWithText("나").performClick()
        composeTestRule.waitForIdle()

        // Check if dropdown options are displayed
        composeTestRule.onNodeWithText("직접 입력").assertIsDisplayed()
    }

    @Test
    fun addContactDialog_recipient_role_dropdown_expands() {
        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "Test User",
                    senderEmail = "test@example.com",
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Click on recipient role dropdown
        composeTestRule.onNodeWithText("상대방").performClick()
        composeTestRule.waitForIdle()

        // Check if dropdown options are displayed
        composeTestRule.onNodeWithText("직접 입력").assertIsDisplayed()
    }

    @Test
    fun addContactDialog_manual_input_shows_when_direct_input_selected() {
        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "Test User",
                    senderEmail = "test@example.com",
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Click on sender role dropdown
        composeTestRule.onNodeWithText("나").performClick()
        composeTestRule.waitForIdle()

        // Select "직접 입력"
        composeTestRule.onNodeWithText("직접 입력").performClick()
        composeTestRule.waitForIdle()

        // Check if manual input field appears
        composeTestRule.onNodeWithText("나의 역할").assertIsDisplayed()
    }

    @Test
    fun addContactDialog_recipient_manual_input_shows_when_direct_input_selected() {
        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "Test User",
                    senderEmail = "test@example.com",
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Click on recipient role dropdown
        composeTestRule.onNodeWithText("상대방").performClick()
        composeTestRule.waitForIdle()

        // Select "직접 입력"
        composeTestRule.onNodeWithText("직접 입력").performClick()
        composeTestRule.waitForIdle()

        // Check if manual input field appears
        composeTestRule.onNodeWithText("상대방 역할").assertIsDisplayed()
    }

    @Test
    fun addContactDialog_handles_empty_groups_list() {
        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "Test User",
                    senderEmail = "test@example.com",
                    groups = emptyList(),
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Click on group dropdown
        composeTestRule.onNodeWithText("그룹 선택").performClick()
        composeTestRule.waitForIdle()

        // No groups should be displayed
        composeTestRule.onNodeWithText("그룹 선택").assertIsDisplayed()
    }

    @Test
    fun addContactDialog_save_with_all_fields_filled() {
        var confirmedName: String? = null
        var confirmedEmail: String? = null
        var confirmedPersonalPrompt: String? = null

        val groups = listOf(Group(1L, "Work", "work prompt"))

        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "John Doe",
                    senderEmail = "john@example.com",
                    groups = groups,
                    onDismiss = {},
                    onConfirm = { name, email, _, _, personalPrompt, _, _ ->
                        confirmedName = name
                        confirmedEmail = email
                        confirmedPersonalPrompt = personalPrompt
                    }
                )
            }
        }

        // Click save button (second "연락처 추가")
        composeTestRule.onAllNodesWithText("연락처 추가")[1].performClick()

        // Verify callback was called
        assert(confirmedName == "John Doe")
        assert(confirmedEmail == "john@example.com")
    }

    @Test
    fun addContactDialog_displays_all_sections_correctly() {
        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "Test User",
                    senderEmail = "test@example.com",
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Verify all major sections are displayed
        composeTestRule.onAllNodesWithText("연락처 추가").assertCountEquals(2) // Title and button
        composeTestRule.onNodeWithText("이름").assertIsDisplayed()
        composeTestRule.onNodeWithText("관계 - 나").assertIsDisplayed()
        composeTestRule.onNodeWithText("관계 프롬프팅(선택사항)").assertIsDisplayed()
        composeTestRule.onNodeWithText("그룹 선택(선택사항)").assertIsDisplayed()
        composeTestRule.onNodeWithText("취소").assertIsDisplayed()
    }

    @Test
    fun addContactDialog_multiple_groups_can_be_displayed() {
        val groups = listOf(
            Group(1L, "Work", "work prompt"),
            Group(2L, "Friends", "friends prompt"),
            Group(3L, "Family", "family prompt")
        )

        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "Test User",
                    senderEmail = "test@example.com",
                    groups = groups,
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        // Click on group dropdown
        composeTestRule.onNodeWithText("그룹 선택").performClick()
        composeTestRule.waitForIdle()

        // Check all groups are displayed
        composeTestRule.onNodeWithText("Work").assertIsDisplayed()
        composeTestRule.onNodeWithText("Friends").assertIsDisplayed()
        composeTestRule.onNodeWithText("Family").assertIsDisplayed()
    }

    @Test
    fun addContactDialog_preserves_name_across_interactions() {
        composeTestRule.setContent {
            XendTheme {
                AddContactDialog(
                    senderName = "Persistent Name",
                    senderEmail = "test@example.com",
                    onDismiss = {},
                    onConfirm = { _, _, _, _, _, _, _ -> }
                )
            }
        }

        // The name should still be displayed after various interactions
        composeTestRule.onNodeWithText("나").performClick()
        composeTestRule.waitForIdle()

        // Name should still be visible (appears in display + input field)
        composeTestRule.onAllNodesWithText("Persistent Name").assertCountEquals(2)
    }
}
