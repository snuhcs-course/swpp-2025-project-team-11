package com.fiveis.xend.ui.compose.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.fiveis.xend.ui.theme.Blue60
import com.fiveis.xend.ui.theme.ComposeOutline
import com.fiveis.xend.ui.theme.ComposeSurface
import com.fiveis.xend.ui.theme.TextSecondary

/**
 * AI 기능이 포함된 Rich Text Editor (XendRichEditor 기반)
 * - Rich Text 편집 기능
 * - AI 제안 미리보기
 * - 탭 완성 버튼
 */
@Composable
fun AIEnhancedRichTextEditor(
    editorState: XendRichEditorState,
    isStreaming: Boolean,
    suggestionText: String,
    onAcceptSuggestion: () -> Unit,
    onTextChanged: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    placeholder: String = "내용을 입력하세요",
    showInlineSwipeBar: Boolean = true
) {
    val editorHeight = 320.dp

    // Show/remove suggestion in editor
    androidx.compose.runtime.LaunchedEffect(suggestionText) {
        if (suggestionText.isNotEmpty()) {
            editorState.showSuggestion(suggestionText)
        } else {
            editorState.removeSuggestion()
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, ComposeOutline),
        color = ComposeSurface
    ) {
        Column {
            // Rich Text Editing Controls
            RichTextEditorControls(
                state = editorState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            )

            // Rich Text Editor using AndroidView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(editorHeight)
                    .padding(8.dp)
            ) {
                XendRichEditorView(
                    state = editorState,
                    placeholder = placeholder,
                    onTextChanged = onTextChanged,
                    enabled = !isStreaming,
                    modifier = Modifier.fillMaxWidth()
                )

                if (showInlineSwipeBar && suggestionText.isNotEmpty()) {
                    SwipeBar(
                        onSwipe = {
                            editorState.acceptSuggestion()
                            editorState.requestFocusAndShowKeyboard()
                            onAcceptSuggestion()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .zIndex(10f)
                    )
                }
            }
        }
    }
}

@Composable
fun SwipeSuggestionOverlay(visible: Boolean, modifier: Modifier = Modifier, onSwipe: () -> Unit) {
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val navBottom = WindowInsets.navigationBars.getBottom(density)
    val combinedBottom = maxOf(imeBottom, navBottom)
    val bottomPadding = with(density) { combinedBottom.toDp() } + 2.dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(visible = visible) {
            SwipeBar(
                onSwipe = onSwipe,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = bottomPadding)
            )
        }
    }
}

/**
 * Swipe Bar - dedicated area for swipe gesture to accept AI suggestions
 */
@Composable
private fun SwipeBar(onSwipe: () -> Unit, modifier: Modifier = Modifier) {
    var dragAmount by remember { mutableStateOf(0f) }

    Surface(
        modifier = modifier
            .height(48.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        // Trigger swipe if dragged more than 100px to the right
                        if (dragAmount > 100f) {
                            onSwipe()
                        }
                        dragAmount = 0f
                    },
                    onHorizontalDrag = { _, dragDistance ->
                        dragAmount += dragDistance
                    }
                )
            },
        // Pastel light purple/blue
        color = Color(0xFFE8EAFF),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "→ Swipe to apply →",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Blue60,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic
                )
            )
        }
    }
}

/**
 * AI 제안 미리보기 패널
 */
@Composable
private fun SuggestionPreviewPanel(suggestionText: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Text(
                text = suggestionText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextSecondary.copy(alpha = 0.75f),
                    fontStyle = FontStyle.Italic
                ),
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

/**
 * Rich Text 편집 컨트롤 버튼들
 */
@Composable
private fun RichTextEditorControls(state: XendRichEditorState, modifier: Modifier = Modifier) {
    var showSizeDropdown by remember { mutableStateOf(false) }
    val fontSizes = listOf(12, 14, 16, 18, 22, 24)

    var showColorDropdown by remember { mutableStateOf(false) }
    val colors = listOf(
        Color.Black,
        Color.Red,
        Color.Blue,
        Color.Green,
        Color(0xFF6366F1),
        Color(0xFFEC4899)
    )

    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Bold button
        IconButton(onClick = { state.toggleBold() }) {
            Icon(
                imageVector = Icons.Default.FormatBold,
                contentDescription = null,
                tint = TextSecondary
            )
        }

        // Italic button
        IconButton(onClick = { state.toggleItalic() }) {
            Icon(
                imageVector = Icons.Default.FormatItalic,
                contentDescription = null,
                tint = TextSecondary
            )
        }

        // Underline button
        IconButton(onClick = { state.toggleUnderline() }) {
            Icon(
                imageVector = Icons.Default.FormatUnderlined,
                contentDescription = null,
                tint = TextSecondary
            )
        }

        // Strikethrough button
        IconButton(onClick = { state.toggleStrikethrough() }) {
            Icon(
                imageVector = Icons.Default.FormatStrikethrough,
                contentDescription = null,
                tint = TextSecondary
            )
        }

        // Font size selector
        Box {
            IconButton(onClick = { showSizeDropdown = true }) {
                Icon(
                    Icons.Default.FormatSize,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
            DropdownMenu(
                expanded = showSizeDropdown,
                onDismissRequest = { showSizeDropdown = false }
            ) {
                fontSizes.forEach { size ->
                    DropdownMenuItem(
                        text = { Text("${size}sp") },
                        onClick = {
                            state.changeFontSize(size)
                            showSizeDropdown = false
                        }
                    )
                }
            }
        }

        // Font color selector
        Box {
            IconButton(onClick = { showColorDropdown = true }) {
                Icon(
                    imageVector = Icons.Default.FormatColorText,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
            DropdownMenu(
                expanded = showColorDropdown,
                onDismissRequest = { showColorDropdown = false }
            ) {
                colors.forEach { color ->
                    DropdownMenuItem(
                        text = { Text("Color") },
                        onClick = {
                            state.changeTextColor(color.toArgb())
                            showColorDropdown = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Circle,
                                contentDescription = null,
                                tint = color
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * 탭 완성 버튼
 */
@Composable
private fun TapCompleteButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp, top = 8.dp),
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
