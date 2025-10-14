package com.fiveis.xend.data.model

import com.google.gson.annotations.SerializedName

data class TokenRefreshRequest(
    @SerializedName("refresh") val refreshToken: String
)
