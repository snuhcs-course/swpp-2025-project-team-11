package com.fiveis.xend.ui.compose.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiveis.xend.ui.theme.Blue60
import com.fiveis.xend.ui.theme.UndoBorder

/**
 * AI 기능을 위한 공통 액션 행 (실행취소, AI 완성 버튼)
 * MailComposeActivity와 ReplyDirectComposeActivity에서 재사용
 */
@Composable
fun AIActionRow(
    isStreaming: Boolean,
    onUndo: () -> Unit,
    onAiComplete: () -> Unit,
    onStopStreaming: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 15.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 실행취소 버튼
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

        // AI 완성 / 중지 버튼
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isStreaming) {
                // 스트리밍 중 - 중지 버튼 표시
                OutlinedButton(
                    onClick = onStopStreaming,
                    modifier = Modifier.size(width = 96.dp, height = 35.dp),
                    contentPadding = PaddingValues(horizontal = 15.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFFEF4444)
                    )
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "중지",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = Blue60
                )
            } else {
                // 비활성 - AI 완성 버튼 표시
                OutlinedButton(
                    onClick = onAiComplete,
                    modifier = Modifier.size(width = 96.dp, height = 35.dp),
                    enabled = true,
                    contentPadding = PaddingValues(horizontal = 15.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Blue60),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Blue60,
                        disabledContentColor = Blue60.copy(alpha = 0.4f)
                    )
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
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
}
