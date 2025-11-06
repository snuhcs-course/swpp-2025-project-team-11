package com.fiveis.xend.network

import android.content.Context
import android.util.Log
import com.fiveis.xend.data.model.TokenRefreshRequest
import com.fiveis.xend.data.source.TokenManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.runBlocking
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class MailComposeWebSocketClient(
    private val context: Context,
    private val wsUrl: String
) {
    @Volatile
    private var webSocket: WebSocket? = null
    private var messageCallback: ((String) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null
    private var closeCallback: (() -> Unit)? = null
    private val isConnecting = AtomicBoolean(false)
    private val isConnected = AtomicBoolean(false)
    private val isRetrying = AtomicBoolean(false)
    private val tokenManager = TokenManager(context)

    fun connect(onMessage: (String) -> Unit, onError: (String) -> Unit, onClose: () -> Unit) {
        Log.d(TAG, "========== WebSocket Connect Called ==========")
        Log.d(TAG, "WS URL: $wsUrl")

        // 콜백 먼저 설정
        messageCallback = onMessage
        errorCallback = onError
        closeCallback = onClose

        // 빈 URL 체크
        if (wsUrl.isBlank()) {
            Log.e(TAG, "❌ WS URL is blank")
            errorCallback?.invoke("WS_URL이 설정되지 않았습니다.")
            return
        }

        // 중복 연결 방지
        if (!isConnecting.compareAndSet(false, true)) {
            Log.w(TAG, "⚠️ Connection already in progress")
            errorCallback?.invoke("이미 연결 중입니다.")
            return
        }

        // 기존 연결이 있으면 정리
        if (isConnected.get()) {
            Log.w(TAG, "⚠️ Already connected, closing existing connection")
            disconnect()
        }

        connectWithToken()
    }

    private fun connectWithToken() {
        Log.d(TAG, "---------- connectWithToken() started ----------")
        try {
            var accessToken = tokenManager.getAccessToken()
            val tokenPreview = if (accessToken.isNullOrEmpty()) {
                "EMPTY/NULL"
            } else {
                accessToken.take(30) + "..."
            }
            Log.d(TAG, "Access token retrieved: $tokenPreview")

            if (accessToken.isNullOrEmpty()) {
                Log.e(TAG, "❌ No access token available")
                isConnecting.set(false)
                errorCallback?.invoke("인증 토큰이 없습니다. 다시 로그인해주세요.")
                return
            }

            Log.d(TAG, "Creating WebSocket OkHttpClient...")
            val client = RetrofitClient.getWebSocketClient(context)
            Log.d(TAG, "✅ WebSocket OkHttpClient created (auth interceptor 없음)")

            Log.d(TAG, "Building WebSocket request...")
            Log.d(TAG, "  URL: $wsUrl")
            Log.d(TAG, "  Authorization: Bearer ${accessToken.take(20)}...")

            val request = Request.Builder()
                .url(wsUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .build()

            Log.d(TAG, "✅ Request built, initiating WebSocket connection...")

            webSocket = client.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        Log.d(TAG, "✅✅✅ WebSocket CONNECTED successfully!")
                        Log.d(TAG, "Response code: ${response.code}")
                        Log.d(TAG, "Response message: ${response.message}")
                        Log.d(TAG, "Protocol: ${response.protocol}")
                        isConnecting.set(false)
                        isConnected.set(true)
                        isRetrying.set(false)
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        Log.d(TAG, "Received: $text")

                        try {
                            val json = JSONObject(text)
                            val type = json.optString("type")

                            if (type == "error") {
                                val message = json.optString("message")

                                // 토큰 만료 에러 처리
                                if (message == "token_invalid" && !isRetrying.get()) {
                                    Log.w(TAG, "Token invalid error received, attempting refresh...")
                                    disconnect()

                                    if (refreshTokenAndRetry()) {
                                        return
                                    }
                                }

                                // 기타 에러는 콜백으로 전달
                                Log.e(TAG, "Error message from server: $message")
                                errorCallback?.invoke("서버 에러: $message")
                                return
                            }
                        } catch (e: Exception) {
                            // JSON 파싱 실패는 무시하고 정상 메시지로 처리
                            Log.d(TAG, "Not a JSON error message, processing as normal message")
                        }

                        messageCallback?.invoke(text)
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Log.e(TAG, "❌❌❌ WebSocket FAILED!")
                        Log.e(TAG, "Exception type: ${t.javaClass.simpleName}")
                        Log.e(TAG, "Exception message: ${t.message}")
                        Log.e(TAG, "Response code: ${response?.code}")
                        Log.e(TAG, "Response message: ${response?.message}")
                        Log.e(TAG, "Response body: ${response?.body?.string()}")
                        t.printStackTrace()

                        // 401 Unauthorized - 토큰 만료 가능성
                        if (response?.code == 401 && !isRetrying.get()) {
                            Log.w(TAG, "⚠️ 401 Unauthorized - Token might be expired, attempting refresh...")
                            isConnecting.set(false)
                            isConnected.set(false)

                            // 토큰 갱신 시도
                            if (refreshTokenAndRetry()) {
                                return
                            }
                        }

                        isConnecting.set(false)
                        isConnected.set(false)
                        val errorMsg = "연결 실패: ${t.message} (code: ${response?.code})"
                        Log.e(TAG, "Invoking error callback: $errorMsg")
                        errorCallback?.invoke(errorMsg)
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        Log.w(TAG, "⚠️ WebSocket CLOSING...")
                        Log.w(TAG, "Close code: $code")
                        Log.w(TAG, "Close reason: $reason")

                        // 1008은 policy violation (보통 인증 실패)
                        if (code == 1008 && !isRetrying.get()) {
                            Log.w(TAG, "⚠️ Policy violation (1008), possibly auth failure. Attempting token refresh...")
                            if (refreshTokenAndRetry()) {
                                return
                            }
                        }

                        webSocket.close(1000, null)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        Log.d(TAG, "🔌 WebSocket CLOSED")
                        Log.d(TAG, "Close code: $code")
                        Log.d(TAG, "Close reason: $reason")
                        isConnecting.set(false)
                        isConnected.set(false)
                        closeCallback?.invoke()
                    }
                }
            )
            Log.d(TAG, "✅ WebSocket object created, waiting for callbacks...")
        } catch (e: Exception) {
            Log.e(TAG, "❌❌❌ Exception while creating WebSocket!")
            Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Exception message: ${e.message}")
            e.printStackTrace()
            isConnecting.set(false)
            isConnected.set(false)
            errorCallback?.invoke("연결 실패: ${e.message}")
        }
    }

    private fun refreshTokenAndRetry(): Boolean {
        val refreshToken = tokenManager.getRefreshToken()
        if (refreshToken.isNullOrEmpty()) {
            Log.e(TAG, "No refresh token available")
            errorCallback?.invoke("인증이 만료되었습니다. 다시 로그인해주세요.")
            isRetrying.set(false)
            return false
        }

        return try {
            runBlocking {
                Log.d(TAG, "Attempting to refresh token...")
                isRetrying.set(true)

                val response = RetrofitClient.authApiService.refreshToken(
                    TokenRefreshRequest(refreshToken)
                )

                if (response.isSuccessful && response.body() != null) {
                    val newTokens = response.body()!!
                    tokenManager.saveTokens(
                        newTokens.accessToken,
                        newTokens.refreshToken,
                        tokenManager.getUserEmail()!!
                    )
                    Log.d(TAG, "Token refreshed successfully, retrying WebSocket connection...")

                    // 재시도
                    isConnecting.set(true)
                    connectWithToken()
                    true
                } else {
                    Log.e(TAG, "Token refresh failed: ${response.code()}")
                    tokenManager.clearTokens()
                    isRetrying.set(false)
                    errorCallback?.invoke("인증이 만료되었습니다. 다시 로그인해주세요.")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during token refresh", e)
            isRetrying.set(false)
            errorCallback?.invoke("토큰 갱신 실패: ${e.message}")
            false
        }
    }

    fun sendMessage(text: String, toEmails: List<String>, body: String? = null) {
        val currentSocket = webSocket
        if (currentSocket == null || !isConnected.get()) {
            Log.w(TAG, "Cannot send message: WebSocket not connected")
            errorCallback?.invoke("WebSocket이 연결되지 않았습니다.")
            return
        }

        try {
            val json = JSONObject().apply {
                put("text", text)
                put("to_emails", org.json.JSONArray(toEmails))
                if (body != null) {
                    put("body", body)
                }
            }
            val message = json.toString()
            Log.d(TAG, "Sending: $message")
            currentSocket.send(message)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message", e)
            errorCallback?.invoke("메시지 전송 실패: ${e.message}")
        }
    }

    fun disconnect() {
        Log.d(TAG, "========== Disconnect Called ==========")
        Log.d(TAG, "Current state - isConnecting: ${isConnecting.get()}, isConnected: ${isConnected.get()}")

        isConnecting.set(false)
        isConnected.set(false)

        webSocket?.let { ws ->
            try {
                Log.d(TAG, "Closing WebSocket with code 1000...")
                ws.close(1000, "User closed")
                Log.d(TAG, "✅ WebSocket close() called")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error closing WebSocket", e)
            }
        }
        webSocket = null
        Log.d(TAG, "WebSocket reference cleared")

        // 콜백도 정리 (메모리 누수 방지)
        messageCallback = null
        errorCallback = null
        closeCallback = null
        Log.d(TAG, "Callbacks cleared")
    }

    companion object {
        private const val TAG = "MailComposeWebSocket"
    }
}
