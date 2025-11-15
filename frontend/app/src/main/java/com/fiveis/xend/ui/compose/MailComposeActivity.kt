package com.fiveis.xend.ui.compose

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiveis.xend.BuildConfig
import com.fiveis.xend.data.database.AppDatabase
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.data.model.DraftItem
import com.fiveis.xend.data.model.Group
import com.fiveis.xend.data.repository.ContactBookRepository
import com.fiveis.xend.data.repository.InboxRepository
import com.fiveis.xend.network.MailComposeSseClient
import com.fiveis.xend.network.MailComposeWebSocketClient
import com.fiveis.xend.network.RetrofitClient
import com.fiveis.xend.ui.compose.common.AIEnhancedRichTextEditor
import com.fiveis.xend.ui.compose.common.BodyHeader
import com.fiveis.xend.ui.compose.common.SwipeSuggestionOverlay
import com.fiveis.xend.ui.compose.common.rememberXendRichEditorState
import com.fiveis.xend.ui.inbox.AddContactDialog
import com.fiveis.xend.ui.mail.MailActivity
import com.fiveis.xend.ui.theme.AddButtonText
import com.fiveis.xend.ui.theme.Blue60
import com.fiveis.xend.ui.theme.Blue80
import com.fiveis.xend.ui.theme.ComposeBackground
import com.fiveis.xend.ui.theme.ComposeOutline
import com.fiveis.xend.ui.theme.ComposeSurface
import com.fiveis.xend.ui.theme.StableColor
import com.fiveis.xend.ui.theme.TextPrimary
import com.fiveis.xend.ui.theme.TextSecondary
import com.fiveis.xend.ui.theme.ToolbarIconTint
import com.fiveis.xend.ui.theme.XendTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class BannerState(
    val message: String,
    val type: BannerType,
    val autoDismiss: Boolean = false,
    val actionText: String? = null,
    val onActionClick: (() -> Unit)? = null
)

