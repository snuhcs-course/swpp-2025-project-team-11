package com.fiveis.xend.data.repository

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 인증 관련 Repository
 */
class AuthRepository {

    companion object {
        private const val SERVER_BASE_URL = "http://10.0.2.2:8008"
        private const val AUTH_CALLBACK = "/user/google/callback/"
        private const val LOGOUT_ENDPOINT = "/user/logout/"
    }

    /**
     * Auth Code를 서버로 전송하고 JWT 토큰 받기
     */
    suspend fun sendAuthCodeToServer(authCode: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val url = URL("$SERVER_BASE_URL$AUTH_CALLBACK")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 10_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            val body = JSONObject().put("auth_code", authCode).toString()
            conn.outputStream.use { os -> os.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val text = try {
                if (code in 200..299) {
                    conn.inputStream?.bufferedReader()?.readText().orEmpty()
                } else {
                    conn.errorStream?.bufferedReader()?.readText().orEmpty()
                }
            } finally {
                conn.disconnect()
            }

            if (code in 200..299) {
                try {
                    val json = JSONObject(text)

                    // 명세에 맞춘 파싱: { user:{}, jwt:{ access:"", refresh:"" } }
                    val jwt = json.optJSONObject("jwt")
                    val access = jwt?.optString("access").orEmpty()
                    val refresh = jwt?.optString("refresh").orEmpty()

                    if (access.isNotEmpty() || refresh.isNotEmpty()) {
                        return@withContext AuthResult.Success(
                            accessToken = access.ifEmpty { null },
                            refreshToken = refresh.ifEmpty { null }
                        )
                    } else {
                        // (호환) 혹시 과거 키명을 쓰는 응답도 수용
                        val access2 = json.optString("access_token", "")
                        val refresh2 = json.optString("refresh_token", "")
                        if (access2.isNotEmpty() || refresh2.isNotEmpty()) {
                            return@withContext AuthResult.Success(
                                accessToken = access2.ifEmpty { null },
                                refreshToken = refresh2.ifEmpty { null }
                            )
                        } else {
                            return@withContext AuthResult.Failure("예상치 못한 응답 형식:\n$text")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AuthRepository", "토큰 파싱 실패", e)
                    return@withContext AuthResult.Failure("서버 응답 파싱 실패:\n$text")
                }
            } else {
                return@withContext AuthResult.Failure("서버 오류 (HTTP $code):\n$text")
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "서버 연동 실패", e)
            return@withContext AuthResult.Failure("서버 연동 실패: ${e.message ?: "알 수 없음"}")
        }
    }

    /**
     * 서버에 로그아웃 요청
     */
    suspend fun logout(accessToken: String?, refreshToken: String?) = withContext(Dispatchers.IO) {
        try {
            if (accessToken == null) return@withContext
            val url = URL("$SERVER_BASE_URL$LOGOUT_ENDPOINT")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 10_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val body = JSONObject().put("refresh", refreshToken ?: "").toString()
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            conn.inputStream?.close()
            conn.disconnect()
        } catch (_: Exception) {
            // 로그아웃은 실패해도 무시
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
