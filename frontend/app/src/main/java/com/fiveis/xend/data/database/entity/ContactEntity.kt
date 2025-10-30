package com.fiveis.xend.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 연락처 엔티티 (DB용)
 * - groupId: 그룹 참조
 */
@Entity(
    tableName = "contacts",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("groupId")]
)
data class ContactEntity(
    @PrimaryKey val id: Long,
    val groupId: Long? = null,
    val name: String,
    val email: String,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * 연락처 컨텍스트, 연락처랑 1:1
 */
@Entity(tableName = "contact_contexts")
data class ContactContextEntity(
    @PrimaryKey val contactId: Long,
    val senderRole: String? = null,
    val recipientRole: String? = null,
    val relationshipDetails: String? = null,
    val personalPrompt: String? = null,
    val languagePreference: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
