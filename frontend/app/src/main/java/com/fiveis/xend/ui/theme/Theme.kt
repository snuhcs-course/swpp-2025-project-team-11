package com.fiveis.xend.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = Purple80,
        secondary = PurpleGrey80,
        tertiary = Pink80
    )

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
fun XendTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
