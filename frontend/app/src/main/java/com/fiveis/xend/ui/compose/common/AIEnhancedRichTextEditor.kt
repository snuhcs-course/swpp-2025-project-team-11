package com.fiveis.xend.ui.compose.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiveis.xend.ui.theme.Blue60
import com.fiveis.xend.ui.theme.ComposeOutline
import com.fiveis.xend.ui.theme.ComposeSurface
import com.fiveis.xend.ui.theme.TextPrimary
import com.fiveis.xend.ui.theme.TextSecondary
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults

/**
 * AI 기능이 포함된 Rich Text Editor
 * - Rich Text 편집 기능
 * - AI 제안 미리보기
 * - 탭 완성 버튼
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AIEnhancedRichTextEditor(
    richTextState: RichTextState,
    isStreaming: Boolean,
    suggestionText: String,
    onAcceptSuggestion: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "내용을 입력하세요"
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, ComposeOutline),
        color = ComposeSurface
    ) {
        Column {
            // 서식 도구 모음
            RichTextEditorControls(state = richTextState)

            // Rich Text Editor
            RichTextEditor(
                state = richTextState,
                enabled = !isStreaming,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 240.dp)
                    .padding(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 20.dp),
                colors = RichTextEditorDefaults.richTextEditorColors(
                    containerColor = Color.White
                ),
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary)
                    )
                }
            )

            // AI 제안 미리보기
            if (suggestionText.isNotEmpty()) {
                SuggestionPreviewPanel(suggestionText = suggestionText)
            }

            // 탭 완성 버튼 (제안이 있을 때만 표시)
            if (suggestionText.isNotEmpty()) {
                TapCompleteButton(onClick = onAcceptSuggestion)
            }
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

/**
 * Rich Text Editor 서식 도구 모음
 */
@Composable
private fun RichTextEditorControls(state: RichTextState, modifier: Modifier = Modifier) {
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
