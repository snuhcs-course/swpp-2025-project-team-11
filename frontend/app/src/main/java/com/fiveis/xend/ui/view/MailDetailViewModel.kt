package com.fiveis.xend.ui.view

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiveis.xend.data.database.EmailDao
import com.fiveis.xend.data.model.Attachment
import com.fiveis.xend.data.model.AttachmentAnalysisResponse
import com.fiveis.xend.data.model.EmailItem
import com.fiveis.xend.data.repository.InboxRepository
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody

data class MailDetailUiState(
    val mail: EmailItem? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isDownloadingAttachment: Boolean = false,
    val downloadSuccessMessage: String? = null,
    val downloadErrorMessage: String? = null,
    val showAnalysisPopup: Boolean = false,
    val isAnalyzingAttachment: Boolean = false,
    val analysisResult: AttachmentAnalysisResponse? = null,
    val analysisErrorMessage: String? = null,
    val analysisTarget: Attachment? = null
)

class MailDetailViewModel(
    private val appContext: Context,
    private val emailDao: EmailDao,
    private val inboxRepository: InboxRepository,
    private val messageId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(MailDetailUiState())
    val uiState: StateFlow<MailDetailUiState> = _uiState.asStateFlow()

    init {
        loadMail()
    }

    private fun loadMail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                Log.d("MailDetailViewModel", "Loading mail from DB: $messageId")
                val email = emailDao.getEmailById(messageId)
                if (email != null) {
                    Log.d("MailDetailViewModel", "Email loaded from DB successfully")
                    _uiState.update {
                        it.copy(
                            mail = email,
                            isLoading = false
                        )
                    }
                } else {
                    Log.e("MailDetailViewModel", "Email not found in DB")
                    _uiState.update { it.copy(error = "Email not found", isLoading = false) }
                }
            } catch (e: Exception) {
                Log.e("MailDetailViewModel", "Error loading email from DB", e)
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun downloadAttachment(attachment: Attachment) {
        val mailId = _uiState.value.mail?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isDownloadingAttachment = true,
                    downloadErrorMessage = null,
                    downloadSuccessMessage = null
                )
            }
            try {
                val safeFilename = attachment.filename.ifBlank { "attachment" }
                val safeMimeType = attachment.mimeType.ifBlank { "application/octet-stream" }
                val response = inboxRepository.downloadAttachment(
                    mailId,
                    attachment.attachmentId,
                    safeFilename,
                    safeMimeType
                )
                if (!response.isSuccessful) {
                    Log.e(
                        "MailDetailViewModel",
                        "Attachment download failed with code: ${response.code()}"
                    )
                    _uiState.update {
                        it.copy(
                            isDownloadingAttachment = false,
                            downloadErrorMessage = "다운로드에 실패했습니다. (${response.code()})"
                        )
                    }
                    return@launch
                }

                val body = response.body()
                if (body == null) {
                    _uiState.update {
                        it.copy(
                            isDownloadingAttachment = false,
                            downloadErrorMessage = "다운로드한 파일이 비어 있습니다."
                        )
                    }
                    return@launch
                }

                val savedLocation = body.use { responseBody ->
                    saveAttachmentToDownloads(attachment, responseBody)
                }

                if (savedLocation != null) {
                    _uiState.update {
                        it.copy(
                            isDownloadingAttachment = false,
                            downloadSuccessMessage = savedLocation
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isDownloadingAttachment = false,
                            downloadErrorMessage = "파일 저장에 실패했습니다."
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MailDetailViewModel", "Error downloading attachment", e)
                _uiState.update {
                    it.copy(
                        isDownloadingAttachment = false,
                        downloadErrorMessage = e.message ?: "다운로드 중 오류가 발생했습니다."
                    )
                }
            }
        }
    }

    fun clearDownloadResult() {
        _uiState.update {
            it.copy(
                downloadSuccessMessage = null,
                downloadErrorMessage = null
            )
        }
    }

    fun analyzeAttachment(attachment: Attachment) {
        val mailId = _uiState.value.mail?.id ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    analysisTarget = attachment,
                    showAnalysisPopup = true,
                    isAnalyzingAttachment = true,
                    analysisResult = null,
                    analysisErrorMessage = null
                )
            }
            try {
                val safeFilename = attachment.filename.ifBlank { "attachment" }
                val safeMimeType = attachment.mimeType.ifBlank { "application/octet-stream" }
                val response = withContext(Dispatchers.IO) {
                    inboxRepository.analyzeAttachment(
                        messageId = mailId,
                        attachmentId = attachment.attachmentId,
                        filename = safeFilename,
                        mimeType = safeMimeType
                    )
                }
                if (!response.isSuccessful) {
                    Log.e(
                        "MailDetailViewModel",
                        "Attachment analysis failed with code: ${response.code()}"
                    )
                    _uiState.update {
                        it.copy(
                            isAnalyzingAttachment = false,
                            analysisErrorMessage = "AI 분석에 실패했습니다. (${response.code()})"
                        )
                    }
                    return@launch
                }

                val body = response.body()
                if (body == null) {
                    _uiState.update {
                        it.copy(
                            isAnalyzingAttachment = false,
                            analysisErrorMessage = "AI 분석 결과가 비어 있습니다."
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        isAnalyzingAttachment = false,
                        analysisResult = body,
                        analysisErrorMessage = null
                    )
                }
            } catch (e: Exception) {
                Log.e("MailDetailViewModel", "Error analyzing attachment", e)
                _uiState.update {
                    it.copy(
                        isAnalyzingAttachment = false,
                        analysisErrorMessage = e.message ?: "AI 분석 중 오류가 발생했습니다."
                    )
                }
            }
        }
    }

    fun dismissAnalysisPopup() {
        _uiState.update {
            it.copy(
                showAnalysisPopup = false,
                isAnalyzingAttachment = false,
                analysisResult = null,
                analysisErrorMessage = null,
                analysisTarget = null
            )
        }
    }

    private fun saveAttachmentToDownloads(attachment: Attachment, responseBody: ResponseBody): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = appContext.contentResolver
                val displayName = generateUniqueFileName(
                    attachment.filename.ifBlank { "attachment" }
                )
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(
                        MediaStore.Downloads.MIME_TYPE,
                        attachment.mimeType.ifBlank { "application/octet-stream" }
                    )
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return null

                resolver.openOutputStream(uri)?.use { output ->
                    responseBody.byteStream().use { input ->
                        input.copyTo(output)
                    }
                } ?: return null

                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                "다운로드 완료: ${attachment.filename}"
            } else {
                val downloadsDir =
                    appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return null
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val targetFile = getUniqueFile(downloadsDir, attachment.filename)
                FileOutputStream(targetFile).use { output ->
                    responseBody.byteStream().use { input ->
                        input.copyTo(output)
                    }
                }
                "앱 다운로드 폴더에 저장됨: ${targetFile.absolutePath}"
            }
        } catch (e: Exception) {
            Log.e("MailDetailViewModel", "Failed to save attachment", e)
            null
        }
    }

    // 파일 다운로드 시 파일명에 timestamp 추가하여 unique하도록
    private fun generateUniqueFileName(originalName: String): String {
        val timestamp = System.currentTimeMillis()
        val dotIndex = originalName.lastIndexOf('.')
        return if (dotIndex != -1 && dotIndex != 0) {
            val name = originalName.substring(0, dotIndex)
            val extension = originalName.substring(dotIndex)
            "${name}_${timestamp}$extension"
        } else {
            "${originalName}_$timestamp"
        }
    }

    private fun getUniqueFile(directory: File, originalName: String): File {
        var file = File(directory, originalName)
        if (!file.exists()) {
            return file
        }
        val dotIndex = originalName.lastIndexOf('.')
        val name = if (dotIndex != -1 && dotIndex != 0) {
            originalName.substring(0, dotIndex)
        } else {
            originalName
        }
        val extension = if (dotIndex != -1 && dotIndex != originalName.length - 1) {
            originalName.substring(dotIndex)
        } else {
            ""
        }

        var index = 1
        while (file.exists()) {
            file = File(directory, "${name}_$index$extension")
            index++
        }
        return file
    }
}
