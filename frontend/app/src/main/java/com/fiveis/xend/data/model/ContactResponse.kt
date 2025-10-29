package com.fiveis.xend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContactResponse(
    val id: Long,
    @SerialName("group") val group: GroupResponse? = null,
    val name: String,
    val email: String,
    val context: ContactResponseContext? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class ContactResponseContext(
    val id: Long,
    @SerialName("sender_role") val senderRole: String? = null,
    @SerialName("recipient_role") val recipientRole: String? = null,
    @SerialName("relationship_details") val relationshipDetails: String? = null,
    @SerialName("personal_prompt") val personalPrompt: String? = null,
    @SerialName("language_preference") val languagePreference: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

fun ContactResponse.toDomain(): Contact = Contact(
    id = id,
    group = group?.toDomain(),
    name = name,
    email = email,
    context = context?.toDomain(),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ContactResponseContext.toDomain(): ContactContext = ContactContext(
    id = id,
    senderRole = senderRole,
    recipientRole = recipientRole,
    relationshipDetails = relationshipDetails,
    personalPrompt = personalPrompt,
    languagePreference = languagePreference,
    createdAt = createdAt,
    updatedAt = updatedAt
)
