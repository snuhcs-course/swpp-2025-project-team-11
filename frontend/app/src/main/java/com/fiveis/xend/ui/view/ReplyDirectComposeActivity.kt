package com.fiveis.xend.ui.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiveis.xend.BuildConfig
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.repository.ContactBookRepository
import com.fiveis.xend.network.MailComposeSseClient
import com.fiveis.xend.network.MailComposeWebSocketClient
import com.fiveis.xend.network.RetrofitClient
import com.fiveis.xend.ui.compose.ContactLookupViewModel
import com.fiveis.xend.ui.compose.MailComposeViewModel
import com.fiveis.xend.ui.compose.SendMailViewModel
import com.fiveis.xend.ui.compose.TemplateSelectionScreen
import com.fiveis.xend.ui.compose.common.SwipeSuggestionOverlay
import com.fiveis.xend.ui.compose.common.rememberXendRichEditorState
import com.fiveis.xend.ui.inbox.AddContactDialog
import com.fiveis.xend.ui.mail.MailActivity
import com.fiveis.xend.ui.theme.Blue60
import com.fiveis.xend.ui.theme.Blue80
import com.fiveis.xend.ui.theme.ComposeSurface
import com.fiveis.xend.ui.theme.StableColor
import com.fiveis.xend.ui.theme.TextPrimary
import com.fiveis.xend.ui.theme.TextSecondary
import com.fiveis.xend.ui.theme.ToolbarIconTint
import com.fiveis.xend.ui.theme.XendTheme
import com.fiveis.xend.utils.EmailUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class ReplyDirectComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Intent에서 메일 정보 가져오기
        val recipientEmailRaw = intent.getStringExtra("recipient_email") ?: ""
        val recipientName = intent.getStringExtra("recipient_name") ?: ""
        val initialSubject = intent.getStringExtra("subject") ?: ""
        val groupNames = intent.getStringArrayListExtra("group_names") ?: emptyList()
        val senderEmail = intent.getStringExtra("sender_email") ?: ""
        val date = intent.getStringExtra("date") ?: ""
        val originalBody = intent.getStringExtra("original_body") ?: ""
        val generatedBody = intent.getStringExtra("generated_body")
            ?.normalizeAsHtml()
            .orEmpty()

        // 이메일 주소 추출 ("이름 <email@example.com>" 형식에서 이메일만 추출)
        val recipientEmail = EmailUtils.extractEmailAddress(recipientEmailRaw)

        setContent {
            XendTheme {
                // AI Compose ViewModel 초기화
                val composeVm: MailComposeViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return MailComposeViewModel(
                                api = MailComposeSseClient(
                                    application.applicationContext,
                                    endpointUrl = BuildConfig.BASE_URL + "/api/ai/mail/generate/stream/"
                                ),
                                wsClient = MailComposeWebSocketClient(
                                    context = application.applicationContext,
                                    wsUrl = BuildConfig.WS_URL
                                )
                            ) as T
                        }
                    }
                )
                val composeUi by composeVm.ui.collectAsState()

                // SendMailViewModel 초기화
                val sendVm: SendMailViewModel = viewModel(
                    factory = SendMailViewModel.Factory(application)
                )
                val sendUiState by sendVm.ui.collectAsState()
                val context = LocalContext.current

                LaunchedEffect(sendVm, context) {
                    sendVm.toastEvents.collect { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }

                val lookupVm: ContactLookupViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ContactLookupViewModel(application) as T
                        }
                    }
                )

                // 템플릿 화면 상태
                var showTemplateScreen by remember { mutableStateOf(false) }
                var aiRealtime by rememberSaveable { mutableStateOf(true) }
                var canUndo by rememberSaveable { mutableStateOf(false) }
                var canRedo by rememberSaveable { mutableStateOf(false) }
                var showAddContactDialog by remember { mutableStateOf(false) }
                var showAiPromptDialog by remember { mutableStateOf(false) }
                var selectedContactForDialog by remember { mutableStateOf<Contact?>(null) }
                var availableGroups by remember { mutableStateOf<List<Group>>(emptyList()) }
                var recipientGroupNames by remember { mutableStateOf(groupNames.toList()) }
                val coroutineScope = rememberCoroutineScope()

                // subject와 body 상태 관리
                var currentSubject by rememberSaveable { mutableStateOf(initialSubject) }
                val editorState = rememberXendRichEditorState()
                val contactRepository = remember { ContactBookRepository(application.applicationContext) }

                // Track if generatedBody has been applied
                var generatedBodyApplied by remember { mutableStateOf(false) }

                // Enable/disable realtime mode
                LaunchedEffect(aiRealtime) {
                    composeVm.enableRealtimeMode(aiRealtime)
                }

                // AI가 생성한 본문이 있으면 초기값으로 설정 (한 번만)
                LaunchedEffect(generatedBody) {
                    android.util.Log.d(
                        "ReplyDirectCompose",
                        "LaunchedEffect(generatedBody): len=${generatedBody.length}, " +
                            "editor=${editorState.editor}, applied=$generatedBodyApplied"
                    )
                    if (generatedBody.isNotEmpty() && !generatedBodyApplied) {
                        // Wait for editor to be ready
                        while (editorState.editor == null) {
                            kotlinx.coroutines.delay(50)
                        }
                        // Additional delay to ensure WebView is fully loaded
                        kotlinx.coroutines.delay(200)
                        android.util.Log.d("ReplyDirectCompose", "Setting HTML from generatedBody (after delay)")
                        editorState.setHtml(generatedBody)
                        generatedBodyApplied = true
                        android.util.Log.d(
                            "ReplyDirectCompose",
                            "HTML set complete, applied flag = $generatedBodyApplied"
                        )
                    }
                }

                // Monitor text changes for realtime suggestions (AFTER generatedBody is applied)
                LaunchedEffect(editorState.editor, generatedBodyApplied) {
                    if (generatedBody.isEmpty() || generatedBodyApplied) {
                        editorState.editor?.setTextChangedListener { html ->
                            if (aiRealtime) {
                                composeVm.onTextChanged(
                                    currentText = html,
                                    subject = currentSubject
                                )
                            }
                        }
                    }
                }

                // Sync AI generated content (only when no Intent-provided content)
                LaunchedEffect(composeUi.subject) {
                    android.util.Log.d(
                        "ReplyDirectCompose",
                        "LaunchedEffect(subject): subj='${composeUi.subject}', " +
                            "genEmpty=${generatedBody.isEmpty()}"
                    )
                    if (composeUi.subject.isNotBlank() && generatedBody.isEmpty()) {
                        currentSubject = composeUi.subject
                    }
                }
                LaunchedEffect(composeUi.bodyRendered, generatedBody) {
                    android.util.Log.d(
                        "ReplyDirectCompose",
                        "LaunchedEffect(bodyRendered): bodyLen=${composeUi.bodyRendered.length}, " +
                            "genLen=${generatedBody.length}, editor=${editorState.editor}"
                    )
                    // Only apply AI-streamed body if there's no Intent-provided body
                    if (composeUi.bodyRendered.isNotEmpty() && generatedBody.isEmpty()) {
                        android.util.Log.d("ReplyDirectCompose", "Setting HTML from bodyRendered")
                        editorState.setHtml(composeUi.bodyRendered)
                    }
                }

                LaunchedEffect(Unit) {
                    contactRepository.observeGroups().collect { loadedGroups ->
                        availableGroups = loadedGroups
                    }
                }

                val knownContacts by lookupVm.byEmail.collectAsState()
                val normalizedRecipient = recipientEmail.trim().lowercase()
                val recipientContact = knownContacts[normalizedRecipient]
                val resolvedRecipientName = recipientContact?.let { "${it.name} <${it.email}>" } ?: recipientName

                LaunchedEffect(recipientContact) {
                    recipientGroupNames =
                        recipientContact?.group?.name?.let { listOf(it) } ?: groupNames.toList()
                }

                // Banner state for send results
                var bannerState by remember { mutableStateOf<com.fiveis.xend.ui.compose.BannerState?>(null) }

                LaunchedEffect(recipientContact) {
                    bannerState = if (recipientContact == null) {
                        com.fiveis.xend.ui.compose.BannerState(
                            message = "받는 사람이 연락처에 저장되어 있지 않습니다. 저장하면 향상된 답장을 받을 수 있어요.",
                            type = com.fiveis.xend.ui.compose.BannerType.INFO
                        )
                    } else {
                        // 등록된 연락처인 경우 - AI 작성 방식 안내
                        com.fiveis.xend.ui.compose.BannerState(
                            message = "AI가 연락처 정보를 활용해 메일을 작성해요. 자세히 보기",
                            type = com.fiveis.xend.ui.compose.BannerType.INFO,
                            onActionClick = { showAiPromptDialog = true }
                        )
                    }
                }

                LaunchedEffect(sendUiState.lastSuccessMsg) {
                    sendUiState.lastSuccessMsg?.let {
                        bannerState = com.fiveis.xend.ui.compose.BannerState(
                            message = "메일 전송에 성공했습니다.",
                            type = com.fiveis.xend.ui.compose.BannerType.SUCCESS,
                            actionText = "홈 화면 이동하기",
                            onActionClick = {
                                val intent = Intent(this@ReplyDirectComposeActivity, MailActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                startActivity(intent)
                                finish()
                            }
                        )
                    }
                }

                LaunchedEffect(sendUiState.error) {
                    sendUiState.error?.let {
                        bannerState = com.fiveis.xend.ui.compose.BannerState(
                            message = "메일 전송에 실패했습니다. 다시 시도해주세요.",
                            type = com.fiveis.xend.ui.compose.BannerType.ERROR
                        )
                    }
                }

                val acceptSuggestion: () -> Unit = {
                    composeVm.skipNextTextChangeSend()
                    editorState.acceptSuggestion()
                    editorState.requestFocusAndShowKeyboard()
                    composeVm.acceptSuggestion()
                    // Allow the editor to reflect the accepted text before sending.
                    coroutineScope.launch {
                        delay(50)
                        composeVm.requestImmediateSuggestion(
                            currentText = editorState.getHtml(),
                            subject = currentSubject
                        )
                    }
                }

                val undoAction: () -> Unit = {
                    composeVm.undo(currentSubject, editorState.getHtml())?.let { snapshot ->
                        currentSubject = snapshot.subject
                        editorState.setHtml(snapshot.bodyHtml)
                        canUndo = false
                        canRedo = true
                    }
                }

                val redoAction: () -> Unit = {
                    composeVm.redo(currentSubject, editorState.getHtml())?.let { snapshot ->
                        currentSubject = snapshot.subject
                        editorState.setHtml(snapshot.bodyHtml)
                        canUndo = true
                        canRedo = false
                    }
                }

                if (showTemplateScreen) {
                    // 템플릿 선택 화면
                    TemplateSelectionScreen(
                        onBack = { showTemplateScreen = false },
                        onTemplateSelected = { template ->
                            currentSubject = template.subject
                            editorState.setHtml(template.body)
                            showTemplateScreen = false
                        }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ReplyDirectComposeScreen(
                            recipientEmail = recipientEmail,
                            recipientName = resolvedRecipientName,
                            subject = currentSubject,
                            groups = recipientGroupNames,
                            onBack = { finish() },
                            onSend = { bodyText ->
                                // 이메일 전송
                                sendVm.sendEmail(
                                    to = listOf(recipientEmail),
                                    subject = currentSubject,
                                    body = bodyText
                                )
                            },
                            onTemplateClick = { showTemplateScreen = true },
                            onSubjectChange = { currentSubject = it },
                            editorState = editorState,
                            senderEmail = senderEmail,
                            date = date,
                            originalBody = originalBody,
                            sendUiState = sendUiState,
                            // AI 관련 파라미터
                            isStreaming = composeUi.isStreaming,
                            suggestionText = composeUi.suggestionText,
                            aiRealtime = aiRealtime,
                            onUndo = undoAction,
                            onRedo = redoAction,
                            onAiComplete = {
                                // Save current state before AI generation
                                composeVm.saveUndoSnapshot(
                                    subject = currentSubject,
                                    bodyHtml = editorState.getHtml()
                                )
                                canUndo = true
                                canRedo = false

                                val payload = JSONObject().apply {
                                    put("subject", currentSubject.ifBlank { "제목 생성" })
                                    put("body", editorState.getHtml().ifBlank { "간단한 답장 내용" })
                                    put("to_emails", JSONArray(listOf(recipientEmail)))
                                    // 답장이므로 원본 메일 정보 포함
                                    if (originalBody.isNotEmpty()) {
                                        put("reply_body", originalBody)
                                    }
                                }
                                composeVm.startStreaming(payload)
                            },
                            onStopStreaming = { composeVm.stopStreaming() },
                            onAcceptSuggestion = acceptSuggestion,
                            onAiRealtimeToggle = { aiRealtime = it },
                            bannerState = bannerState,
                            onDismissBanner = { bannerState = null },
                            showInlineSwipeBar = false,
                            canUndo = canUndo,
                            canRedo = canRedo,
                            showAddContactButton = recipientContact == null,
                            onAddContactClick = {
                                selectedContactForDialog = Contact(
                                    id = -1L,
                                    name = resolvedRecipientName.substringBefore("<").trim().ifBlank { recipientEmail },
                                    email = recipientEmail,
                                    group = null
                                )
                                showAddContactDialog = true
                            }
                        )

                        SwipeSuggestionOverlay(
                            visible = composeUi.suggestionText.isNotEmpty(),
                            onSwipe = acceptSuggestion
                        )
                    }
                }

                if (showAddContactDialog) {
                    selectedContactForDialog?.let { contact ->
                        AddContactDialog(
                            senderName = contact.name.orEmpty(),
                            senderEmail = contact.email,
                            groups = availableGroups,
                            onDismiss = {
                                showAddContactDialog = false
                                selectedContactForDialog = null
                            },
                            onConfirm = { name, email, senderRole, recipientRole, personalPrompt, groupId, language ->
                                coroutineScope.launch {
                                    try {
                                        contactRepository.addContact(
                                            name = name,
                                            email = email,
                                            groupId = groupId,
                                            senderRole = senderRole,
                                            recipientRole = recipientRole,
                                            personalPrompt = personalPrompt,
                                            languagePreference = language
                                        )
                                        showAddContactDialog = false
                                        selectedContactForDialog = null
                                        recipientGroupNames = groupId?.let { id ->
                                            availableGroups.find { it.id == id }?.name?.let { listOf(it) }
                                        } ?: emptyList()
                                        bannerState = com.fiveis.xend.ui.compose.BannerState(
                                            message = "연락처를 추가했습니다.",
                                            type = com.fiveis.xend.ui.compose.BannerType.SUCCESS,
                                            autoDismiss = true
                                        )
                                    } catch (e: Exception) {
                                        bannerState = com.fiveis.xend.ui.compose.BannerState(
                                            message = "연락처 추가 실패: ${e.message}",
                                            type = com.fiveis.xend.ui.compose.BannerType.ERROR,
                                            autoDismiss = true
                                        )
                                    }
                                }
                            }
                        )
                    }
                }

                // Show AI Prompt Preview Dialog
                if (showAiPromptDialog) {
                    recipientContact?.let { contact ->
                        AiPromptPreviewDialog(
                            contacts = listOf(contact),
                            onDismiss = { showAiPromptDialog = false }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Normalize a plain-text reply body so the rich editor respects formatting.
 */
private fun String.normalizeAsHtml(): String {
    if (isBlank()) return ""
    return if (containsHtmlMarkup()) {
        this
    } else {
        toHtmlPreservingLineBreaks()
    }
}

private fun String.containsHtmlMarkup(): Boolean {
    val trimmed = trim()
    if (!trimmed.contains('<')) return false
    val lower = trimmed.lowercase()
    return lower.contains("<br") ||
        lower.contains("</") ||
        lower.contains("<p") ||
        lower.contains("<div") ||
        lower.contains("<span")
}

private fun String.toHtmlPreservingLineBreaks(): String {
    val sb = StringBuilder(length + 16)
    var index = 0
    while (index < length) {
        when (val ch = this[index]) {
            '&' -> sb.append("&amp;")
            '<' -> sb.append("&lt;")
            '>' -> sb.append("&gt;")
            '"' -> sb.append("&quot;")
            '\'' -> sb.append("&#39;")
            '\r' -> {
                if (index + 1 < length && this[index + 1] == '\n') {
                    index++
                }
                sb.append("<br>")
            }
            '\n' -> sb.append("<br>")
            else -> sb.append(ch)
        }
        index++
    }
    return sb.toString()
}

@Composable
private fun AiPromptPreviewDialog(contacts: List<Contact>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var promptData by remember { mutableStateOf<com.fiveis.xend.network.PromptPreviewResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Fetch prompt preview data
    LaunchedEffect(contacts) {
        isLoading = true
        errorMessage = null
        try {
            val aiApiService = RetrofitClient.getAiApiService(context)
            val requestEmails = contacts.map { it.email }
            Log.d("AiPromptPreview", "Requesting prompt preview for emails: $requestEmails")

            val response = aiApiService.getPromptPreview(
                com.fiveis.xend.network.PromptPreviewRequest(
                    to = requestEmails
                )
            )

            Log.d("AiPromptPreview", "Response code: ${response.code()}")

            if (response.isSuccessful) {
                promptData = response.body()
                Log.d("AiPromptPreview", "Success! Data: $promptData")
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("AiPromptPreview", "Error ${response.code()}: $errorBody")
                errorMessage = "프롬프트 정보를 불러오는데 실패했습니다.\n코드: ${response.code()}\n${errorBody?.take(200) ?: ""}"
            }
        } catch (e: Exception) {
            Log.e("AiPromptPreview", "Exception: ${e.message}", e)
            errorMessage = "네트워크 오류: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "AI 작성 방식",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Recipient info
                Text(
                    text = "받는 사람",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = ToolbarIconTint
                    )
                )
                contacts.forEach { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ComposeSurface)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(StableColor.forId(contact.id)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (contact.name?.firstOrNull() ?: '?').uppercaseChar().toString(),
                                style = MaterialTheme.typography.labelMedium.copy(color = Color.White)
                            )
                        }
                        Column {
                            Text(
                                text = contact.name ?: "",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = contact.email,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                }

                // Loading/Error/Data state
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Blue60)
                        }
                    }
                    errorMessage != null -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = errorMessage ?: "오류가 발생했습니다",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    promptData != null -> {
                        Text(
                            text = "AI는 다음 정보를 활용해요",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = ToolbarIconTint
                            )
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = Blue80.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, Blue80.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = promptData?.previewText?.takeIf { it.isNotBlank() }
                                    ?: "해당 수신자에 대한 프롬프트 정보를 아직 준비하지 못했습니다.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "확인",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Blue60,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    )
}
