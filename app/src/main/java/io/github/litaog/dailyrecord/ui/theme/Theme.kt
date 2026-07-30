package io.github.litaog.dailyrecord.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DailyRecordColorScheme = lightColorScheme(
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
    MaterialTheme(
        colorScheme = DailyRecordColorScheme,
        typography = Typography,
        content = content,
    )
}
