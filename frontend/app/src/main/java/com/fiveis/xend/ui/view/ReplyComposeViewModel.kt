package com.fiveis.xend.ui.view

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.network.MailReplySseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReplyOptionState(
    val id: Int,
    val type: String,
    val title: String,
    val body: String = "",
    val isComplete: Boolean = false,
    val totalSeq: Int = 0
)

data class ReplyComposeUiState(
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val options: List<ReplyOptionState> = emptyList(),
    val error: String? = null
)

class ReplyComposeViewModel(
    private val api: MailReplySseClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReplyComposeUiState())
    val uiState: StateFlow<ReplyComposeUiState> = _uiState

    // Throttle을 위한 변수들
    private var lastUiUpdateTime = 0L
    private val throttleMs = 80L // 80ms마다 한 번씩 UI 업데이트

    /**
     * 답장 옵션 추천 시작
     */
    fun startReplyOptions(subject: String, body: String, toEmail: String) {
        Log.d("ReplyComposeVM", "startReplyOptions 호출")
        _uiState.value = ReplyComposeUiState(isLoading = true, isStreaming = true)

        api.start(
            subject = subject,
            body = body,
            toEmail = toEmail,
            onReady = {
                Log.d("ReplyComposeVM", "onReady 호출")
                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.update { it.copy(isLoading = false) }
                }
            },
            onOptions = { optionInfos ->
                Log.d("ReplyComposeVM", "onOptions 호출: ${optionInfos.size}개 옵션")
                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            options = optionInfos.map { info ->
                                ReplyOptionState(
                                    id = info.id,
                                    type = info.type,
                                    title = info.title
                                )
                            }
                        )
                    }
                    Log.d(
                        "ReplyComposeVM",
                        "onOptions 완료: isLoading=${_uiState.value.isLoading}, options=${_uiState.value.options.size}"
                    )
                }
            },
            onOptionDelta = { optionId, seq, text ->
                // 상태를 즉시 업데이트
                _uiState.update { state ->
                    state.copy(
                        options = state.options.map { option ->
                            if (option.id == optionId) {
                                option.copy(body = option.body + text)
                            } else {
                                option
                            }
                        }
                    )
                }

                // Throttle: 80ms마다 한 번씩만 UI 업데이트 트리거
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastUiUpdateTime >= throttleMs) {
                    lastUiUpdateTime = currentTime
                    viewModelScope.launch(Dispatchers.Main) {
                        // 새 리스트 객체를 만들어서 Compose가 변경을 감지하도록
                        val currentState = _uiState.value
                        _uiState.value = currentState.copy(options = currentState.options.toList())
                        Log.d(
                            "ReplyComposeVM",
                            "UI update: totalBodyLength=${currentState.options.sumOf { it.body.length }}"
                        )
                    }
                }
            },
            onOptionDone = { optionId, totalSeq ->
                Log.d("ReplyComposeVM", "onOptionDone: id=$optionId, totalSeq=$totalSeq")
                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.update { state ->
                        state.copy(
                            options = state.options.map { option ->
                                if (option.id == optionId) {
                                    option.copy(
                                        isComplete = true,
                                        totalSeq = totalSeq
                                    )
                                } else {
                                    option
                                }
                            }
                        )
                    }
                }
            },
            onOptionError = { optionId, message ->
                Log.e("ReplyComposeVM", "onOptionError: id=$optionId, msg=$message")
                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.update { state ->
                        state.copy(
                            error = "옵션 $optionId 오류: $message",
                            options = state.options.map { option ->
                                if (option.id == optionId) {
                                    option.copy(isComplete = true)
                                } else {
                                    option
                                }
                            }
                        )
                    }
                }
            },
            onDone = { reason ->
                Log.d("ReplyComposeVM", "onDone: reason=$reason")
                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.update { state ->
                        state.copy(isStreaming = false)
                    }
                }
            },
            onError = { message ->
                Log.e("ReplyComposeVM", "onError: $message")
                viewModelScope.launch(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isStreaming = false,
                            error = message
                        )
                    }
                }
            }
        )
    }

    fun stopStreaming() {
        api.stop()
        _uiState.update { it.copy(isStreaming = false, isLoading = false) }
    }

    override fun onCleared() {
        super.onCleared()
        api.stop()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReplyComposeViewModel::class.java)) {
                val api = MailReplySseClient(
                    context = application,
                    endpointUrl = com.fiveis.xend.BuildConfig.BASE_URL + "/api/ai/mail/reply/stream/"
                )
                return ReplyComposeViewModel(api) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
