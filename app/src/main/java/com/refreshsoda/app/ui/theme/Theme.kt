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
    primary = Color(0xFF366A55),          // Forest Green from reference mockup
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF5C7A9), // Warm Peach
    onPrimaryContainer = Color(0xFF431407),
    secondary = Color(0xFFE8A87C),        // Apricot Peach
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFDEEE3),
    onSecondaryContainer = Color(0xFF5A2A18),
    tertiary = Color(0xFF2E5A47),
    background = Color(0xFFF6F5F2),       // Light warm cream background
    surface = Color.White,                // Clean white cards
    onSurface = Color(0xFF1E293B),         // Charcoal slate text
    surfaceVariant = Color(0xFFF1F0EC),    // Soft cream tint
    onSurfaceVariant = Color(0xFF64748B),  // Muted gray
    outline = Color(0xFFE2E0D8)           // Soft outline
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
