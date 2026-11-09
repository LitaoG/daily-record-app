package io.github.litaog.dailyrecord.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Built per selected theme; a theme change recreates the activity. */
private fun dailyRecordColorScheme(): ColorScheme = lightColorScheme(
    primary = DailyRecordDefaultAccent,
    onPrimary = DailyRecordOnAccent,
    primaryContainer = DailyRecordDefaultAccentSoft,
    onPrimaryContainer = DailyRecordText,
    secondary = DailyRecordDefaultAccentStrong,
    onSecondary = DailyRecordOnAccent,
    background = DailyRecordCanvas,
    onBackground = DailyRecordText,
    surface = DailyRecordSurface,
    onSurface = DailyRecordText,
    surfaceVariant = DailyRecordSurfaceMuted,
    onSurfaceVariant = DailyRecordTextSecondary,
    outline = DailyRecordDivider,
    outlineVariant = DailyRecordSurfaceMuted,
    error = DailyRecordDanger,
    onError = DailyRecordOnAccent,
    errorContainer = DailyRecordDangerContainer,
    onErrorContainer = DailyRecordText,
    surfaceTint = DailyRecordDefaultAccent,
    inverseSurface = DailyRecordText,
    inverseOnSurface = DailyRecordSurface,
    inversePrimary = DailyRecordDefaultAccentSoft,
)

@Composable
fun DailyRecordTheme(content: @Composable () -> Unit) {
    val theme = DailyRecordThemeState.current
    val colorScheme = remember(theme) { dailyRecordColorScheme() }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = DailyRecordShapes.Material3Shapes,
        content = content,
    )
}
