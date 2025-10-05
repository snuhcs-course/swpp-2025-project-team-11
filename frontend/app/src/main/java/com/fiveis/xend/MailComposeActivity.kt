package com.fiveis.xend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

// ========================================================
// Models & UI States
// ========================================================
data class SendUiState(
    val isSending: Boolean = false,
    val lastSuccessMsg: String? = null,
    val error: String? = null
)

data class SendResponse(val id: String, val threadId: String, val labelIds: List<String>)

// ========================================================
// ViewModels
// ========================================================
class SendMailViewModel(
    private val endpointUrl: String,
    private val accessToken: String?,
    private val repo: MailSendRepository = MailSendRepository()
) : ViewModel() {

    private val _ui = MutableStateFlow(SendUiState())
    val ui: StateFlow<SendUiState> = _ui

    fun sendEmail(to: String, subject: String, body: String) {
        if (to.isBlank()) {
            _ui.value = SendUiState(isSending = false, error = "수신자(to)가 비어있습니다.")
            return
        }
        _ui.value = SendUiState(isSending = true)
        viewModelScope.launch {
            try {
                val res = repo.sendEmail(endpointUrl, to, subject, body, accessToken)
                _ui.value = SendUiState(
                    isSending = false,
                    lastSuccessMsg = "전송 완료: ${res.id}",
                    error = null
                )
            } catch (e: Exception) {
                _ui.value = SendUiState(
                    isSending = false,
                    error = e.message ?: "알 수 없는 오류"
                )
            }
        }
    }
}

// ========================================================
// Repositories
// ========================================================
class MailSendRepository(
    private val client: OkHttpClient = OkHttpClient()
) {
    private val jsonMT = "application/json".toMediaType()

    suspend fun sendEmail(
        endpointUrl: String,
        to: String,
        subject: String,
        body: String,
        accessToken: String?
    ): SendResponse = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("to", to)
            put("subject", subject)
            put("body", body)
        }
        val json = payload.toString()

        val req = Request.Builder()
            .url(endpointUrl)
            .post(json.toRequestBody(jsonMT))
            .apply {
                if (!accessToken.isNullOrEmpty()) {
                    header("Authorization", "Bearer $accessToken")
                }
            }
            .build()

        client.newCall(req).execute().use { resp ->
            val respText = resp.body?.string().orEmpty()
            if (resp.code == 201 && resp.isSuccessful) {
                val obj = JSONObject(respText)
                val ids = obj.optJSONArray("labelIds")?.let { arr ->
                    List(arr.length()) { i -> arr.getString(i) }
                } ?: emptyList()
                return@use SendResponse(
                    id = obj.getString("id"),
                    threadId = obj.optString("threadId"),
                    labelIds = ids
                )
            } else {
                throw IllegalStateException(
                    "Send failed: HTTP ${resp.code} ${resp.message} | body=${respText.take(500)}"
                )
            }
        }
    }
}