// ========================================================
// Screen
// ========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailComposeScreen(
    subject: String,
    onSubjectChange: (String) -> Unit,
    editorState: com.fiveis.xend.ui.compose.common.XendRichEditorState,
    contacts: List<Contact>,
    onContactsChange: (List<Contact>) -> Unit,
    newContact: TextFieldValue,
    onNewContactChange: (TextFieldValue) -> Unit,
    knownContactsByEmail: Map<String, Contact> = emptyMap(),
    isStreaming: Boolean,
    error: String?,
    onBack: () -> Unit = {},
    onTemplateClick: () -> Unit = {},
    onUndo: () -> Unit = {},
    onAiComplete: () -> Unit = {},
    onStopStreaming: () -> Unit = {},
    modifier: Modifier = Modifier,
    sendUiState: SendUiState,
    onSend: () -> Unit,
    suggestionText: String = "",
    onAcceptSuggestion: () -> Unit = {},
    aiRealtime: Boolean = true,
    onAiRealtimeToggle: (Boolean) -> Unit = {},
    onAddContactClick: ((Contact) -> Unit)? = null,
    bannerState: BannerState?,
    onDismissBanner: () -> Unit,
    showInlineSwipeBar: Boolean = true,
    canUndo: Boolean,
    canRedo: Boolean,
    onRedo: () -> Unit,
    contactSuggestions: List<Contact>,
    onSuggestionClick: (Contact) -> Unit,
    onShowPrompt: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = ComposeBackground,
        topBar = {
            ComposeTopBar(
                scrollBehavior = scrollBehavior,
                onBack = onBack,
                onTemplateClick = onTemplateClick,
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
            AnimatedVisibility(visible = bannerState != null) {
                bannerState?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Banner(
                            message = it.message,
                            type = it.type,
                            onDismiss = onDismissBanner,
                            actionText = null,
                            onActionClick = null,
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(bottom = 8.dp)
                                .then(
                                    if (it.onActionClick != null) {
                                        Modifier.clickable { it.onActionClick.invoke() }
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }
            }

            SectionHeaderWithAction(
                title = "받는 사람",
                actionLabel = null,
                actionEnabled = false,
                onActionClick = null
            )
            Spacer(modifier = Modifier.height(4.dp))
            RecipientSection(
                contacts = contacts,
                onContactsChange = onContactsChange,
                newContact = newContact,
                onNewContactChange = onNewContactChange,
                knownContactsByEmail = knownContactsByEmail,
                onAddContactClick = onAddContactClick,
                contactSuggestions = contactSuggestions,
                onSuggestionClick = onSuggestionClick
            )

            Spacer(modifier = Modifier.height(8.dp))

            SubjectControlRow(
                label = "제목",
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = onUndo,
                onRedo = onRedo,
                undoRedoVisible = !isStreaming && (canUndo || canRedo),
                isStreaming = isStreaming,
                aiCompleteEnabled = contacts.isNotEmpty(),
                onAiComplete = onAiComplete,
                onStopStreaming = onStopStreaming
            )

            SubjectField(
                value = subject,
                enabled = !isStreaming,
                onValueChange = onSubjectChange
            )

            Spacer(modifier = Modifier.height(10.dp))

            BodyHeader(
                isRealtimeOn = aiRealtime,
                onToggle = onAiRealtimeToggle
            )
            AIEnhancedRichTextEditor(
                editorState = editorState,
                isStreaming = isStreaming,
                suggestionText = suggestionText,
                onAcceptSuggestion = onAcceptSuggestion,
                showInlineSwipeBar = showInlineSwipeBar,
                onEditorFocused = {
                    coroutineScope.launch {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                }
            )

            Spacer(modifier = Modifier.height(240.dp))

            error?.let { ErrorMessage(it) }
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
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        title = {
            Text(
                "메일 작성",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary),
                modifier = Modifier.padding(start = 8.dp)
            )
        },
        navigationIcon = {
            ToolbarIconButton(
                onClick = onBack,
                border = null,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로가기",
                    tint = ToolbarIconTint
                )
            }
        },
        actions = {
            ToolbarIconButton(
                onClick = onTemplateClick,
                border = null,
                modifier = Modifier.padding(end = 2.dp)
            ) {
                Icon(TemplateTIcon, contentDescription = "템플릿", tint = Color(0xFFF59E0B))
            }
            Spacer(modifier = Modifier.width(8.dp))
            ToolbarIconButton(
                onClick = onAttachmentClick,
                border = null,
                modifier = Modifier.padding(end = 2.dp)
            ) {
                Icon(Icons.Default.Attachment, contentDescription = "첨부파일", tint = ToolbarIconTint)
            }
            Spacer(modifier = Modifier.width(8.dp))
            ToolbarIconButton(
                onClick = onSend,
                border = null,
                enabled = !sendUiState.isSending,
                containerColor = Color.Transparent,
                contentTint = if (canSend) Blue60 else ToolbarIconTint.copy(alpha = 0.4f),
                modifier = Modifier.padding(start = 2.dp)
            ) {
                if (sendUiState.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Blue60
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "전송",
                        tint = if (canSend) Blue60 else ToolbarIconTint.copy(alpha = 0.4f)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = ComposeBackground,
            scrolledContainerColor = ComposeBackground
        ),
        windowInsets = WindowInsets(0, 0, 0, 0),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun ToolbarIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: Color = Color.Transparent,
    border: BorderStroke? = BorderStroke(1.dp, ComposeOutline),
    contentTint: Color = TextSecondary,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.size(40.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.5f),
        border = border
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = contentTint
            )
        ) {
            content()
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    SectionHeaderWithAction(title = text, actionLabel = null, actionEnabled = true, onActionClick = null)
}

@Composable
private fun SectionHeaderWithAction(
    title: String,
    actionLabel: String?,
    actionEnabled: Boolean,
    onActionClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                color = ToolbarIconTint,
                fontWeight = FontWeight.SemiBold
            )
        )

        if (!actionLabel.isNullOrEmpty() && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                enabled = actionEnabled,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (actionEnabled) Blue60 else Blue60.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun UndoRedoButton(icon: ImageVector, contentDescription: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ComposeOutline),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) ToolbarIconTint else ToolbarIconTint.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SubjectControlRow(
    label: String,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    undoRedoVisible: Boolean,
    isStreaming: Boolean,
    onAiComplete: () -> Unit,
    onStopStreaming: () -> Unit,
    aiCompleteEnabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                color = ToolbarIconTint,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(visible = undoRedoVisible) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UndoRedoButton(
                        icon = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "실행취소",
                        enabled = canUndo,
                        onClick = onUndo
                    )
                    UndoRedoButton(
                        icon = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "다시 실행",
                        enabled = canRedo,
                        onClick = onRedo
                    )
                }
            }
            if (isStreaming) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onStopStreaming,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = "AI 중지",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "중지",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFEF4444)
                            )
                        )
                    }
                    Text(
                        text = "AI 플래너가 메일 구조를 설계 중입니다",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = Blue60
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onAiComplete,
                    enabled = aiCompleteEnabled,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(
                        1.dp,
                        if (aiCompleteEnabled) Blue60 else Blue60.copy(alpha = 0.3f)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Blue60,
                        disabledContentColor = Blue60.copy(alpha = 0.4f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "AI 완성",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI 완성",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipientSection(
    contacts: List<Contact>,
    onContactsChange: (List<Contact>) -> Unit,
    newContact: TextFieldValue,
    onNewContactChange: (TextFieldValue) -> Unit,
    knownContactsByEmail: Map<String, Contact>,
    onAddContactClick: ((Contact) -> Unit)? = null,
    contactSuggestions: List<Contact> = emptyList(),
    onSuggestionClick: (Contact) -> Unit = {}
) {
    fun normalizeEmail(s: String) = s.trim().lowercase()

    val addContact: () -> Unit = fun() {
        val raw = newContact.text.trim()
        if (raw.isEmpty()) return

        val email = Regex("<([^>]+)>").find(raw)?.groupValues?.getOrNull(1)?.trim() ?: raw

        // 이메일 중복 방지
        if (contacts.any { normalizeEmail(it.email) == normalizeEmail(email) }) {
            onNewContactChange(TextFieldValue(""))
            return
        }

        // DB 매칭
        val saved = knownContactsByEmail[normalizeEmail(email)]

        val contactToAdd = saved ?: Contact(id = -1L, name = email, email = email, group = null)

        onContactsChange(contacts + contactToAdd)
        onNewContactChange(TextFieldValue(""))
    }

    val scrollState = rememberScrollState()
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, ComposeOutline),
            color = ComposeSurface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 36.dp)
                    .padding(horizontal = 12.dp)
                    .horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically
            ) {
                contacts.forEachIndexed { index, contact ->
                    ContactChip(
                        contact = contact,
                        onRemove = {
                            onContactsChange(contacts.filterNot { it.email == contact.email })
                        },
                        onEdit = {
                            onNewContactChange(TextFieldValue(contact.email))
                            onContactsChange(contacts.filterNot { it.email == contact.email })
                        },
                        onAddToContacts = if (contact.id < 0L && onAddContactClick != null) {
                            { onAddContactClick(contact) }
                        } else {
                            null
                        }
                    )
                    if (index != contacts.lastIndex) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
                if (contacts.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
                RecipientInputField(
                    value = newContact,
                    onValueChange = onNewContactChange,
                    onAdd = addContact
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        AnimatedVisibility(visible = contactSuggestions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "연락처 추천",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = ToolbarIconTint,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                contactSuggestions.forEach { suggestion ->
                    RecipientSuggestionRow(
                        contact = suggestion,
                        onClick = { onSuggestionClick(suggestion) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipientSuggestionRow(contact: Contact, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(ComposeSurface)
            .border(BorderStroke(1.dp, ComposeOutline), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val displayName = contact.name?.takeIf { it.isNotBlank() } ?: contact.email
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Blue80.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.labelLarge.copy(color = Blue80)
            )
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = contact.email,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecipientInputField(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, onAdd: () -> Unit) {
    val borderModifier = if (value.text.isNotEmpty()) {
        Modifier.border(BorderStroke(1.dp, AddButtonText), RoundedCornerShape(6.dp))
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .height(24.dp)
            .widthIn(min = 80.dp, max = 200.dp)
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(ComposeSurface)
            .then(borderModifier),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = AddButtonText),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onAdd() })
        ) { inner ->
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
            ) {
                if (value.text.isEmpty()) {
                    Text(
                        text = "이메일 입력",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = AddButtonText.copy(alpha = 0.5f)
                        )
                    )
                }
                inner()
            }
        }
    }
}

@Composable
private fun SubjectField(value: String, enabled: Boolean, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                "제목을 입력하세요",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
            )
        },
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = composeOutlinedTextFieldColors(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(48.dp),
        textStyle = MaterialTheme.typography.bodySmall
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
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFDCFCE7),
        modifier = Modifier.height(30.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "실시간 AI",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color(0xFF166534),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Switch(
                checked = isChecked,
                onCheckedChange = onToggle,
                modifier = Modifier
                    .scale(0.6f)
                    .width(20.dp)
                    .height(20.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF166534),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = ComposeOutline
                )
            )
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
fun ContactChip(
    contact: Contact,
    onRemove: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onAddToContacts: (() -> Unit)? = null
) {
    val isKnown = contact.id >= 0L
    val borderColor = if (!isKnown) Color.Black else StableColor.forId(contact.id)
    val dotColor = if (!isKnown) Color.Black else StableColor.forId(contact.id)

    Surface(
        modifier = Modifier
            .height(24.dp)
            .clickable(enabled = onEdit != null) { onEdit?.invoke() },
        shape = RoundedCornerShape(12.dp),
        color = ComposeSurface,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .height(24.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(dotColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (contact.id < 0L) "?" else (contact.name.firstOrNull() ?: '?').uppercaseChar().toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (contact.id < 0L) {
                    // 연락처가 없으면 이메일만 표시
                    contact.email
                } else {
                    // 연락처가 있으면 이름 (이메일) 형식
                    "${contact.name} (${contact.email})"
                },
                style =
                if (contact.id < 0L) {
                    MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                } else {
                    MaterialTheme.typography.labelSmall.copy(color = Blue80)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(6.dp))

            // Add contact button for unknown contacts
            if (!isKnown && onAddToContacts != null) {
                Icon(
                    imageVector = Icons.Outlined.PersonAdd,
                    contentDescription = "연락처 추가",
                    tint = Blue80,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { onAddToContacts() }
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "삭제",
                tint = AddButtonText,
                modifier = Modifier
                    .size(12.dp)
                    .clickable { onRemove() }
            )
        }
    }
}

class ComposeVmFactory(
    private val sseClient: MailComposeSseClient,
    private val wsClient: MailComposeWebSocketClient
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MailComposeViewModel(sseClient, wsClient) as T
    }
}

// ========================================================
// Activity: wire screen <-> ViewModel <-> SSE
// ========================================================
class MailComposeActivity : ComponentActivity() {
    companion object {
        const val EXTRA_PREFILL_CONTACT_ID = "extra_prefill_contact_id"
        const val EXTRA_PREFILL_CONTACT_NAME = "extra_prefill_contact_name"
        const val EXTRA_PREFILL_CONTACT_EMAIL = "extra_prefill_contact_email"

        const val REQUEST_CODE_COMPOSE = 1001
        const val RESULT_DRAFT_SAVED = RESULT_FIRST_USER + 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefilledContact = intent?.let { incomingIntent ->
            val email = incomingIntent.getStringExtra(EXTRA_PREFILL_CONTACT_EMAIL)?.trim().orEmpty()
            if (email.isBlank()) {
                null
            } else {
                val contactId = incomingIntent.getLongExtra(EXTRA_PREFILL_CONTACT_ID, -1L)
                val contactName = incomingIntent.getStringExtra(EXTRA_PREFILL_CONTACT_NAME)
                Contact(
                    id = if (contactId >= 0) contactId else -1L,
                    name = contactName?.takeIf { it.isNotBlank() } ?: email,
                    email = email
                )
            }
        }

        setContent {
            XendTheme {
                // 1) AI Compose VM
                val composeVm: MailComposeViewModel = viewModel(
                    factory = ComposeVmFactory(
                        sseClient = MailComposeSseClient(
                            application.applicationContext,
                            endpointUrl = BuildConfig.BASE_URL + "/api/ai/mail/generate/stream/"
                        ),
                        wsClient = MailComposeWebSocketClient(
                            context = application.applicationContext,
                            wsUrl = BuildConfig.WS_URL
                        )
                    )
                )

                // 2) Mail Send VM
                val sendVm: SendMailViewModel = viewModel(
                    key = "sendVm",
                    factory = SendMailViewModel.Factory(application)
                )

                // 3) Contact Lookup VM
                val lookupVm: ContactLookupViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ContactLookupViewModel(application) as T
                        }
                    }
                )
                val knownByEmail by lookupVm.byEmail.collectAsState() // Map<String, Contact

                // Hoisted states
                var subject by rememberSaveable { mutableStateOf("") }
                val editorState = rememberXendRichEditorState()

                // Draft loading states
                var showLoadDraftDialog by remember { mutableStateOf(false) }
                var loadedDraft: DraftItem? by remember { mutableStateOf(null) }

                var contacts by remember { mutableStateOf(prefilledContact?.let(::listOf) ?: emptyList<Contact>()) }
                var newContact by remember { mutableStateOf(TextFieldValue("")) }
                var contactSuggestions by remember { mutableStateOf<List<Contact>>(emptyList()) }

                var showTemplateScreen by remember { mutableStateOf(false) }
                var aiRealtime by rememberSaveable { mutableStateOf(true) }
                var canUndo by rememberSaveable { mutableStateOf(false) }
                var canRedo by rememberSaveable { mutableStateOf(false) }

                // Banner state
                var bannerState by remember { mutableStateOf<BannerState?>(null) }
                // Auto-dismiss logic for banners
                LaunchedEffect(bannerState) {
                    if (bannerState?.autoDismiss == true) {
                        kotlinx.coroutines.delay(3000)
                        bannerState = null
                    }
                }

                // AI Prompt Dialog state
                var showAiPromptDialog by remember { mutableStateOf(false) }

                // Show banner based on contact status
                LaunchedEffect(contacts, knownByEmail) {
                    val hasUnknown = contacts.any { contact ->
                        contact.id < 0L && contact.email.lowercase() !in knownByEmail
                    }
                    val hasKnownContacts = contacts.any { contact ->
                        contact.id >= 0L
                    }

                    bannerState = when {
                        hasUnknown -> {
                            // 미등록 연락처가 있는 경우
                            BannerState(
                                message = "저장되지 않은 연락처가 있습니다. 저장하면 더 향상된 AI 메일 작성이 가능해요.",
                                type = BannerType.INFO
                            )
                        }
                        hasKnownContacts -> {
                            // 등록된 연락처만 있는 경우 - AI 작성 방식 안내
                            BannerState(
                                message = "AI가 연락처 정보를 활용해 메일을 작성해요. 자세히 보기",
                                type = BannerType.INFO,
                                onActionClick = { showAiPromptDialog = true }
                            )
                        }
                        else -> null
                    }
                }

                // Add Contact Dialog states
                var showAddContactDialog by remember { mutableStateOf(false) }
                var selectedContactForDialog by remember { mutableStateOf<Contact?>(null) }
                var groups by remember { mutableStateOf<List<Group>>(emptyList()) }
                val coroutineScope = rememberCoroutineScope()

                // Save Draft Dialog states
                var showSaveDraftDialog by remember { mutableStateOf(false) }
                var draftSubjectToSave by remember { mutableStateOf("") }
                var draftBodyToSave by remember { mutableStateOf("") }

                // Repositories
                val contactRepository = remember { ContactBookRepository(application.applicationContext) }
                val inboxRepository = remember {
                    InboxRepository(
                        mailApiService = RetrofitClient.getMailApiService(application.applicationContext),
                        emailDao = AppDatabase.getDatabase(application.applicationContext).emailDao()
                    )
                }
                LaunchedEffect(Unit) {
                    contactRepository.observeGroups().collect { loadedGroups ->
                        groups = loadedGroups
                    }
                }

                // Check for existing draft for the first recipient
                LaunchedEffect(contacts) {
                    val firstRecipientEmail = contacts.firstOrNull()?.email
                    if (firstRecipientEmail != null) {
                        val draft = inboxRepository.getDraftByRecipient(firstRecipientEmail)
                        if (draft != null) {
                            loadedDraft = draft
                            showLoadDraftDialog = true
                        }
                    }
                }

                LaunchedEffect(newContact.text) {
                    val query = newContact.text.trim()
                    if (query.length < 2) {
                        contactSuggestions = emptyList()
                    } else {
                        delay(250)
                        val results = runCatching {
                            contactRepository.searchContacts(query).first()
                        }.getOrDefault(emptyList())
                        contactSuggestions = results
                            .filterNot { candidate ->
                                contacts.any { it.email.equals(candidate.email, ignoreCase = true) }
                            }
                            .take(5)
                    }
                }

                // Handle back press to show save draft dialog
                val onBackPressedCallback = remember {
                    object : OnBackPressedCallback(true) {
                        override fun handleOnBackPressed() {
                            Log.d("SaveDraftDebug", "Back button pressed in MailComposeActivity.")
                            // Check if there's content to save
                            val hasContent = subject.isNotBlank() || editorState.getText().isNotBlank()
                            val hasRecipient = contacts.isNotEmpty() // Check for recipients
                            Log.d("SaveDraftDebug", "hasContent: $hasContent, hasRecipient: $hasRecipient")
                            if (hasContent && hasRecipient) { // Only show dialog if there's content AND a recipient
                                draftSubjectToSave = subject
                                draftBodyToSave = editorState.getHtml()
                                showSaveDraftDialog = true
                                Log.d("SaveDraftDebug", "showSaveDraftDialog set to true.")
                            } else {
                                Log.d("SaveDraftDebug", "No content or no recipient, finishing activity.")
                                finish()
                            }
                        }
                    }
                }
                DisposableEffect(onBackPressedCallback) {
                    onBackPressedDispatcher.addCallback(onBackPressedCallback)
                    onDispose {
                        onBackPressedCallback.remove()
                    }
                }

                // Collect UI states from ViewModels
                val composeUi by composeVm.ui.collectAsState()
                val sendUi by sendVm.ui.collectAsState()

                val undoAction: () -> Unit = {
                    composeVm.undo(subject, editorState.getHtml())?.let { snapshot ->
                        subject = snapshot.subject
                        editorState.setHtml(snapshot.bodyHtml)
                        canUndo = false
                        canRedo = true
                    }
                }

                val redoAction: () -> Unit = {
                    composeVm.redo(subject, editorState.getHtml())?.let { snapshot ->
                        subject = snapshot.subject
                        editorState.setHtml(snapshot.bodyHtml)
                        canUndo = true
                        canRedo = false
                    }
                }

                // Enable/disable realtime mode when toggle changes
                DisposableEffect(aiRealtime) {
                    composeVm.enableRealtimeMode(aiRealtime)

                    onDispose {
                        // Composable이 사라질 때 무조건 WebSocket 끊기
                        if (aiRealtime) {
                            composeVm.enableRealtimeMode(false)
                        }
                    }
                }

                // Sync state from AI ViewModel to local state
                LaunchedEffect(composeUi.subject) { if (composeUi.subject.isNotBlank()) subject = composeUi.subject }
                LaunchedEffect(composeUi.bodyRendered) {
                    if (composeUi.bodyRendered.isNotEmpty()) {
                        editorState.setHtml(composeUi.bodyRendered)
                    }
                }

                // Monitor text changes for realtime suggestions
                LaunchedEffect(editorState.editor) {
                    editorState.editor?.setOnTextChangeListener { html ->
                        if (aiRealtime) {
                            composeVm.onTextChanged(html)
                        }
                    }
                }

                // Update banner state for send results
                LaunchedEffect(sendUi.lastSuccessMsg) {
                    sendUi.lastSuccessMsg?.let {
                        bannerState = BannerState(
                            message = "메일 전송에 성공했습니다.",
                            type = BannerType.SUCCESS,
                            actionText = "홈 화면 이동하기",
                            onActionClick = {
                                // Navigate to MailActivity
                                val intent = Intent(this@MailComposeActivity, MailActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                startActivity(intent)
                                finish()
                            }
                        )
                    }
                }

                LaunchedEffect(sendUi.error) {
                    sendUi.error?.let {
                        bannerState = BannerState(
                            message = "메일 전송에 실패했습니다. 다시 시도해주세요.",
                            type = BannerType.ERROR
                        )
                    }
                }

                val acceptSuggestion: () -> Unit = {
                    editorState.acceptSuggestion()
                    editorState.requestFocusAndShowKeyboard()
                    composeVm.acceptSuggestion()
                    composeVm.requestImmediateSuggestion(editorState.getHtml())
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    Scaffold { innerPadding ->
                        if (showTemplateScreen) {
                            // 템플릿 선택 화면
                            TemplateSelectionScreen(
                                onBack = { showTemplateScreen = false },
                                onTemplateSelected = { template ->
                                    subject = template.subject
                                    editorState.setHtml(template.body)
                                    showTemplateScreen = false
                                },
                                modifier = Modifier.padding(innerPadding)
                            )
                        } else {
                            // 메일 작성 화면
                            EmailComposeScreen(
                                modifier = Modifier.padding(innerPadding),
                                subject = subject,
                                onSubjectChange = { subject = it },
                                editorState = editorState,
                                contacts = contacts,
                                onContactsChange = { contacts = it },
                                newContact = newContact,
                                onNewContactChange = { newContact = it },
                                knownContactsByEmail = knownByEmail,
                                isStreaming = composeUi.isStreaming,
                                error = composeUi.error,
                                sendUiState = sendUi,
                                // Trigger our custom back press handling
                                onBack = { onBackPressedDispatcher.onBackPressed() },
                                onTemplateClick = { showTemplateScreen = true },
                                onUndo = undoAction,
                                suggestionText = composeUi.suggestionText,
                                onAcceptSuggestion = acceptSuggestion,
                                aiRealtime = aiRealtime,
                                onAiRealtimeToggle = { aiRealtime = it },
                                onAiComplete = {
                                    // Save current state before AI generation
                                    composeVm.saveUndoSnapshot(
                                        subject = subject,
                                        bodyHtml = editorState.getHtml()
                                    )
                                    canUndo = true
                                    canRedo = false

                                    val payload = JSONObject().apply {
                                        put("subject", subject.ifBlank { "제목 생성" })
                                        // Use HTML content for AI prompt
                                        put("body", editorState.getHtml().ifBlank { "간단한 인사와 핵심 내용으로 작성" })
                                        put("to_emails", JSONArray(contacts.map { it.email }))
//                                    put("relationship", "업무 관련")
//                                    put("situational_prompt", "정중하고 간결한 결과 보고 메일")
//                                    put("style_prompt", "정중, 명료, 불필요한 수식어 제외")
//                                    put("format_prompt", "문단 구분, 끝인사 포함")
//                                    put("language", "Korean")
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
                                    // Send HTML content
                                    sendVm.sendEmail(
                                        to = contacts.map { it.email },
                                        subject = subject.ifBlank { "(제목 없음)" },
                                        body = editorState.getHtml()
                                    )
                                },
                                onAddContactClick = { contact ->
                                    selectedContactForDialog = contact
                                    showAddContactDialog = true
                                },
                                bannerState = bannerState,
                                onDismissBanner = { bannerState = null },
                                showInlineSwipeBar = false,
                                canUndo = canUndo,
                                canRedo = canRedo,
                                onRedo = redoAction,
                                contactSuggestions = contactSuggestions,
                                onSuggestionClick = { suggestion ->
                                    if (contacts.none { it.email.equals(suggestion.email, ignoreCase = true) }) {
                                        contacts = contacts + suggestion
                                    }
                                    newContact = TextFieldValue("")
                                    contactSuggestions = emptyList()
                                },
                                onShowPrompt = {
                                    // TODO: Hook up prompt viewer once backend API is ready
                                }
                            )

                            // Show Add Contact Dialog
                            if (showAddContactDialog) {
                                selectedContactForDialog?.let { contact ->
                                    AddContactDialog(
                                        senderName = "",
                                        senderEmail = contact.email,
                                        groups = groups,
                                        onDismiss = {
                                            showAddContactDialog = false
                                            selectedContactForDialog = null
                                        },
                                        onConfirm = { name, email, senderRole, recipientRole, personalPrompt, groupId ->
                                            coroutineScope.launch {
                                                try {
                                                    contactRepository.addContact(
                                                        name = name,
                                                        email = email,
                                                        groupId = groupId,
                                                        senderRole = senderRole,
                                                        recipientRole = recipientRole,
                                                        personalPrompt = personalPrompt
                                                    )
                                                    showAddContactDialog = false
                                                    selectedContactForDialog = null
                                                    // Show success banner
                                                    bannerState = BannerState(
                                                        message = "연락처가 추가되었습니다.",
                                                        type = BannerType.SUCCESS,
                                                        autoDismiss = true
                                                    )
                                                } catch (e: Exception) {
                                                    // Handle error
                                                    bannerState = BannerState(
                                                        message = "오류: ${e.message}",
                                                        type = BannerType.ERROR,
                                                        autoDismiss = true
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            // Show Save Draft Confirmation Dialog
                            if (showSaveDraftDialog) {
                                SaveDraftConfirmationDialog(
                                    onDismiss = { showSaveDraftDialog = false },
                                    onSave = {
                                        coroutineScope.launch {
                                            val draftId = inboxRepository.saveDraft(
                                                DraftItem(
                                                    subject = draftSubjectToSave,
                                                    body = draftBodyToSave,
                                                    recipients = contacts.map { it.email }
                                                )
                                            )
                                            // Optionally show a banner for draft saved
                                            bannerState = BannerState(
                                                message = "임시 저장되었습니다. (ID: $draftId)",
                                                type = BannerType.INFO,
                                                autoDismiss = true
                                            )
                                            showSaveDraftDialog = false
                                            setResult(RESULT_DRAFT_SAVED, Intent()) // Set result for parent activity
                                            finish()
                                        }
                                    },
                                    onDiscard = {
                                        showSaveDraftDialog = false
                                        finish()
                                    }
                                )
                            }

                            // Show Load Draft Confirmation Dialog
                            if (showLoadDraftDialog) {
                                LoadDraftConfirmationDialog(
                                    onDismiss = { showLoadDraftDialog = false },
                                    onLoad = {
                                        loadedDraft?.let { draft ->
                                            subject = draft.subject
                                            editorState.setHtml(draft.body)
                                            // Populate contacts from draft recipients
                                            contacts = draft.recipients.map { email ->
                                                val normalized = email.trim().lowercase()
                                                knownByEmail[normalized]
                                                    ?: Contact(id = -1L, name = email, email = email, group = null)
                                            }
                                            coroutineScope.launch {
                                                inboxRepository.deleteDraft(draft.id) // Delete loaded draft
                                            }
                                        }
                                        showLoadDraftDialog = false
                                    },
                                    onDiscard = {
                                        coroutineScope.launch {
                                            loadedDraft?.let { draft ->
                                                inboxRepository.deleteDraft(draft.id) // Delete discarded draft
                                            }
                                        }
                                        showLoadDraftDialog = false
                                    }
                                )
                            }

                            // Show AI Prompt Preview Dialog
                            if (showAiPromptDialog) {
                                AiPromptPreviewDialog(
                                    contacts = contacts,
                                    onDismiss = { showAiPromptDialog = false }
                                )
                            }
                        }
                    }
                }

                SwipeSuggestionOverlay(
                    visible = composeUi.suggestionText.isNotEmpty(),
                    onSwipe = acceptSuggestion
                )
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
        val editorState = rememberXendRichEditorState()
        EmailComposeScreen(
            subject = "초안 제목",
            onSubjectChange = {},
            editorState = editorState,
            contacts = listOf(Contact(0L, null, "홍길동", "test@example.com")),

            onContactsChange = {},
            newContact = TextFieldValue(""),
            onNewContactChange = {},
            isStreaming = false,
            error = null,
            sendUiState = SendUiState(),
            onTemplateClick = {},
            onSend = {},
            suggestionText = "",
            onAcceptSuggestion = {},
            aiRealtime = true,
            onAiRealtimeToggle = {},
            bannerState = null,
            onDismissBanner = {},
            canUndo = false,
            canRedo = false,
            onRedo = {},
            contactSuggestions = emptyList(),
            onSuggestionClick = {},
            onShowPrompt = {}
        )
    }
}

@Composable
private fun SaveDraftConfirmationDialog(onDismiss: () -> Unit, onSave: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("메일 임시 저장") },
        text = { Text("작성 중인 메일 내용이 있습니다. 임시 저장하시겠습니까?") },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("임시 저장")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDiscard) {
                    Text("삭제")
                }
                TextButton(onClick = onDismiss) {
                    Text("취소")
                }
            }
        }
    )
}

@Composable
private fun LoadDraftConfirmationDialog(onDismiss: () -> Unit, onLoad: () -> Unit, onDiscard: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("임시 저장된 메일 불러오기") },
        text = { Text("이전에 작성하던 메일이 있습니다. 불러오시겠습니까?") },
        confirmButton = {
            TextButton(onClick = onLoad) {
                Text("불러오기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}

@Composable
private fun AiPromptPreviewDialog(contacts: List<Contact>, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
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
                                text = (contact.name.firstOrNull() ?: '?').uppercaseChar().toString(),
                                style = MaterialTheme.typography.labelMedium.copy(color = Color.White)
                            )
                        }
                        Column {
                            Text(
                                text = contact.name,
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
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                promptData?.relationship?.let {
                                    InfoRow(label = "관계", value = it)
                                }
                                if (promptData?.sender_role != null || promptData?.recipient_role != null) {
                                    val sender = promptData?.sender_role ?: ""
                                    val recipient = promptData?.recipient_role ?: ""
                                    val separator = if (sender.isNotEmpty() && recipient.isNotEmpty()) " → " else ""
                                    InfoRow(
                                        label = "역할",
                                        value = "$sender$separator$recipient"
                                    )
                                }
                                promptData?.personal_prompt?.let {
                                    InfoRow(label = "개인 프롬프트", value = it)
                                }
                                promptData?.situational_prompt?.let {
                                    InfoRow(label = "상황", value = it)
                                }
                                promptData?.style_prompt?.let {
                                    InfoRow(label = "스타일", value = it)
                                }
                                promptData?.format_prompt?.let {
                                    InfoRow(label = "형식", value = it)
                                }
                            }
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

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = ToolbarIconTint,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(0.3f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
            modifier = Modifier.weight(0.7f)
        )
    }
}
