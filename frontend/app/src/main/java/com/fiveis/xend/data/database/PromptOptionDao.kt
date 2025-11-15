package com.fiveis.xend.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.fiveis.xend.data.database.entity.GroupPromptOptionCrossRef
import com.fiveis.xend.data.database.entity.PromptOptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PromptOptionDao {
    @Upsert
    suspend fun upsertOptions(options: List<PromptOptionEntity>)

    @Upsert
    suspend fun upsertCrossRefs(refs: List<GroupPromptOptionCrossRef>)

    @Query("DELETE FROM group_prompt_option_cross_ref WHERE groupId = :groupId")
    suspend fun deleteCrossRefsByGroup(groupId: Long)

    @Query("DELETE FROM group_prompt_option_cross_ref WHERE optionId = :optionId")
    suspend fun deleteCrossRefsByOption(optionId: Long)

    @Query("SELECT * FROM prompt_options WHERE id IN (:ids)")
    suspend fun getOptionsByIds(ids: List<Long>): List<PromptOptionEntity>

    @Query("SELECT * FROM prompt_options")
    fun observeAllOptions(): Flow<List<PromptOptionEntity>>

    @Query("DELETE FROM group_prompt_option_cross_ref")
    suspend fun deleteAllCrossRefs()

    @Query("DELETE FROM prompt_options WHERE id = :id")
    suspend fun deleteOptionById(id: Long)
}
