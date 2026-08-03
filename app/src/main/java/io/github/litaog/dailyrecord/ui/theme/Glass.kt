package io.github.litaog.dailyrecord.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Shared material language for app surfaces.
 *
 * This intentionally stays a modifier instead of a new layout composable: callers keep
 * their existing measurement, padding and semantics while only the paint layer changes.
 */
enum class DailyRecordGlassLevel {
    Base,
    Muted,
    Elevated,
    Emphasis,
}

private val GlassNeutralTint = Color(0xFFF3EDF8)
private val GlassNeutralWarm = Color(0xFFFFF8F1)

fun Modifier.dailyRecordGlassBackground(
    moduleColors: RecordModuleColorTokens? = null,
    level: DailyRecordGlassLevel = DailyRecordGlassLevel.Base,
): Modifier = background(
    brush = dailyRecordGlassBrush(moduleColors = moduleColors, level = level),
)

fun Modifier.dailyRecordGlass(
    shape: Shape,
    moduleColors: RecordModuleColorTokens? = null,
    level: DailyRecordGlassLevel = DailyRecordGlassLevel.Base,
    edgeColor: Color? = null,
): Modifier {
    val edge = edgeColor ?: when (level) {
        DailyRecordGlassLevel.Muted -> Color.White.copy(alpha = .58f)
        DailyRecordGlassLevel.Base -> Color.White.copy(alpha = .74f)
        DailyRecordGlassLevel.Elevated -> Color.White.copy(alpha = .84f)
        DailyRecordGlassLevel.Emphasis -> Color.White.copy(alpha = .90f)
    }
    return clip(shape)
        .background(dailyRecordGlassBrush(moduleColors, level))
        .border(BorderStroke(1.dp, edge), shape)
}

fun dailyRecordAccentBrush(colors: RecordModuleColorTokens): Brush =
    Brush.verticalGradient(
        colors = listOf(
            colors.primary.copy(alpha = .94f),
            colors.primary,
            colors.primary.copy(alpha = .96f),
        ),
    )

fun dailyRecordAccentBrush(accent: Color): Brush =
    Brush.verticalGradient(
        colors = listOf(
            accent.copy(alpha = .94f),
            accent,
            accent.copy(alpha = .96f),
        ),
    )

fun dailyRecordPeriodTrackBrush(colors: RecordModuleColorTokens): Brush =
    Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = .96f),
            colors.soft.copy(alpha = .22f),
            Color.White.copy(alpha = .88f),
            colors.medium.copy(alpha = .12f),
        ),
    )

fun dailyRecordPeriodSliderBrush(colors: RecordModuleColorTokens): Brush =
    Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = .98f),
            Color.White.copy(alpha = .88f),
            colors.soft.copy(alpha = .18f),
        ),
    )

fun dailyRecordBackdropBrush(colors: RecordModuleColorTokens): Brush =
    Brush.linearGradient(
        colors = listOf(
            DailyRecordCanvas,
            colors.soft.copy(alpha = .08f),
            DailyRecordCanvas,
            colors.medium.copy(alpha = .05f),
            DailyRecordCanvas,
        ),
    )

private fun dailyRecordGlassBrush(
    moduleColors: RecordModuleColorTokens?,
    level: DailyRecordGlassLevel,
): Brush {
    val tint = moduleColors?.soft ?: GlassNeutralTint
    val glow = moduleColors?.medium ?: GlassNeutralWarm
    val tintAlpha = when (level) {
        DailyRecordGlassLevel.Muted -> .10f
        DailyRecordGlassLevel.Base -> .16f
        DailyRecordGlassLevel.Elevated -> .21f
        DailyRecordGlassLevel.Emphasis -> .28f
    }
    val whiteAlpha = when (level) {
        DailyRecordGlassLevel.Muted -> .88f
        DailyRecordGlassLevel.Base -> .94f
        DailyRecordGlassLevel.Elevated -> .96f
        DailyRecordGlassLevel.Emphasis -> .97f
    }
    return Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = whiteAlpha),
            tint.copy(alpha = tintAlpha),
            glow.copy(alpha = (tintAlpha * .72f).coerceAtMost(.22f)),
            Color.White.copy(alpha = (whiteAlpha - .04f).coerceAtLeast(.82f)),
        ),
    )
}
