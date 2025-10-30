package com.fiveis.xend.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.network.MailComposeSseClient
import com.fiveis.xend.network.MailComposeWebSocketClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

data class MailComposeUiState(
    val isStreaming: Boolean = false,
    val subject: String = "",
    val bodyRendered: String = "",
    val error: String? = null,
    val suggestionText: String = "",
    val isRealtimeEnabled: Boolean = false
)

class MailComposeViewModel(
    private val api: MailComposeSseClient,
    private val wsClient: MailComposeWebSocketClient? = null
) : ViewModel() {

    private val _ui = MutableStateFlow(MailComposeUiState())
    val ui: StateFlow<MailComposeUiState> = _ui

    private val bodyBuffer = StringBuilder()
    private var throttleJob: Job? = null
    private var debounceJob: Job? = null
    private val suggestionBuffer = StringBuilder()

    fun startStreaming(payload: JSONObject) {
        bodyBuffer.clear()
        _ui.value = MailComposeUiState(isStreaming = true)

        // 80ms 스로틀로 화면 반영, 깜빡임 방지
        throttleJob?.cancel()
        throttleJob = viewModelScope.launch {
            while (isActive) {
                delay(80)
                val text = bodyBuffer.toString().replace("\n", "<br>")
                _ui.update { it.copy(bodyRendered = text) }
            }
        }

        api.start(
            payload = payload,
            onSubject = { title ->
                _ui.update { it.copy(subject = title.ifBlank { it.subject }) }
            },
            onBodyDelta = { _, text ->
                bodyBuffer.append(text)
            },
            onDone = {
                throttleJob?.cancel()
                _ui.update { it.copy(isStreaming = false, bodyRendered = bodyBuffer.toString().replace("\n", "<br>")) }
            },
            onError = { msg ->
                throttleJob?.cancel()
                _ui.update { it.copy(isStreaming = false, error = msg) }
            }
        )
    }

    fun stopStreaming() {
        api.stop()
        throttleJob?.cancel()
        _ui.update { it.copy(isStreaming = false) }
    }

    fun enableRealtimeMode(enabled: Boolean) {
        _ui.update { it.copy(isRealtimeEnabled = enabled) }
        if (enabled) {
            connectWebSocket()
        } else {
            disconnectWebSocket()
        }
    }

    private fun connectWebSocket() {
        wsClient?.connect(
            onMessage = { message ->
                try {
                    val json = JSONObject(message)
                    val type = json.optString("type")
                    when (type) {
                        "gpu.message" -> {
                            val data = json.optJSONObject("data")
                            val text = data?.optString("text") ?: ""
                            suggestionBuffer.append(text)
                            _ui.update { it.copy(suggestionText = suggestionBuffer.toString()) }
                        }
                    }
                } catch (e: Exception) {
                    _ui.update { it.copy(error = "메시지 파싱 실패: ${e.message}") }
                }
            },
            onError = { error ->
                _ui.update { it.copy(error = error) }
            },
            onClose = {
                _ui.update { it.copy(isRealtimeEnabled = false) }
            }
        )
    }

    private fun disconnectWebSocket() {
        wsClient?.disconnect()
        suggestionBuffer.clear()
        _ui.update { it.copy(suggestionText = "") }
    }

    fun onTextChanged(currentText: String) {
        if (!_ui.value.isRealtimeEnabled) return

        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(500)
            suggestionBuffer.clear()
            wsClient?.sendMessage(
                systemPrompt = "메일 초안 작성",
                text = currentText,
                maxTokens = 50
            )
        }
    }

    fun acceptSuggestion() {
        val suggestion = _ui.value.suggestionText
        if (suggestion.isNotEmpty()) {
            suggestionBuffer.clear()
            _ui.update { it.copy(suggestionText = "") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectWebSocket()
    }
}
