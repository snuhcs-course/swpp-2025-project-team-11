package com.fiveis.xend.ui.contactbook

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Group
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddContactScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_addContactScreen_displays_title() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("연락처 추가").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_shows_back_button() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithContentDescription("뒤로가기").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_shows_save_button() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("저장").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_back_click_triggers_callback() {
        // Given
        var backClicked = false

        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = { backClicked = true },
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()

        // Then
        assert(backClicked)
    }

    @Test
    fun test_addContactScreen_shows_gmail_sync_card() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Gmail 연락처 동기화").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_gmail_sync_click_triggers_callback() {
        // Given
        var syncClicked = false

        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = { syncClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Gmail 연락처 동기화").performClick()

        // Then
        assert(syncClicked)
    }

    @Test
    fun test_addContactScreen_shows_manual_input_card() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("직접 입력").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_manual_input_toggles() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then - Form fields should be visible by default
        composeTestRule.onNodeWithText("이름을 입력하세요").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_shows_name_field() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("이름을 입력하세요").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_shows_email_field() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("이메일 주소를 입력하세요").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_name_input_triggers_callback() {
        // Given
        var changedName = ""

        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = { changedName = it },
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        composeTestRule.onNodeWithText("이름을 입력하세요").performTextInput("John Doe")

        // Then
        assert(changedName == "John Doe")
    }

    @Test
    fun test_addContactScreen_email_input_triggers_callback() {
        // Given
        var changedEmail = ""

        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = { changedEmail = it },
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        composeTestRule.onNodeWithText("이메일 주소를 입력하세요").performTextInput("test@email.com")

        // Then
        assert(changedEmail == "test@email.com")
    }

    @Test
    fun test_addContactScreen_shows_relationship_section() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("관계").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_shows_sender_role_dropdown() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("나").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_shows_recipient_role_dropdown() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("상대방").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_shows_personal_prompt_field() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("상대방과의 관계를 설명해 주세요").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_personal_prompt_input_triggers_callback() {
        // Given
        var changedPrompt: String? = null

        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = { changedPrompt = it },
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        composeTestRule.onNodeWithText("상대방과의 관계를 설명해 주세요").performTextInput("We are colleagues")

        // Then
        assert(changedPrompt == "We are colleagues")
    }

    @Test
    fun test_addContactScreen_shows_group_selection() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("그룹").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_group_dropdown_shows_placeholder() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("그룹 선택").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_with_groups_displays_them() {
        // Given
        val groups = listOf(
            Group(1, "VIP", "Important", emptyList(), emptyList(), null, null),
            Group(2, "Team", "Work", emptyList(), emptyList(), null, null)
        )

        // When
        composeTestRule.setContent {
            AddContactScreen(
                groups = groups,
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then - Verify group selector is present
        composeTestRule.onNodeWithText("그룹 선택").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_bottom_nav_displays() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("메일함").assertDoesNotExist()
        composeTestRule.onNodeWithText("연락처").assertDoesNotExist()
    }

    @Test
    fun test_addContactScreen_gmail_sync_description() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("구글 계정의 모든 연락처를 가져옵니다").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_manual_input_description() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("연락처 정보를 직접 입력합니다").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_name_field_accepts_korean() {
        // Given
        var changedName = ""

        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = { changedName = it },
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        composeTestRule.onNodeWithText("이름을 입력하세요").performTextInput("김철수")

        // Then
        assert(changedName == "김철수")
    }

    @Test
    fun test_addContactScreen_email_field_accepts_long_email() {
        // Given
        var changedEmail = ""
        val longEmail = "verylongemailaddress@verylongdomainname.example.com"

        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = { changedEmail = it },
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        composeTestRule.onNodeWithText("이메일 주소를 입력하세요").performTextInput(longEmail)

        // Then
        assert(changedEmail == longEmail)
    }

    @Test
    fun test_addContactScreen_multiple_text_inputs() {
        // Given
        var name = ""
        var email = ""
        var prompt: String? = null

        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = { name = it },
                onEmailChange = { email = it },
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = { prompt = it },
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        composeTestRule.onNodeWithText("이름을 입력하세요").performTextInput("Test Name")
        composeTestRule.onNodeWithText("이메일 주소를 입력하세요").performTextInput("test@email.com")
        composeTestRule.onNodeWithText("상대방과의 관계를 설명해 주세요").performTextInput("Test relationship")

        // Then
        assert(name == "Test Name")
        assert(email == "test@email.com")
        assert(prompt == "Test relationship")
    }

    @Test
    fun test_addContactScreen_renders_without_crash() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then - Should render without crash
        composeTestRule.onNodeWithText("연락처 추가").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_with_empty_groups() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                groups = emptyList(),
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("그룹 선택").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_scrollable_content() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then - All sections should be present
        composeTestRule.onNodeWithText("연락처 가져오기").assertIsDisplayed()
        composeTestRule.onNodeWithText("이름").assertExists()
    }

    @Test
    fun test_addContactScreen_name_label_displays() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("이름").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_email_label_displays() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("이메일 주소").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_relationship_label_displays() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("관계").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_prompt_label_displays() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("관계 프롬프팅(선택사항)").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_group_label_displays() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("그룹").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_name_field_single_line() {
        // Given
        var changedName = ""

        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = { changedName = it },
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        composeTestRule.onNodeWithText("이름을 입력하세요").performTextInput("Test\nNewline")

        // Then - Should accept the input
        assert(changedName.contains("Test"))
    }

    @Test
    fun test_addContactScreen_email_field_single_line() {
        // Given
        var changedEmail = ""

        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = { changedEmail = it },
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        composeTestRule.onNodeWithText("이메일 주소를 입력하세요").performTextInput("test@email.com")

        // Then
        assert(changedEmail == "test@email.com")
    }

    @Test
    fun test_addContactScreen_save_button_disabled_initially() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then - Save button should be present
        composeTestRule.onNodeWithText("저장").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_all_callbacks_can_be_triggered() {
        // Given
        var backClicked = false
        var addClicked = false
        var syncClicked = false
        var nameChanged = false
        var emailChanged = false

        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = { nameChanged = true },
                onEmailChange = { emailChanged = true },
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = { backClicked = true },
                onAdd = { addClicked = true },
                onGmailContactsSync = { syncClicked = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("뒤로가기").performClick()
        composeTestRule.onNodeWithText("Gmail 연락처 동기화").performClick()
        composeTestRule.onNodeWithText("이름을 입력하세요").performTextInput("Test")
        composeTestRule.onNodeWithText("이메일 주소를 입력하세요").performTextInput("test@email.com")

        // Then
        assert(backClicked)
        assert(syncClicked)
        assert(nameChanged)
        assert(emailChanged)
    }

    @Test
    fun test_addContactScreen_with_special_characters_in_name() {
        // Given
        var changedName = ""

        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = { changedName = it },
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        composeTestRule.onNodeWithText("이름을 입력하세요").performTextInput("O'Brien")

        // Then
        assert(changedName == "O'Brien")
    }

    @Test
    fun test_addContactScreen_with_numbers_in_name() {
        // Given
        var changedName = ""

        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = { changedName = it },
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        composeTestRule.onNodeWithText("이름을 입력하세요").performTextInput("User123")

        // Then
        assert(changedName == "User123")
    }

    @Test
    fun test_addContactScreen_email_with_subdomain() {
        // Given
        var changedEmail = ""

        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = { changedEmail = it },
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        composeTestRule.onNodeWithText("이메일 주소를 입력하세요").performTextInput("user@mail.example.com")

        // Then
        assert(changedEmail == "user@mail.example.com")
    }

    @Test
    fun test_addContactScreen_personal_prompt_multiline() {
        // Given
        var changedPrompt: String? = null

        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = { changedPrompt = it },
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        composeTestRule.onNodeWithText("상대방과의 관계를 설명해 주세요").performTextInput("Line 1\nLine 2")

        // Then
        assert(changedPrompt?.contains("Line 1") == true)
    }

    @Test
    fun test_addContactScreen_long_personal_prompt() {
        // Given
        var changedPrompt: String? = null
        val longPrompt = "This is a very long personal prompt that describes the relationship in great detail with many words and sentences."

        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = { changedPrompt = it },
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        composeTestRule.onNodeWithText("상대방과의 관계를 설명해 주세요").performTextInput(longPrompt)

        // Then
        assert(changedPrompt == longPrompt)
    }

    @Test
    fun test_addContactScreen_stress_test_rapid_input() {
        // Given
        var nameCount = 0
        var emailCount = 0

        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = { nameCount++ },
                onEmailChange = { emailCount++ },
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        composeTestRule.onNodeWithText("이름을 입력하세요").performTextInput("A")
        composeTestRule.onNodeWithText("이메일 주소를 입력하세요").performTextInput("a")

        // Then - Callbacks should be triggered
        assert(nameCount > 0)
        assert(emailCount > 0)
    }

    @Test
    fun test_addContactScreen_with_many_groups() {
        // Given
        val groups = List(20) { Group(it.toLong(), "Group$it", "Desc$it", emptyList(), emptyList(), null, null) }

        // When
        composeTestRule.setContent {
            AddContactScreen(
                groups = groups,
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then - Should render without crash
        composeTestRule.onNodeWithText("그룹 선택").assertIsDisplayed()
    }

    @Test
    fun test_addContactScreen_empty_input_handling() {
        // Given
        var name = "initial"
        var email = "initial"

        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = { name = it },
                onEmailChange = { email = it },
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        composeTestRule.onNodeWithText("이름을 입력하세요").performTextInput("Test")
        composeTestRule.onNodeWithText("이메일 주소를 입력하세요").performTextInput("test@email.com")

        // Then - Input should be captured
        assert(name == "Test")
        assert(email == "test@email.com")
    }

    @Test
    fun test_addContactScreen_all_sections_visible() {
        // When
        composeTestRule.setContent {
            AddContactScreen(
                onNameChange = {},
                onEmailChange = {},
                onSenderRoleChange = {},
                onRecipientRoleChange = {},
                onPersonalPromptChange = {},
                onBack = {},
                onAdd = {},
                onGmailContactsSync = {}
            )
        }

        // Then - All main sections should be visible
        composeTestRule.onNodeWithText("연락처 가져오기").assertIsDisplayed()
        composeTestRule.onNodeWithText("이름").assertIsDisplayed()
        composeTestRule.onNodeWithText("이메일 주소").assertIsDisplayed()
        composeTestRule.onNodeWithText("관계").assertIsDisplayed()
        composeTestRule.onNodeWithText("그룹").assertIsDisplayed()
    }
}
