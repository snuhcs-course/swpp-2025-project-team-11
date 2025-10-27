package com.fiveis.xend.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddGroupRequest(
    val name: String,
    val description: String,
    @SerialName("option_ids") val optionIds: List<Long> = emptyList()
)
