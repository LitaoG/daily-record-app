package io.github.litaog.dailyrecord.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val HandBrewColorScheme = lightColorScheme(
    primary = Terracotta500,
    onPrimary = White,
    primaryContainer = Terracotta400,
    onPrimaryContainer = Ink900,
    secondary = Terracotta600,
    onSecondary = White,
    background = Paper50,
    onBackground = Ink900,
    surface = Paper0,
    onSurface = Ink900,
    surfaceVariant = Paper100,
    onSurfaceVariant = Ink700,
    outline = Neutral300,
    outlineVariant = Paper100,
)

@Composable
fun DailyRecordTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HandBrewColorScheme,
        typography = Typography,
        content = content,
    )
}
