package com.fiveis.xend.data.model

import com.google.gson.annotations.SerializedName

data class TokenRefreshResponse(
    @SerializedName("access") val accessToken: String,
    @SerializedName("refresh") val refreshToken: String
)
