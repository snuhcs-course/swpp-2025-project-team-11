package com.fiveis.xend.data.source

import com.fiveis.xend.data.model.AuthCodeRequest
import com.fiveis.xend.data.model.AuthResponse
import com.fiveis.xend.data.model.LogoutRequest
import com.fiveis.xend.data.model.LogoutResponse
import com.fiveis.xend.data.model.TokenRefreshRequest
import com.fiveis.xend.data.model.TokenRefreshResponse
import retrofit2.Response
import retrofit2.http.Body
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
     * 토큰 리프레시
     */
    @POST("api/user/refresh/")
    suspend fun refreshToken(@Body request: TokenRefreshRequest): Response<TokenRefreshResponse>

    /**
     * 로그아웃
     */
    @POST("api/user/logout/")
    suspend fun logout(@Body request: LogoutRequest): Response<LogoutResponse>
}
