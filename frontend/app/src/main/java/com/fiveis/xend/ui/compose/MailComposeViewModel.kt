package com.fiveis.xend.ui.compose

import android.util.Log
import androidx.core.text.HtmlCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.network.AiApiService
import com.fiveis.xend.network.MailComposeSseClient
import com.fiveis.xend.network.MailSuggestRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val aiApiService: AiApiService
) : ViewModel() {

    companion object {
        private const val REALTIME_MIN_CHARS = 20
        private const val PLACEHOLDER_EMAIL = "placeholder@example.com"
        private const val RETRY_DELAY_MS = 2000L
        private const val MAX_RETRY_COUNT = 3
    }

    private val _ui = MutableStateFlow(MailComposeUiState())
    val ui: StateFlow<MailComposeUiState> = _ui

    private val bodyBuffer = StringBuilder()
    private val suggestionBuffer = StringBuilder()
    private var debounceJob: Job? = null
    private var realtimeRequestJob: Job? = null
    private var retryJob: Job? = null
    private var skipNextDebouncedSend = false
    private var latestPlainText: String = ""
    private var latestSubject: String = ""
    private var lastRealtimeRequestSignature: String? = null
    private var lastObservedHtml: String? = null
    private var retryCount = 0

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
            Log.d("MailComposeVM", "Realtime mode enabled")
            _ui.update {
                it.copy(
                    isRealtimeEnabled = true,
                    realtimeStatus = RealtimeConnectionStatus.CONNECTED,
                    realtimeErrorMessage = null
                )
            }
        } else {
            Log.d("MailComposeVM", "Realtime mode disabled")
            debounceJob?.cancel()
            realtimeRequestJob?.cancel()
            suggestionBuffer.clear()
            lastRealtimeRequestSignature = null
            _ui.update {
                it.copy(
                    isRealtimeEnabled = false,
                    realtimeStatus = RealtimeConnectionStatus.IDLE,
                    realtimeErrorMessage = null,
                    suggestionText = ""
                )
            }
        }
    }

    fun skipNextTextChangeSend() {
        skipNextDebouncedSend = true
        debounceJob?.cancel()
        realtimeRequestJob?.cancel()
        lastRealtimeRequestSignature = null
    }

    fun onTextChanged(currentText: String, subject: String, toEmails: List<String>, cursorPosition: Int) {
        if (!_ui.value.isRealtimeEnabled) return

        val sanitizedHtml = stripSuggestionFromText(currentText)
        Log.d("MailComposeVM1", "sanitizedHtml(afterChange)=${sanitizedHtml.take(120)}")
        val plainText = normalizePlainText(sanitizedHtml)
        Log.d("MailComposeVM", "onTextChanged plainLength=${plainText.length}, subject='$subject'")
        latestPlainText = plainText
        latestSubject = subject

        if (skipNextDebouncedSend) {
            skipNextDebouncedSend = false
            return
        }

        if (plainText.length <= REALTIME_MIN_CHARS) {
            debounceJob?.cancel()
            suggestionBuffer.clear()
            Log.d("MailComposeVM", "onTextChanged -> below min chars, skipping")
            _ui.update { it.copy(suggestionText = "") }
            return
        }

        debounceJob?.cancel()
        retryJob?.cancel()

        // 중복 검사: 중복이면 현재 추천 유지, 아니면 바로 지움
        val signature = buildRealtimeSignature(
            normalizedText = plainText,
            htmlText = sanitizedHtml,
            subject = subject,
            cursorPosition = cursorPosition
        )
        if (signature == lastRealtimeRequestSignature) {
            Log.d("MailComposeVM", "duplicate context, keeping current suggestion")
            return
        }

        // 중복이 아니면 바로 추천 지우기
        suggestionBuffer.clear()
        _ui.update { it.copy(suggestionText = "") }

        debounceJob = viewModelScope.launch {
            delay(400)
            if (plainText != latestPlainText || subject != latestSubject) {
                Log.d("MailComposeVM", "onTextChanged debounced text changed, abort")
                return@launch
            }
            launchRealtimeSuggestion(
                htmlText = sanitizedHtml,
                normalizedText = plainText,
                subject = subject,
                toEmails = toEmails,
                cursorPosition = cursorPosition,
                includeHtmlInSignature = true,
                force = false
            )
        }
    }

    fun requestImmediateSuggestion(
        currentText: String,
        subject: String,
        toEmails: List<String>,
        cursorPosition: Int,
        force: Boolean = false
    ) {
        if (!_ui.value.isRealtimeEnabled && !force) return

        val sanitizedHtml = stripSuggestionFromText(currentText)
        Log.d("MailComposeVM1", "sanitizedHtml(immediate)=${sanitizedHtml.take(120)}")
        val plainText = normalizePlainText(sanitizedHtml)
        lastObservedHtml = sanitizedHtml

        if (plainText.length <= REALTIME_MIN_CHARS && !force) {
            debounceJob?.cancel()
            suggestionBuffer.clear()
            Log.d("MailComposeVM", "requestImmediateSuggestion -> below min chars")
            _ui.update { it.copy(suggestionText = "") }
            return
        }

        latestPlainText = plainText
        latestSubject = subject
        debounceJob?.cancel()
        realtimeRequestJob?.cancel()
        suggestionBuffer.clear()
        _ui.update { it.copy(suggestionText = "") }

        launchRealtimeSuggestion(
            htmlText = sanitizedHtml,
            normalizedText = plainText,
            subject = subject,
            toEmails = toEmails,
            cursorPosition = cursorPosition,
            includeHtmlInSignature = true,
            force = force
        )
    }

    fun acceptNextWord(): String? {
        val suggestion = _ui.value.suggestionText
        if (suggestion.isEmpty()) return null

        val words = suggestion.trim().split("\\s+".toRegex())
        if (words.isEmpty()) return null

        val firstWord = words.first()
        val remainingText = words.drop(1).joinToString(" ")

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
            realtimeRequestJob?.cancel()
            retryJob?.cancel()
            retryCount = 0
        }
    }

    private fun launchRealtimeSuggestion(
        htmlText: String,
        normalizedText: String,
        subject: String,
        toEmails: List<String>,
        cursorPosition: Int,
        includeHtmlInSignature: Boolean,
        force: Boolean
    ) {
        if (normalizedText.isBlank()) return

        val signature = buildRealtimeSignature(
            normalizedText = normalizedText,
            htmlText = htmlText.takeIf { includeHtmlInSignature },
            subject = subject,
            cursorPosition = cursorPosition
        )
        if (!force && signature == lastRealtimeRequestSignature) {
            Log.d("MailComposeVM", "duplicate context, skip")
            return
        }
        lastRealtimeRequestSignature = signature

        val request = buildSuggestRequest(
            htmlText = htmlText,
            subject = subject,
            toEmails = toEmails,
            cursorPosition = cursorPosition,
            plainTextLength = normalizedText.length
        )
        realtimeRequestJob?.cancel()
        realtimeRequestJob = viewModelScope.launch {
            try {
                _ui.update {
                    it.copy(
                        realtimeStatus = RealtimeConnectionStatus.CONNECTING,
                        realtimeErrorMessage = null
                    )
                }
                val response = withContext(Dispatchers.IO) { aiApiService.suggestMail(request) }
                Log.d("MailComposeVM", "suggestMail response=${response.code()}")
                if (response.isSuccessful) {
                    val suggestion = response.body()?.suggestion.orEmpty()
                    val singleSentence = extractFirstSentence(suggestion)
                    Log.d(
                        "MailComposeVM",
                        "suggestion raw='${suggestion.take(100)}', " +
                            "extracted='${singleSentence.take(100)}'"
                    )
                    suggestionBuffer.clear()
                    suggestionBuffer.append(singleSentence)
                    _ui.update {
                        it.copy(
                            suggestionText = singleSentence,
                            realtimeStatus = RealtimeConnectionStatus.CONNECTED,
                            realtimeErrorMessage = null
                        )
                    }
                    // 추천이 비어있으면 일정 시간 후 재시도
                    if (singleSentence.isEmpty() && retryCount < MAX_RETRY_COUNT) {
                        retryCount++
                        lastRealtimeRequestSignature = null // 시그니처 초기화하여 재요청 허용
                        Log.d("MailComposeVM", "Empty suggestion, scheduling retry ($retryCount/$MAX_RETRY_COUNT)")
                        scheduleRetry(htmlText, normalizedText, subject, toEmails, cursorPosition)
                    } else if (singleSentence.isNotEmpty()) {
                        retryCount = 0 // 성공하면 재시도 카운트 초기화
                    }
                } else {
                    val errorBody = response.errorBody()?.string().orEmpty()
                    val message = "실시간 추천 요청 실패 (${response.code()}) ${errorBody.take(200)}"
                    Log.e("MailComposeVM", message)
                    lastRealtimeRequestSignature = null
                    _ui.update {
                        it.copy(
                            realtimeStatus = RealtimeConnectionStatus.ERROR,
                            realtimeErrorMessage = message
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                lastRealtimeRequestSignature = null
                _ui.update {
                    it.copy(
                        realtimeStatus = RealtimeConnectionStatus.ERROR,
                        realtimeErrorMessage = e.message ?: "실시간 추천 처리 중 오류가 발생했습니다."
                    )
                }
            }
        }
    }

    private fun buildSuggestRequest(
        htmlText: String,
        subject: String,
        toEmails: List<String>,
        cursorPosition: Int,
        plainTextLength: Int
    ): MailSuggestRequest {
        val normalizedRecipients = toEmails.filter { it.isNotBlank() }
            .ifEmpty { listOf(PLACEHOLDER_EMAIL) }
        Log.d("MailComposeVM", "buildSuggestRequest recipients=$normalizedRecipients")
        return MailSuggestRequest(
            subject = subject.takeIf { it.isNotBlank() },
            body = htmlText,
            toEmails = normalizedRecipients,
            target = "body",
            cursor = cursorPosition.coerceIn(0, plainTextLength)
        )
    }

    private fun stripSuggestionFromText(text: String): String {
        return text.replace(
            Regex(
                """<span[^>]*id=["']ai-suggestion["'][^>]*>.*?</span>""",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            ),
            ""
        ).replace("\u200B", "") // 제로 폭 문자 제거
    }

    private fun normalizePlainText(html: String): String {
        return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
            .toString()
            .replace("\u00A0", " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun buildRealtimeSignature(
        normalizedText: String,
        htmlText: String?,
        subject: String,
        cursorPosition: Int
    ): String {
        val base = "$subject:$cursorPosition:${normalizedText.hashCode()}"
        return if (htmlText != null) "$base:${htmlText.hashCode()}" else base
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

    private fun scheduleRetry(
        htmlText: String,
        normalizedText: String,
        subject: String,
        toEmails: List<String>,
        cursorPosition: Int
    ) {
        retryJob?.cancel()
        retryJob = viewModelScope.launch {
            delay(RETRY_DELAY_MS)
            if (!_ui.value.isRealtimeEnabled) return@launch
            Log.d("MailComposeVM", "Retry triggered for suggestion")
            launchRealtimeSuggestion(
                htmlText = htmlText,
                normalizedText = normalizedText,
                subject = subject,
                toEmails = toEmails,
                cursorPosition = cursorPosition,
                includeHtmlInSignature = true,
                force = true
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        debounceJob?.cancel()
        realtimeRequestJob?.cancel()
        retryJob?.cancel()
    }
}
