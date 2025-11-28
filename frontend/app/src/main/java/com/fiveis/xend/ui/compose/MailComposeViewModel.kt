package com.fiveis.xend.ui.compose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.network.MailComposeSseClient
import com.fiveis.xend.network.MailComposeWebSocketClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

enum class RealtimeConnectionStatus {
    IDLE,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class MailComposeUiState(
    val isStreaming: Boolean = false,
    val subject: String = "",
    val bodyRendered: String = "",
    val error: String? = null,
    val suggestionText: String = "",
    val isRealtimeEnabled: Boolean = false,
    val realtimeStatus: RealtimeConnectionStatus = RealtimeConnectionStatus.IDLE,
    val realtimeErrorMessage: String? = null
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
    private var pendingSuggestionText: String? = null

    // Undo/redo snapshots
    private var undoSnapshot: UndoSnapshot? = null
    private var redoSnapshot: UndoSnapshot? = null

    data class UndoSnapshot(
        val subject: String,
        val bodyHtml: String
    )

    fun saveUndoSnapshot(subject: String, bodyHtml: String) {
        undoSnapshot = UndoSnapshot(subject, bodyHtml)
        redoSnapshot = null
    }

    fun undo(currentSubject: String? = null, currentBodyHtml: String? = null): UndoSnapshot? {
        val snapshot = undoSnapshot ?: return null
        if (currentSubject != null && currentBodyHtml != null) {
            redoSnapshot = UndoSnapshot(currentSubject, currentBodyHtml)
        }
        undoSnapshot = null
        return snapshot
    }

    fun redo(currentSubject: String? = null, currentBodyHtml: String? = null): UndoSnapshot? {
        val snapshot = redoSnapshot ?: return null
        if (currentSubject != null && currentBodyHtml != null) {
            undoSnapshot = UndoSnapshot(currentSubject, currentBodyHtml)
        }
        redoSnapshot = null
        return snapshot
    }

    fun startStreaming(payload: JSONObject) {
        bodyBuffer.clear()
        _ui.update {
            it.copy(
                isStreaming = true,
                bodyRendered = "",
                error = null,
                suggestionText = ""
            )
        }

        api.start(
            payload = payload,
            onSubject = { title ->
                _ui.update { state ->
                    state.copy(subject = title.ifBlank { state.subject })
                }
            },
            onBodyDelta = { _, text ->
                bodyBuffer.append(text)

                val partialHtml = bodyBuffer.toString().replace("\n", "<br>")
                _ui.update { state ->
                    state.copy(bodyRendered = partialHtml)
                }
            },
            onDone = {
                val finalText = bodyBuffer.toString().replace("\n", "<br>")
                _ui.update { state ->
                    state.copy(isStreaming = false, bodyRendered = finalText)
                }
            },
            onError = { msg ->
                _ui.update { state -> state.copy(isStreaming = false, error = msg) }
            }
        )
    }

    fun stopStreaming() {
        api.stop()
        _ui.update { it.copy(isStreaming = false) }
    }

    fun enableRealtimeMode(enabled: Boolean) {
        if (enabled) {
            _ui.update {
                it.copy(
                    isRealtimeEnabled = true,
                    realtimeErrorMessage = null
                )
            }
            connectWebSocket()
        } else {
            _ui.update {
                it.copy(
                    isRealtimeEnabled = false,
                    realtimeStatus = RealtimeConnectionStatus.IDLE,
                    realtimeErrorMessage = null,
                    suggestionText = ""
                )
            }
            disconnectWebSocket()
        }
    }

    fun ensureRealtimeConnection() {
        if (_ui.value.isRealtimeEnabled) {
            connectWebSocket()
        }
    }

    private fun connectWebSocket() {
        if (_ui.value.realtimeStatus == RealtimeConnectionStatus.CONNECTING ||
            _ui.value.realtimeStatus == RealtimeConnectionStatus.CONNECTED
        ) {
            return
        }
        wsClient?.let { client ->
            _ui.update { it.copy(realtimeStatus = RealtimeConnectionStatus.CONNECTING, realtimeErrorMessage = null) }
            client.connect(
                onMessage = { message ->
                    try {
                        val json = JSONObject(message)
                        val type = json.optString("type")
                        when (type) {
                            "gpu.message" -> {
                                val data = json.optJSONObject("data")
                                val rawText = data?.optString("text") ?: ""

                                if (suggestionBuffer.isNotEmpty() && rawText.isNotEmpty()) {
                                    suggestionBuffer.append(" ")
                                }
                                suggestionBuffer.append(rawText)

                                val parsed = parseOutputFromMarkdown(suggestionBuffer.toString())
                                val singleSentence = extractFirstSentence(parsed)
                                _ui.update { it.copy(suggestionText = singleSentence) }
                            }
                            "gpu.done" -> {
                                suggestionBuffer.clear()
                            }
                        }
                    } catch (e: Exception) {
                        _ui.update { it.copy(error = "메시지 파싱 실패: ${e.message}") }
                    }
                },
                onError = { error ->
                    handleRealtimeError(error)
                },
                onClose = {
                    val stillEnabled = _ui.value.isRealtimeEnabled
                    _ui.update {
                        it.copy(
                            isRealtimeEnabled = if (stillEnabled) it.isRealtimeEnabled else false,
                            realtimeStatus = if (stillEnabled) {
                                RealtimeConnectionStatus.ERROR
                            } else {
                                RealtimeConnectionStatus.IDLE
                            },
                            realtimeErrorMessage = if (stillEnabled) "실시간 AI 연결이 종료되었습니다. 다시 시도해 주세요." else null
                        )
                    }
                },
                onConnected = {
                    _ui.update {
                        it.copy(
                            realtimeStatus = RealtimeConnectionStatus.CONNECTED,
                            realtimeErrorMessage = null
                        )
                    }
                    sendPendingSuggestion()
                }
            )
            client.connectIfNeeded()
        }
    }

    private fun disconnectWebSocket() {
        wsClient?.disconnect()
        suggestionBuffer.clear()
        _ui.update {
            it.copy(
                suggestionText = "",
                realtimeStatus = RealtimeConnectionStatus.IDLE,
                realtimeErrorMessage = null
            )
        }
    }

    private fun handleRealtimeError(rawMessage: String) {
        val friendlyMessage = mapRealtimeError(rawMessage) ?: return
        _ui.update {
            it.copy(
                error = rawMessage,
                realtimeStatus = RealtimeConnectionStatus.ERROR,
                realtimeErrorMessage = friendlyMessage
            )
        }
    }

    private fun mapRealtimeError(rawMessage: String): String? {
        val lower = rawMessage.lowercase()
        return when {
            lower.contains("이미 연결 중") -> null
            lower.contains("websocket이 연결되지 않았습니다") ->
                "실시간 AI 연결이 아직 준비 중입니다. 잠시 후 다시 시도해 주세요."
            lower.contains("연결 실패") || lower.contains("서버 에러") ||
                lower.contains("failed") -> "실시간 AI 연결에 문제가 발생했습니다. 잠시 후 다시 시도해 주세요."
            else -> "실시간 AI 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
        }
    }

    fun onTextChanged(currentText: String) {
        if (!_ui.value.isRealtimeEnabled) return

        debounceJob?.cancel()
        suggestionBuffer.clear()
        _ui.update { it.copy(suggestionText = "") }
        debounceJob = viewModelScope.launch {
            delay(500)
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
            debounceJob?.cancel()
        }
    }

    /**
     * Request new suggestion immediately (for tab completion)
     */
    fun requestImmediateSuggestion(currentText: String, force: Boolean = false) {
        if (!_ui.value.isRealtimeEnabled && !force) return

        debounceJob?.cancel()
        suggestionBuffer.clear()
        _ui.update { it.copy(suggestionText = "") }

        viewModelScope.launch(Dispatchers.Main) {
            pendingSuggestionText = currentText
        }

        wsClient?.let { client ->
            if (client.isActive()) {
                sendPendingSuggestion()
            } else {
                // 연결이 준비되면 onConnected에서 처리
                ensureRealtimeConnection()
                schedulePendingSendFallback()
            }
        }
    }

    private fun sendPendingSuggestion() {
        viewModelScope.launch(Dispatchers.Main) {
            val text = pendingSuggestionText ?: return@launch
            pendingSuggestionText = null
            delay(100) // Short delay to let the UI update
            wsClient?.sendMessage(
                systemPrompt = "메일 초안 작성",
                text = text,
                maxTokens = 50
            )
        }
    }

    private fun schedulePendingSendFallback(timeoutMs: Long = 2000, intervalMs: Long = 100) {
        viewModelScope.launch {
            var waited = 0L
            while (waited < timeoutMs && !(wsClient?.isActive() ?: false)) {
                delay(intervalMs)
                waited += intervalMs
            }
            if (wsClient?.isActive() == true) {
                sendPendingSuggestion()
            }
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

    private fun extractFirstSentence(text: String): String {
        val normalized = text
            .replace("\n", " ")
            .replace("\\s+".toRegex(), " ")
            .trim()

        if (normalized.isEmpty()) return ""

        val endIndex = normalized.indexOfFirst { it == '.' || it == '!' || it == '?' }
        val cutoff = if (endIndex == -1) normalized.length else endIndex + 1

        return normalized.substring(0, cutoff).trim()
    }

    override fun onCleared() {
        super.onCleared()
        disconnectWebSocket()
    }
}
