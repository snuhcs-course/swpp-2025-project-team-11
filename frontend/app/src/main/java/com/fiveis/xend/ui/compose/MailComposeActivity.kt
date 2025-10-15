package com.fiveis.xend.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.fiveis.xend.BuildConfig
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.network.MailComposeSseClient
import com.fiveis.xend.ui.theme.BannerBackground
import com.fiveis.xend.ui.theme.BannerBorder
import com.fiveis.xend.ui.theme.BannerText
import com.fiveis.xend.ui.theme.Blue60
import com.fiveis.xend.ui.theme.ChipBackground
import com.fiveis.xend.ui.theme.ComposeBackground
import com.fiveis.xend.ui.theme.ComposeOutline
import com.fiveis.xend.ui.theme.ComposeSurface
import com.fiveis.xend.ui.theme.KeyboardPlaceholderColor
import com.fiveis.xend.ui.theme.Purple60
import com.fiveis.xend.ui.theme.SuccessBorder
import com.fiveis.xend.ui.theme.SuccessSurface
import com.fiveis.xend.ui.theme.TextPrimary
import com.fiveis.xend.ui.theme.TextSecondary
import com.fiveis.xend.ui.theme.ToolbarIconBackground
import org.json.JSONObject

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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scrollState = rememberScrollState()
    var showBanner by rememberSaveable { mutableStateOf(true) }
    var aiRealtime by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = ComposeBackground,
        topBar = {
            ComposeTopBar(
                scrollBehavior = scrollBehavior,
                onBack = onBack,
                onTemplateClick = { /* 템플릿 진입 예정 */ },
                onAttachmentClick = { /* 첨부파일 선택 예정 */ },
                onSend = onSend,
                sendUiState = sendUiState,
                canSend = contacts.isNotEmpty()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(ComposeBackground)
                .verticalScroll(scrollState)
                .imePadding()
        ) {
            if (showBanner) {
                ComposeInfoBanner(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .align(Alignment.CenterHorizontally),
                    onDismiss = { showBanner = false }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            ComposeActionRow(
                isStreaming = isStreaming,
                onUndo = onUndo,
                onAiComplete = onAiComplete,
                onStopStreaming = onStopStreaming
            )

            SectionHeader("받는 사람")
            RecipientSection(
                contacts = contacts,
                onContactsChange = onContactsChange,
                newContact = newContact,
                onNewContactChange = onNewContactChange
            )

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader("제목")
            SubjectField(
                value = subject,
                enabled = !isStreaming,
                onValueChange = onSubjectChange
            )

            Spacer(modifier = Modifier.height(24.dp))

            BodyHeader(
                isRealtimeOn = aiRealtime,
                onToggle = { aiRealtime = it }
            )
            MailBodyCard(
                body = body,
                onBodyChange = onBodyChange,
                isStreaming = isStreaming,
                onTapComplete = onAiComplete
            )

            error?.let { ErrorMessage(it) }

            KeyboardPlaceholder(
                modifier = Modifier
                    .padding(top = 24.dp, bottom = 32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    onBack: () -> Unit,
    onTemplateClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    onSend: () -> Unit,
    sendUiState: SendUiState,
    canSend: Boolean
) {
    TopAppBar(
        modifier = Modifier
            .height(72.dp)
            .padding(horizontal = 12.dp),
        title = {
            Text(
                "메일 작성",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary),
                modifier = Modifier.padding(start = 15.dp)
            )
        },
        navigationIcon = {
            ToolbarIconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = TextSecondary
                )
            }
        },
        actions = {
            ToolbarIconButton(
                onClick = onTemplateClick,
                modifier = Modifier.padding(end = 2.dp)
            ) {
                Icon(Icons.Default.GridView, contentDescription = "템플릿", tint = TextSecondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            ToolbarIconButton(
                onClick = onAttachmentClick,
                modifier = Modifier.padding(end = 2.dp)
            ) {
                Icon(Icons.Default.Attachment, contentDescription = "첨부파일", tint = TextSecondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            ToolbarIconButton(
                onClick = onSend,
                enabled = canSend && !sendUiState.isSending,
                containerColor = Blue60,
                modifier = Modifier.padding(start = 2.dp)
            ) {
                if (sendUiState.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "전송", tint = Color.White)
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ComposeSurface,
            scrolledContainerColor = ComposeSurface
        ),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun ToolbarIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: Color = ToolbarIconBackground,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.5f))
    ) {
        content()
    }
}

@Composable
private fun ComposeInfoBanner(modifier: Modifier = Modifier, onDismiss: () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BannerBorder),
        color = BannerBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = BannerText,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "연락처를 저장하면 향상된 AI 메일 작성이 가능합니다.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "닫기", tint = TextSecondary)
            }
        }
    }
}

@Composable
private fun ComposeActionRow(
    isStreaming: Boolean,
    onUndo: () -> Unit,
    onAiComplete: () -> Unit,
    onStopStreaming: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onUndo,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, ComposeOutline),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
        ) {
            Text("실행취소")
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(visible = isStreaming) {
                OutlinedButton(
                    onClick = onStopStreaming,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, BannerBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BannerText)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "중지", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("중지")
                }
            }
            Button(
                onClick = onAiComplete,
                enabled = !isStreaming,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple60,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("AI 완성")
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun BodyHeader(isRealtimeOn: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "본문",
            style = MaterialTheme.typography.titleSmall.copy(
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
        )
        RealtimeToggleChip(
            isChecked = isRealtimeOn,
            onToggle = onToggle
        )
    }
}

