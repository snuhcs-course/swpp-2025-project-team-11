package com.fiveis.xend.data.model

import androidx.room.ColumnInfo
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

    @SerializedName("to_email")
    val toEmail: String = "",

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

    @ColumnInfo(defaultValue = "")
    @SerializedName("body")
    val body: String = "",

    @ColumnInfo(defaultValue = "[]")
    @SerializedName("attachments")
    val attachments: List<Attachment> = emptyList(),

    val cachedAt: Long = System.currentTimeMillis(),

    /**
     * Epoch timestamp in milliseconds for proper chronological sorting.
     * Parsed from dateRaw during insertion.
     */
    @ColumnInfo(defaultValue = "0")
    val dateTimestamp: Long = 0L
)
