package com.fiveis.xend.data.model

import androidx.compose.ui.graphics.Color

data class Group(
    val id: Long,
    val name: String,
    val description: String,
    val promptOptions: List<PromptOption> = emptyList(),
    val members: List<Contact> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val color: Color = Color(0xFF5A7DFF)
)
