package com.fiveis.xend.data.model

import com.google.gson.annotations.SerializedName

data class ProfileData(
    @SerializedName("display_name")
    val displayName: String? = null,
    val info: String? = null
)
