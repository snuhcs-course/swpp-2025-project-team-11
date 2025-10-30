package com.fiveis.xend.network

import android.content.Context
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
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

    fun connect(onMessage: (String) -> Unit, onError: (String) -> Unit, onClose: () -> Unit) {
        // 콜백 먼저 설정
        messageCallback = onMessage
        errorCallback = onError
        closeCallback = onClose

        // 빈 URL 체크
        if (wsUrl.isBlank()) {
            Log.e(TAG, "WS URL is blank")
            errorCallback?.invoke("WS_URL이 설정되지 않았습니다.")
            return
        }

        // 중복 연결 방지
        if (!isConnecting.compareAndSet(false, true)) {
            Log.w(TAG, "Connection already in progress")
            errorCallback?.invoke("이미 연결 중입니다.")
            return
        }

        // 기존 연결이 있으면 정리
        if (isConnected.get()) {
            Log.w(TAG, "Already connected, closing existing connection")
            disconnect()
        }

        try {
            val client = RetrofitClient.getClient(context)
            val request = Request.Builder()
                .url(wsUrl)
                .build()

            webSocket = client.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        Log.d(TAG, "WebSocket connected")
                        isConnecting.set(false)
                        isConnected.set(true)
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        Log.d(TAG, "Received: $text")
                        messageCallback?.invoke(text)
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Log.e(TAG, "WebSocket failed: ${t.message}")
                        isConnecting.set(false)
                        isConnected.set(false)
                        errorCallback?.invoke(t.message ?: "Unknown error")
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        Log.d(TAG, "WebSocket closing: $reason")
                        webSocket.close(1000, null)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        Log.d(TAG, "WebSocket closed: $reason")
                        isConnecting.set(false)
                        isConnected.set(false)
                        closeCallback?.invoke()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create WebSocket", e)
            isConnecting.set(false)
            isConnected.set(false)
            errorCallback?.invoke("연결 실패: ${e.message}")
        }
    }

    fun sendMessage(systemPrompt: String, text: String, maxTokens: Int = 50) {
        val currentSocket = webSocket
        if (currentSocket == null || !isConnected.get()) {
            Log.w(TAG, "Cannot send message: WebSocket not connected")
            errorCallback?.invoke("WebSocket이 연결되지 않았습니다.")
            return
        }

        try {
            val json = JSONObject().apply {
                put("system_prompt", systemPrompt)
                put("text", text)
                put("max_tokens", maxTokens)
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
        isConnecting.set(false)
        isConnected.set(false)

        webSocket?.let { ws ->
            try {
                ws.close(1000, "User closed")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing WebSocket", e)
            }
        }
        webSocket = null

        // 콜백도 정리 (메모리 누수 방지)
        messageCallback = null
        errorCallback = null
        closeCallback = null
    }

    companion object {
        private const val TAG = "MailComposeWebSocket"
    }
}
