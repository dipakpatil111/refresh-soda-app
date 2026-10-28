package com.refreshsoda.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F766E),        // Emerald Teal
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF115E59),
    secondary = Color(0xFFD97706),      // Warm Amber
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF0284C7),       // Sky Blue
    background = Color(0xFFF8FAFC),     // Slate 50
    surface = Color.White,
    onSurface = Color(0xFF0F172A),       // Slate 900
    surfaceVariant = Color(0xFFF1F5F9),  // Slate 100
    onSurfaceVariant = Color(0xFF475569),// Slate 600
    outline = Color(0xFFE2E8F0)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF),        // Bright Mint Teal
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF134E4A),
    onPrimaryContainer = Color(0xFF99F6E4),
    secondary = Color(0xFFFBBF24),      // Bright Amber
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = Color(0xFF38BDF8),       // Bright Sky
    background = Color(0xFF0F172A),     // Deep Slate 900
    surface = Color(0xFF1E293B),        // Slate 800
    onSurface = Color(0xFFF8FAFC),      // Slate 50
    surfaceVariant = Color(0xFF334155), // Slate 700
    onSurfaceVariant = Color(0xFFCBD5E1),// Slate 300
    outline = Color(0xFF475569)
)

@Composable
fun RefreshSodaTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
