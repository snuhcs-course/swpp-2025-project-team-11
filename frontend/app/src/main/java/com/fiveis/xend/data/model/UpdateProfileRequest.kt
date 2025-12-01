package com.fiveis.xend.data.model

import com.google.gson.annotations.SerializedName

data class UpdateProfileRequest(
    @SerializedName("display_name")
    val displayName: String? = null,
    val info: String? = null,
    @SerializedName("language_preference")
    val languagePreference: String? = null
)
