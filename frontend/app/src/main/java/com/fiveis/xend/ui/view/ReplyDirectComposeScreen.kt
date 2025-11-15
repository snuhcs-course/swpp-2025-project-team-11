package com.fiveis.xend.ui.view

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.fiveis.xend.ui.compose.TemplateTIcon
import com.fiveis.xend.ui.compose.common.AIEnhancedRichTextEditor
import com.fiveis.xend.ui.compose.common.BodyHeader
import com.fiveis.xend.ui.compose.common.XendRichEditorState
import com.fiveis.xend.ui.compose.common.rememberXendRichEditorState
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
import kotlinx.coroutines.launch

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
    editorState: XendRichEditorState = rememberXendRichEditorState(),
    senderEmail: String = "",
    date: String = "",
    originalBody: String = "",
    sendUiState: com.fiveis.xend.ui.compose.SendUiState = com.fiveis.xend.ui.compose.SendUiState(),
    // AI 관련 파라미터
    isStreaming: Boolean = false,
    suggestionText: String = "",
    aiRealtime: Boolean = true,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onAiComplete: () -> Unit = {},
    onStopStreaming: () -> Unit = {},
    onAcceptSuggestion: () -> Unit = {},
    onAiRealtimeToggle: (Boolean) -> Unit = {},
    bannerState: com.fiveis.xend.ui.compose.BannerState? = null,
    onDismissBanner: () -> Unit = {},
    showInlineSwipeBar: Boolean = true,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    showAddContactButton: Boolean = false,
    onAddContactClick: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var isMailContentExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ComposeBackground,
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
                        Icon(TemplateTIcon, contentDescription = "템플릿", tint = Color(0xFFF59E0B))
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
                            onSend(editorState.getHtml())
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
                .verticalScroll(scrollState)
                .imePadding()
        ) {
            // Banner for send results
            androidx.compose.animation.AnimatedVisibility(visible = bannerState != null) {
                bannerState?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        com.fiveis.xend.ui.compose.Banner(
                            message = it.message,
                            type = it.type,
                            onDismiss = onDismissBanner,
                            actionText = it.actionText,
                            onActionClick = it.onActionClick,
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .padding(bottom = 8.dp)
                        )
                    }
                }
            }

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
                groupNames = groups,
                showAddContactButton = showAddContactButton,
                onAddContactClick = onAddContactClick
            )

            Spacer(modifier = Modifier.height(8.dp))

            SubjectControlRow(
                canUndo = canUndo,
                canRedo = canRedo,
                undoRedoVisible = !isStreaming && (canUndo || canRedo),
                onUndo = onUndo,
                onRedo = onRedo,
                isStreaming = isStreaming,
                aiCompleteEnabled = recipientEmail.isNotEmpty(),
                onAiComplete = onAiComplete,
                onStopStreaming = onStopStreaming
            )
            DirectComposeSubjectField(
                value = subject,
                onValueChange = onSubjectChange,
                enabled = !isStreaming
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 본문 헤더 + 실시간 AI 토글
            BodyHeader(
                isRealtimeOn = aiRealtime,
                onToggle = onAiRealtimeToggle
            )

            // Rich Text Editor with AI
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

            // Extra spacer so keyboard focus can scroll a bit further down
            Spacer(modifier = Modifier.height(240.dp))
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
private fun RecipientInfoSection(
    recipientName: String,
    recipientEmail: String,
    groupNames: List<String>,
    showAddContactButton: Boolean = false,
    onAddContactClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = "받는 사람",
            style = MaterialTheme.typography.titleSmall.copy(
                color = Color(0xFF64748B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = extractName(recipientName),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = extractEmailAddress(recipientName),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                )
            }

            if (groupNames.isNotEmpty()) {
                val label = groupNames.joinToString(", ")
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
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp,
                                color = Color(0xFF6366F1)
                            )
                        )
                    }
                }
            }

            if (showAddContactButton && onAddContactClick != null) {
                TextButton(onClick = onAddContactClick) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = "연락처 추가",
                        tint = Blue60,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "연락처 추가",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Blue60,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectControlRow(
    canUndo: Boolean,
    canRedo: Boolean,
    undoRedoVisible: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    isStreaming: Boolean,
    aiCompleteEnabled: Boolean,
    onAiComplete: () -> Unit,
    onStopStreaming: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "제목",
            style = MaterialTheme.typography.titleSmall.copy(
                color = Color(0xFF64748B),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
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
                        modifier = Modifier.size(20.dp),
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
        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF1E293B)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(48.dp)
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
            groups = listOf("임원진", "업무"),
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
            groupNames = listOf("임원진", "업무"),
            showAddContactButton = true,
            onAddContactClick = {}
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
    // 원본 메시지 분리
    val markers = listOf(
        "-- original message --",
        "--original message--",
        "-----Original Message-----",
        "-----원본 메시지-----",
        "<br><br>From:",
        "<br><br>from:"
    )

    var splitIndex = -1
    for (marker in markers) {
        val index = body.indexOf(marker, ignoreCase = true)
        if (index != -1) {
            splitIndex = index
            break
        }
    }

    // 원본 메시지가 있으면 CollapsibleBodyPreview 사용
    if (splitIndex != -1) {
        CollapsibleBodyPreview(
            bodyPreview = body,
            modifier = Modifier.padding(horizontal = 20.dp),
            showHeader = false,
            backgroundColor = MailDetailBodyBg,
            borderColor = MailDetailBodyBg,
            textColor = androidx.compose.ui.graphics.Color(0xFF202124)
        )
        return
    }

    // 원본 메시지가 없으면 기존 방식대로 (전체 접기/펼치기)
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
