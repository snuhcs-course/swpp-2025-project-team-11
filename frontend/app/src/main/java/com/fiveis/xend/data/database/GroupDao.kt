package com.fiveis.xend.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.fiveis.xend.data.database.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Upsert
    suspend fun upsertGroups(groups: List<GroupEntity>)

    @Query("SELECT * FROM `groups`")
    suspend fun getAllGroups(): List<GroupEntity>

    @Transaction
    @Query("SELECT * FROM `groups`")
    suspend fun getGroupsWithMembersAndOptions(): List<GroupWithMembersAndOptions>

    @Transaction
    @Query("SELECT * FROM `groups` WHERE id = :groupId")
    suspend fun getGroupWithMembersAndOptions(groupId: Long): GroupWithMembersAndOptions?

    @Transaction
    @Query("SELECT * FROM `groups`")
    fun observeGroupsWithMembersAndOptions(): Flow<List<GroupWithMembersAndOptions>>

    @Transaction
    @Query("SELECT * FROM `groups` WHERE id = :groupId")
    fun observeGroup(groupId: Long): Flow<GroupWithMembersAndOptions?>

    // 그룹 이름, 그룹 설명 수정
    @Query("UPDATE `groups` SET name = :name, description = :description WHERE id = :groupId")
    suspend fun updateGroupInfo(groupId: Long, name: String, description: String)

    @Query("DELETE FROM `groups`")
    suspend fun deleteAllGroups()

    @Query("DELETE FROM `groups` WHERE id = :groupId")
    suspend fun deleteById(groupId: Long)
}
