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
import com.fiveis.xend.data.model.MailDetailResponse
import com.fiveis.xend.data.repository.InboxRepository
import com.fiveis.xend.utils.EmailUtils
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import kotlinx.coroutines.CoroutineDispatcher
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
    val analysisTarget: Attachment? = null,
    val showPreviewDialog: Boolean = false,
    val previewTarget: Attachment? = null,
    val isPreviewLoading: Boolean = false,
    val previewContent: AttachmentPreviewContent? = null,
    val previewErrorMessage: String? = null,
    val isExternalOpenLoading: Boolean = false,
    val externalOpenErrorMessage: String? = null,
    val externalOpenContent: AttachmentExternalContent? = null
)

sealed interface AttachmentPreviewContent {
    data class Text(val text: String) : AttachmentPreviewContent
    data class Pdf(val filePath: String) : AttachmentPreviewContent
}

data class AttachmentExternalContent(
    val filePath: String,
    val mimeType: String
)

class MailDetailViewModel(
    private val appContext: Context,
    private val emailDao: EmailDao,
    private val inboxRepository: InboxRepository,
    private val messageId: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(MailDetailUiState())
    val uiState: StateFlow<MailDetailUiState> = _uiState.asStateFlow()
    private var markReadInProgress = false

    init {
        loadMail()
    }

    private fun loadMail() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
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
                    markEmailAsReadIfNeeded(email)
                } else {
                    Log.e("MailDetailViewModel", "Email not found in DB cache")
                    _uiState.update { it.copy(error = "메일을 찾을 수 없습니다.", isLoading = true) }
                }
            } catch (e: Exception) {
                Log.e("MailDetailViewModel", "Error loading email from DB", e)
                _uiState.update { it.copy(error = e.message, isLoading = true) }
            }

            fetchMailDetailFromServer()
        }
    }

    private suspend fun fetchMailDetailFromServer() {
        try {
            val response = withContext(ioDispatcher) {
                inboxRepository.getMail(messageId)
            }
            if (!response.isSuccessful) {
                Log.e("MailDetailViewModel", "Failed to fetch mail detail: ${response.code()}")
                handleMailDetailError("메일을 불러오지 못했습니다. (${response.code()})")
                return
            }

            val detail = response.body()
            if (detail == null) {
                Log.e("MailDetailViewModel", "Mail detail response body is null")
                handleMailDetailError("메일 상세 정보가 비어 있습니다.")
                return
            }

            val updatedMail = detail.toEmailItem(_uiState.value.mail)
            withContext(Dispatchers.IO) {
                emailDao.insertEmail(updatedMail)
            }

            _uiState.update {
                it.copy(
                    mail = updatedMail,
                    isLoading = false,
                    error = null
                )
            }

            markEmailAsReadIfNeeded(updatedMail)
        } catch (e: Exception) {
            Log.e("MailDetailViewModel", "Exception fetching mail detail", e)
            handleMailDetailError(e.message ?: "메일을 불러오는 중 오류가 발생했습니다.")
        }
    }

    private fun handleMailDetailError(message: String) {
        _uiState.update { state ->
            if (state.mail == null) {
                state.copy(isLoading = false, error = message)
            } else {
                state.copy(isLoading = false)
            }
        }
    }

    private fun MailDetailResponse.toEmailItem(existing: EmailItem?): EmailItem {
        return EmailItem(
            id = id,
            threadId = threadId,
            subject = subject,
            fromEmail = fromEmail,
            toEmail = toEmail.ifBlank { existing?.toEmail ?: "" },
            snippet = snippet,
            date = date,
            dateRaw = dateRaw,
            isUnread = existing?.isUnread ?: isUnread,
            labelIds = labelIds,
            body = body,
            attachments = attachments,
            sourceLabel = existing?.sourceLabel ?: inferSourceLabel(labelIds),
            cachedAt = existing?.cachedAt ?: System.currentTimeMillis(),
            dateTimestamp = existing?.dateTimestamp ?: EmailUtils.parseDateToTimestamp(dateRaw)
        )
    }

    private fun markEmailAsReadIfNeeded(email: EmailItem) {
        if (!email.isUnread || markReadInProgress) {
            return
        }

        markReadInProgress = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    inboxRepository.updateReadStatus(email.id, isUnread = false)
                }
                _uiState.update { state ->
                    val currentMail = state.mail ?: return@update state
                    state.copy(mail = currentMail.copy(isUnread = false))
                }
            } catch (e: Exception) {
                Log.e("MailDetailViewModel", "Failed to mark email as read", e)
            } finally {
                markReadInProgress = false
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

    private fun inferSourceLabel(labelIds: List<String>): String {
        val hasInbox = labelIds.any { it.equals("INBOX", ignoreCase = true) }
        val hasSent = labelIds.any { it.equals("SENT", ignoreCase = true) }

        return when {
            hasInbox -> "INBOX"
            hasSent -> "SENT"
            else -> labelIds.firstOrNull() ?: ""
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

    fun openAttachmentExternally(attachment: Attachment) {
        val mailId = _uiState.value.mail?.id ?: return
        viewModelScope.launch(Dispatchers.IO) {
            clearExternalOpenContentFile(_uiState.value.externalOpenContent)
            _uiState.update {
                it.copy(
                    isExternalOpenLoading = true,
                    externalOpenErrorMessage = null,
                    externalOpenContent = null
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
                    _uiState.update {
                        it.copy(
                            isExternalOpenLoading = false,
                            externalOpenErrorMessage = "파일을 불러오지 못했습니다. (${response.code()})"
                        )
                    }
                    return@launch
                }

                val body = response.body()
                if (body == null) {
                    _uiState.update {
                        it.copy(
                            isExternalOpenLoading = false,
                            externalOpenErrorMessage = "첨부파일이 비어 있습니다."
                        )
                    }
                    return@launch
                }

                val cachedFile = body.use { responseBody ->
                    writeExternalOpenFile(safeFilename, responseBody)
                }

                if (cachedFile == null) {
                    _uiState.update {
                        it.copy(
                            isExternalOpenLoading = false,
                            externalOpenErrorMessage = "파일을 준비하지 못했습니다."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isExternalOpenLoading = false,
                            externalOpenContent = AttachmentExternalContent(
                                filePath = cachedFile.absolutePath,
                                mimeType = safeMimeType
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MailDetailViewModel", "Error opening attachment externally", e)
                _uiState.update {
                    it.copy(
                        isExternalOpenLoading = false,
                        externalOpenErrorMessage = e.message ?: "외부 앱으로 여는 중 오류가 발생했습니다."
                    )
                }
            }
        }
    }

    fun consumeExternalOpenContent() {
        _uiState.update { it.copy(externalOpenContent = null) }
    }

    fun clearExternalOpenError() {
        _uiState.update { it.copy(externalOpenErrorMessage = null) }
    }

    fun previewAttachment(attachment: Attachment) {
        val mailId = _uiState.value.mail?.id ?: return
        val previewType = attachment.previewType()
        clearCachedPreviewFile(_uiState.value.previewContent)
        _uiState.update {
            it.copy(
                showPreviewDialog = true,
                previewTarget = attachment,
                previewContent = null,
                previewErrorMessage = null,
                isPreviewLoading = previewType != AttachmentPreviewType.UNSUPPORTED
            )
        }

        if (previewType == AttachmentPreviewType.UNSUPPORTED) {
            _uiState.update {
                it.copy(
                    isPreviewLoading = false,
                    previewErrorMessage = "이 형식은 미리보기를 지원하지 않습니다. 외부 앱으로 열어주세요."
                )
            }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
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
                    _uiState.update {
                        it.copy(
                            isPreviewLoading = false,
                            previewErrorMessage = "첨부파일을 불러오지 못했습니다. (${response.code()})"
                        )
                    }
                    return@launch
                }
                val body = response.body()
                if (body == null) {
                    _uiState.update {
                        it.copy(
                            isPreviewLoading = false,
                            previewErrorMessage = "첨부파일이 비어 있습니다."
                        )
                    }
                    return@launch
                }

                val previewContent = body.use { responseBody ->
                    when (previewType) {
                        AttachmentPreviewType.TEXT -> AttachmentPreviewContent.Text(readTextPreview(responseBody))
                        AttachmentPreviewType.PDF -> writePreviewFile(safeFilename, responseBody)?.let {
                            AttachmentPreviewContent.Pdf(it.absolutePath)
                        }
                        AttachmentPreviewType.UNSUPPORTED -> null
                    }
                }

                if (previewContent == null) {
                    _uiState.update {
                        it.copy(
                            isPreviewLoading = false,
                            previewErrorMessage = "미리보기를 준비하지 못했습니다."
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isPreviewLoading = false,
                            previewContent = previewContent,
                            previewErrorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("MailDetailViewModel", "Error previewing attachment", e)
                _uiState.update {
                    it.copy(
                        isPreviewLoading = false,
                        previewErrorMessage = e.message ?: "첨부파일 미리보기에 실패했습니다."
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

    fun dismissPreviewDialog() {
        clearCachedPreviewFile(_uiState.value.previewContent)
        _uiState.update {
            it.copy(
                showPreviewDialog = false,
                previewTarget = null,
                previewContent = null,
                previewErrorMessage = null,
                isPreviewLoading = false
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

    private fun readTextPreview(responseBody: ResponseBody, maxChars: Int = MAX_TEXT_PREVIEW_CHARS): String {
        val builder = StringBuilder()
        var truncated = false
        responseBody.charStream().use { reader ->
            val buffer = CharArray(2048)
            var total = 0
            while (true) {
                val read = reader.read(buffer)
                if (read == -1) break
                val remaining = maxChars - total
                if (remaining <= 0) {
                    truncated = true
                    break
                }
                val toAppend = min(remaining, read)
                builder.append(buffer, 0, toAppend)
                total += toAppend
                if (toAppend < read) {
                    truncated = true
                    break
                }
            }
        }
        if (truncated) {
            builder.append("\n\n… (내용이 길어 일부만 표시됩니다)")
        }
        return builder.toString().ifBlank { "내용을 불러오지 못했습니다." }
    }

    private fun writePreviewFile(filename: String, responseBody: ResponseBody): File? =
        writeCachedAttachmentFile("attachment_previews", "preview", filename, responseBody)

    private fun writeExternalOpenFile(filename: String, responseBody: ResponseBody): File? =
        writeCachedAttachmentFile("attachment_external", "external", filename, responseBody)

    private fun writeCachedAttachmentFile(
        subdirectory: String,
        prefix: String,
        filename: String,
        responseBody: ResponseBody
    ): File? {
        return try {
            val targetDir = File(appContext.cacheDir, subdirectory).apply {
                if (!exists()) {
                    mkdirs()
                }
            }
            val extension = filename.substringAfterLast('.', "")
            val suffix = if (extension.isNotBlank()) ".$extension" else ".tmp"
            val tempFile = File.createTempFile(
                "${prefix}_${System.currentTimeMillis()}",
                suffix,
                targetDir
            )
            FileOutputStream(tempFile).use { output ->
                responseBody.byteStream().use { input ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            Log.e("MailDetailViewModel", "Failed to cache attachment file", e)
            null
        }
    }

    private fun clearCachedPreviewFile(content: AttachmentPreviewContent?) {
        if (content is AttachmentPreviewContent.Pdf) {
            runCatching {
                val file = File(content.filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }

    private fun clearExternalOpenContentFile(content: AttachmentExternalContent?) {
        content?.let {
            runCatching {
                val file = File(it.filePath)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        clearCachedPreviewFile(_uiState.value.previewContent)
        clearExternalOpenContentFile(_uiState.value.externalOpenContent)
    }

    companion object {
        private const val MAX_TEXT_PREVIEW_CHARS = 20_000
    }
}
