package com.fiveis.xend.data.database

import androidx.room.TypeConverter
import com.fiveis.xend.data.model.Attachment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun fromAttachmentList(value: List<Attachment>?): String {
        return gson.toJson(value ?: emptyList<Attachment>())
    }

    @TypeConverter
    fun toAttachmentList(value: String): List<Attachment> {
        if (value.isBlank()) return emptyList()
        val listType = object : TypeToken<List<Attachment>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
}
