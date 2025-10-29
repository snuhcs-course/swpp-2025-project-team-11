package com.fiveis.xend.data.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.fiveis.xend.data.database.entity.GroupEntity

@Dao
interface GroupDao {
    @Upsert
    suspend fun upsertGroups(groups: List<GroupEntity>)

    @Query("SELECT * FROM 'groups'")
    suspend fun getAllGroups(): List<GroupEntity>

    @Transaction
    @Query("SELECT * FROM 'groups'")
    suspend fun getGroupsWithMembersAndOptions(): List<GroupWithMembersAndOptions>

    @Query("DELETE FROM 'groups'")
    suspend fun deleteAllGroups()
}
