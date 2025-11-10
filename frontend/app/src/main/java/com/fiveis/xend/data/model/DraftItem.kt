package com.fiveis.xend.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drafts")
data class DraftItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val subject: String,
    val body: String,
    val recipients: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
