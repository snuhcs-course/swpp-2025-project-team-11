package com.fiveis.xend.data.model

data class Contact(
    val id: Long,
    val group: Group? = null,
    val name: String,
    val email: String,
    val context: ContactContext? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class ContactContext(
    val id: Long,
    val senderRole: String? = null,
    val recipientRole: String? = null,
    val relationshipDetails: String? = null,
    val personalPrompt: String? = null,
    val languagePreference: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
