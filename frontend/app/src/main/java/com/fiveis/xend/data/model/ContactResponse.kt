package com.fiveis.xend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContactResponse(
    val id: Long,
    @SerialName("group") val groupId: Long,
    val name: String,
    val email: String,
    val context: ContactResponseContext? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ContactResponseContext(
    val id: Long,
    val relationshipRole: String? = null,
    val relationshipDetails: String? = null,
    val personalPrompt: String? = null,
    val languagePreference: String? = null,
    val createdAt: String,
    val updatedAt: String
)
