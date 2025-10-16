package com.fiveis.xend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddContactRequest(
    @SerialName("group") val groupId: Long = 0,
    val name: String,
    val email: String,
    val context: AddContactRequestContext? = null
)

@Serializable
data class AddContactRequestContext(
    @SerialName("relationship_role") val relationshipRole: String? = null,
    @SerialName("relationship_details") val relationshipDetails: String? = null,
    @SerialName("personal_prompt") val personalPrompt: String? = null,
    @SerialName("language_preference") val languagePreference: String? = null
)
