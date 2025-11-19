package com.fiveis.xend.data.model

data class Group(
    val id: Long,
    val name: String,
    val description: String? = null,
    val emoji: String? = "",
    val options: List<PromptOption> = emptyList(),
    val members: List<Contact> = emptyList(),
    val createdAt: String? = null,
    val updatedAt: String? = null
)
