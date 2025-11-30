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
    @SerialName("sender_role") val senderRole: String = "",
    @SerialName("recipient_role") val recipientRole: String = "",
    @SerialName("relationship_details") val relationshipDetails: String = "",
    @SerialName("personal_prompt") val personalPrompt: String = "",
    @SerialName("language_preference") val languagePreference: String = ""
)

class ContactRequestBuilder {
    private var name: String? = null
    private var email: String? = null
    private var groupId: Long? = null
    private var senderRole: String? = null
    private var recipientRole: String? = null
    private var relationshipDetails: String? = null
    private var personalPrompt: String? = null
    private var languagePreference: String? = null

    fun name(value: String) = apply { name = value.trim() }
    fun email(value: String) = apply { email = value.trim() }
    fun groupId(value: Long?) = apply { groupId = value }
    fun senderRole(value: String?) = apply { senderRole = value?.trim() }
    fun recipientRole(value: String?) = apply { recipientRole = value?.trim() }
    fun relationshipDetails(value: String?) = apply { relationshipDetails = value?.trim() }
    fun personalPrompt(value: String?) = apply { personalPrompt = value?.trim() }
    fun languagePreference(value: String?) = apply { languagePreference = value?.trim() }

    fun build(): AddContactRequest {
        val finalName = name?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("Contact name is required")
        val finalEmail = email?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("Contact email is required")

        val context = if (
            senderRole.isNullOrBlank() &&
            recipientRole.isNullOrBlank() &&
            relationshipDetails.isNullOrBlank() &&
            personalPrompt.isNullOrBlank() &&
            languagePreference.isNullOrBlank()
        ) {
            null
        } else {
            AddContactRequestContext(
                senderRole = senderRole ?: "",
                recipientRole = recipientRole ?: "",
                relationshipDetails = relationshipDetails ?: "",
                personalPrompt = personalPrompt ?: "",
                languagePreference = languagePreference ?: ""
            )
        }

        return AddContactRequest(
            name = finalName,
            email = finalEmail,
            groupId = groupId,
            context = context
        )
    }
}
