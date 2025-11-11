package com.fiveis.xend.network

import android.content.Context
import android.content.Intent
import com.fiveis.xend.data.model.TokenRefreshRequest
import com.fiveis.xend.data.source.TokenManager
import com.fiveis.xend.ui.login.MainActivity
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenRefreshAuthenticator(private val context: Context, private val tokenManager: TokenManager) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        val refreshToken = tokenManager.getRefreshToken() ?: return null

        // 토큰 갱신 시작 로그
        val currentTime = java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS",
            java.util.Locale.getDefault()
        ).format(java.util.Date())
        android.util.Log.d("TokenAuthenticator", "========== 토큰 갱신 시작 ==========")
        android.util.Log.d("TokenAuthenticator", "현재 시각: $currentTime")
        android.util.Log.d("TokenAuthenticator", "사용할 Refresh Token: ${refreshToken.take(30)}...")
        android.util.Log.d("TokenAuthenticator", "만료된 Access Token: ${tokenManager.getAccessToken()?.take(30)}...")

        return runBlocking {
            val tokenResponse = RetrofitClient.authApiService.refreshToken(TokenRefreshRequest(refreshToken))

            if (tokenResponse.isSuccessful && tokenResponse.body() != null) {
                val newTokens = tokenResponse.body()!!

                // 토큰 갱신 성공 로그
                val successTime = java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss.SSS",
                    java.util.Locale.getDefault()
                ).format(java.util.Date())
                android.util.Log.d("TokenAuthenticator", "✅ 토큰 갱신 성공")
                android.util.Log.d("TokenAuthenticator", "갱신 완료 시각: $successTime")
                android.util.Log.d("TokenAuthenticator", "새 Access Token: ${newTokens.accessToken.take(30)}...")
                android.util.Log.d("TokenAuthenticator", "새 Refresh Token: ${newTokens.refreshToken.take(30)}...")
                android.util.Log.d("TokenAuthenticator", "==========================================")

                tokenManager.saveTokens(newTokens.accessToken, newTokens.refreshToken, tokenManager.getUserEmail()!!)
                val fullToken = "Bearer ${newTokens.accessToken}"
                response.request.newBuilder()
                    .header("Authorization", fullToken)
                    .build()
            } else {
                tokenManager.clearTokens()
                // Navigate to login screen
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                context.startActivity(intent)
                null
            }
        }
    }
}
