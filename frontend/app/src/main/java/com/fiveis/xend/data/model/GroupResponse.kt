package com.fiveis.xend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupResponse(
    val id: Long,
    val name: String,
    val description: String? = null,
    val options: List<PromptOption> = emptyList(),
    val contacts: List<ContactResponse>? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

fun GroupResponse.toDomain(): Group = Group(
    id = id,
    name = name,
    description = description,
    options = options,
    members = contacts?.map { contactResponse ->
        Contact(
            id = contactResponse.id,
            // 순환 참조 방지
            group = null,
            name = contactResponse.name,
            email = contactResponse.email,
            context = contactResponse.context?.toDomain(),
            createdAt = contactResponse.createdAt,
            updatedAt = contactResponse.updatedAt
        )
    } ?: emptyList(),
    createdAt = createdAt,
    updatedAt = updatedAt
)
