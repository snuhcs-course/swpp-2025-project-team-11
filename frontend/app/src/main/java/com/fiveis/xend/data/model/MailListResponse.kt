package com.fiveis.xend.data.model

import com.google.gson.annotations.SerializedName

data class MailListResponse(
    @SerializedName("messages")
    val messages: List<EmailItem>,

    @SerializedName("next_page_token")
    val nextPageToken: String?,

    @SerializedName("result_size_estimate")
    val resultSizeEstimate: Int
)
