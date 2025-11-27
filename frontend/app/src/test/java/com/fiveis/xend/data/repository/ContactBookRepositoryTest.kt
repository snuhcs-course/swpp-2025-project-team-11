package com.fiveis.xend.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.fiveis.xend.data.database.AppDatabase
import com.fiveis.xend.data.database.ContactDao
import com.fiveis.xend.data.database.GroupDao
import com.fiveis.xend.data.database.PromptOptionDao
import com.fiveis.xend.data.model.ContactResponse
import com.fiveis.xend.data.model.ContactResponseContext
import com.fiveis.xend.data.model.GroupResponse
import com.fiveis.xend.data.model.PromptOption
import com.fiveis.xend.network.ContactApiService
import com.fiveis.xend.network.RetrofitClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class ContactBookRepositoryTest {

    private lateinit var context: Context
    private lateinit var contactApiService: ContactApiService

    // 1. DB와 DAO 변수 선언
    private lateinit var db: AppDatabase
    private lateinit var contactDao: ContactDao
    private lateinit var groupDao: GroupDao
    private lateinit var optionDao: PromptOptionDao

    private lateinit var repository: ContactBookRepository

    @Before
    fun setup() {
        // API 모킹
        context = mockk(relaxed = true)
        contactApiService = mockk()
        mockkObject(RetrofitClient)
        every { RetrofitClient.getContactApiService(context) } returns contactApiService

        // 2. DB와 DAO 모킹 (NPE 해결의 핵심)
        db = mockk(relaxed = true)
        contactDao = mockk(relaxed = true)
        groupDao = mockk(relaxed = true)
        optionDao = mockk(relaxed = true)

        // 3. Mock DB가 Mock DAO를 반환하도록 연결
        every { db.contactDao() } returns contactDao
        every { db.groupDao() } returns groupDao
        every { db.promptOptionDao() } returns optionDao

        // 4. Mock withTransaction to execute the block immediately
        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Any>()) } coAnswers {
            secondArg<suspend () -> Any>().invoke()
        }

        // 5. Repository 생성 시 Mock DB를 두 번째 파라미터로 주입
        repository = ContactBookRepository(context, db)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun refresh_groups_updates_local_database() = runTest {
        val mockGroups = listOf(
            GroupResponse(
                id = 1L,
                name = "Test Group",
                description = "Test Description"
            )
        )

        coEvery { contactApiService.getAllGroups() } returns Response.success(mockGroups)
        coEvery { groupDao.deleteAllGroups() } returns Unit
        coEvery { groupDao.upsertGroups(any()) } returns Unit
        coEvery { optionDao.deleteAllCrossRefs() } returns Unit

        repository.refreshGroups()

        coVerify { contactApiService.getAllGroups() }
        coVerify { groupDao.deleteAllGroups() }
    }

    @Test
    fun refresh_contacts_updates_local_database() = runTest {
        val mockContacts = listOf(
            ContactResponse(
                id = 1L,
                name = "Test Contact",
                email = "test@example.com"
            )
        )

        coEvery { contactApiService.getAllContacts() } returns Response.success(mockContacts)
        coEvery { contactDao.deleteAllContexts() } returns Unit
        coEvery { contactDao.deleteAllContacts() } returns Unit
        coEvery { contactDao.upsertContacts(any()) } returns Unit

        repository.refreshContacts()

        coVerify { contactApiService.getAllContacts() }
        coVerify { contactDao.deleteAllContacts() }
    }

    @Test
    fun add_contact_with_successful_response_returns_contact_response() = runTest {
        val name = "John Doe"
        val email = "john@example.com"
        val groupId = 1L
        val senderRole = "Manager"
        val recipientRole = "Employee"
        val personalPrompt = "Be professional"

        val expectedResponse = ContactResponse(
            id = 1L,
            name = name,
            email = email
        )

        coEvery {
            contactApiService.addContact(any())
        } returns Response.success(expectedResponse)
        coEvery { contactDao.upsertContacts(any()) } returns Unit

        val result = repository.addContact(
            name = name,
            email = email,
            groupId = groupId,
            senderRole = senderRole,
            recipientRole = recipientRole,
            personalPrompt = personalPrompt
        )

        assertEquals(expectedResponse, result)
        coVerify {
            contactApiService.addContact(
                match { request ->
                    request.name == name &&
                        request.email == email &&
                        request.groupId == groupId &&
                        request.context?.senderRole == senderRole &&
                        request.context?.recipientRole == recipientRole &&
                        request.context?.personalPrompt == personalPrompt
                }
            )
        }
    }

    @Test
    fun add_contact_with_null_sender_role_uses_default() = runTest {
        val name = "John Doe"
        val email = "john@example.com"
        val recipientRole = "Employee"

        val expectedResponse = ContactResponse(
            id = 1L,
            name = name,
            email = email
        )

        coEvery {
            contactApiService.addContact(any())
        } returns Response.success(expectedResponse)
        coEvery { contactDao.upsertContacts(any()) } returns Unit

        repository.addContact(
            name = name,
            email = email,
            groupId = null,
            senderRole = null,
            recipientRole = recipientRole,
            personalPrompt = null
        )

        coVerify {
            contactApiService.addContact(
                match { request ->
                    request.context?.senderRole == ""
                }
            )
        }
    }

    @Test
    fun add_contact_throws_exception_when_response_body_is_null() = runTest {
        coEvery {
            contactApiService.addContact(any())
        } returns Response.success(null)

        val exception = try {
            repository.addContact(
                name = "Test",
                email = "test@example.com",
                groupId = null,
                senderRole = null,
                recipientRole = "Test",
                personalPrompt = null
            )
            null
        } catch (e: Exception) {
            e
        }

        assertTrue(exception != null)
        assertTrue(exception?.message?.contains("Success but body null") == true)
    }

    @Test
    fun add_contact_throws_exception_on_error_response() = runTest {
        coEvery {
            contactApiService.addContact(any())
        } returns Response.error(400, "Bad request".toResponseBody())

        val exception = try {
            repository.addContact(
                name = "Test",
                email = "test@example.com",
                groupId = null,
                senderRole = null,
                recipientRole = "Test",
                personalPrompt = null
            )
            null
        } catch (e: IllegalStateException) {
            e
        }

        assertTrue(exception != null)
        assertTrue(exception?.message?.contains("Add contact failed: HTTP 400") == true)
    }

    @Test
    fun get_contact_returns_contact_with_all_fields() = runTest {
        val contactId = 1L
        val mockContactEntity = com.fiveis.xend.data.database.entity.ContactEntity(
            id = contactId,
            groupId = null,
            name = "Test Contact",
            email = "test@example.com"
        )
        val mockContext = com.fiveis.xend.data.database.ContactWithContext(
            contact = mockContactEntity,
            context = null
        )

        coEvery { contactDao.getContactWithContext(contactId) } returns mockContext

        val result = repository.getContact(contactId)

        assertEquals(contactId, result.id)
        assertEquals("Test Contact", result.name)
        assertEquals("test@example.com", result.email)
    }

    @Test
    fun get_contact_returns_contact_successfully() = runTest {
        val contactId = 1L
        val mockContactEntity = com.fiveis.xend.data.database.entity.ContactEntity(
            id = contactId,
            groupId = null,
            name = "Test",
            email = "test@example.com"
        )
        val mockContext = com.fiveis.xend.data.database.ContactWithContext(
            contact = mockContactEntity,
            context = null
        )

        coEvery { contactDao.getContactWithContext(contactId) } returns mockContext

        val result = repository.getContact(contactId)

        assertEquals(contactId, result.id)
    }

    @Test
    fun get_all_contacts_returns_list_of_contacts() = runTest {
        val mockEntity1 = com.fiveis.xend.data.database.entity.ContactEntity(
            id = 1L,
            groupId = null,
            name = "Contact 1",
            email = "contact1@example.com"
        )
        val mockEntity2 = com.fiveis.xend.data.database.entity.ContactEntity(
            id = 2L,
            groupId = null,
            name = "Contact 2",
            email = "contact2@example.com"
        )
        val mockContact1 = com.fiveis.xend.data.database.ContactWithContext(mockEntity1, null)
        val mockContact2 = com.fiveis.xend.data.database.ContactWithContext(mockEntity2, null)

        coEvery { contactDao.getAllWithContext() } returns listOf(mockContact1, mockContact2)

        val result = repository.getAllContacts()

        assertEquals(2, result.size)
        assertEquals("Contact 1", result[0].name)
        assertEquals("Contact 2", result[1].name)
    }

    @Test
    fun get_all_contacts_returns_empty_list_when_body_is_null() = runTest {
        coEvery { contactDao.getAllWithContext() } returns emptyList()

        val result = repository.getAllContacts()

        assertEquals(0, result.size)
    }

    @Test
    fun get_all_contacts_throws_exception_on_error() = runTest {
        coEvery { contactDao.getAllWithContext() } throws Exception("Database error")

        val exception = try {
            repository.getAllContacts()
            null
        } catch (e: Exception) {
            e
        }

        assertTrue(exception != null)
        assertTrue(exception?.message?.contains("Database error") == true)
    }

    @Test
    fun delete_contact_succeeds() = runTest {
        val contactId = 1L

        coEvery {
            contactApiService.deleteContact(contactId)
        } returns Response.success(null)
        coEvery { contactDao.deleteById(contactId) } returns Unit

        repository.deleteContact(contactId)

        coVerify {
            contactApiService.deleteContact(contactId)
        }
        coVerify { contactDao.deleteById(contactId) }
    }

    @Test
    fun delete_contact_throws_exception_on_failure() = runTest {
        val contactId = 1L

        coEvery {
            contactApiService.deleteContact(contactId)
        } returns Response.error(404, "Not found".toResponseBody())

        val exception = try {
            repository.deleteContact(contactId)
            null
        } catch (e: IllegalStateException) {
            e
        }

        assertTrue(exception != null)
        assertTrue(exception?.message?.contains("Failed to delete contact") == true)
    }

    @Test
    fun add_group_with_successful_response_returns_group_response() = runTest {
        val name = "Test Group"
        val description = "Test Description"
        val options = listOf(
            PromptOption(
                id = 1L,
                key = "tone",
                name = "Formal",
                prompt = "Be formal"
            )
        )

        val expectedResponse = GroupResponse(
            id = 1L,
            name = name,
            description = description,
            emoji = null,
            options = options
        )

        coEvery {
            contactApiService.addGroup(any())
        } returns Response.success(expectedResponse)
        coEvery { groupDao.upsertGroups(any()) } returns Unit
        coEvery { optionDao.upsertOptions(any()) } returns Unit
        coEvery { optionDao.deleteCrossRefsByGroup(any()) } returns Unit
        coEvery { optionDao.upsertCrossRefs(any()) } returns Unit

        val result = repository.addGroup(name, description, "", options)

        assertEquals(expectedResponse, result)
        coVerify {
            contactApiService.addGroup(
                match { request ->
                    request.name == name &&
                        request.description == description &&
                        request.optionIds == listOf(1L)
                }
            )
        }
    }

    @Test
    fun add_group_throws_exception_when_response_body_is_null() = runTest {
        coEvery {
            contactApiService.addGroup(any())
        } returns Response.success(null)

        val exception = try {
            repository.addGroup("Test", "Description", "", emptyList())
            null
        } catch (e: Exception) {
            e
        }

        assertTrue(exception != null)
        assertTrue(exception?.message?.contains("Success but body null") == true)
    }

    @Test
    fun get_group_returns_group_with_all_fields() = runTest {
        val groupId = 1L
        val mockGroupEntity = com.fiveis.xend.data.database.entity.GroupEntity(
            id = groupId,
            name = "Test Group",
            description = "Test Description"
        )
        val mockOptionEntity = com.fiveis.xend.data.database.entity.PromptOptionEntity(
            id = 1L,
            key = "tone",
            name = "Formal",
            prompt = "Be formal"
        )
        val mockGroup = com.fiveis.xend.data.database.GroupWithMembersAndOptions(
            group = mockGroupEntity,
            members = emptyList(),
            options = listOf(mockOptionEntity)
        )

        coEvery { groupDao.getGroupWithMembersAndOptions(groupId) } returns mockGroup

        val result = repository.getGroup(groupId)

        assertEquals(groupId, result.id)
        assertEquals("Test Group", result.name)
        assertEquals("Test Description", result.description)
        assertEquals(1, result.options.size)
    }

    @Test
    fun get_all_groups_returns_list_of_groups() = runTest {
        val mockEntity1 = com.fiveis.xend.data.database.entity.GroupEntity(
            id = 1L,
            name = "Group 1",
            description = "Description 1"
        )
        val mockEntity2 = com.fiveis.xend.data.database.entity.GroupEntity(
            id = 2L,
            name = "Group 2",
            description = "Description 2"
        )
        val mockGroup1 = com.fiveis.xend.data.database.GroupWithMembersAndOptions(mockEntity1, emptyList(), emptyList())
        val mockGroup2 = com.fiveis.xend.data.database.GroupWithMembersAndOptions(mockEntity2, emptyList(), emptyList())

        coEvery { groupDao.getGroupsWithMembersAndOptions() } returns listOf(mockGroup1, mockGroup2)

        val result = repository.getAllGroups()

        assertEquals(2, result.size)
        assertEquals("Group 1", result[0].name)
        assertEquals("Group 2", result[1].name)
    }

    @Test
    fun get_all_groups_throws_exception_on_error() = runTest {
        coEvery { groupDao.getGroupsWithMembersAndOptions() } throws Exception("Database error")

        val exception = try {
            repository.getAllGroups()
            null
        } catch (e: Exception) {
            e
        }

        assertTrue(exception != null)
        assertTrue(exception?.message?.contains("Database error") == true)
    }

    @Test
    fun delete_group_succeeds() = runTest {
        val groupId = 1L

        coEvery {
            contactApiService.deleteGroup(groupId)
        } returns Response.success(null)
        coEvery { groupDao.deleteById(groupId) } returns Unit

        repository.deleteGroup(groupId)

        coVerify {
            contactApiService.deleteGroup(groupId)
        }
        coVerify { groupDao.deleteById(groupId) }
    }

    @Test
    fun delete_group_throws_exception_on_failure() = runTest {
        val groupId = 1L

        coEvery {
            contactApiService.deleteGroup(groupId)
        } returns Response.error(404, "Not found".toResponseBody())

        val exception = try {
            repository.deleteGroup(groupId)
            null
        } catch (e: IllegalStateException) {
            e
        }

        assertTrue(exception != null)
        assertTrue(exception?.message?.contains("Failed to delete group") == true)
    }

    @Test
    fun add_prompt_option_with_successful_response_returns_prompt_option() = runTest {
        val key = "tone"
        val name = "Formal"
        val prompt = "Be formal"

        val expectedResponse = PromptOption(
            id = 1L,
            key = key,
            name = name,
            prompt = prompt
        )

        coEvery {
            contactApiService.addPromptOption(any())
        } returns Response.success(expectedResponse)
        coEvery { optionDao.upsertOptions(any()) } returns Unit

        val result = repository.addPromptOption(key, name, prompt)

        assertEquals(expectedResponse, result)
        coVerify {
            contactApiService.addPromptOption(
                match { request ->
                    request.key == key &&
                        request.name == name &&
                        request.prompt == prompt
                }
            )
        }
    }

    @Test
    fun add_prompt_option_throws_exception_when_response_body_is_null() = runTest {
        coEvery {
            contactApiService.addPromptOption(any())
        } returns Response.success(null)

        val exception = try {
            repository.addPromptOption("key", "name", "prompt")
            null
        } catch (e: Exception) {
            e
        }

        assertTrue(exception != null)
        assertTrue(exception?.message?.contains("Success but body null") == true)
    }

    @Test
    fun get_all_prompt_options_returns_separated_tone_and_format_options() = runTest {
        val mockOptions = listOf(
            PromptOption(
                id = 1L,
                key = "tone",
                name = "Formal",
                prompt = "Be formal"
            ),
            PromptOption(
                id = 2L,
                key = "format",
                name = "Short",
                prompt = "Keep it short"
            ),
            PromptOption(
                id = 3L,
                key = "tone",
                name = "Casual",
                prompt = "Be casual"
            )
        )

        coEvery {
            contactApiService.getAllPromptOptions()
        } returns Response.success(mockOptions)
        coEvery { optionDao.upsertOptions(any()) } returns Unit

        val result = repository.getAllPromptOptions()

        assertEquals(2, result.first.size)
        assertEquals(1, result.second.size)
        assertEquals("Formal", result.first[0].name)
        assertEquals("Casual", result.first[1].name)
        assertEquals("Short", result.second[0].name)
    }

    @Test
    fun get_all_prompt_options_throws_exception_on_error() = runTest {
        coEvery {
            contactApiService.getAllPromptOptions()
        } returns Response.error(500, "Server error".toResponseBody())

        val exception = try {
            repository.getAllPromptOptions()
            null
        } catch (e: IllegalStateException) {
            e
        }

        assertTrue(exception != null)
        assertTrue(exception?.message?.contains("Failed to get all prompt options") == true)
    }

    @Test
    fun refresh_groups_with_options_upserts_options_and_crossrefs() = runTest {
        val resp = listOf(
            GroupResponse(
                id = 10L, name = "G", description = "D",
                options = listOf(
                    PromptOption(101L, "tone", "Formal", "f"),
                    PromptOption(102L, "format", "Short", "s")
                )
            )
        )
        coEvery { contactApiService.getAllGroups() } returns Response.success(resp)
        coEvery { groupDao.deleteAllGroups() } returns Unit
        coEvery { optionDao.deleteAllCrossRefs() } returns Unit

        val groupsSlot = slot<List<com.fiveis.xend.data.database.entity.GroupEntity>>()
        val optsSlot = slot<List<com.fiveis.xend.data.database.entity.PromptOptionEntity>>()
        val refsSlot = slot<List<com.fiveis.xend.data.database.entity.GroupPromptOptionCrossRef>>()

        coEvery { groupDao.upsertGroups(capture(groupsSlot)) } returns Unit
        coEvery { optionDao.upsertOptions(capture(optsSlot)) } returns Unit
        coEvery { optionDao.upsertCrossRefs(capture(refsSlot)) } returns Unit

        repository.refreshGroups()

        assertEquals(1, groupsSlot.captured.size)
        assertEquals(setOf(101L, 102L), optsSlot.captured.map { it.id }.toSet())
        assertEquals(setOf(101L, 102L), refsSlot.captured.map { it.optionId }.toSet())
        assertTrue(refsSlot.captured.all { it.groupId == 10L })
    }

    @Test
    fun refresh_groups_http_error_throws() = runTest {
        coEvery { contactApiService.getAllGroups() } returns Response.error(500, "x".toResponseBody())
        val e = runCatching { repository.refreshGroups() }.exceptionOrNull()
        assertTrue(e is IllegalStateException)
    }

    @Test
    fun refresh_contacts_with_contexts_upserts_contexts() = runTest {
        val resp = listOf(
            ContactResponse(
                id = 1L, name = "A", email = "a@x.com",
                context = ContactResponseContext(id = 1L, senderRole = "S", recipientRole = "R")
            )
        )
        coEvery { contactApiService.getAllContacts() } returns Response.success(resp)
        coEvery { contactDao.deleteAllContexts() } returns Unit
        coEvery { contactDao.deleteAllContacts() } returns Unit

        val contactsSlot = slot<List<com.fiveis.xend.data.database.entity.ContactEntity>>()
        val ctxSlot = slot<List<com.fiveis.xend.data.database.entity.ContactContextEntity>>()

        coEvery { contactDao.upsertContacts(capture(contactsSlot)) } returns Unit
        coEvery { contactDao.upsertContexts(capture(ctxSlot)) } returns Unit

        repository.refreshContacts()

        assertEquals(listOf(1L), contactsSlot.captured.map { it.id })
        assertEquals(listOf(1L), ctxSlot.captured.map { it.contactId })
        assertEquals("S", ctxSlot.captured.first().senderRole)
    }

    @Test
    fun refresh_contacts_http_error_throws() = runTest {
        coEvery { contactApiService.getAllContacts() } returns Response.error(400, "x".toResponseBody())
        val e = runCatching { repository.refreshContacts() }.exceptionOrNull()
        assertTrue(e is IllegalStateException)
    }

    @Test
    fun refresh_group_replaces_crossrefs_and_upserts_options() = runTest {
        val resp = GroupResponse(
            id = 3L, name = "G", description = null,
            options = listOf(PromptOption(10L, "tone", "F", "f"))
        )
        coEvery { contactApiService.getGroup(3L) } returns Response.success(resp)
        coEvery { groupDao.upsertGroups(any()) } returns Unit
        coEvery { optionDao.upsertOptions(any()) } returns Unit
        coEvery { optionDao.deleteCrossRefsByGroup(3L) } returns Unit

        val refsSlot = slot<List<com.fiveis.xend.data.database.entity.GroupPromptOptionCrossRef>>()
        coEvery { optionDao.upsertCrossRefs(capture(refsSlot)) } returns Unit

        repository.refreshGroup(3L)

        coVerify { optionDao.deleteCrossRefsByGroup(3L) }
        assertEquals(listOf(10L), refsSlot.captured.map { it.optionId })
    }

    @Test
    fun refresh_group_with_no_options_clears_crossrefs_only() = runTest {
        val resp = GroupResponse(id = 4L, name = "G4", description = null, options = emptyList())
        coEvery { contactApiService.getGroup(4L) } returns Response.success(resp)
        coEvery { groupDao.upsertGroups(any()) } returns Unit
        coEvery { optionDao.deleteCrossRefsByGroup(4L) } returns Unit

        repository.refreshGroup(4L)

        coVerify(exactly = 0) { optionDao.upsertCrossRefs(any()) }
        coVerify(exactly = 0) { optionDao.upsertOptions(any()) }
    }

    @Test
    fun refresh_group_http_error_throws() = runTest {
        coEvery { contactApiService.getGroup(9L) } returns Response.error(404, "x".toResponseBody())
        val e = runCatching { repository.refreshGroup(9L) }.exceptionOrNull()
        assertTrue(e is IllegalStateException)
    }

    @Test
    fun refresh_group_and_members_calls_both_endpoints() = runTest {
        coEvery { contactApiService.getGroup(1L) } returns Response.success(
            GroupResponse(1L, "G", null, null, emptyList())
        )
        coEvery { contactApiService.getAllContacts() } returns Response.success(emptyList())
        coEvery { groupDao.upsertGroups(any()) } returns Unit
        coEvery { optionDao.deleteCrossRefsByGroup(1L) } returns Unit
        coEvery { contactDao.deleteAllContexts() } returns Unit
        coEvery { contactDao.deleteAllContacts() } returns Unit
        coEvery { contactDao.upsertContacts(any()) } returns Unit

        repository.refreshGroupAndMembers(1L)

        coVerify { contactApiService.getGroup(1L) }
        coVerify { contactApiService.getAllContacts() }
    }

    @Test
    fun refresh_prompt_options_upserts_entities() = runTest {
        val opts = listOf(PromptOption(1L, "tone", "F", "f"))
        coEvery { contactApiService.getAllPromptOptions() } returns Response.success(opts)
        coEvery { optionDao.upsertOptions(any()) } returns Unit

        repository.refreshPromptOptions()

        coVerify { optionDao.upsertOptions(match { it.size == 1 && it[0].id == 1L }) }
    }

    @Test
    fun refresh_prompt_options_http_error_throws() = runTest {
        coEvery { contactApiService.getAllPromptOptions() } returns Response.error(500, "err".toResponseBody())
        val e = runCatching { repository.refreshPromptOptions() }.exceptionOrNull()
        assertTrue(e is IllegalStateException)
    }

    @Test
    fun update_contact_group_success_updates_local() = runTest {
        val response = ContactResponse(id = 5L, name = "Kim", email = "kim@example.com", group = GroupResponse(2L, "Dev"))
        coEvery { contactApiService.updateContact(5L, any()) } returns Response.success(response)
        coEvery { contactDao.upsertContacts(any()) } returns Unit
        coEvery { contactDao.upsertContexts(any()) } returns Unit

        repository.updateContactGroup(5L, 2L)

        coVerify { contactDao.upsertContacts(match { it.single().id == 5L && it.single().groupId == 2L }) }
    }

    @Test
    fun update_contact_group_error_throws_and_does_not_update() = runTest {
        coEvery { contactApiService.updateContact(5L, any()) } returns Response.error(400, "Bad".toResponseBody())

        val e = runCatching { repository.updateContactGroup(5L, 2L) }.exceptionOrNull()
        assertTrue(e is IllegalStateException)
        coVerify(exactly = 0) { contactDao.upsertContacts(any()) }
    }

    @Test
    fun update_contact_success_updates_local() = runTest {
        val response = ContactResponse(id = 9L, name = "Lee", email = "lee@example.com")
        coEvery { contactApiService.updateContact(9L, any()) } returns Response.success(response)
        coEvery { contactDao.upsertContacts(any()) } returns Unit
        coEvery { contactDao.upsertContexts(any()) } returns Unit

        repository.updateContact(9L, "Lee", "lee@example.com", null, null, null, null)

        coVerify { contactDao.upsertContacts(match { it.single().id == 9L && it.single().name == "Lee" }) }
    }

    @Test
    fun add_contact_null_personal_prompt_becomes_blank_string() = runTest {
        val reqSlot = slot<com.fiveis.xend.data.model.AddContactRequest>()
        coEvery { contactApiService.addContact(capture(reqSlot)) } returns Response.success(
            ContactResponse(id = 1L, name = "N", email = "e@x.com")
        )
        coEvery { contactDao.upsertContacts(any()) } returns Unit

        repository.addContact(
            name = "N", email = "e@x.com",
            groupId = null, senderRole = "Writer", recipientRole = "Emp", personalPrompt = null
        )

        assertEquals("", reqSlot.captured.context?.personalPrompt)
    }

    @Test
    fun add_contact_with_context_persists_context_locally() = runTest {
        val resp = ContactResponse(
            id = 77L, name = "New", email = "n@x.com",
            context = ContactResponseContext(id = 77L, senderRole = "Mgr", recipientRole = "Emp")
        )
        coEvery { contactApiService.addContact(any()) } returns Response.success(resp)

        val ctxSlot = slot<List<com.fiveis.xend.data.database.entity.ContactContextEntity>>()
        coEvery { contactDao.upsertContacts(any()) } returns Unit
        coEvery { contactDao.upsertContexts(capture(ctxSlot)) } returns Unit

        repository.addContact("New", "n@x.com", null, "Mgr", "Emp", null)

        assertEquals(listOf(77L), ctxSlot.captured.map { it.contactId })
        assertEquals("Mgr", ctxSlot.captured.first().senderRole)
    }

    @Test
    fun get_contact_throws_when_not_found_locally() = runTest {
        coEvery { contactDao.getContactWithContext(123L) } returns null
        val e = runCatching { repository.getContact(123L) }.exceptionOrNull()
        assertTrue(e is IllegalStateException)
    }

    @Test
    fun get_group_throws_when_not_found_locally() = runTest {
        coEvery { groupDao.getGroupWithMembersAndOptions(999L) } returns null
        val e = runCatching { repository.getGroup(999L) }.exceptionOrNull()
        assertTrue(e is IllegalStateException)
    }

    @Test
    fun get_all_prompt_options_upserts_to_local() = runTest {
        val all = listOf(
            PromptOption(1L, "tone", "Formal", "f"),
            PromptOption(2L, "format", "Short", "s")
        )
        coEvery { contactApiService.getAllPromptOptions() } returns Response.success(all)
        coEvery { optionDao.upsertOptions(any()) } returns Unit

        repository.getAllPromptOptions()

        coVerify { optionDao.upsertOptions(match { it.map { e -> e.id }.toSet() == setOf(1L, 2L) }) }
    }

    @Test
    fun observe_prompt_options_maps_entities_to_domain() = runTest {
        val e1 = com.fiveis.xend.data.database.entity.PromptOptionEntity(
            id = 1L, key = "tone", name = "Formal", prompt = "f"
        )
        every { optionDao.observeAllOptions() } returns flowOf(listOf(e1))

        val first = repository.observePromptOptions().first()

        assertEquals(1, first.size)
        assertEquals("tone", first[0].key)
        assertEquals("Formal", first[0].name)
    }

    @Test
    fun local_groups_returns_list_of_groups_from_dao() = runTest {
        val mockEntity1 = com.fiveis.xend.data.database.entity.GroupEntity(
            id = 1L,
            name = "Group 1",
            description = "Description 1"
        )
        val mockEntity2 = com.fiveis.xend.data.database.entity.GroupEntity(
            id = 2L,
            name = "Group 2",
            description = "Description 2"
        )
        val mockGroup1 = com.fiveis.xend.data.database.GroupWithMembersAndOptions(mockEntity1, emptyList(), emptyList())
        val mockGroup2 = com.fiveis.xend.data.database.GroupWithMembersAndOptions(mockEntity2, emptyList(), emptyList())

        coEvery { groupDao.getGroupsWithMembersAndOptions() } returns listOf(mockGroup1, mockGroup2)

        val result = repository.localGroups()

        assertEquals(2, result.size)
        assertEquals("Group 1", result[0].name)
        assertEquals("Group 2", result[1].name)
        coVerify { groupDao.getGroupsWithMembersAndOptions() }
    }

    @Test
    fun local_group_returns_single_group_from_dao() = runTest {
        val groupId = 1L
        val mockGroupEntity = com.fiveis.xend.data.database.entity.GroupEntity(
            id = groupId,
            name = "Test Group",
            description = "Test Description"
        )
        val mockGroup = com.fiveis.xend.data.database.GroupWithMembersAndOptions(
            group = mockGroupEntity,
            members = emptyList(),
            options = emptyList()
        )

        coEvery { groupDao.getGroupWithMembersAndOptions(groupId) } returns mockGroup

        val result = repository.localGroup(groupId)

        assertEquals(groupId, result?.id)
        assertEquals("Test Group", result?.name)
        coVerify { groupDao.getGroupWithMembersAndOptions(groupId) }
    }

    @Test
    fun local_contacts_returns_list_of_contacts_from_dao() = runTest {
        val mockEntity1 = com.fiveis.xend.data.database.entity.ContactEntity(
            id = 1L,
            groupId = null,
            name = "Contact 1",
            email = "contact1@example.com"
        )
        val mockEntity2 = com.fiveis.xend.data.database.entity.ContactEntity(
            id = 2L,
            groupId = null,
            name = "Contact 2",
            email = "contact2@example.com"
        )
        val mockContact1 = com.fiveis.xend.data.database.ContactWithContext(mockEntity1, null)
        val mockContact2 = com.fiveis.xend.data.database.ContactWithContext(mockEntity2, null)

        coEvery { contactDao.getAllWithContext() } returns listOf(mockContact1, mockContact2)

        val result = repository.localContacts()

        assertEquals(2, result.size)
        assertEquals("Contact 1", result[0].name)
        assertEquals("Contact 2", result[1].name)
        coVerify { contactDao.getAllWithContext() }
    }

    @Test
    fun local_contact_returns_single_contact_from_dao() = runTest {
        val contactId = 1L
        val mockContactEntity = com.fiveis.xend.data.database.entity.ContactEntity(
            id = contactId,
            groupId = null,
            name = "Test Contact",
            email = "test@example.com"
        )
        val mockContext = com.fiveis.xend.data.database.ContactWithContext(
            contact = mockContactEntity,
            context = null
        )

        coEvery { contactDao.getContactWithContext(contactId) } returns mockContext

        val result = repository.localContact(contactId)

        assertEquals(contactId, result?.id)
        assertEquals("Test Contact", result?.name)
        coVerify { contactDao.getContactWithContext(contactId) }
    }

    @Test
    fun local_contacts_by_group_returns_filtered_contacts() = runTest {
        val groupId = 1L
        val mockEntity1 = com.fiveis.xend.data.database.entity.ContactEntity(
            id = 1L,
            groupId = groupId,
            name = "Contact 1",
            email = "contact1@example.com"
        )
        val mockContact1 = com.fiveis.xend.data.database.ContactWithContext(mockEntity1, null)

        coEvery { contactDao.getContactsByGroupIdWithContext(groupId) } returns listOf(mockContact1)

        val result = repository.localContactsByGroup(groupId)

        assertEquals(1, result.size)
        assertEquals("Contact 1", result[0].name)
        coVerify { contactDao.getContactsByGroupIdWithContext(groupId) }
    }

    @Test
    fun local_contact_with_group_returns_contact_with_group_info() = runTest {
        val contactId = 1L
        val mockContactEntity = com.fiveis.xend.data.database.entity.ContactEntity(
            id = contactId,
            groupId = 1L,
            name = "Test Contact",
            email = "test@example.com"
        )
        val mockGroupEntity = com.fiveis.xend.data.database.entity.GroupEntity(
            id = 1L,
            name = "Test Group",
            description = "Test Description"
        )
        val mockContactWithGroup = com.fiveis.xend.data.database.ContactWithGroupAndContext(
            contact = mockContactEntity,
            group = mockGroupEntity,
            context = null
        )

        coEvery { contactDao.getByIdWithGroup(contactId) } returns mockContactWithGroup

        val result = repository.localContactWithGroup(contactId)

        assertEquals(contactId, result?.id)
        assertEquals("Test Contact", result?.name)
        assertEquals("Test Group", result?.group?.name)
        coVerify { contactDao.getByIdWithGroup(contactId) }
    }

    @Test
    fun observe_groups_returns_flow_of_groups() = runTest {
        val mockEntity = com.fiveis.xend.data.database.entity.GroupEntity(
            id = 1L,
            name = "Test Group",
            description = "Test Description"
        )
        val mockGroup = com.fiveis.xend.data.database.GroupWithMembersAndOptions(
            group = mockEntity,
            members = emptyList(),
            options = emptyList()
        )

        every { groupDao.observeGroupsWithMembersAndOptions() } returns kotlinx.coroutines.flow.flowOf(listOf(mockGroup))

        val result = repository.observeGroups()

        result.collect { groups ->
            assertEquals(1, groups.size)
            assertEquals("Test Group", groups[0].name)
        }
        io.mockk.verify { groupDao.observeGroupsWithMembersAndOptions() }
    }

    @Test
    fun observe_group_returns_flow_of_single_group() = runTest {
        val groupId = 1L
        val mockEntity = com.fiveis.xend.data.database.entity.GroupEntity(
            id = groupId,
            name = "Test Group",
            description = "Test Description"
        )
        val mockGroup = com.fiveis.xend.data.database.GroupWithMembersAndOptions(
            group = mockEntity,
            members = emptyList(),
            options = emptyList()
        )

        every { groupDao.observeGroup(groupId) } returns kotlinx.coroutines.flow.flowOf(mockGroup)

        val result = repository.observeGroup(groupId)

        result.collect { group ->
            assertEquals(groupId, group?.id)
            assertEquals("Test Group", group?.name)
        }
        io.mockk.verify { groupDao.observeGroup(groupId) }
    }

    @Test
    fun observe_contacts_returns_flow_of_contacts() = runTest {
        val mockEntity = com.fiveis.xend.data.database.entity.ContactEntity(
            id = 1L,
            groupId = null,
            name = "Test Contact",
            email = "test@example.com"
        )
        val mockContact = com.fiveis.xend.data.database.ContactWithGroupAndContext(
            contact = mockEntity,
            context = null,
            group = null
        )

        every { contactDao.observeAllWithGroup() } returns kotlinx.coroutines.flow.flowOf(listOf(mockContact))

        val result = repository.observeContacts()

        result.collect { contacts ->
            assertEquals(1, contacts.size)
            assertEquals("Test Contact", contacts[0].name)
        }
        io.mockk.verify { contactDao.observeAllWithGroup() }
    }

    @Test
    fun observe_prompt_options_returns_flow_of_options() = runTest {
        val mockOption = com.fiveis.xend.data.database.entity.PromptOptionEntity(
            id = 1L,
            key = "tone",
            name = "Formal",
            prompt = "Be formal"
        )

        every { optionDao.observeAllOptions() } returns kotlinx.coroutines.flow.flowOf(listOf(mockOption))

        val result = repository.observePromptOptions()

        result.collect { options ->
            assertEquals(1, options.size)
            assertEquals("Formal", options[0].name)
        }
        io.mockk.verify { optionDao.observeAllOptions() }
    }

    @Test
    fun observe_contact_returns_flow_of_single_contact() = runTest {
        val contactId = 1L
        val mockContactEntity = com.fiveis.xend.data.database.entity.ContactEntity(
            id = contactId,
            groupId = 1L,
            name = "Test Contact",
            email = "test@example.com"
        )
        val mockGroupEntity = com.fiveis.xend.data.database.entity.GroupEntity(
            id = 1L,
            name = "Test Group",
            description = "Test Description"
        )
        val mockContactWithGroup = com.fiveis.xend.data.database.ContactWithGroupAndContext(
            contact = mockContactEntity,
            group = mockGroupEntity,
            context = null
        )

        every { contactDao.observeByIdWithGroup(contactId) } returns kotlinx.coroutines.flow.flowOf(mockContactWithGroup)

        val result = repository.observeContact(contactId)

        result.collect { contact ->
            assertEquals(contactId, contact?.id)
            assertEquals("Test Contact", contact?.name)
        }
        io.mockk.verify { contactDao.observeByIdWithGroup(contactId) }
    }

    @Test
    fun refresh_group_updates_single_group_in_database() = runTest {
        val groupId = 1L
        val mockGroup = GroupResponse(
            id = groupId,
            name = "Updated Group",
            description = "Updated Description",
            options = emptyList()
        )

        coEvery { contactApiService.getGroup(groupId) } returns Response.success(mockGroup)
        coEvery { groupDao.upsertGroups(any()) } returns Unit
        coEvery { optionDao.deleteCrossRefsByGroup(groupId) } returns Unit

        repository.refreshGroup(groupId)

        coVerify { contactApiService.getGroup(groupId) }
        coVerify { groupDao.upsertGroups(any()) }
    }

    @Test
    fun refresh_contact_updates_single_contact_in_database() = runTest {
        val contactId = 1L
        val mockContact = ContactResponse(
            id = contactId,
            name = "Updated Contact",
            email = "updated@example.com"
        )

        coEvery { contactApiService.getContact(contactId) } returns Response.success(mockContact)
        coEvery { contactDao.upsertContacts(any()) } returns Unit

        repository.refreshContact(contactId)

        coVerify { contactApiService.getContact(contactId) }
        coVerify { contactDao.upsertContacts(any()) }
    }

    @Test
    fun refresh_group_and_members_refreshes_both_group_and_contacts() = runTest {
        val groupId = 1L
        val mockGroup = GroupResponse(
            id = groupId,
            name = "Test Group",
            description = "Test Description"
        )
        val mockContacts = listOf(
            ContactResponse(
                id = 1L,
                name = "Test Contact",
                email = "test@example.com"
            )
        )

        coEvery { contactApiService.getGroup(groupId) } returns Response.success(mockGroup)
        coEvery { contactApiService.getAllContacts() } returns Response.success(mockContacts)
        coEvery { groupDao.upsertGroups(any()) } returns Unit
        coEvery { optionDao.deleteCrossRefsByGroup(groupId) } returns Unit
        coEvery { contactDao.deleteAllContexts() } returns Unit
        coEvery { contactDao.deleteAllContacts() } returns Unit
        coEvery { contactDao.upsertContacts(any()) } returns Unit

        repository.refreshGroupAndMembers(groupId)

        coVerify { contactApiService.getGroup(groupId) }
        coVerify { contactApiService.getAllContacts() }
    }

    @Test
    fun refresh_prompt_options_updates_options_in_database() = runTest {
        val mockOptions = listOf(
            PromptOption(
                id = 1L,
                key = "tone",
                name = "Formal",
                prompt = "Be formal"
            )
        )

        coEvery { contactApiService.getAllPromptOptions() } returns Response.success(mockOptions)
        coEvery { optionDao.upsertOptions(any()) } returns Unit

        repository.refreshPromptOptions()

        coVerify { contactApiService.getAllPromptOptions() }
        coVerify { optionDao.upsertOptions(any()) }
    }

    @Test
    fun update_contact_group_updates_contact_group_id() = runTest {
        val contactId = 1L
        val groupId = 2L
        val mockGroup = GroupResponse(
            id = groupId,
            name = "Test Group",
            description = null,
            emoji = null
        )
        val mockContact = ContactResponse(
            id = contactId,
            name = "Test",
            email = "test@example.com",
            group = mockGroup
        )

        coEvery {
            contactApiService.updateContact(contactId, mapOf("group_id" to groupId))
        } returns Response.success(mockContact)
        coEvery { contactDao.upsertContacts(any()) } returns Unit

        repository.updateContactGroup(contactId, groupId)

        coVerify { contactApiService.updateContact(contactId, mapOf("group_id" to groupId)) }
        coVerify { contactDao.upsertContacts(any()) }
    }

    @Test
    fun update_group_with_all_params_succeeds() = runTest {
        val groupId = 1L
        val updatedGroup = GroupResponse(
            id = groupId,
            name = "Updated Name",
            description = "Updated Desc",
            emoji = "🔥",
            options = emptyList()
        )

        coEvery { contactApiService.updateGroup(groupId, any()) } returns Response.success(updatedGroup)
        coEvery { groupDao.upsertGroups(any()) } returns Unit
        coEvery { optionDao.deleteCrossRefsByGroup(groupId) } returns Unit

        val result = repository.updateGroup(
            groupId = groupId,
            name = "Updated Name",
            description = "Updated Desc",
            emoji = "🔥",
            emojiProvided = true,
            optionIds = emptyList()
        )

        assertEquals(updatedGroup, result)
        coVerify { contactApiService.updateGroup(groupId, match {
            it["name"] == "Updated Name" &&
            it["description"] == "Updated Desc" &&
            it["emoji"] == "🔥" &&
            it["option_ids"] == emptyList<Long>()
        }) }
    }

    @Test
    fun search_contacts_returns_filtered_flow() = runTest {
        val allContacts = listOf(
            com.fiveis.xend.data.database.ContactWithGroupAndContext(
                contact = com.fiveis.xend.data.database.entity.ContactEntity(1L, null, "Alice", "alice@test.com"),
                group = null,
                context = null
            ),
            com.fiveis.xend.data.database.ContactWithGroupAndContext(
                contact = com.fiveis.xend.data.database.entity.ContactEntity(2L, null, "Bob", "bob@test.com"),
                group = null,
                context = null
            )
        )

        every { contactDao.observeAllWithGroup() } returns flowOf(allContacts)

        val result = repository.searchContacts("ali")

        result.collect { contacts ->
            assertEquals(1, contacts.size)
            assertEquals("Alice", contacts[0].name)
        }
    }

    @Test
    fun contact_data_and_group_data_classes_work() {
        val contact = com.fiveis.xend.data.model.Contact(
            id = 1L,
            name = "John",
            email = "john@example.com"
        )
        val contactData = com.fiveis.xend.data.repository.ContactData(listOf(contact))
        assertEquals(1, contactData.contacts.size)
        assertEquals("John", contactData.contacts[0].name)

        val group = com.fiveis.xend.data.model.Group(
            id = 1L,
            name = "Team",
            description = "Description"
        )
        val groupData = com.fiveis.xend.data.repository.GroupData(listOf(group))
        assertEquals(1, groupData.groups.size)
        assertEquals("Team", groupData.groups[0].name)
    }
}
