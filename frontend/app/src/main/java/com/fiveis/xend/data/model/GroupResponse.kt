package com.fiveis.xend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupResponse(
    val id: Long,
    val name: String,
    val description: String? = null,
    val options: List<PromptOption> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

fun GroupResponse.toDomain(): Group = Group(
    id = id,
    name = name,
    description = description,
    options = options,
    createdAt = createdAt,
    updatedAt = updatedAt
)
