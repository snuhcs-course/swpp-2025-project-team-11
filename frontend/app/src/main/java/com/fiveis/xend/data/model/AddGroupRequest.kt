package com.fiveis.xend.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AddGroupRequest(
    val name: String,
    val description: String
)
