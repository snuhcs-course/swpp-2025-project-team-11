package com.fiveis.xend.ui.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fiveis.xend.BuildConfig
import com.fiveis.xend.data.model.Contact
import com.fiveis.xend.network.MailComposeSseClient
import com.fiveis.xend.network.MailComposeWebSocketClient
import com.fiveis.xend.ui.theme.AddButtonBackground
import com.fiveis.xend.ui.theme.AddButtonText
import com.fiveis.xend.ui.theme.BannerBackground
import com.fiveis.xend.ui.theme.BannerBorder
import com.fiveis.xend.ui.theme.BannerText
import com.fiveis.xend.ui.theme.Blue60
import com.fiveis.xend.ui.theme.ComposeBackground
import com.fiveis.xend.ui.theme.ComposeOutline
import com.fiveis.xend.ui.theme.ComposeSurface
import com.fiveis.xend.ui.theme.SuccessSurface
import com.fiveis.xend.ui.theme.TextPrimary
import com.fiveis.xend.ui.theme.TextSecondary
import com.fiveis.xend.ui.theme.ToolbarIconTint
import com.fiveis.xend.ui.theme.UndoBorder
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import org.json.JSONArray
import org.json.JSONObject

// ========================================================
// Screen
// ========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailComposeScreen(
    subject: String,
    onSubjectChange: (String) -> Unit,
    richTextState: com.mohamedrejeb.richeditor.model.RichTextState,
    contacts: List<Contact>,
    onContactsChange: (List<Contact>) -> Unit,
    newContact: TextFieldValue,
    onNewContactChange: (TextFieldValue) -> Unit,
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
    onAiRealtimeToggle: (Boolean) -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val scrollState = rememberScrollState()
    var showBanner by rememberSaveable { mutableStateOf(true) }

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
                onToggle = onAiRealtimeToggle
            )
            RichTextEditorCard(
                richTextState = richTextState,
                isStreaming = isStreaming,
                onTapComplete = onAiComplete,
                suggestionText = suggestionText,
                onAcceptSuggestion = onAcceptSuggestion
            )

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
                Icon(Icons.Default.GridView, contentDescription = "템플릿", tint = Color(0xFFF59E0B))
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
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = BannerText,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "연락처를 저장하면 향상된 AI 메일 작성이 가능합니다.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = TextSecondary
                )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "닫기",
                    modifier = Modifier.size(16.dp)
                )
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
            .padding(horizontal = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onUndo,
            modifier = Modifier.size(width = 94.dp, height = 35.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, UndoBorder),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = UndoBorder,
                disabledContentColor = UndoBorder.copy(alpha = 0.2f)
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "실행취소",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "실행취소",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                )
            }
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
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    modifier = Modifier.size(width = 104.dp, height = 35.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = BannerText,
                        disabledContentColor = BannerText.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "중지", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "중지",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
            OutlinedButton(
                onClick = onAiComplete,
                modifier = Modifier.size(width = 96.dp, height = 35.dp),
                enabled = !isStreaming,
                contentPadding = PaddingValues(horizontal = 15.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Blue60),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Blue60,
                    disabledContentColor = Blue60.copy(alpha = 0.4f)
                )
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "AI 완성",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(
            color = ToolbarIconTint,
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
                color = ToolbarIconTint,
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
            onContactsChange(
                contacts + Contact(
                    id = 0,
                    name = trimmed,
                    email = trimmed,
                    group = null
                )
            )
            onNewContactChange(TextFieldValue(""))
        }
    }

    val scrollState = rememberScrollState()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AddButtonText.copy(alpha = 0.15f)),
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
            .background(Color.White)
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
        shape = RoundedCornerShape(18.dp),
        color = SuccessSurface
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "실시간 AI",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color(0xFF166534),
                    fontWeight = FontWeight.Bold
                )
            )
            Switch(
                checked = isChecked,
                onCheckedChange = onToggle,
                modifier = Modifier.scale(0.6f),
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
private fun RichTextEditorControls(
    state: com.mohamedrejeb.richeditor.model.RichTextState,
    modifier: Modifier = Modifier
) {
    var showSizeDropdown by remember { mutableStateOf(false) }
    val fontSizes = listOf(14.sp, 18.sp, 22.sp)

    var showColorDropdown by remember { mutableStateOf(false) }
    val colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bold button
        IconButton(onClick = { state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) }) {
            Icon(
                imageVector = Icons.Default.FormatBold,
                contentDescription = "Bold",
                tint = if (state.currentSpanStyle.fontWeight == FontWeight.Bold) Blue60 else TextSecondary
            )
        }
        // Italic button
        IconButton(onClick = { state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) }) {
            Icon(
                imageVector = Icons.Default.FormatItalic,
                contentDescription = "Italic",
                tint = if (state.currentSpanStyle.fontStyle == FontStyle.Italic) Blue60 else TextSecondary
            )
        }
        // Underline button
        IconButton(onClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) }) {
            Icon(
                imageVector = Icons.Default.FormatUnderlined,
                contentDescription = "Underline",
                tint = if (state.currentSpanStyle.textDecoration
                        ?.contains(TextDecoration.Underline) == true
                ) {
                    Blue60
                } else {
                    TextSecondary
                }
            )
        }
        // Strikethrough button
        IconButton(onClick = { state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) }) {
            Icon(
                imageVector = Icons.Default.FormatStrikethrough,
                contentDescription = "Strikethrough",
                tint = if (state.currentSpanStyle.textDecoration
                        ?.contains(TextDecoration.LineThrough) == true
                ) {
                    Blue60
                } else {
                    TextSecondary
                }
            )
        }

        // Font size selector
        Box {
            IconButton(onClick = { showSizeDropdown = true }) {
                Icon(Icons.Default.FormatSize, contentDescription = "Font Size")
            }
            DropdownMenu(expanded = showSizeDropdown, onDismissRequest = { showSizeDropdown = false }) {
                fontSizes.forEach { size ->
                    DropdownMenuItem(text = { Text("${size.value}") }, onClick = {
                        state.toggleSpanStyle(SpanStyle(fontSize = size))
                        showSizeDropdown = false
                    })
                }
            }
        }

        // Font color selector
        Box {
            IconButton(onClick = { showColorDropdown = true }) {
                Icon(
                    imageVector = Icons.Default.FormatColorText,
                    contentDescription = "Font Color",
                    tint = state.currentSpanStyle.color
                )
            }
            DropdownMenu(expanded = showColorDropdown, onDismissRequest = { showColorDropdown = false }) {
                colors.forEach { color ->
                    DropdownMenuItem(text = { Text("Color") }, onClick = {
                        state.toggleSpanStyle(SpanStyle(color = color))
                        showColorDropdown = false
                    }, leadingIcon = {
                        Icon(Icons.Default.Circle, contentDescription = null, tint = color)
                    })
                }
            }
        }
    }
}

