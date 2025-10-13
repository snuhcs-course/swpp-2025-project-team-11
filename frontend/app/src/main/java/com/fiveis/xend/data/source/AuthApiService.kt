package com.fiveis.xend.data.source

import com.fiveis.xend.data.model.AuthCodeRequest
import com.fiveis.xend.data.model.AuthResponse
import com.fiveis.xend.data.model.LogoutRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Retrofit API 서비스 인터페이스
 */
interface AuthApiService {
    /**
     * 구글 로그인 콜백
     */
    @POST("api/user/google/callback/")
    suspend fun sendAuthCode(@Body request: AuthCodeRequest): Response<AuthResponse>

    /**
     * 로그아웃
     */
    @POST("/user/logout/")
    suspend fun logout(@Header("Authorization") accessToken: String, @Body request: LogoutRequest): Response<Unit>
}
