package com.fiveis.xend.data.repository

import android.content.Context
import android.util.Log
import com.fiveis.xend.data.model.AuthCodeRequest
import com.fiveis.xend.data.model.LogoutRequest
import com.fiveis.xend.data.source.AuthApiService
import com.fiveis.xend.network.RetrofitClient

/**
 * 인증 관련 Repository
 */
class AuthRepository(private val context: Context? = null) {

    private val apiService: AuthApiService = RetrofitClient.authApiService

    /**
     * Auth Code를 서버로 전송하고 JWT 토큰 받기
     */
    suspend fun sendAuthCodeToServer(authCode: String): AuthResult {
        return try {
            val response = apiService.sendAuthCode(AuthCodeRequest(authCode = authCode))

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val accessFromJwt = body.jwt?.access
                    val refreshFromJwt = body.jwt?.refresh

                    if (!accessFromJwt.isNullOrEmpty() || !refreshFromJwt.isNullOrEmpty()) {
                        AuthResult.Success(
                            accessToken = accessFromJwt,
                            refreshToken = refreshFromJwt
                        )
                    } else if (!body.accessToken.isNullOrEmpty() || !body.refreshToken.isNullOrEmpty()) {
                        AuthResult.Success(
                            accessToken = body.accessToken,
                            refreshToken = body.refreshToken
                        )
                    } else {
                        AuthResult.Failure("서버 응답에 토큰이 없습니다: ${response.body()}")
                    }
                } else {
                    AuthResult.Failure("서버로부터 비어있는 응답을 받았습니다.")
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"
                AuthResult.Failure("서버 오류 (HTTP ${response.code()}):\n$errorBody")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "서버 연동 실패", e)
            AuthResult.Failure("서버 연동 실패: ${e.message ?: "알 수 없음"}")
        }
    }

    /**
     * 서버에 로그아웃 요청
     */
    suspend fun logout(refreshToken: String?): LogoutResult {
        if (refreshToken.isNullOrEmpty()) {
            return LogoutResult.Failure("Refresh token이 없습니다")
        }

        // context가 있으면 인증이 포함된 API 서비스 사용
        val logoutApiService = if (context != null) {
            RetrofitClient.getAuthApiService(context)
        } else {
            apiService
        }

        return try {
            val requestBody = LogoutRequest(refresh = refreshToken)
            val response = logoutApiService.logout(requestBody)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.detail == "Successfully logged out") {
                    LogoutResult.Success
                } else {
                    LogoutResult.Failure("로그아웃 응답이 올바르지 않습니다")
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"
                LogoutResult.Failure("서버 오류 (HTTP ${response.code()}): $errorBody")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "로그아웃 요청 실패", e)
            LogoutResult.Failure("로그아웃 요청 실패: ${e.message}")
        }
    }
}

/**
 * 인증 결과
 */
sealed class AuthResult {
    data class Success(
        val accessToken: String?,
        val refreshToken: String?
    ) : AuthResult()

    data class Failure(val message: String) : AuthResult()
}

/**
 * 로그아웃 결과
 */
sealed class LogoutResult {
    object Success : LogoutResult()
    data class Failure(val message: String) : LogoutResult()
}
