package com.refreshsoda.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

enum class AppThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

/**
 * Semantic colors that Material3 does not provide (success / warning / info / danger + hero cards).
 * Every value has a light and a dark variant with checked contrast, so screens never need raw hex codes.
 */
@Immutable
data class AppColors(
    val success: Color, val onSuccess: Color, val successContainer: Color, val onSuccessContainer: Color,
    val warning: Color, val onWarning: Color, val warningContainer: Color, val onWarningContainer: Color,
    val info: Color, val onInfo: Color, val infoContainer: Color, val onInfoContainer: Color,
    val danger: Color, val onDanger: Color, val dangerContainer: Color, val onDangerContainer: Color,
    // Hero / summary cards: always a deep tone with white text, in both modes.
    val heroTeal: Color, val heroAmber: Color, val heroSlate: Color,
    val onHero: Color, val onHeroMuted: Color,
    val heroPositive: Color, val heroNegative: Color, val heroCaution: Color,
    val isDark: Boolean
)

private val LightAppColors = AppColors(
    success = Color(0xFF15803D), onSuccess = Color.White,
    successContainer = Color(0xFFDCFCE7), onSuccessContainer = Color(0xFF14532D),
    warning = Color(0xFFB45309), onWarning = Color.White,
    warningContainer = Color(0xFFFEF3C7), onWarningContainer = Color(0xFF78350F),
    info = Color(0xFF0369A1), onInfo = Color.White,
    infoContainer = Color(0xFFE0F2FE), onInfoContainer = Color(0xFF075985),
    danger = Color(0xFFB91C1C), onDanger = Color.White,
    dangerContainer = Color(0xFFFEE2E2), onDangerContainer = Color(0xFF7F1D1D),
    heroTeal = Color(0xFF0F766E), heroAmber = Color(0xFFB45309), heroSlate = Color(0xFF1E293B),
    onHero = Color.White, onHeroMuted = Color(0xFFE2E8F0),
    heroPositive = Color(0xFF86EFAC), heroNegative = Color(0xFFFECACA), heroCaution = Color(0xFFFDE68A),
    isDark = false
)

private val DarkAppColors = AppColors(
    success = Color(0xFF4ADE80), onSuccess = Color(0xFF052E16),
    successContainer = Color(0xFF14532D), onSuccessContainer = Color(0xFFBBF7D0),
    warning = Color(0xFFFBBF24), onWarning = Color(0xFF451A03),
    warningContainer = Color(0xFF422006), onWarningContainer = Color(0xFFFDE68A),
    info = Color(0xFF38BDF8), onInfo = Color(0xFF082F49),
    infoContainer = Color(0xFF0C4A6E), onInfoContainer = Color(0xFFBAE6FD),
    danger = Color(0xFFF87171), onDanger = Color(0xFF450A0A),
    dangerContainer = Color(0xFF450A0A), onDangerContainer = Color(0xFFFECACA),
    heroTeal = Color(0xFF115E59), heroAmber = Color(0xFF92400E), heroSlate = Color(0xFF0B1220),
    onHero = Color.White, onHeroMuted = Color(0xFFCBD5E1),
    heroPositive = Color(0xFF86EFAC), heroNegative = Color(0xFFFCA5A5), heroCaution = Color(0xFFFDE68A),
    isDark = true
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF042F2E),
    secondary = Color(0xFFB45309),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF042F2E),
    tertiary = Color(0xFF0369A1),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF082F49),
    error = Color(0xFFB91C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    surfaceTint = Color(0xFF0F766E),
    inverseSurface = Color(0xFF1E293B),
    inverseOnSurface = Color(0xFFF8FAFC),
    inversePrimary = Color(0xFF2DD4BF),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFE2E8F0),
    scrim = Color.Black,
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE2E8F0),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFE2E8F0)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF134E4A),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFFFBBF24),
    onSecondary = Color(0xFF451A03),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF082F49),
    tertiaryContainer = Color(0xFF0C4A6E),
    onTertiaryContainer = Color(0xFFE0F2FE),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111A2E),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFB6C2D4),
    surfaceTint = Color(0xFF2DD4BF),
    inverseSurface = Color(0xFFF1F5F9),
    inverseOnSurface = Color(0xFF0F172A),
    inversePrimary = Color(0xFF0F766E),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF283548),
    scrim = Color.Black,
    surfaceBright = Color(0xFF263449),
    surfaceDim = Color(0xFF0B1220),
    surfaceContainerLowest = Color(0xFF080E1A),
    surfaceContainerLow = Color(0xFF162036),
    surfaceContainer = Color(0xFF1A253B),
    surfaceContainerHigh = Color(0xFF202D46),
    surfaceContainerHighest = Color(0xFF2A3853)
)

private val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/** Usage: MaterialTheme.appColors.success */
val MaterialTheme.appColors: AppColors
    @Composable @ReadOnlyComposable get() = LocalAppColors.current

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

    // Phone ke system font size bada hone par text wrap na ho, isliye font scale ko 1.0 par cap kiya hai
    val baseDensity = LocalDensity.current
    val cappedDensity = Density(
        density = baseDensity.density,
        fontScale = baseDensity.fontScale.coerceAtMost(1.0f)
    )

    CompositionLocalProvider(
        LocalDensity provides cappedDensity,
        LocalAppColors provides (if (isDark) DarkAppColors else LightAppColors)
    ) {
        MaterialTheme(
            colorScheme = if (isDark) DarkColorScheme else LightColorScheme,
            content = content
        )
    }
}
