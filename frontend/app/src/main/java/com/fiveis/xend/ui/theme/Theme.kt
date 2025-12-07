package com.fiveis.xend.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme =
    lightColorScheme(
        // Primary - Google Blue for main actions
        primary = Blue60,
        onPrimary = BackgroundWhite,
        primaryContainer = Blue80,
        onPrimaryContainer = TextPrimary,

        // Secondary - Purple for FAB
        secondary = Purple60,
        onSecondary = BackgroundWhite,

        // Error - Red for unread indicator
        error = Red60,
        onError = BackgroundWhite,

        // Background & Surface
        background = BackgroundWhite,
        onBackground = TextPrimary,
        surface = BackgroundWhite,
        onSurface = TextPrimary,
        surfaceVariant = BackgroundLight,
        onSurfaceVariant = TextSecondary,

        // Outline
        outline = BackgroundGray
    )

@Composable
fun XendTheme(darkTheme: Boolean = false, dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
