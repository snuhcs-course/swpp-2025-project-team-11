package com.fiveis.xend.network

import android.content.Context
import android.util.Log
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class MailComposeWebSocketClient(
    private val context: Context,
    private val wsUrl: String
) {
    private var webSocket: WebSocket? = null
    private var messageCallback: ((String) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null
    private var closeCallback: (() -> Unit)? = null

    fun connect(onMessage: (String) -> Unit, onError: (String) -> Unit, onClose: () -> Unit) {
        messageCallback = onMessage
        errorCallback = onError
        closeCallback = onClose

        val client = RetrofitClient.getClient(context)
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket connected")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d(TAG, "Received: $text")
                    messageCallback?.invoke(text)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket failed: ${t.message}")
                    errorCallback?.invoke(t.message ?: "Unknown error")
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closing: $reason")
                    webSocket.close(1000, null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closed: $reason")
                    closeCallback?.invoke()
                }
            }
        )
    }

    fun sendMessage(systemPrompt: String, text: String, maxTokens: Int = 50) {
        val json = JSONObject().apply {
            put("system_prompt", systemPrompt)
            put("text", text)
            put("max_tokens", maxTokens)
        }
        val message = json.toString()
        Log.d(TAG, "Sending: $message")
        webSocket?.send(message)
    }

    fun disconnect() {
        webSocket?.close(1000, "User closed")
        webSocket = null
    }

    companion object {
        private const val TAG = "MailComposeWebSocket"
    }
}
