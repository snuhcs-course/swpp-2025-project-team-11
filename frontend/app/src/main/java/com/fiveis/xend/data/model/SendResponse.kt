package com.fiveis.xend.data.model

import com.google.gson.annotations.SerializedName

data class SendResponse(
    @SerializedName("id")
    val id: String,
    @SerializedName("threadId")
    val threadId: String?,
    @SerializedName("labelIds")
    val labelIds: List<String>
)