@Composable
private fun RichTextEditorCard(
    richTextState: com.mohamedrejeb.richeditor.model.RichTextState,
    isStreaming: Boolean,
    onTapComplete: () -> Unit,
    suggestionText: String = "",
    onAcceptSuggestion: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, ComposeOutline),
        color = ComposeSurface
    ) {
        Column {
            RichTextEditorControls(state = richTextState)
            Box {
                RichTextEditor(
                    state = richTextState,
                    enabled = !isStreaming,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 240.dp)
                        .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 20.dp),
                    placeholder = {
                        Text(
                            text = "내용을 입력하세요",
                            style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary)
                        )
                    }
                )
                if (suggestionText.isNotEmpty()) {
                    Text(
                        text = suggestionText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = TextSecondary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 20.dp)
                    )
                }

                // 제안 수락 플로팅 버튼
                if (suggestionText.isNotEmpty()) {
                    androidx.compose.material3.FloatingActionButton(
                        onClick = onAcceptSuggestion,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = Blue60,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "제안 수락",
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "수락",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
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
fun ContactChip(contact: Contact, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier.height(24.dp),
        shape = RoundedCornerShape(12.dp),
        color = AddButtonBackground,
        border = BorderStroke(1.dp, Color(0xFF6366F1))
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
                    .background(contact.color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${contact.name} (${contact.email})",
                style = MaterialTheme.typography.labelSmall.copy(color = AddButtonText),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(6.dp))
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
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

                // Hoisted states
                var subject by rememberSaveable { mutableStateOf("") }
                val richTextState = rememberRichTextState()

                var contacts by remember { mutableStateOf(emptyList<Contact>()) }
                var newContact by remember { mutableStateOf(TextFieldValue("")) }
                var showTemplateScreen by remember { mutableStateOf(false) }
                var aiRealtime by rememberSaveable { mutableStateOf(true) }

                // Collect UI states from ViewModels
                val composeUi by composeVm.ui.collectAsState()
                val sendUi by sendVm.ui.collectAsState()

                // Enable/disable realtime mode when toggle changes
                LaunchedEffect(aiRealtime) {
                    composeVm.enableRealtimeMode(aiRealtime)
                }

                // Sync state from AI ViewModel to local state
                LaunchedEffect(composeUi.subject) { if (composeUi.subject.isNotBlank()) subject = composeUi.subject }
                LaunchedEffect(composeUi.bodyRendered) {
                    if (composeUi.bodyRendered.isNotEmpty()) {
                        richTextState.setHtml(composeUi.bodyRendered)
                    }
                }

                // Monitor text changes for realtime suggestions
                LaunchedEffect(richTextState.annotatedString.text) {
                    if (aiRealtime) {
                        composeVm.onTextChanged(richTextState.toHtml())
                    }
                }

                // Show snackbar for send results
                val snackHost = remember { SnackbarHostState() }
                LaunchedEffect(sendUi.lastSuccessMsg, sendUi.error) {
                    sendUi.lastSuccessMsg?.let { snackHost.showSnackbar(it) }
                    sendUi.error?.let { snackHost.showSnackbar("전송 실패: $it") }
                }

                Scaffold(snackbarHost = { SnackbarHost(snackHost) }) { innerPadding ->
                    if (showTemplateScreen) {
                        // 템플릿 선택 화면
                        TemplateSelectionScreen(
                            onBack = { showTemplateScreen = false },
                            onTemplateSelected = { template ->
                                subject = template.subject
                                richTextState.setHtml(template.body)
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
                            richTextState = richTextState,
                            contacts = contacts,
                            onContactsChange = { contacts = it },
                            newContact = newContact,
                            onNewContactChange = { newContact = it },
                            isStreaming = composeUi.isStreaming,
                            error = composeUi.error,
                            sendUiState = sendUi,
                            onBack = { finish() },
                            onTemplateClick = { showTemplateScreen = true },
                            onUndo = { /* TODO */ },
                            suggestionText = composeUi.suggestionText,
                            onAcceptSuggestion = {
                                // 다음 단어 하나만 가져오기
                                val nextWord = composeVm.acceptNextWord()
                                if (nextWord != null) {
                                    val currentText = richTextState.toHtml()
                                    // 현재 텍스트가 공백으로 끝나지 않으면 공백 추가
                                    val separator = if (currentText.endsWith(" ") || currentText.isEmpty()) "" else " "
                                    richTextState.setHtml(currentText + separator + nextWord)
                                }
                            },
                            aiRealtime = aiRealtime,
                            onAiRealtimeToggle = { aiRealtime = it },
                            onAiComplete = {
                                val payload = JSONObject().apply {
                                    put("subject", subject.ifBlank { "제목 생성" })
                                    // Use HTML content for AI prompt
                                    put("body", richTextState.toHtml().ifBlank { "간단한 인사와 핵심 내용으로 작성" })
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
                                    body = richTextState.toHtml()
                                )
                            }
                        )
                    }
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
        val richTextState = rememberRichTextState()
        EmailComposeScreen(
            subject = "초안 제목",
            onSubjectChange = {},
            richTextState = richTextState,
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
            onAiRealtimeToggle = {}
        )
    }
}
