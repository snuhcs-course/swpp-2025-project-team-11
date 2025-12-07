package com.fiveis.xend.ui.compose.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiveis.xend.ui.compose.RealtimeConnectionStatus
import com.fiveis.xend.ui.theme.ComposeOutline

/**
 * 실시간 AI 토글 칩
 * 본문 헤더에 표시되는 실시간 AI 켜기/끄기 스위치
 */
@Composable
fun RealtimeAIToggle(isChecked: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFDCFCE7),
        modifier = modifier.height(30.dp)
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

/**
 * 본문 헤더 (제목 + 실시간 AI 토글)
 */
@Composable
fun BodyHeader(isRealtimeOn: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
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
        RealtimeAIToggle(
            isChecked = isRealtimeOn,
            onToggle = onToggle
        )
    }
}

@Composable
fun RealtimeStatusLabel(status: RealtimeConnectionStatus, errorMessage: String?, modifier: Modifier = Modifier) {
    val statusText = when (status) {
        RealtimeConnectionStatus.CONNECTING -> "실시간 AI 연결 중..."
        RealtimeConnectionStatus.ERROR -> errorMessage ?: "실시간 AI 연결 실패. 다시 시도해 주세요."
        else -> null
    }
//    statusText?.let {
//        val color = if (status == RealtimeConnectionStatus.ERROR) Color(0xFFDC2626) else TextSecondary
//        Text(
//            text = it,
//            color = color,
//            fontSize = 11.sp,
//            fontWeight = FontWeight.Medium,
//            modifier = modifier
//        )
//    }
}
