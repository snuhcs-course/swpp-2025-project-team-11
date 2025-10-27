package com.fiveis.xend.data.model

import com.google.gson.annotations.SerializedName

data class AuthResponse(
    val jwt: JwtTokens? = null,

    // 호환성을 위한 과거 키명 필드
    @SerializedName("access_token")
    val accessToken: String? = null,

    @SerializedName("refresh_token")
    val refreshToken: String? = null
)
