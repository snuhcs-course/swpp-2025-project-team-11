package com.fiveis.xend.data.repository

import android.content.Context
import com.fiveis.xend.data.database.AppDatabase // <-- Room DB 임포트
import com.fiveis.xend.data.database.ContactDao // <-- DAO 임포트
import com.fiveis.xend.data.database.GroupDao // <-- DAO 임포트
import com.fiveis.xend.data.database.PromptOptionDao // <-- DAO 임포트
import com.fiveis.xend.data.model.ContactResponse
import com.fiveis.xend.data.model.ContactResponseContext
import com.fiveis.xend.data.model.GroupResponse
import com.fiveis.xend.data.model.PromptOption
import com.fiveis.xend.network.ContactApiService
import com.fiveis.xend.network.RetrofitClient
import androidx.room.withTransaction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
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
                    request.context?.senderRole == "Mail writer"
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
            options = options
        )

        coEvery {
            contactApiService.addGroup(any())
        } returns Response.success(expectedResponse)
        coEvery { groupDao.upsertGroups(any()) } returns Unit
        coEvery { optionDao.upsertOptions(any()) } returns Unit
        coEvery { optionDao.deleteCrossRefsByGroup(any()) } returns Unit
        coEvery { optionDao.upsertCrossRefs(any()) } returns Unit

        val result = repository.addGroup(name, description, options)

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
            repository.addGroup("Test", "Description", emptyList())
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
}
