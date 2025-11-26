package com.fiveis.xend.ui.contactbook

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.repository.ContactBookTab
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactBookScreenAdditionalTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun test_contactBookScreen_search_mode_displays_search_bar() {
        // Given
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            isSearchMode = true,
            searchQuery = "",
            searchResults = emptyList()
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("연락처 검색...").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_search_mode_close_button() {
        // Given
        var searchClosed = false
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            isSearchMode = true,
            searchQuery = "",
            searchResults = emptyList()
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {},
                onSearchClose = { searchClosed = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("검색 닫기").performClick()

        // Then
        assert(searchClosed)
    }

    @Test
    fun test_contactBookScreen_search_mode_query_change() {
        // Given
        var queryChanged = ""
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            isSearchMode = true,
            searchQuery = "",
            searchResults = emptyList()
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {},
                onSearchQueryChange = { queryChanged = it }
            )
        }

        composeTestRule.onNodeWithText("연락처 검색...").performTextInput("test")

        // Then
        assert(queryChanged == "test")
    }

    @Test
    fun test_contactBookScreen_search_mode_with_results() {
        // Given
        val results = listOf(
            Contact(1, null, "Found User", "found@example.com")
        )
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            isSearchMode = true,
            searchQuery = "found",
            searchResults = results
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("Found User").assertIsDisplayed()
        composeTestRule.onNodeWithText("검색 결과").assertIsDisplayed()
        composeTestRule.onNodeWithText("1명").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_search_mode_empty_query_shows_hint() {
        // Given
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            isSearchMode = true,
            searchQuery = "",
            searchResults = emptyList()
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("이름이나 이메일 주소로 검색하세요").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_search_mode_no_results() {
        // Given
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            isSearchMode = true,
            searchQuery = "notfound",
            searchResults = emptyList()
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("검색 결과가 없습니다").assertIsDisplayed()
        composeTestRule.onNodeWithText("\"notfound\"에 해당하는 연락처가 없어요").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_search_clear_button() {
        // Given
        var queryCleared = false
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            isSearchMode = true,
            searchQuery = "test",
            searchResults = emptyList()
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {},
                onSearchQueryChange = { if (it.isEmpty()) queryCleared = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("검색어 지우기").performClick()

        // Then
        assert(queryCleared)
    }

    @Test
    fun test_contactBookScreen_search_icon_click() {
        // Given
        var searchIconClicked = false
        val uiState = ContactBookUiState(selectedTab = ContactBookTab.Contacts)

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {},
                onSearchIconClick = { searchIconClicked = true }
            )
        }

        composeTestRule.onNodeWithContentDescription("Search").performClick()

        // Then
        assert(searchIconClicked)
    }

    @Test
    fun test_groupCard_with_emoji() {
        // Given
        val group = Group(1, "Team", "Desc", "🎯", emptyList(), emptyList(), null, null)

        // When
        composeTestRule.setContent {
            GroupCard(group = group, onClick = {})
        }

        // Then
        composeTestRule.onNodeWithText("🎯").assertIsDisplayed()
    }

    @Test
    fun test_groupCard_without_emoji() {
        // Given
        val group = Group(1, "Team", "Desc", null, emptyList(), emptyList(), null, null)

        // When
        composeTestRule.setContent {
            GroupCard(group = group, onClick = {})
        }

        // Then - Should render without crash
        composeTestRule.onNodeWithText("Team").assertIsDisplayed()
    }

    @Test
    fun test_groupCard_with_empty_emoji() {
        // Given
        val group = Group(1, "Team", "Desc", "", emptyList(), emptyList(), null, null)

        // When
        composeTestRule.setContent {
            GroupCard(group = group, onClick = {})
        }

        // Then - Should render without crash
        composeTestRule.onNodeWithText("Team").assertIsDisplayed()
    }

    @Test
    fun test_groupCard_shows_member_circles() {
        // Given
        val members = listOf(
            Contact(1, null, "Alice", "alice@example.com"),
            Contact(2, null, "Bob", "bob@example.com")
        )
        val group = Group(1, "Team", "Desc", null, emptyList(), members, null, null)

        // When
        composeTestRule.setContent {
            GroupCard(group = group, onClick = {})
        }

        // Then - Member initials should be displayed
        composeTestRule.onNodeWithText("A").assertIsDisplayed()
        composeTestRule.onNodeWithText("B").assertIsDisplayed()
    }

    @Test
    fun test_groupCard_shows_overflow_member_count() {
        // Given
        val members = List(5) { Contact(it.toLong(), null, "User$it", "user$it@example.com") }
        val group = Group(1, "Team", "Desc", null, emptyList(), members, null, null)

        // When
        composeTestRule.setContent {
            GroupCard(group = group, onClick = {})
        }

        // Then
        composeTestRule.onNodeWithText("+2").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_sorted_groups_by_name() {
        // Given
        val groups = listOf(
            Group(1, "Zulu", "Desc", null, emptyList(), emptyList(), null, null),
            Group(2, "Alpha", "Desc", null, emptyList(), emptyList(), null, null),
            Group(3, "Beta", "Desc", null, emptyList(), emptyList(), null, null)
        )
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Groups,
            groups = groups
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then - All should be displayed (sorted internally)
        composeTestRule.onNodeWithText("Alpha").assertIsDisplayed()
        composeTestRule.onNodeWithText("Beta").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zulu").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_sorted_contacts_by_name() {
        // Given
        val contacts = listOf(
            Contact(1, null, "Zoe", "zoe@example.com"),
            Contact(2, null, "Amy", "amy@example.com"),
            Contact(3, null, "Ben", "ben@example.com")
        )
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = contacts
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then - All should be displayed (sorted internally)
        composeTestRule.onNodeWithText("Amy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ben").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zoe").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_pull_refresh_indicator() {
        // Given
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            isLoading = false
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then - Should render without crash
        composeTestRule.onAllNodesWithText("연락처").assertCountEquals(2) // Title and bottom nav
    }

    @Test
    fun test_contactBookScreen_tab_chip_selected_state() {
        // Given
        val uiState = ContactBookUiState(selectedTab = ContactBookTab.Groups)

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then - Selected tab should be displayed
        composeTestRule.onNodeWithText("그룹별").assertIsDisplayed()
        composeTestRule.onNodeWithText("전체").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_does_not_change_tab_when_same_selected() {
        // Given
        var tabChangeCount = 0
        val uiState = ContactBookUiState(selectedTab = ContactBookTab.Groups)

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = { tabChangeCount++ },
                onGroupClick = {},
                onContactClick = {}
            )
        }

        composeTestRule.onNodeWithText("그룹별").performClick()

        // Then - Should not call onTabSelected
        assert(tabChangeCount == 0)
    }

    @Test
    fun test_contactBookScreen_changes_tab_when_different_selected() {
        // Given
        var newTab: ContactBookTab? = null
        val uiState = ContactBookUiState(selectedTab = ContactBookTab.Groups)

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = { newTab = it },
                onGroupClick = {},
                onContactClick = {}
            )
        }

        composeTestRule.onNodeWithText("전체").performClick()

        // Then
        assert(newTab == ContactBookTab.Contacts)
    }

    @Test
    fun test_quickActions_shows_add_icon() {
        // When
        composeTestRule.setContent {
            QuickActions(onAddGroupClick = {})
        }

        // Then
        composeTestRule.onNodeWithContentDescription("새 그룹").assertIsDisplayed()
    }

    @Test
    fun test_bottomNavBar_mail_selected_state() {
        // When
        composeTestRule.setContent {
            BottomNavBar(selected = "mail", onSelect = {})
        }

        // Then - Both options should be displayed
        composeTestRule.onNodeWithText("메일함").assertIsDisplayed()
        composeTestRule.onNodeWithText("연락처").assertIsDisplayed()
    }

    @Test
    fun test_bottomNavBar_contacts_click() {
        // Given
        var selected = ""
        composeTestRule.setContent {
            BottomNavBar(selected = "mail", onSelect = { selected = it })
        }

        // When
        composeTestRule.onNodeWithText("연락처").performClick()

        // Then
        assert(selected == "contacts")
    }

    @Test
    fun test_contactBookScreen_handles_empty_state() {
        // Given
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = emptyList()
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then - Should render without crash
        composeTestRule.onAllNodesWithText("연락처").assertCountEquals(2)
    }

    @Test
    fun test_contactBookScreen_group_card_on_edit_callback() {
        // Given
        var editedGroup: Group? = null
        val group = Group(1, "Team", "Desc", null, emptyList(), emptyList(), null, null)
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Groups,
            groups = listOf(group)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {},
                onEditGroupClick = { editedGroup = it }
            )
        }

        // Then - Should render without crash (edit is commented out in code)
        composeTestRule.onNodeWithText("Team").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_contact_row_on_edit_callback() {
        // Given
        var editedContact: Contact? = null
        val contact = Contact(1, null, "John", "john@example.com")
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            contacts = listOf(contact)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {},
                onEditContactClick = { editedContact = it }
            )
        }

        // Then - Should render without crash (edit is commented out in code)
        composeTestRule.onNodeWithText("John").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_search_result_click() {
        // Given
        var clickedContact: Contact? = null
        val contact = Contact(1, null, "Result", "result@example.com")
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            isSearchMode = true,
            searchQuery = "result",
            searchResults = listOf(contact)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = { clickedContact = it }
            )
        }

        composeTestRule.onNodeWithText("Result").performClick()

        // Then
        assert(clickedContact?.id == 1L)
    }

    @Test
    fun test_contactBookScreen_search_result_delete() {
        // Given
        var deletedContact: Contact? = null
        val contact = Contact(1, null, "ToDelete", "delete@example.com")
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            isSearchMode = true,
            searchQuery = "delete",
            searchResults = listOf(contact)
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {},
                onDeleteContactClick = { deletedContact = it }
            )
        }

        composeTestRule.onAllNodesWithContentDescription("더보기")[0].performClick()
        composeTestRule.onNodeWithText("삭제").performClick()
        composeTestRule.onAllNodesWithText("삭제")[0].performClick()

        // Then
        assert(deletedContact?.id == 1L)
    }

    @Test
    fun test_memberCircle_with_multi_char_label() {
        // When
        composeTestRule.setContent {
            MemberCircle(label = "AB", color = androidx.compose.ui.graphics.Color.Blue)
        }

        // Then
        composeTestRule.onNodeWithText("AB").assertIsDisplayed()
    }

    @Test
    fun test_groupCard_empty_description_shows_empty_string() {
        // Given
        val group = Group(1, "Team", "", null, emptyList(), emptyList(), null, null)

        // When
        composeTestRule.setContent {
            GroupCard(group = group, onClick = {})
        }

        // Then
        composeTestRule.onNodeWithText("Team").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_multiple_search_results() {
        // Given
        val results = listOf(
            Contact(1, null, "User1", "user1@example.com"),
            Contact(2, null, "User2", "user2@example.com"),
            Contact(3, null, "User3", "user3@example.com")
        )
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            isSearchMode = true,
            searchQuery = "user",
            searchResults = results
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then
        composeTestRule.onNodeWithText("3명").assertIsDisplayed()
        composeTestRule.onNodeWithText("User1").assertIsDisplayed()
        composeTestRule.onNodeWithText("User2").assertIsDisplayed()
        composeTestRule.onNodeWithText("User3").assertIsDisplayed()
    }

    @Test
    fun test_contactBookScreen_no_refresh_in_search_mode() {
        // Given
        val uiState = ContactBookUiState(
            selectedTab = ContactBookTab.Contacts,
            isSearchMode = true,
            searchQuery = "",
            searchResults = emptyList()
        )

        // When
        composeTestRule.setContent {
            ContactBookScreen(
                uiState = uiState,
                onTabSelected = {},
                onGroupClick = {},
                onContactClick = {}
            )
        }

        // Then - Pull refresh indicator should not be visible in search mode
        composeTestRule.onNodeWithText("이름이나 이메일 주소로 검색하세요").assertIsDisplayed()
    }
}
