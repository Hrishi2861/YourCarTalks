package com.hrishi.yourcartalks.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.hrishi.yourcartalks.data.ThemeMode

val DarkBackground = Color(0xFF0A0A0A)
val DarkSurface = Color(0xFF111111)
val DarkSurfaceVariant = Color(0xFF1F1F1F)
val DarkPrimary = Color(0xFFE8A020)
val DarkOnPrimary = Color(0xFF000000)
val DarkOnBackground = Color(0xFFFFFFFF)
val DarkOnSurface = Color(0xFFFFFFFF)
val DarkOnSurfaceVariant = Color(0xFF9E9E9E)
val DarkSecondary = Color(0xFF757575)
val DarkOnSecondary = Color(0xFFFFFFFF)
val DarkError = Color(0xFFCF4444)
val DarkOutline = Color(0xFF1F1F1F)
val DarkSuccess = Color(0xFF4CAF50)

val LightBackground = Color(0xFFF5F5F0)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE8E8E8)
val LightPrimary = Color(0xFFE8A020)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightOnBackground = Color(0xFF1A1A1A)
val LightOnSurface = Color(0xFF1A1A1A)
val LightOnSurfaceVariant = Color(0xFF757575)
val LightSecondary = Color(0xFF757575)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightError = Color(0xFFCF4444)
val LightOutline = Color(0xFFE0E0E0)

private val DarkColorScheme = darkColorScheme(
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    error = DarkError,
    outline = DarkOutline,
)

private val LightColorScheme = lightColorScheme(
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    onBackground = LightOnBackground,
    onSurface = LightOnSurface,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    error = LightError,
    outline = LightOutline,
)

@Composable
fun YourCarTalksTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.decorView.setBackgroundColor(colorScheme.background.toArgb())
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OutfitTypography,
        content = content
    )
}

object AutoColors {
    val selectedBg: Color get() = Color(0x1AE8A020)
    val success: Color get() = Color(0xFF4CAF50)
    val destructive: Color get() = Color(0xFFCF4444)
    val pageDotInactive: Color get() = Color(0xFF2C2C2C)
}
