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
    primary = Color(0xFFEA580C),        // Terracotta Warm Orange (User's Design)
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDD5), // Soft Peach Cream
    onPrimaryContainer = Color(0xFF9A3412),
    secondary = Color(0xFFF97316),      // Bright Amber Orange
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF7ED),
    onSecondaryContainer = Color(0xFF7C2D12),
    tertiary = Color(0xFF0284C7),       // Sky Blue
    background = Color(0xFFFAF6F2),     // Warm porcelain off-white background
    surface = Color.White,              // Pure white cards
    onSurface = Color(0xFF1C1917),       // Deep warm charcoal slate
    surfaceVariant = Color(0xFFF5ECE5),  // Soft sand/peach tint
    onSurfaceVariant = Color(0xFF78716C),// Warm grey
    outline = Color(0xFFE7DFD8)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFB923C),        // Luminous Peach Orange
    onPrimary = Color(0xFF431407),
    primaryContainer = Color(0xFF7C2D12),
    onPrimaryContainer = Color(0xFFFFEDD5),
    secondary = Color(0xFFFDBA74),
    onSecondary = Color(0xFF431407),
    secondaryContainer = Color(0xFF9A3412),
    onSecondaryContainer = Color(0xFFFFF7ED),
    tertiary = Color(0xFF38BDF8),
    background = Color(0xFF1C1917),     // Deep warm slate
    surface = Color(0xFF292524),        // Warm dark card
    onSurface = Color(0xFFFAF6F2),
    surfaceVariant = Color(0xFF3C3633),
    onSurfaceVariant = Color(0xFFA8A29E),
    outline = Color(0xFF57534E)
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
