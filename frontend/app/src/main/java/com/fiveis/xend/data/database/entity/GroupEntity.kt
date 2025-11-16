package com.fiveis.xend.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 그룹 엔티티 (DB용)
 * - options, members 필드는 저장하지 않음
 */
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val description: String? = null,
    val emoji: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
