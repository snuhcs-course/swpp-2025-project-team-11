package com.fiveis.xend.data.source

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 토큰 저장/로드를 위한 EncryptedSharedPreferences 관리
 */
class TokenManager(context: Context) {

    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Access Token 조회
     */
    fun getAccessToken(): String? {
        val token = encryptedPrefs.getString("access_token", null)
        Log.d("TokenManager", "Access Token 조회: ${token?.take(20) ?: "(없음)"}...")
        return token
    }

    /**
     * Refresh Token 조회
     */
    fun getRefreshToken(): String? = encryptedPrefs.getString("refresh_token", null)

    /**
     * 사용자 이메일 조회
     */
    fun getUserEmail(): String? = encryptedPrefs.getString("user_email", null)

    /**
     * 토큰 저장
     */
    fun saveTokens(access: String?, refresh: String?, email: String) {
        encryptedPrefs.edit().apply {
            if (!access.isNullOrEmpty()) putString("access_token", access)
            if (!refresh.isNullOrEmpty()) putString("refresh_token", refresh)
            putString("user_email", email)
            apply()
        }
        Log.d("TokenManager", "✅ 토큰 저장 완료")
        Log.d("TokenManager", "Access Token: ${access?.take(20) ?: "(없음)"}...")
        Log.d("TokenManager", "Refresh Token: ${refresh?.take(20) ?: "(없음)"}...")
    }

    /**
     * Access Token만 갱신 (refresh 시나리오용)
     */
    fun saveAccessToken(accessToken: String) {
        encryptedPrefs.edit().apply {
            putString("access_token", accessToken)
            apply()
        }
        Log.d("TokenManager", "✅ Access Token 갱신 완료: ${accessToken.take(20)}...")
    }

    /**
     * Refresh Token만 갱신 (token rotation 시나리오용)
     */
    fun saveRefreshToken(refreshToken: String) {
        encryptedPrefs.edit().apply {
            putString("refresh_token", refreshToken)
            apply()
        }
        Log.d("TokenManager", "✅ Refresh Token 갱신 완료: ${refreshToken.take(20)}...")
    }

    /**
     * 모든 토큰 삭제
     */
    fun clearTokens() {
        encryptedPrefs.edit().apply {
            remove("access_token")
            remove("refresh_token")
            remove("user_email")
            apply()
        }
        Log.d("TokenManager", "🗑️ 모든 토큰 삭제 완료")
    }

    /**
     * 로그인 상태 확인
     */
    fun isLoggedIn(): Boolean {
        val accessToken = getAccessToken()
        val email = getUserEmail()
        return !accessToken.isNullOrEmpty() && !email.isNullOrEmpty()
    }
}
