package com.fiveis.xend.ui.contactbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.repository.ContactBookRepository
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class AddContactUiState(
    val isLoading: Boolean = false,
    val showSuccessBanner: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)

class AddContactViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ContactBookRepository = ContactBookRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(AddContactUiState())
    val uiState: StateFlow<AddContactUiState> = _uiState.asStateFlow()

    fun addContact(
        name: String,
        email: String,
        senderRole: String?,
        recipientRole: String,
        personalPrompt: String?,
        group: Group?,
        languagePreference: String? = null
    ) {
        if (name.isBlank()) {
            _uiState.value = AddContactUiState(isLoading = false, error = "이름을 입력해 주세요.")
            return
        }
        if (email.isBlank()) {
            _uiState.value = AddContactUiState(isLoading = false, error = "이메일을 입력해 주세요.")
            return
        }

        _uiState.value = AddContactUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val res = repository.addContact(
                    name = name,
                    email = email,
                    groupId = group?.id,
                    senderRole = senderRole,
                    recipientRole = recipientRole,
                    personalPrompt = personalPrompt,
                    languagePreference = languagePreference
                )
                _uiState.value = AddContactUiState(
                    isLoading = false,
                    showSuccessBanner = true,
                    successMessage = "연락처가 추가되었습니다",
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = AddContactUiState(
                    isLoading = false,
                    error = mapAddContactError(e)
                )
            }
        }
    }

    fun dismissSuccessBanner() {
        _uiState.value = _uiState.value.copy(showSuccessBanner = false, successMessage = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun mapAddContactError(error: Throwable): String {
        val fallback = "연락처 저장에 실패했습니다. 잠시 후 다시 시도해 주세요."
        val rawMessage = error.message ?: return fallback
        val serverMessage = extractServerMessage(rawMessage)
        val localized = localizeKnownError(serverMessage ?: rawMessage)
        return localized ?: fallback
    }

    private fun extractServerMessage(raw: String): String? {
        val bodyPart = raw.substringAfter("| body=", "")
        if (bodyPart.isNotBlank()) {
            parseErrorBody(bodyPart)?.let { return it }
        }
        // No explicit body, fallback to substring after colon (e.g., "HTTP 400 Bad Request")
        val afterColon = raw.substringAfter(": ", "")
        return afterColon.takeIf { it.isNotBlank() }
    }

    private fun parseErrorBody(body: String): String? {
        val trimmed = body.trim().trim('"')
        if (trimmed.isBlank() || trimmed == "Unknown error") return null

        // JSON object response
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return try {
                val json = JSONObject(trimmed)
                when {
                    json.has("detail") -> json.optString("detail")
                    json.has("message") -> json.optString("message")
                    else -> {
                        val messages = mutableListOf<String>()
                        val keys = json.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val value = json.get(key)
                            val message = when (value) {
                                is JSONArray -> (0 until value.length()).joinToString("\n") { idx ->
                                    value.optString(idx)
                                }
                                is JSONObject -> value.toString()
                                else -> value.toString()
                            }
                            if (message.isNotBlank()) messages += message
                        }
                        messages.joinToString("\n").ifBlank { null }
                    }
                }
            } catch (e: Exception) {
                null
            }
        }

        // JSON array response
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return try {
                val arr = JSONArray(trimmed)
                (0 until arr.length()).joinToString("\n") { idx ->
                    arr.optString(idx)
                }.ifBlank { null }
            } catch (e: Exception) {
                null
            }
        }

        return trimmed
    }

    private fun localizeKnownError(message: String?): String? {
        if (message.isNullOrBlank()) return null
        val normalized = message.lowercase(Locale.getDefault())
        return when {
            normalized.contains("valid email") ||
                normalized.contains("email address") ||
                normalized.contains("email format") -> "이메일 형식이 유효하지 않습니다."
            normalized.contains("already exists") ||
                normalized.contains("duplicate") -> "이미 등록된 이메일입니다."
            normalized.contains("required") && normalized.contains("recipient role") ->
                "수신자 역할을 선택해 주세요."
            normalized.contains("group") && normalized.contains("not found") ->
                "선택한 그룹을 찾을 수 없습니다."
            normalized.contains("permission") && normalized.contains("denied") ->
                "연락처를 저장할 권한이 없습니다."
            normalized.contains("timeout") -> "요청이 지연되고 있습니다. 잠시 후 다시 시도해 주세요."
            else -> null
        }
    }

    class Factory(private val application: Application) : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AddContactViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AddContactViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