@Composable
private fun RecipientSection(
    contacts: List<Contact>,
    onContactsChange: (List<Contact>) -> Unit,
    newContact: TextFieldValue,
    onNewContactChange: (TextFieldValue) -> Unit
) {
    val addContact = {
        val trimmed = newContact.text.trim()
        if (trimmed.isNotEmpty()) {
            onContactsChange(contacts + Contact(trimmed, trimmed))
            onNewContactChange(TextFieldValue(""))
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ComposeOutline),
        color = ComposeSurface
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 4.dp)
            ) {
                items(contacts, key = { it.email }) { contact ->
                    ContactChip(contact = contact) {
                        onContactsChange(contacts.filterNot { it.email == contact.email })
                    }
                }
                item {
                    AddRecipientChip(
                        enabled = newContact.text.isNotBlank(),
                        onClick = addContact
                    )
                }
            }

            OutlinedTextField(
                value = newContact,
                onValueChange = onNewContactChange,
                placeholder = { Text("이메일 주소 입력", color = TextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = composeOutlinedTextFieldColors(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { addContact() }
                )
            )
        }
    }
}

@Composable
private fun AddRecipientChip(enabled: Boolean, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        label = { Text("추가") },
        leadingIcon = {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
        },
        border = AssistChipDefaults.assistChipBorder(
            enabled = enabled,
            borderColor = ComposeOutline
        ),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = ComposeSurface,
            labelColor = if (enabled) Blue60 else TextSecondary,
            leadingIconContentColor = if (enabled) Blue60 else TextSecondary
        )
    )
}

@Composable
private fun SubjectField(value: String, enabled: Boolean, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("제목을 입력하세요", color = TextSecondary) },
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = composeOutlinedTextFieldColors(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    )
}

@Composable
private fun composeOutlinedTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = ComposeSurface,
    unfocusedContainerColor = ComposeSurface,
    disabledContainerColor = ComposeSurface,
    errorContainerColor = ComposeSurface,
    focusedIndicatorColor = Blue60,
    unfocusedIndicatorColor = ComposeOutline,
    disabledIndicatorColor = ComposeOutline,
    errorIndicatorColor = MaterialTheme.colorScheme.error,
    cursorColor = Blue60,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    disabledTextColor = TextPrimary.copy(alpha = 0.4f),
    focusedPlaceholderColor = TextSecondary,
    unfocusedPlaceholderColor = TextSecondary,
    disabledPlaceholderColor = TextSecondary.copy(alpha = 0.4f),
    focusedSupportingTextColor = TextSecondary,
    unfocusedSupportingTextColor = TextSecondary,
    disabledSupportingTextColor = TextSecondary.copy(alpha = 0.4f)
)

@Composable
private fun RealtimeToggleChip(isChecked: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = SuccessSurface,
        border = BorderStroke(1.dp, SuccessBorder)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "실시간 AI",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = SuccessBorder,
                    fontWeight = FontWeight.Medium
                )
            )
            Switch(
                checked = isChecked,
                onCheckedChange = onToggle,
                modifier = Modifier.scale(0.8f),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = SuccessBorder,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = ComposeOutline
                )
            )
        }
    }
}

@Composable
private fun MailBodyCard(
    body: String,
    onBodyChange: (String) -> Unit,
    isStreaming: Boolean,
    onTapComplete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, ComposeOutline),
        color = ComposeSurface
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 240.dp)
        ) {
            BasicTextField(
                value = body,
                onValueChange = onBodyChange,
                enabled = !isStreaming,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 68.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (body.isEmpty()) {
                            Text(
                                text = "내용을 입력하세요",
                                style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary)
                            )
                        }
                        innerTextField()
                    }
                }
            )

            TextButton(
                onClick = onTapComplete,
                enabled = !isStreaming,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("탭 완성")
            }
        }
    }
}

@Composable
private fun ErrorMessage(message: String) {
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    )
}

@Composable
private fun KeyboardPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(KeyboardPlaceholderColor),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Keyboard, contentDescription = null, tint = TextSecondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "가상 키보드 영역",
                style = MaterialTheme.typography.labelLarge.copy(color = TextSecondary)
            )
        }
    }
}

@Composable
fun ContactChip(contact: Contact, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = ChipBackground,
        border = BorderStroke(1.dp, ComposeOutline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
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
            Spacer(modifier = Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.labelLarge.copy(color = TextPrimary)
                )
                Text(
                    text = contact.email,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "삭제", tint = TextSecondary)
            }
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
