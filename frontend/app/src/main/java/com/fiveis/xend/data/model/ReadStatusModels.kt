package com.fiveis.xend.data.model

import com.google.gson.annotations.SerializedName

data class ReadStatusUpdateRequest(
    @SerializedName("is_read")
    val isRead: Boolean
)

data class ReadStatusUpdateResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("labelIds")
    val labelIds: List<String> = emptyList()
)
