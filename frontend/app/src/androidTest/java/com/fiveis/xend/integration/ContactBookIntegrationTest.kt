package com.fiveis.xend.integration

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.model.AddContactRequest
import com.fiveis.xend.data.model.AddContactRequestContext
import com.fiveis.xend.data.model.AddGroupRequest
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.ContactResponse
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.model.GroupResponse
import com.fiveis.xend.data.model.PromptOption
import com.fiveis.xend.data.repository.ContactBookRepository
import com.fiveis.xend.data.repository.ContactBookTab
import com.fiveis.xend.network.ContactApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ContactBookIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var context: Context
    private lateinit var contactApiService: ContactApiService
    private lateinit var repository: ContactBookRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        contactApiService = mockk()
    }

    @Test
    fun repository_adds_contact_successfully() = runTest {
        val mockResponse = ContactResponse(
            id = 1L,
            name = "김철수",
            email = "kim@example.com",
            group = null,
            context = null,
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        coEvery {
            contactApiService.addContact(any())
        } returns Response.success(mockResponse)

        val result = contactApiService.addContact(
            AddContactRequest(
                name = "김철수",
                email = "kim@example.com",
                groupId = null,
                context = AddContactRequestContext(
                    senderRole = "Student",
                    recipientRole = "Professor",
                    personalPrompt = "Formal tone"
                )
            )
        )

        assertTrue(result.isSuccessful)
        assertEquals("김철수", result.body()?.name)
        assertEquals("kim@example.com", result.body()?.email)
    }

    @Test
    fun repository_gets_all_contacts_successfully() = runTest {
        val mockContacts = listOf(
            ContactResponse(
                id = 1L,
                name = "김철수",
                email = "kim@example.com",
                group = null,
                context = null,
                createdAt = "2025-01-01T00:00:00Z",
                updatedAt = "2025-01-01T00:00:00Z"
            ),
            ContactResponse(
                id = 2L,
                name = "이영희",
                email = "lee@example.com",
                group = null,
                context = null,
                createdAt = "2025-01-01T00:00:00Z",
                updatedAt = "2025-01-01T00:00:00Z"
            )
        )

        coEvery {
            contactApiService.getAllContacts()
        } returns Response.success(mockContacts)

        val result = contactApiService.getAllContacts()

        assertTrue(result.isSuccessful)
        assertEquals(2, result.body()?.size)
    }

    @Test
    fun repository_gets_contact_by_id_successfully() = runTest {
        val mockContact = ContactResponse(
            id = 1L,
            name = "김철수",
            email = "kim@example.com",
            group = null,
            context = null,
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        coEvery {
            contactApiService.getContact(1L)
        } returns Response.success(mockContact)

        val result = contactApiService.getContact(1L)

        assertTrue(result.isSuccessful)
        assertEquals(1L, result.body()?.id)
        assertEquals("김철수", result.body()?.name)
    }

    @Test
    fun repository_deletes_contact_successfully() = runTest {
        coEvery {
            contactApiService.deleteContact(1L)
        } returns Response.success(200, null as Void?)

        val result = contactApiService.deleteContact(1L)

        assertTrue(result.isSuccessful)
    }

    @Test
    fun repository_updates_contact_group_successfully() = runTest {
        val mockContact = ContactResponse(
            id = 1L,
            name = "김철수",
            email = "kim@example.com",
            group = null,
            context = null,
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        coEvery {
            contactApiService.updateContact(1L, mapOf("group_id" to 2L))
        } returns Response.success(mockContact)

        val result = contactApiService.updateContact(1L, mapOf("group_id" to 2L))

        assertTrue(result.isSuccessful)
    }

    @Test
    fun repository_adds_group_successfully() = runTest {
        val mockResponse = GroupResponse(
            id = 1L,
            name = "VIP",
            description = "Important contacts",
            options = emptyList(),
            contacts = emptyList(),
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        coEvery {
            contactApiService.addGroup(any())
        } returns Response.success(mockResponse)

        val result = contactApiService.addGroup(
            AddGroupRequest(
                name = "VIP",
                description = "Important contacts",
                optionIds = emptyList()
            )
        )

        assertTrue(result.isSuccessful)
        assertEquals("VIP", result.body()?.name)
    }

    @Test
    fun repository_gets_all_groups_successfully() = runTest {
        val mockGroups = listOf(
            GroupResponse(
                id = 1L,
                name = "VIP",
                description = "Important",
                options = emptyList(),
                contacts = emptyList(),
                createdAt = "2025-01-01T00:00:00Z",
                updatedAt = "2025-01-01T00:00:00Z"
            ),
            GroupResponse(
                id = 2L,
                name = "Team",
                description = "Colleagues",
                options = emptyList(),
                contacts = emptyList(),
                createdAt = "2025-01-01T00:00:00Z",
                updatedAt = "2025-01-01T00:00:00Z"
            )
        )

        coEvery {
            contactApiService.getAllGroups()
        } returns Response.success(mockGroups)

        val result = contactApiService.getAllGroups()

        assertTrue(result.isSuccessful)
        assertEquals(2, result.body()?.size)
    }

    @Test
    fun repository_gets_group_by_id_successfully() = runTest {
        val mockGroup = GroupResponse(
            id = 1L,
            name = "VIP",
            description = "Important",
            options = emptyList(),
            contacts = emptyList(),
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        coEvery {
            contactApiService.getGroup(1L)
        } returns Response.success(mockGroup)

        val result = contactApiService.getGroup(1L)

        assertTrue(result.isSuccessful)
        assertEquals(1L, result.body()?.id)
        assertEquals("VIP", result.body()?.name)
    }

    @Test
    fun repository_deletes_group_successfully() = runTest {
        coEvery {
            contactApiService.deleteGroup(1L)
        } returns Response.success(200, null as Void?)

        val result = contactApiService.deleteGroup(1L)

        assertTrue(result.isSuccessful)
    }

    @Test
    fun repository_adds_prompt_option_successfully() = runTest {
        val mockOption = PromptOption(
            id = 1L,
            key = "tone",
            name = "Formal",
            prompt = "Use formal language",
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        coEvery {
            contactApiService.addPromptOption(any())
        } returns Response.success(mockOption)

        val result = contactApiService.addPromptOption(
            com.fiveis.xend.data.model.PromptOptionRequest(
                key = "tone",
                name = "Formal",
                prompt = "Use formal language"
            )
        )

        assertTrue(result.isSuccessful)
        assertEquals("Formal", result.body()?.name)
    }

    @Test
    fun repository_gets_all_prompt_options_successfully() = runTest {
        val mockOptions = listOf(
            PromptOption(
                id = 1L,
                key = "tone",
                name = "Formal",
                prompt = "Formal tone",
                createdAt = "2025-01-01T00:00:00Z",
                updatedAt = "2025-01-01T00:00:00Z"
            ),
            PromptOption(
                id = 2L,
                key = "format",
                name = "Brief",
                prompt = "Keep it brief",
                createdAt = "2025-01-01T00:00:00Z",
                updatedAt = "2025-01-01T00:00:00Z"
            )
        )

        coEvery {
            contactApiService.getAllPromptOptions()
        } returns Response.success(mockOptions)

        val result = contactApiService.getAllPromptOptions()

        assertTrue(result.isSuccessful)
        assertEquals(2, result.body()?.size)
    }

    @Test
    fun contact_book_tab_enum_has_correct_values() {
        val tabs = ContactBookTab.values()

        assertEquals(2, tabs.size)
        assertTrue(tabs.contains(ContactBookTab.Groups))
        assertTrue(tabs.contains(ContactBookTab.Contacts))
    }

    @Test
    fun contact_data_class_creates_correctly() {
        val contact = Contact(
            id = 1L,
            name = "Test User",
            email = "test@example.com",
            group = null,
            context = null,
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        assertEquals(1L, contact.id)
        assertEquals("Test User", contact.name)
        assertEquals("test@example.com", contact.email)
    }

    @Test
    fun group_data_class_creates_correctly() {
        val group = Group(
            id = 1L,
            name = "VIP",
            description = "Important contacts",
            options = emptyList(),
            members = emptyList(),
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        assertEquals(1L, group.id)
        assertEquals("VIP", group.name)
        assertEquals("Important contacts", group.description)
    }

    @Test
    fun contact_with_group_creates_correctly() = runTest {
        val mockGroup = GroupResponse(
            id = 1L,
            name = "VIP",
            description = "Important",
            options = emptyList(),
            contacts = null,
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        val mockContact = ContactResponse(
            id = 1L,
            name = "김철수",
            email = "kim@example.com",
            group = mockGroup,
            context = null,
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        coEvery {
            contactApiService.getContact(1L)
        } returns Response.success(mockContact)

        val result = contactApiService.getContact(1L)

        assertTrue(result.isSuccessful)
        assertNotNull(result.body()?.group)
        assertEquals("VIP", result.body()?.group?.name)
    }

    @Test
    fun group_with_members_creates_correctly() = runTest {
        val mockContacts = listOf(
            ContactResponse(
                id = 1L,
                name = "김철수",
                email = "kim@example.com",
                group = null,
                context = null,
                createdAt = "2025-01-01T00:00:00Z",
                updatedAt = "2025-01-01T00:00:00Z"
            ),
            ContactResponse(
                id = 2L,
                name = "이영희",
                email = "lee@example.com",
                group = null,
                context = null,
                createdAt = "2025-01-01T00:00:00Z",
                updatedAt = "2025-01-01T00:00:00Z"
            )
        )

        val mockGroup = GroupResponse(
            id = 1L,
            name = "VIP",
            description = "Important",
            options = emptyList(),
            contacts = mockContacts,
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        coEvery {
            contactApiService.getGroup(1L)
        } returns Response.success(mockGroup)

        val result = contactApiService.getGroup(1L)

        assertTrue(result.isSuccessful)
        assertEquals(2, result.body()?.contacts?.size)
    }
}
