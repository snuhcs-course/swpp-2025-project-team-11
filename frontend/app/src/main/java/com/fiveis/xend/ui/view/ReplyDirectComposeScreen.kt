package com.fiveis.xend.ui.view

import android.webkit.WebView
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.fiveis.xend.ui.theme.Blue60
import com.fiveis.xend.ui.theme.ComposeBackground
import com.fiveis.xend.ui.theme.ComposeOutline
import com.fiveis.xend.ui.theme.MailDetailBodyBg
import com.fiveis.xend.ui.theme.Purple60
import com.fiveis.xend.ui.theme.Slate50
import com.fiveis.xend.ui.theme.TextPrimary
import com.fiveis.xend.ui.theme.TextSecondary
import com.fiveis.xend.ui.theme.ToolbarIconTint
import com.fiveis.xend.ui.theme.UndoBorder
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReplyDirectComposeScreen(
    recipientEmail: String,
    recipientName: String,
    subject: String,
    groups: List<String>,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onTemplateClick: () -> Unit = {},
    onSubjectChange: (String) -> Unit = {},
    richTextState: com.mohamedrejeb.richeditor.model.RichTextState = rememberRichTextState(),
    senderEmail: String = "",
    date: String = "",
    originalBody: String = "",
    sendUiState: com.fiveis.xend.ui.compose.SendUiState = com.fiveis.xend.ui.compose.SendUiState()
) {
    var isRealtimeAiOn by rememberSaveable { mutableStateOf(true) }
    var isStreaming by rememberSaveable { mutableStateOf(false) }
    var isMailContentExpanded by remember { mutableStateOf(false) }

    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    // 전송 상태 처리
    LaunchedEffect(sendUiState) {
        when {
            sendUiState.lastSuccessMsg != null -> {
                snackbarHostState.showSnackbar(sendUiState.lastSuccessMsg)
            }
            sendUiState.error != null -> {
                snackbarHostState.showSnackbar("전송 실패: ${sendUiState.error}")
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ComposeBackground,
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        topBar = {
            val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
            TopAppBar(
                modifier = Modifier
                    .padding(top = statusBarPadding.calculateTopPadding())
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                title = {
                    Text(
                        "답장 작성",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                },
                navigationIcon = {
                    DirectComposeToolbarIconButton(
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
                    DirectComposeToolbarIconButton(
                        onClick = onTemplateClick,
                        border = null,
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Icon(Icons.Default.GridView, contentDescription = "템플릿", tint = Color(0xFFF59E0B))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    DirectComposeToolbarIconButton(
                        onClick = { /* TODO: 첨부파일 */ },
                        border = null,
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Icon(Icons.Default.Attachment, contentDescription = "첨부파일", tint = ToolbarIconTint)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    DirectComposeToolbarIconButton(
                        onClick = {
                            // HTML body를 전달하여 전송
                            onSend(richTextState.toHtml())
                        },
                        border = null,
                        contentTint = Blue60,
                        modifier = Modifier.padding(start = 2.dp),
                        enabled = !sendUiState.isSending
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "전송",
                            tint = Blue60
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ComposeBackground,
                    scrolledContainerColor = ComposeBackground
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(ComposeBackground)
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            // Action Row (실행취소 + AI 완성)
            DirectComposeActionRow(
                isStreaming = isStreaming,
                onUndo = { /* TODO */ },
                onAiComplete = { /* TODO */ },
                onStopStreaming = { isStreaming = false }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 발신자 정보 섹션 (접기/펼치기)
            if (senderEmail.isNotEmpty()) {
                DirectComposeCollapsibleSenderInfoSection(
                    senderEmail = senderEmail,
                    date = date,
                    isExpanded = isMailContentExpanded,
                    onToggle = { isMailContentExpanded = !isMailContentExpanded }
                )
                HorizontalDivider(thickness = 1.dp, color = ComposeOutline)

                // 메일 제목 + 본문 (조건부 표시)
                AnimatedVisibility(visible = isMailContentExpanded) {
                    Column {
                        DirectComposeSubjectDisplaySection(subject = subject)
                        DirectComposeCollapsibleBodySection(body = originalBody)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // 받는 사람 섹션
            RecipientInfoSection(
                recipientName = recipientName,
                recipientEmail = recipientEmail,
                hasGroups = groups.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 제목 섹션
            SubjectSectionHeader()
            DirectComposeSubjectField(
                value = subject,
                onValueChange = onSubjectChange,
                enabled = !isStreaming
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 본문 헤더 + 실시간 AI 토글
            DirectComposeBodyHeader(
                isRealtimeOn = isRealtimeAiOn,
                onToggle = { isRealtimeAiOn = it }
            )

            // Rich Text Editor
            DirectComposeRichTextEditorCard(
                richTextState = richTextState,
                isStreaming = isStreaming,
                onTapComplete = { /* TODO */ }
            )
        }
    }
}

@Composable
private fun DirectComposeToolbarIconButton(
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
private fun DirectComposeActionRow(
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
private fun RecipientInfoSection(recipientName: String, recipientEmail: String, hasGroups: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 받는 사람 텍스트 - Row로 한 줄에 표시
            Column(
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "받는 사람: ",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = extractName(recipientName),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                Text(
                    text = "<${extractEmailAddress(recipientName)}>",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = Color(0xFF9AA0A6),
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // 그룹 칩
            if (hasGroups) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier.height(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Groups,
                            contentDescription = "그룹",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "그룹",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp,
                                color = Color(0xFF6366F1)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectSectionHeader() {
    Text(
        text = "제목",
        style = MaterialTheme.typography.titleSmall.copy(
            color = Color(0xFF64748B),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun DirectComposeSubjectField(value: String, onValueChange: (String) -> Unit, enabled: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                "제목을 입력하세요",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            )
        },
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Slate50,
            unfocusedContainerColor = Slate50,
            disabledContainerColor = Slate50,
            errorContainerColor = Slate50,
            focusedIndicatorColor = Color(0xFFE2E8F0),
            unfocusedIndicatorColor = Color(0xFFE2E8F0),
            disabledIndicatorColor = Color(0xFFE2E8F0),
            errorIndicatorColor = MaterialTheme.colorScheme.error,
            cursorColor = Blue60,
            focusedTextColor = Color(0xFF1E293B),
            unfocusedTextColor = Color(0xFF1E293B),
            disabledTextColor = Color(0xFF1E293B).copy(alpha = 0.4f),
            focusedPlaceholderColor = TextSecondary,
            unfocusedPlaceholderColor = TextSecondary,
            disabledPlaceholderColor = TextSecondary.copy(alpha = 0.4f)
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 13.sp,
            color = Color(0xFF1E293B)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    )
}

@Composable
private fun DirectComposeBodyHeader(isRealtimeOn: Boolean, onToggle: (Boolean) -> Unit) {
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
                color = Color(0xFF64748B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
        DirectComposeRealtimeToggleChip(
            isChecked = isRealtimeOn,
            onToggle = onToggle
        )
    }
}

@Composable
private fun DirectComposeRealtimeToggleChip(isChecked: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFDCFCE7),
        modifier = Modifier.height(30.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
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
                modifier = Modifier.scale(0.6f).width(20.dp).height(20.dp),
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
private fun DirectComposeRichTextEditorCard(
    richTextState: com.mohamedrejeb.richeditor.model.RichTextState,
    isStreaming: Boolean,
    onTapComplete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, ComposeOutline),
        color = ComposeBackground
    ) {
        Column {
            DirectComposeRichTextEditorControls(state = richTextState)
            RichTextEditor(
                state = richTextState,
                enabled = !isStreaming,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                modifier = Modifier.background(Color.White)
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

            // 탭 완성 버튼
            TapCompleteButton(onClick = onTapComplete)
        }
    }
}

@Composable
private fun DirectComposeRichTextEditorControls(
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
            .padding(horizontal = 20.dp, vertical = 8.dp)
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
private fun TapCompleteButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.End
    ) {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier
                .widthIn(min = 68.dp)
                .height(28.dp),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF6366F1)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = "탭 완성",
                    tint = Blue60,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "탭 완성",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Blue60
                    )
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ReplyDirectComposeScreenPreview() {
    MaterialTheme {
        ReplyDirectComposeScreen(
            recipientEmail = "ceo@company.com",
            recipientName = "김대표",
            subject = "Re: Q4 실적 보고서 검토 부탁드립니다",
            groups = listOf("대이사", "업무"),
            onBack = {},
            onSend = { _ -> },
            senderEmail = "김대표 <ceo@company.com>",
            date = "Q4 실적 보고서 검토 요청 · 협업미팅 2개",
            originalBody = "안녕하세요, 대표님.<br><br>Q4 실적 보고서를 검토했습니다.<br><br>전반적으로 매출 증가율이 목표치를 상회하는 우수한 성과라고 판단됩니다."
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecipientInfoSectionPreview() {
    MaterialTheme {
        RecipientInfoSection(
            recipientName = "김대표",
            recipientEmail = "ceo@company.com",
            hasGroups = true
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TapCompleteButtonPreview() {
    MaterialTheme {
        TapCompleteButton(onClick = {})
    }
}

// Helper function to parse sender email
private fun parseSenderEmail(senderEmail: String): Pair<String, String> {
    val match = Regex("(.+)\\s*<(.+)>").find(senderEmail)
    return if (match != null) {
        val (name, email) = match.destructured
        Pair(name.trim(), email.trim())
    } else {
        Pair(senderEmail.substringBefore("@"), senderEmail)
    }
}

@Composable
private fun DirectComposeCollapsibleSenderInfoSection(
    senderEmail: String,
    date: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val (senderName, email) = parseSenderEmail(senderEmail)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = senderName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = date,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (isExpanded) "접기" else "펼치기",
            tint = ToolbarIconTint,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun DirectComposeSubjectDisplaySection(subject: String) {
    Text(
        text = subject,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        color = Purple60,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun DirectComposeCollapsibleBodySection(body: String) {
    var isExpanded by remember { mutableStateOf(false) }
    val collapsedHeight = 200.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Surface(
            onClick = { isExpanded = !isExpanded },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MailDetailBodyBg
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = false
                            loadWithOverviewMode = true
                            useWideViewPort = false
                            setSupportZoom(false)
                        }
                    }
                },
                update = { webView ->
                    val htmlContent = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0">
                            <style>
                                body {
                                    margin: 16px;
                                    padding: 0;
                                    font-family: sans-serif;
                                    font-size: 14px;
                                    line-height: 1.5;
                                    color: #202124;
                                    background-color: transparent;
                                }
                                img {
                                    max-width: 100%;
                                    height: auto;
                                }
                                a {
                                    color: #1A73E8;
                                    text-decoration: none;
                                }
                            </style>
                        </head>
                        <body>
                            $body
                        </body>
                        </html>
                    """.trimIndent()

                    webView.loadDataWithBaseURL(
                        null,
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isExpanded) {
                            Modifier.heightIn(min = collapsedHeight, max = 2000.dp)
                        } else {
                            Modifier.height(collapsedHeight)
                        }
                    )
            )
        }

        // 접기/펼치기 힌트 (클릭 영역 확장)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 12.dp, horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isExpanded) "접기" else "더보기",
                fontSize = 13.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// Helper function to extract just the email from a formatted string
private fun extractEmailAddress(formattedEmail: String): String {
    val emailRegex = "<([^>]+)>".toRegex() // Matches content inside < >
    val matches = emailRegex.findAll(formattedEmail).toList()
    // 마지막 매치를 사용 (이중 괄호가 있을 경우 대응)
    return matches.lastOrNull()?.groupValues?.get(1)?.trim() ?: formattedEmail.trim()
}

// Helper function to extract just the name (before <email>)
private fun extractName(formattedString: String): String {
    val nameRegex = "(.+?)\\s*<".toRegex() // Matches everything before <
    val matchResult = nameRegex.find(formattedString)
    return matchResult?.groupValues?.get(1)?.trim() ?: formattedString.substringBefore("<").trim().ifEmpty {
        formattedString
    }
}
