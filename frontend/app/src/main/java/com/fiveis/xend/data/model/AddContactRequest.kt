package com.fiveis.xend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddContactRequest(
    val name: String,
    val email: String,
    @SerialName("group_id") val groupId: Long? = null,
    val context: AddContactRequestContext? = null
)

@Serializable
data class AddContactRequestContext(
    @SerialName("sender_role") val senderRole: String? = "Mail writer",
    @SerialName("recipient_role") val recipientRole: String? = "",
    @SerialName("relationship_details") val relationshipDetails: String? = "",
    @SerialName("personal_prompt") val personalPrompt: String? = "",
    @SerialName("language_preference") val languagePreference: String? = "KOR"
)
