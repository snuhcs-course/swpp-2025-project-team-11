package com.fiveis.xend.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class PromptOption(
    val id: Long,
    val key: String,
    val name: String,
    val prompt: String,
    @SerialName("created_at")val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) : Parcelable

@Serializable
data class PromptOptionRequest(
    val key: String,
    val name: String,
    val prompt: String
)
