package com.fiveis.xend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContactResponse(
    val id: Long,
    @SerialName("group") val groupId: Long? = null,
    val name: String,
    val email: String,
    val context: ContactResponseContext? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class ContactResponseContext(
    val id: Long,
    @SerialName("relationship_role") val relationshipRole: String? = null,
    @SerialName("relationship_details") val relationshipDetails: String? = null,
    @SerialName("personal_prompt") val personalPrompt: String? = null,
    @SerialName("language_preference") val languagePreference: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)
