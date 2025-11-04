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

        return runBlocking {
            val tokenResponse = RetrofitClient.authApiService.refreshToken(TokenRefreshRequest(refreshToken))

            if (tokenResponse.isSuccessful && tokenResponse.body() != null) {
                val newTokens = tokenResponse.body()!!
                tokenManager.saveTokens(newTokens.accessToken, newTokens.refreshToken, tokenManager.getUserEmail()!!)
                val fullToken = "Bearer ${newTokens.accessToken}"
                android.util.Log.d("TokenAuthenticator", "Refreshed token added to header: $fullToken")
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
