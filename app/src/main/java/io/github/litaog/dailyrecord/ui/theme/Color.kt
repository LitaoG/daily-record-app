package io.github.litaog.dailyrecord.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

val DailyRecordCanvas = Color(0xFFFAF8F3)
val DailyRecordSurface = Color(0xFFFFFEFB)
val DailyRecordSurfaceMuted = Color(0xFFF2EFEA)
/**
 * Disabled/future cells need to read as unavailable rather than as another
 * empty record. Keep this neutral (not module tinted), but give it enough
 * separation from [DailyRecordSurfaceMuted] used by past unset cells.
 */
val DailyRecordSurfaceDisabled = Color(0xFFE9E2D8)
val DailyRecordText = Color(0xFF2D2926)
val DailyRecordTextSecondary = Color(0xFF514A45)
val DailyRecordTextMuted = Color(0xFF706761)
val DailyRecordDivider = Color(0xFFD8D0C6)
val DailyRecordOnAccent = Color(0xFFFFFFFF)
val DailyRecordSuccess = Color(0xFF3F6F5A)
val DailyRecordWarning = Color(0xFF8A6A18)
val DailyRecordDanger = Color(0xFF9B3A32)
val DailyRecordDangerContainer = Color(0xFFF5E3E1)

/** Shared glass-period control atmosphere; kept neutral so both modules stay coherent. */
val DailyRecordPeriodGlassTint = Color(0xFFEFF1FF)
val DailyRecordPeriodGlassGlow = Color(0xFFB9AEFF)
val DailyRecordPeriodInactiveText = Color(0xFF5C677C)

enum class RecordVisualState {
    Unset,
    ExplicitZero,
    One,
    Two,
    ThreePlus,
    Focused,
    Disabled,
}

@Immutable
data class RecordVisualColors(
    val background: Color,
    val content: Color,
    val outline: Color,
)

@Immutable
data class RecordModuleColorTokens(
    val primary: Color,
    val strong: Color,
    val soft: Color,
    val medium: Color,
    val onPrimary: Color = DailyRecordOnAccent,
) {
    fun colorsFor(state: RecordVisualState): RecordVisualColors = when (state) {
        RecordVisualState.Unset -> RecordVisualColors(
            background = DailyRecordSurfaceMuted,
            content = DailyRecordTextSecondary,
            outline = DailyRecordDivider,
        )
        RecordVisualState.ExplicitZero -> RecordVisualColors(
            background = DailyRecordSurface,
            content = primary,
            outline = primary,
        )
        RecordVisualState.One -> RecordVisualColors(
            background = soft,
            content = DailyRecordText,
            outline = soft,
        )
        RecordVisualState.Two -> RecordVisualColors(
            background = medium,
            content = DailyRecordText,
            outline = medium,
        )
        RecordVisualState.ThreePlus -> RecordVisualColors(
            background = primary,
            content = onPrimary,
            outline = primary,
        )
        RecordVisualState.Focused -> RecordVisualColors(
            background = strong,
            content = onPrimary,
            outline = strong,
        )
        RecordVisualState.Disabled -> RecordVisualColors(
            background = DailyRecordSurfaceDisabled,
            // The stronger neutral text preserves the disabled distinction
            // without sacrificing readable contrast on the darker future fill.
            content = DailyRecordTextSecondary,
            outline = DailyRecordDivider,
        )
    }
}

val HandBrewColorTokens = RecordModuleColorTokens(
    primary = Color(0xFF72517C),
    strong = Color(0xFF4B3354),
    soft = Color(0xFFE9DDEA),
    medium = Color(0xFFC8AFCF),
)

val SexColorTokens = RecordModuleColorTokens(
    primary = Color(0xFF8D3E45),
    strong = Color(0xFF5F272C),
    soft = Color(0xFFF2DCDD),
    medium = Color(0xFFDFAEB2),
)

/**
 * Shared controls outside a selected record module use the hand-brew purple as
 * the app-level accent. Module-aware screens must pass their own color tokens.
 */
val DailyRecordDefaultAccent = HandBrewColorTokens.primary
val DailyRecordDefaultAccentStrong = HandBrewColorTokens.strong
val DailyRecordDefaultAccentSoft = HandBrewColorTokens.soft
