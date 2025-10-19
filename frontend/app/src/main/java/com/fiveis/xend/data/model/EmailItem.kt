package com.fiveis.xend.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.fiveis.xend.data.database.Converters
import com.google.gson.annotations.SerializedName

@Entity(tableName = "emails")
@TypeConverters(Converters::class)
data class EmailItem(
    @PrimaryKey
    @SerializedName("id")
    val id: String,

    @SerializedName("thread_id")
    val threadId: String,

    @SerializedName("subject")
    val subject: String,

    @SerializedName("from_email")
    val fromEmail: String,

    @SerializedName("snippet")
    val snippet: String,

    @SerializedName("date")
    val date: String,

    @SerializedName("date_raw")
    val dateRaw: String,

    @SerializedName("is_unread")
    val isUnread: Boolean,

    @SerializedName("label_ids")
    val labelIds: List<String>,

    val cachedAt: Long = System.currentTimeMillis()
)
