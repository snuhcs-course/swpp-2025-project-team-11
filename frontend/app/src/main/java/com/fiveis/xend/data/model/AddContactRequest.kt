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
    @SerialName("relationship_role") val relationshipRole: String? = "",
    @SerialName("relationship_details") val relationshipDetails: String? = "",
    @SerialName("personal_prompt") val personalPrompt: String? = "",
    @SerialName("language_preference") val languagePreference: String? = "KOR"
)
