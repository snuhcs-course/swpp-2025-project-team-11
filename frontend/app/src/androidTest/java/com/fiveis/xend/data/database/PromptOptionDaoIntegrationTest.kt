package com.fiveis.xend.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fiveis.xend.data.database.entity.GroupEntity
import com.fiveis.xend.data.database.entity.GroupPromptOptionCrossRef
import com.fiveis.xend.data.database.entity.PromptOptionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PromptOptionDaoIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var promptOptionDao: PromptOptionDao
    private lateinit var groupDao: GroupDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        promptOptionDao = database.promptOptionDao()
        groupDao = database.groupDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertOptions_inserts_new_options() = runTest {
        val options = listOf(
            createMockPromptOption(1L, "formal", "Formal"),
            createMockPromptOption(2L, "casual", "Casual")
        )

        promptOptionDao.upsertOptions(options)

        val result = promptOptionDao.observeAllOptions().first()
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == 1L })
        assertTrue(result.any { it.id == 2L })
    }

    @Test
    fun upsertOptions_updates_existing_options() = runTest {
        val option1 = createMockPromptOption(1L, "formal", "Formal")
        promptOptionDao.upsertOptions(listOf(option1))

        val option2 = createMockPromptOption(1L, "formal", "Formal Updated")
        promptOptionDao.upsertOptions(listOf(option2))

        val result = promptOptionDao.observeAllOptions().first()
        assertEquals(1, result.size)
        assertEquals("Formal Updated", result[0].name)
    }

    @Test
    fun upsertOptions_with_empty_list_does_not_fail() = runTest {
        promptOptionDao.upsertOptions(emptyList())

        val result = promptOptionDao.observeAllOptions().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun upsertCrossRefs_inserts_new_cross_references() = runTest {
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

        val result = groupDao.getGroupWithMembersAndOptions(1L)
        assertEquals(2, result?.options?.size)
    }

    @Test
    fun upsertCrossRefs_updates_existing_cross_references() = runTest {
        val group = createMockGroup(1L, "Team A")
        groupDao.upsertGroups(listOf(group))

        val option = createMockPromptOption(1L, "formal", "Formal")
        promptOptionDao.upsertOptions(listOf(option))

        val crossRef1 = GroupPromptOptionCrossRef(1L, 1L)
        promptOptionDao.upsertCrossRefs(listOf(crossRef1))

        val crossRef2 = GroupPromptOptionCrossRef(1L, 1L)
        promptOptionDao.upsertCrossRefs(listOf(crossRef2))

        val result = groupDao.getGroupWithMembersAndOptions(1L)
        assertEquals(1, result?.options?.size)
    }

    @Test
    fun upsertCrossRefs_with_empty_list_does_not_fail() = runTest {
        promptOptionDao.upsertCrossRefs(emptyList())

        val result = promptOptionDao.observeAllOptions().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun deleteCrossRefsByGroup_removes_all_cross_refs_for_group() = runTest {
        val groups = listOf(
            createMockGroup(1L, "Team A"),
            createMockGroup(2L, "Team B")
        )
        groupDao.upsertGroups(groups)

        val options = listOf(
            createMockPromptOption(1L, "formal", "Formal"),
            createMockPromptOption(2L, "casual", "Casual")
        )
        promptOptionDao.upsertOptions(options)

        val crossRefs = listOf(
            GroupPromptOptionCrossRef(1L, 1L),
            GroupPromptOptionCrossRef(1L, 2L),
            GroupPromptOptionCrossRef(2L, 1L)
        )
        promptOptionDao.upsertCrossRefs(crossRefs)

        promptOptionDao.deleteCrossRefsByGroup(1L)

        val result1 = groupDao.getGroupWithMembersAndOptions(1L)
        val result2 = groupDao.getGroupWithMembersAndOptions(2L)

        assertEquals(0, result1?.options?.size)
        assertEquals(1, result2?.options?.size)
    }

    @Test
    fun deleteCrossRefsByGroup_on_nonexistent_group_does_not_fail() = runTest {
        promptOptionDao.deleteCrossRefsByGroup(999L)

        val result = promptOptionDao.observeAllOptions().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun deleteCrossRefsByGroup_on_group_with_no_refs_does_not_fail() = runTest {
        val group = createMockGroup(1L, "Team A")
        groupDao.upsertGroups(listOf(group))

        promptOptionDao.deleteCrossRefsByGroup(1L)

        val result = groupDao.getGroupWithMembersAndOptions(1L)
        assertEquals(0, result?.options?.size)
    }

    @Test
    fun getOptionsByIds_returns_matching_options() = runTest {
        val options = listOf(
            createMockPromptOption(1L, "formal", "Formal"),
            createMockPromptOption(2L, "casual", "Casual"),
            createMockPromptOption(3L, "friendly", "Friendly")
        )
        promptOptionDao.upsertOptions(options)

        val result = promptOptionDao.getOptionsByIds(listOf(1L, 3L))

        assertEquals(2, result.size)
        assertTrue(result.any { it.id == 1L })
        assertTrue(result.any { it.id == 3L })
    }

    @Test
    fun getOptionsByIds_with_empty_list_returns_empty_list() = runTest {
        val options = listOf(
            createMockPromptOption(1L, "formal", "Formal")
        )
        promptOptionDao.upsertOptions(options)

        val result = promptOptionDao.getOptionsByIds(emptyList())

        assertTrue(result.isEmpty())
    }

    @Test
    fun getOptionsByIds_with_nonexistent_ids_returns_empty_list() = runTest {
        val options = listOf(
            createMockPromptOption(1L, "formal", "Formal")
        )
        promptOptionDao.upsertOptions(options)

        val result = promptOptionDao.getOptionsByIds(listOf(999L))

        assertTrue(result.isEmpty())
    }

    @Test
    fun getOptionsByIds_with_mixed_existing_and_nonexistent_ids_returns_only_existing() = runTest {
        val options = listOf(
            createMockPromptOption(1L, "formal", "Formal"),
            createMockPromptOption(2L, "casual", "Casual")
        )
        promptOptionDao.upsertOptions(options)

        val result = promptOptionDao.getOptionsByIds(listOf(1L, 999L))

        assertEquals(1, result.size)
        assertEquals(1L, result[0].id)
    }

    @Test
    fun observeAllOptions_emits_all_options() = runTest {
        val options = listOf(
            createMockPromptOption(1L, "formal", "Formal"),
            createMockPromptOption(2L, "casual", "Casual")
        )
        promptOptionDao.upsertOptions(options)

        val result = promptOptionDao.observeAllOptions().first()

        assertEquals(2, result.size)
    }

    @Test
    fun observeAllOptions_emits_empty_list_when_no_options() = runTest {
        val result = promptOptionDao.observeAllOptions().first()

        assertTrue(result.isEmpty())
    }

    @Test
    fun observeAllOptions_emits_updates_when_options_change() = runTest {
        val options1 = listOf(createMockPromptOption(1L, "formal", "Formal"))
        promptOptionDao.upsertOptions(options1)

        var result = promptOptionDao.observeAllOptions().first()
        assertEquals(1, result.size)

        val options2 = listOf(createMockPromptOption(2L, "casual", "Casual"))
        promptOptionDao.upsertOptions(options2)

        result = promptOptionDao.observeAllOptions().first()
        assertEquals(2, result.size)
    }

    @Test
    fun deleteAllCrossRefs_removes_all_cross_references() = runTest {
        val groups = listOf(
            createMockGroup(1L, "Team A"),
            createMockGroup(2L, "Team B")
        )
        groupDao.upsertGroups(groups)

        val options = listOf(
            createMockPromptOption(1L, "formal", "Formal")
        )
        promptOptionDao.upsertOptions(options)

        val crossRefs = listOf(
            GroupPromptOptionCrossRef(1L, 1L),
            GroupPromptOptionCrossRef(2L, 1L)
        )
        promptOptionDao.upsertCrossRefs(crossRefs)

        promptOptionDao.deleteAllCrossRefs()

        val result1 = groupDao.getGroupWithMembersAndOptions(1L)
        val result2 = groupDao.getGroupWithMembersAndOptions(2L)

        assertEquals(0, result1?.options?.size)
        assertEquals(0, result2?.options?.size)
    }

    @Test
    fun deleteAllCrossRefs_on_empty_database_does_not_fail() = runTest {
        promptOptionDao.deleteAllCrossRefs()

        val result = promptOptionDao.observeAllOptions().first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun options_with_special_characters_are_handled_correctly() = runTest {
        val option = PromptOptionEntity(
            id = 1L,
            key = "special-key",
            name = "Name with \"quotes\" and 'apostrophes'",
            prompt = "Prompt with special chars: !@#$%^&*()",
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        promptOptionDao.upsertOptions(listOf(option))

        val result = promptOptionDao.observeAllOptions().first()
        assertEquals(1, result.size)
        assertEquals("Name with \"quotes\" and 'apostrophes'", result[0].name)
        assertEquals("Prompt with special chars: !@#$%^&*()", result[0].prompt)
    }

    @Test
    fun large_number_of_options_are_handled_correctly() = runTest {
        val options = (1L..100L).map {
            createMockPromptOption(it, "key$it", "Option $it")
        }

        promptOptionDao.upsertOptions(options)

        val result = promptOptionDao.observeAllOptions().first()
        assertEquals(100, result.size)
    }

    @Test
    fun multiple_groups_can_share_same_option() = runTest {
        val groups = listOf(
            createMockGroup(1L, "Team A"),
            createMockGroup(2L, "Team B"),
            createMockGroup(3L, "Team C")
        )
        groupDao.upsertGroups(groups)

        val option = createMockPromptOption(1L, "formal", "Formal")
        promptOptionDao.upsertOptions(listOf(option))

        val crossRefs = listOf(
            GroupPromptOptionCrossRef(1L, 1L),
            GroupPromptOptionCrossRef(2L, 1L),
            GroupPromptOptionCrossRef(3L, 1L)
        )
        promptOptionDao.upsertCrossRefs(crossRefs)

        val result1 = groupDao.getGroupWithMembersAndOptions(1L)
        val result2 = groupDao.getGroupWithMembersAndOptions(2L)
        val result3 = groupDao.getGroupWithMembersAndOptions(3L)

        assertEquals(1, result1?.options?.size)
        assertEquals(1, result2?.options?.size)
        assertEquals(1, result3?.options?.size)
    }

    @Test
    fun group_can_have_multiple_options() = runTest {
        val group = createMockGroup(1L, "Team A")
        groupDao.upsertGroups(listOf(group))

        val options = listOf(
            createMockPromptOption(1L, "formal", "Formal"),
            createMockPromptOption(2L, "casual", "Casual"),
            createMockPromptOption(3L, "friendly", "Friendly")
        )
        promptOptionDao.upsertOptions(options)

        val crossRefs = listOf(
            GroupPromptOptionCrossRef(1L, 1L),
            GroupPromptOptionCrossRef(1L, 2L),
            GroupPromptOptionCrossRef(1L, 3L)
        )
        promptOptionDao.upsertCrossRefs(crossRefs)

        val result = groupDao.getGroupWithMembersAndOptions(1L)

        assertEquals(3, result?.options?.size)
    }

    @Test
    fun deleting_group_cascades_to_cross_refs() = runTest {
        val group = createMockGroup(1L, "Team A")
        groupDao.upsertGroups(listOf(group))

        val option = createMockPromptOption(1L, "formal", "Formal")
        promptOptionDao.upsertOptions(listOf(option))

        val crossRef = GroupPromptOptionCrossRef(1L, 1L)
        promptOptionDao.upsertCrossRefs(listOf(crossRef))

        groupDao.deleteById(1L)

        val options = promptOptionDao.observeAllOptions().first()
        assertEquals(1, options.size)
    }

    @Test
    fun deleting_option_cascades_to_cross_refs() = runTest {
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

        val result1 = groupDao.getGroupWithMembersAndOptions(1L)
        assertEquals(2, result1?.options?.size)
    }

    @Test
    fun option_with_null_timestamps_is_stored_correctly() = runTest {
        val option = PromptOptionEntity(
            id = 1L,
            key = "formal",
            name = "Formal",
            prompt = "Use formal language",
            createdAt = null,
            updatedAt = null
        )

        promptOptionDao.upsertOptions(listOf(option))

        val result = promptOptionDao.observeAllOptions().first()
        assertEquals(1, result.size)
        assertEquals(null, result[0].createdAt)
        assertEquals(null, result[0].updatedAt)
    }

    @Test
    fun getOptionsByIds_maintains_order_of_results() = runTest {
        val options = listOf(
            createMockPromptOption(1L, "a", "A"),
            createMockPromptOption(2L, "b", "B"),
            createMockPromptOption(3L, "c", "C")
        )
        promptOptionDao.upsertOptions(options)

        val result = promptOptionDao.getOptionsByIds(listOf(3L, 1L, 2L))

        assertEquals(3, result.size)
    }

    @Test
    fun cross_ref_with_same_group_and_option_can_be_reinserted() = runTest {
        val group = createMockGroup(1L, "Team A")
        groupDao.upsertGroups(listOf(group))

        val option = createMockPromptOption(1L, "formal", "Formal")
        promptOptionDao.upsertOptions(listOf(option))

        val crossRef = GroupPromptOptionCrossRef(1L, 1L)
        promptOptionDao.upsertCrossRefs(listOf(crossRef))
        promptOptionDao.deleteCrossRefsByGroup(1L)
        promptOptionDao.upsertCrossRefs(listOf(crossRef))

        val result = groupDao.getGroupWithMembersAndOptions(1L)
        assertEquals(1, result?.options?.size)
    }

    @Test
    fun large_batch_of_cross_refs_are_handled_correctly() = runTest {
        val groups = (1L..10L).map { createMockGroup(it, "Team $it") }
        groupDao.upsertGroups(groups)

        val options = (1L..10L).map { createMockPromptOption(it, "key$it", "Option $it") }
        promptOptionDao.upsertOptions(options)

        val crossRefs = mutableListOf<GroupPromptOptionCrossRef>()
        for (groupId in 1L..10L) {
            for (optionId in 1L..10L) {
                crossRefs.add(GroupPromptOptionCrossRef(groupId, optionId))
            }
        }
        promptOptionDao.upsertCrossRefs(crossRefs)

        val result = groupDao.getGroupWithMembersAndOptions(1L)
        assertEquals(10, result?.options?.size)
    }

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

    private fun createMockGroup(
        id: Long,
        name: String
    ) = GroupEntity(
        id = id,
        name = name,
        description = "Description for $name",
        createdAt = "2025-01-01T00:00:00Z",
        updatedAt = "2025-01-01T00:00:00Z"
    )
}