// ========================================================
// Screen
// ========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailComposeScreen(
    subject: String,
    onSubjectChange: (String) -> Unit,
    body: String,
    onBodyChange: (String) -> Unit,
    contacts: List<Contact>,
    onContactsChange: (List<Contact>) -> Unit,
    newContact: TextFieldValue,
    onNewContactChange: (TextFieldValue) -> Unit,
    isStreaming: Boolean,
    error: String?,
    onBack: () -> Unit = {},
    onUndo: () -> Unit = {},
    onAiComplete: () -> Unit = {},
    onStopStreaming: () -> Unit = {},
    modifier: Modifier = Modifier,
    sendUiState: SendUiState,
    onSend: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "메일 작성",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    IconButton(onClick = { /* 템플릿 */ }) {
                        Icon(Icons.Default.GridView, contentDescription = "템플릿")
                    }
                    IconButton(onClick = { /* 첨부파일 */ }) {
                        Icon(Icons.Default.Attachment, contentDescription = "첨부파일")
                    }
                    IconButton(
                        onClick = onSend,
                        enabled = !sendUiState.isSending && contacts.isNotEmpty()
                    ) {
                        if (sendUiState.isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "전송")
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { inner ->
        Column(
            Modifier
                .padding(inner)
                .fillMaxSize()
        ) {
            // 상단 액션: 실행취소 / AI완성 (Stop 버튼 표시)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onUndo,
                    shape = RoundedCornerShape(20.dp)
                ) { Text("실행취소") }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isStreaming) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = onStopStreaming) { Text("Stop") }
                        Spacer(Modifier.width(8.dp))
                    }
                    FilledTonalButton(
                        onClick = onAiComplete,
                        shape = RoundedCornerShape(20.dp),
                        enabled = !isStreaming
                    ) { Text("AI 완성") }
                }
            }

            // 섹션: 받는 사람
            SectionHeader(text = "받는 사람")
            Column(Modifier.padding(horizontal = 16.dp)) {
                LazyRow(contentPadding = PaddingValues(end = 8.dp)) {
                    items(contacts, key = { it.email }) { r ->
                        ContactChip(contact = r, onRemove = {
                            onContactsChange(contacts.filterNot { it.email == r.email })
                        })
                        Spacer(Modifier.width(8.dp))
                    }
                    item {
                        AssistChip(
                            onClick = {
                                val t = newContact.text.trim()
                                if (t.isNotEmpty()) {
                                    onContactsChange(contacts + Contact(t, t))
                                    onNewContactChange(TextFieldValue(""))
                                }
                            },
                            label = { Text("+") }
                        )
                    }
                }
                OutlinedTextField(
                    value = newContact,
                    onValueChange = onNewContactChange,
                    placeholder = { Text("이메일 입력 후 Enter") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }

            // 섹션: 제목
            SectionHeader(text = "제목")
            OutlinedTextField(
                value = subject,
                onValueChange = onSubjectChange,
                placeholder = { Text("제목") },
                singleLine = true,
                enabled = !isStreaming,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            // 섹션: 본문 + 실시간 AI 토글
            var aiRealtime by rememberSaveable { mutableStateOf(false) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("본문", style = MaterialTheme.typography.titleSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("실시간 AI", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(6.dp))
                    Switch(checked = aiRealtime, onCheckedChange = { aiRealtime = it })
                }
            }

            OutlinedTextField(
                value = body,
                onValueChange = onBodyChange,
                placeholder = { Text("내용을 입력하세요") },
                enabled = !isStreaming,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                minLines = 10
            )

            // Error (if any)
            error?.let {
                Text(
                    text = "Error: $it",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun ContactChip(contact: Contact, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(contact.color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.toString() ?: "?",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${contact.name} (${contact.email})",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "×",
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onRemove() }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

class ComposeVmFactory(private val client: MailComposeSseClient) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MailComposeViewModel(client) as T
    }
}

// ========================================================
// Activity: wire screen <-> ViewModel <-> SSE
// ========================================================
class MailComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Read access token from EncryptedSharedPreferences
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val encryptedPrefs = EncryptedSharedPreferences.create(
            applicationContext,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        val accessToken = encryptedPrefs.getString("access_token", null)

        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                // 1) AI Compose VM
                val composeVm: MailComposeViewModel = viewModel(
                    factory = ComposeVmFactory(MailComposeSseClient(BuildConfig.SSE_URL))
                )

                // 2) Mail Send VM
                val sendVm: SendMailViewModel = viewModel(
                    key = "sendVm",
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SendMailViewModel(BuildConfig.SEND_URL, accessToken) as T
                        }
                    }
                )

                // Hoisted states
                var subject by rememberSaveable { mutableStateOf("") }
                var body by rememberSaveable {
                    mutableStateOf(
                        "안녕하세요, 대표님.\n\nQ4 실적 보고서를 검토했습니다.\n\n전반적으로 매출 증가율이 목표치를 상회하는 우수한 성과라고 판단됩니다."
                    )
                }
                var contacts by remember { mutableStateOf(emptyList<Contact>()) }
                var newContact by remember { mutableStateOf(TextFieldValue("")) }

                // Collect UI states from ViewModels
                val composeUi by composeVm.ui.collectAsState()
                val sendUi by sendVm.ui.collectAsState()

                // Sync state from AI ViewModel to local state
                LaunchedEffect(composeUi.subject) { if (composeUi.subject.isNotBlank()) subject = composeUi.subject }
                LaunchedEffect(
                    composeUi.bodyRendered
                ) { if (composeUi.bodyRendered.isNotEmpty()) body = composeUi.bodyRendered }

                // Show snackbar for send results
                val snackHost = remember { SnackbarHostState() }
                LaunchedEffect(sendUi.lastSuccessMsg, sendUi.error) {
                    sendUi.lastSuccessMsg?.let { snackHost.showSnackbar(it) }
                    sendUi.error?.let { snackHost.showSnackbar("전송 실패: $it") }
                }

                Scaffold(snackbarHost = { SnackbarHost(snackHost) }) { innerPadding ->
                    EmailComposeScreen(
                        modifier = Modifier.padding(innerPadding),
                        subject = subject,
                        onSubjectChange = { subject = it },
                        body = body,
                        onBodyChange = { body = it },
                        contacts = contacts,
                        onContactsChange = { contacts = it },
                        newContact = newContact,
                        onNewContactChange = { newContact = it },
                        isStreaming = composeUi.isStreaming,
                        error = composeUi.error,
                        sendUiState = sendUi,
                        onBack = { finish() },
                        onUndo = { /* TODO */ },
                        onAiComplete = {
                            val payload = JSONObject().apply {
                                put("subject", subject.ifBlank { "제목 생성" })
                                put("body", body.ifBlank { "간단한 인사와 핵심 내용으로 작성해 주세요." })
                                put("relationship", "업무 관련")
                                put("situational_prompt", "정중하고 간결한 결과 보고 메일")
                                put("style_prompt", "정중, 명료, 불필요한 수식어 제외")
                                put("format_prompt", "문단 구분, 끝인사 포함")
                                put("language", "Korean")
                            }
                            composeVm.startStreaming(payload)
                        },
                        onStopStreaming = { composeVm.stopStreaming() },
                        onSend = {
                            val recipient = contacts.firstOrNull()?.email
                            if (recipient == null) {
                                // This case is handled by button's enabled state, but as a safeguard:
                                return@EmailComposeScreen
                            }
                            sendVm.sendEmail(to = recipient, subject = subject, body = body)
                        }
                    )
                }
            }
        }
    }
}

// ========================================================
// Preview
// ========================================================
@Preview(showBackground = true)
@Composable
private fun EmailComposePreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        EmailComposeScreen(
            subject = "초안 제목",
            onSubjectChange = {},
            body = "초안 본문...",
            onBodyChange = {},
            contacts = listOf(Contact("홍길동", "test@example.com")),
            onContactsChange = {},
            newContact = TextFieldValue(""),
            onNewContactChange = {},
            isStreaming = false,
            error = null,
            sendUiState = SendUiState(),
            onSend = {}
        )
    }
}
