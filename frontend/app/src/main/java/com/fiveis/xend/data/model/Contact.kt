package com.fiveis.xend.data.model

import androidx.compose.ui.graphics.Color

data class Contact(
    val id: Int,
    val groupId: Int? = null,
    val name: String,
    val email: String,
    val context: ContactContext? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val color: Color = Color(0xFF5A7DFF)
)

data class ContactContext(
    val id: Int,
    val relationshipRole: String? = null,
    val relationshipDetails: String? = null,
    val personalPrompt: String? = null,
    val languagePreference: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)
