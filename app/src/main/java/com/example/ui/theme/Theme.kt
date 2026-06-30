package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PremiumBlue,
    secondary = PremiumAccent,
    background = DarkBg,
    surface = DarkCard,
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color(0xFFE5E7EB),
    onSurfaceVariant = Color(0xFF9CA3AF),
    error = DangerRed
)

private val LightColorScheme = lightColorScheme(
    primary = PremiumBlue,
    secondary = PremiumAccent,
    background = LightBg,
    surface = LightCard,
    onPrimary = Color.White,
    onBackground = LightText,
    onSurface = LightText,
    onSurfaceVariant = LightTextSecondary,
    error = DangerRed
)

@Composable
fun MyApplicationTheme(
    themeMode: String = "dark", // "dark", "light", "system"
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true // Premium dark default
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
