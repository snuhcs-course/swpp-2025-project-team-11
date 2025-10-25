package com.fiveis.xend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddContactRequest(
    val name: String,
    val email: String,
    @SerialName("group_id") val groupId: Long = 0,
    val context: AddContactRequestContext? = null
)

@Serializable
data class AddContactRequestContext(
    @SerialName("relationship_role") val relationshipRole: String? = null,
    @SerialName("relationship_details") val relationshipDetails: String? = null,
    @SerialName("personal_prompt") val personalPrompt: String? = null,
    @SerialName("language_preference") val languagePreference: String? = "Korean"
)
