package com.fiveis.xend.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prompt_options")
data class PromptOptionEntity(
    @PrimaryKey val id: Long,
    val key: String,
    val name: String,
    val prompt: String,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
