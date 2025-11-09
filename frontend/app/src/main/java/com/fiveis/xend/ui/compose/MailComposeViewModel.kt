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
    private var debounceJob: Job? = null
    private val suggestionBuffer = StringBuilder()

    // Undo snapshot
    private var undoSnapshot: UndoSnapshot? = null

    data class UndoSnapshot(
        val subject: String,
        val bodyHtml: String
    )

    fun saveUndoSnapshot(subject: String, bodyHtml: String) {
        undoSnapshot = UndoSnapshot(subject, bodyHtml)
    }

    fun undo(): UndoSnapshot? {
        val snapshot = undoSnapshot
        undoSnapshot = null
        return snapshot
    }

    fun startStreaming(payload: JSONObject) {
        bodyBuffer.clear()
        _ui.value = MailComposeUiState(isStreaming = true)

        api.start(
            payload = payload,
            onSubject = { title ->
                _ui.update { it.copy(subject = title.ifBlank { it.subject }) }
            },
            onBodyDelta = { _, text ->
                bodyBuffer.append(text)
            },
            onDone = {
                val finalText = bodyBuffer.toString().replace("\n", "<br>")
                _ui.update { it.copy(isStreaming = false, bodyRendered = finalText) }
            },
            onError = { msg ->
                _ui.update { it.copy(isStreaming = false, error = msg) }
            }
        )
    }

    fun stopStreaming() {
        api.stop()
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
                            val rawText = data?.optString("text") ?: ""

                            // GPU sends word by word, append with space
                            if (suggestionBuffer.isNotEmpty() && rawText.isNotEmpty()) {
                                suggestionBuffer.append(" ")
                            }
                            suggestionBuffer.append(rawText)

                            // Parse the entire buffer
                            val parsed = parseOutputFromMarkdown(suggestionBuffer.toString())
                            _ui.update { it.copy(suggestionText = parsed) }
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

    fun acceptNextWord(): String? {
        val suggestion = _ui.value.suggestionText
        if (suggestion.isEmpty()) return null

        // 공백 기준으로 단어 분리
        val words = suggestion.trim().split("\\s+".toRegex())
        if (words.isEmpty()) return null

        // 첫 번째 단어 가져오기
        val firstWord = words.first()

        // 남은 단어들로 업데이트
        val remainingText = words.drop(1).joinToString(" ")

        // suggestionBuffer도 업데이트
        suggestionBuffer.clear()
        suggestionBuffer.append(remainingText)

        _ui.update { it.copy(suggestionText = remainingText) }

        return firstWord
    }

    fun acceptSuggestion() {
        val suggestion = _ui.value.suggestionText
        if (suggestion.isNotEmpty()) {
            suggestionBuffer.clear()
            _ui.update { it.copy(suggestionText = "") }
        }
    }

    private fun parseOutputFromMarkdown(rawText: String): String {
        // Remove markdown code block markers: ```json ... ```
        var text = rawText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        // Try to parse JSON and extract "output" field
        return try {
            val json = JSONObject(text)
            json.optString("output", text)
        } catch (e: Exception) {
            // If parsing fails, return original text
            text
        }
    }

    override fun onCleared() {
        super.onCleared()
        disconnectWebSocket()
    }
}
