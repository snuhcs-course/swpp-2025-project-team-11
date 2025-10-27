package com.fiveis.xend.data.model

import com.google.gson.annotations.SerializedName

data class AuthCodeRequest(
    @SerializedName("auth_code")
    val authCode: String
)
