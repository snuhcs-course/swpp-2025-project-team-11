package com.fiveis.xend.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 0,
    val displayName: String? = null,
    val info: String? = null,
    val languagePreference: String? = null
)
