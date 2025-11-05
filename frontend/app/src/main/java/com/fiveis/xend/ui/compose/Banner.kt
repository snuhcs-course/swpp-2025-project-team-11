package com.fiveis.xend.ui.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiveis.xend.ui.theme.BannerBackground
import com.fiveis.xend.ui.theme.BannerBorder
import com.fiveis.xend.ui.theme.BannerText
import com.fiveis.xend.ui.theme.SuccessSurface

enum class BannerType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

data class BannerConfig(
    val backgroundColor: Color,
    val contentColor: Color,
    val icon: ImageVector,
    val borderColor: Color
)

@Composable
fun getBannerConfig(type: BannerType): BannerConfig {
    return when (type) {
        BannerType.SUCCESS -> BannerConfig(
            backgroundColor = SuccessSurface,
            contentColor = Color(0xFF166534),
            icon = Icons.Filled.Check,
            borderColor = SuccessSurface
        )
        BannerType.ERROR -> BannerConfig(
            backgroundColor = Color(0xFFFEE2E2),
            contentColor = Color(0xFF991B1B),
            icon = Icons.Filled.Error,
            borderColor = Color(0xFFFCA5A5)
        )
        BannerType.WARNING -> BannerConfig(
            backgroundColor = Color(0xFFFEF3C7),
            contentColor = Color(0xFF92400E),
            icon = Icons.Filled.Warning,
            borderColor = Color(0xFFFDE68A)
        )
        BannerType.INFO -> BannerConfig(
            backgroundColor = BannerBackground,
            contentColor = BannerText,
            icon = Icons.Outlined.Info,
            borderColor = BannerBorder
        )
    }
}

@Composable
fun Banner(
    message: String,
    type: BannerType = BannerType.SUCCESS,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val config = getBannerConfig(type)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = config.backgroundColor,
        border = BorderStroke(1.dp, config.borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = config.icon,
                contentDescription = null,
                tint = config.contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = config.contentColor,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                modifier = Modifier.weight(1f)
            )
            if (onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = config.contentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
