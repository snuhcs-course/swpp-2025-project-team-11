package com.fiveis.xend.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.database.entity.ContactEntity
import com.fiveis.xend.data.database.entity.GroupEntity
import com.fiveis.xend.data.database.entity.GroupPromptOptionCrossRef
import com.fiveis.xend.data.database.entity.PromptOptionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GroupDaoIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var groupDao: GroupDao
    private lateinit var contactDao: ContactDao
    private lateinit var promptOptionDao: PromptOptionDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        groupDao = database.groupDao()
        contactDao = database.contactDao()
        promptOptionDao = database.promptOptionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertGroups_inserts_new_groups() = runTest {
        val groups = listOf(
            createMockGroup(1L, "Team A"),
            createMockGroup(2L, "Team B")
        )

        groupDao.upsertGroups(groups)

        val result = groupDao.getAllGroups()
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == 1L })
        assertTrue(result.any { it.id == 2L })
    }

    @Test
    fun upsertGroups_updates_existing_groups() = runTest {
        val group1 = createMockGroup(1L, "Team A")
        groupDao.upsertGroups(listOf(group1))

        val group2 = createMockGroup(1L, "Team A Updated")
        groupDao.upsertGroups(listOf(group2))

        val result = groupDao.getAllGroups()
        assertEquals(1, result.size)
        assertEquals("Team A Updated", result[0].name)
    }

    @Test
    fun upsertGroups_with_empty_list_does_not_fail() = runTest {
        groupDao.upsertGroups(emptyList())

        val result = groupDao.getAllGroups()
        assertTrue(result.isEmpty())
    }

    @Test
    fun getAllGroups_returns_all_groups() = runTest {
        val groups = listOf(
            createMockGroup(1L, "Team A"),
            createMockGroup(2L, "Team B"),
            createMockGroup(3L, "Team C")
        )
        groupDao.upsertGroups(groups)

        val result = groupDao.getAllGroups()

        assertEquals(3, result.size)
        assertEquals("Team A", result.find { it.id == 1L }?.name)
        assertEquals("Team B", result.find { it.id == 2L }?.name)
        assertEquals("Team C", result.find { it.id == 3L }?.name)
    }

    @Test
    fun getAllGroups_returns_empty_list_when_no_groups() = runTest {
        val result = groupDao.getAllGroups()

        assertTrue(result.isEmpty())
    }

    @Test
    fun getGroupsWithMembersAndOptions_returns_groups_with_members() = runTest {
        val group = createMockGroup(1L, "Team A")
        groupDao.upsertGroups(listOf(group))

        val contacts = listOf(
            createMockContact(1L, "John", "john@example.com", groupId = 1L),
            createMockContact(2L, "Jane", "jane@example.com", groupId = 1L)
        )
        contactDao.upsertContacts(contacts)

        val result = groupDao.getGroupsWithMembersAndOptions()

        assertEquals(1, result.size)
        assertEquals(2, result[0].members.size)
        assertTrue(result[0].members.any { it.contact.name == "John" })
        assertTrue(result[0].members.any { it.contact.name == "Jane" })
    }

    @Test
    fun getGroupsWithMembersAndOptions_returns_groups_with_options() = runTest {
        val group = createMockGroup(1L, "Team A")
        groupDao.upsertGroups(listOf(group))

        val options = listOf(
            createMockPromptOption(1L, "formal", "Formal"),
            createMockPromptOption(2L, "casual", "Casual")
        )
        promptOptionDao.upsertOptions(options)

        val crossRefs = listOf(
            GroupPromptOptionCrossRef(1L, 1L),
            GroupPromptOptionCrossRef(1L, 2L)
        )
        promptOptionDao.upsertCrossRefs(crossRefs)

        val result = groupDao.getGroupsWithMembersAndOptions()

        assertEquals(1, result.size)
        assertEquals(2, result[0].options.size)
        assertTrue(result[0].options.any { it.key == "formal" })
        assertTrue(result[0].options.any { it.key == "casual" })
    }

    @Test
    fun getGroupsWithMembersAndOptions_returns_groups_with_members_and_options() = runTest {
        val group = createMockGroup(1L, "Team A")
        groupDao.upsertGroups(listOf(group))

        val contacts = listOf(
            createMockContact(1L, "John", "john@example.com", groupId = 1L)
        )
        contactDao.upsertContacts(contacts)

        val options = listOf(
            createMockPromptOption(1L, "formal", "Formal")
        )
        promptOptionDao.upsertOptions(options)
        promptOptionDao.upsertCrossRefs(listOf(GroupPromptOptionCrossRef(1L, 1L)))

        val result = groupDao.getGroupsWithMembersAndOptions()

        assertEquals(1, result.size)
        assertEquals(1, result[0].members.size)
        assertEquals(1, result[0].options.size)
    }

    @Test
    fun getGroupsWithMembersAndOptions_returns_empty_list_when_no_groups() = runTest {
        val result = groupDao.getGroupsWithMembersAndOptions()

        assertTrue(result.isEmpty())
    }

    @Test
    fun getGroupWithMembersAndOptions_returns_specific_group() = runTest {
        val groups = listOf(
            createMockGroup(1L, "Team A"),
            createMockGroup(2L, "Team B")
        )
        groupDao.upsertGroups(groups)

        val result = groupDao.getGroupWithMembersAndOptions(1L)

        assertNotNull(result)
        assertEquals(1L, result?.group?.id)
        assertEquals("Team A", result?.group?.name)
    }

    @Test
    fun getGroupWithMembersAndOptions_returns_null_when_group_not_found() = runTest {
        val result = groupDao.getGroupWithMembersAndOptions(999L)

        assertNull(result)
    }

    @Test
    fun getGroupWithMembersAndOptions_includes_members() = runTest {
        val group = createMockGroup(1L, "Team A")
        groupDao.upsertGroups(listOf(group))

        val contacts = listOf(
            createMockContact(1L, "John", "john@example.com", groupId = 1L),
            createMockContact(2L, "Jane", "jane@example.com", groupId = 1L)
        )
        contactDao.upsertContacts(contacts)

        val result = groupDao.getGroupWithMembersAndOptions(1L)

        assertNotNull(result)
        assertEquals(2, result?.members?.size)
    }

    @Test
    fun getGroupWithMembersAndOptions_includes_options() = runTest {
        val group = createMockGroup(1L, "Team A")
        groupDao.upsertGroups(listOf(group))

        val options = listOf(
            createMockPromptOption(1L, "formal", "Formal")
        )
        promptOptionDao.upsertOptions(options)
        promptOptionDao.upsertCrossRefs(listOf(GroupPromptOptionCrossRef(1L, 1L)))

        val result = groupDao.getGroupWithMembersAndOptions(1L)

        assertNotNull(result)
        assertEquals(1, result?.options?.size)
    }

    @Test
    fun observeGroupsWithMembersAndOptions_emits_groups() = runTest {
        val groups = listOf(
            createMockGroup(1L, "Team A"),
            createMockGroup(2L, "Team B")
        )
        groupDao.upsertGroups(groups)

        val result = groupDao.observeGroupsWithMembersAndOptions().first()

        assertEquals(2, result.size)
    }

    @Test
    fun observeGroupsWithMembersAndOptions_emits_updates_when_groups_change() = runTest {
        val groups1 = listOf(createMockGroup(1L, "Team A"))
        groupDao.upsertGroups(groups1)

        var result = groupDao.observeGroupsWithMembersAndOptions().first()
        assertEquals(1, result.size)

        val groups2 = listOf(createMockGroup(2L, "Team B"))
        groupDao.upsertGroups(groups2)

        result = groupDao.observeGroupsWithMembersAndOptions().first()
        assertEquals(2, result.size)
    }

    @Test
    fun observeGroupsWithMembersAndOptions_emits_updates_when_members_change() = runTest {
        val group = createMockGroup(1L, "Team A")
        groupDao.upsertGroups(listOf(group))

        var result = groupDao.observeGroupsWithMembersAndOptions().first()
        assertEquals(0, result[0].members.size)

        val contacts = listOf(createMockContact(1L, "John", "john@example.com", groupId = 1L))
        contactDao.upsertContacts(contacts)

        result = groupDao.observeGroupsWithMembersAndOptions().first()
        assertEquals(1, result[0].members.size)
    }

    @Test
    fun observeGroup_emits_specific_group() = runTest {
        val group = createMockGroup(1L, "Team A")
        groupDao.upsertGroups(listOf(group))

        val result = groupDao.observeGroup(1L).first()

        assertNotNull(result)
        assertEquals(1L, result?.group?.id)
    }

    @Test
    fun observeGroup_emits_null_when_group_not_found() = runTest {
        val result = groupDao.observeGroup(999L).first()

        assertNull(result)
    }

    @Test
    fun observeGroup_emits_updates_when_group_changes() = runTest {
        val group1 = createMockGroup(1L, "Team A")
        groupDao.upsertGroups(listOf(group1))

        var result = groupDao.observeGroup(1L).first()
        assertEquals("Team A", result?.group?.name)

        val group2 = createMockGroup(1L, "Team A Updated")
        groupDao.upsertGroups(listOf(group2))

        result = groupDao.observeGroup(1L).first()
        assertEquals("Team A Updated", result?.group?.name)
    }

    @Test
    fun deleteAllGroups_removes_all_groups() = runTest {
        val groups = listOf(
            createMockGroup(1L, "Team A"),
            createMockGroup(2L, "Team B")
        )
        groupDao.upsertGroups(groups)

        groupDao.deleteAllGroups()

        val result = groupDao.getAllGroups()
        assertTrue(result.isEmpty())
    }

    @Test
    fun deleteAllGroups_on_empty_database_does_not_fail() = runTest {
        groupDao.deleteAllGroups()

        val result = groupDao.getAllGroups()
        assertTrue(result.isEmpty())
    }

    @Test
    fun deleteById_removes_specific_group() = runTest {
        val groups = listOf(
            createMockGroup(1L, "Team A"),
            createMockGroup(2L, "Team B")
        )
        groupDao.upsertGroups(groups)

        groupDao.deleteById(1L)

        val result = groupDao.getAllGroups()
        assertEquals(1, result.size)
        assertEquals(2L, result[0].id)
    }

    @Test
    fun deleteById_on_nonexistent_group_does_not_fail() = runTest {
        groupDao.deleteById(999L)

        val result = groupDao.getAllGroups()
        assertTrue(result.isEmpty())
    }

    @Test
    fun deleteById_sets_contacts_groupId_to_null_due_to_foreign_key() = runTest {
        val group = createMockGroup(1L, "Team A")
        groupDao.upsertGroups(listOf(group))

        val contact = createMockContact(1L, "John", "john@example.com", groupId = 1L)
        contactDao.upsertContacts(listOf(contact))

        groupDao.deleteById(1L)

        val contacts = contactDao.getAllContacts()
        assertEquals(1, contacts.size)
        assertNull(contacts[0].groupId)
    }

    @Test
    fun deleteById_removes_associated_cross_refs_due_to_cascade() = runTest {
        val group = createMockGroup(1L, "Team A")
        groupDao.upsertGroups(listOf(group))

        val option = createMockPromptOption(1L, "formal", "Formal")
        promptOptionDao.upsertOptions(listOf(option))
        promptOptionDao.upsertCrossRefs(listOf(GroupPromptOptionCrossRef(1L, 1L)))

        groupDao.deleteById(1L)

        val result = groupDao.getGroupWithMembersAndOptions(1L)
        assertNull(result)
    }

    @Test
    fun groups_with_special_characters_are_handled_correctly() = runTest {
        val group = createMockGroup(
            1L,
            "Team \"A\" & B's Group",
            "Description with special chars: !@#$%^&*()"
        )

        groupDao.upsertGroups(listOf(group))

        val result = groupDao.getAllGroups()
        assertEquals(1, result.size)
        assertEquals("Team \"A\" & B's Group", result[0].name)
        assertEquals("Description with special chars: !@#$%^&*()", result[0].description)
    }

    @Test
    fun large_number_of_groups_are_handled_correctly() = runTest {
        val groups = (1L..100L).map {
            createMockGroup(it, "Team $it")
        }

        groupDao.upsertGroups(groups)

        val result = groupDao.getAllGroups()
        assertEquals(100, result.size)
    }

    @Test
    fun group_with_many_members_is_handled_correctly() = runTest {
        val group = createMockGroup(1L, "Large Team")
        groupDao.upsertGroups(listOf(group))

        val contacts = (1L..50L).map {
            createMockContact(it, "Contact $it", "contact$it@example.com", groupId = 1L)
        }
        contactDao.upsertContacts(contacts)

        val result = groupDao.getGroupWithMembersAndOptions(1L)

        assertNotNull(result)
        assertEquals(50, result?.members?.size)
    }

    @Test
    fun group_with_many_options_is_handled_correctly() = runTest {
        val group = createMockGroup(1L, "Team A")
        groupDao.upsertGroups(listOf(group))

        val options = (1L..20L).map {
            createMockPromptOption(it, "key$it", "Option $it")
        }
        promptOptionDao.upsertOptions(options)

        val crossRefs = (1L..20L).map {
            GroupPromptOptionCrossRef(1L, it)
        }
        promptOptionDao.upsertCrossRefs(crossRefs)

        val result = groupDao.getGroupWithMembersAndOptions(1L)

        assertNotNull(result)
        assertEquals(20, result?.options?.size)
    }

    @Test
    fun group_with_null_description_is_stored_correctly() = runTest {
        val group = GroupEntity(
            id = 1L,
            name = "Team A",
            description = null,
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        groupDao.upsertGroups(listOf(group))

        val result = groupDao.getAllGroups()
        assertEquals(1, result.size)
        assertNull(result[0].description)
    }

    @Test
    fun multiple_groups_with_shared_options_work_correctly() = runTest {
        val groups = listOf(
            createMockGroup(1L, "Team A"),
            createMockGroup(2L, "Team B")
        )
        groupDao.upsertGroups(groups)

        val option = createMockPromptOption(1L, "formal", "Formal")
        promptOptionDao.upsertOptions(listOf(option))

        val crossRefs = listOf(
            GroupPromptOptionCrossRef(1L, 1L),
            GroupPromptOptionCrossRef(2L, 1L)
        )
        promptOptionDao.upsertCrossRefs(crossRefs)

        val result1 = groupDao.getGroupWithMembersAndOptions(1L)
        val result2 = groupDao.getGroupWithMembersAndOptions(2L)

        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals(1, result1?.options?.size)
        assertEquals(1, result2?.options?.size)
    }

    @Test
    fun group_without_members_or_options_returns_empty_lists() = runTest {
        val group = createMockGroup(1L, "Empty Team")
        groupDao.upsertGroups(listOf(group))

        val result = groupDao.getGroupWithMembersAndOptions(1L)

        assertNotNull(result)
        assertTrue(result?.members?.isEmpty() ?: false)
        assertTrue(result?.options?.isEmpty() ?: false)
    }

    private fun createMockGroup(
        id: Long,
        name: String,
        description: String = "Description for $name"
    ) = GroupEntity(
        id = id,
        name = name,
        description = description,
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z"
    )

    private fun createMockContact(
        id: Long,
        name: String,
        email: String,
        groupId: Long? = null
    ) = ContactEntity(
        id = id,
        groupId = groupId,
        name = name,
        email = email,
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z"
    )

    private fun createMockPromptOption(
        id: Long,
        key: String,
        name: String
    ) = PromptOptionEntity(
        id = id,
        key = key,
        name = name,
        prompt = "Prompt for $name",
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z"
    )
}
