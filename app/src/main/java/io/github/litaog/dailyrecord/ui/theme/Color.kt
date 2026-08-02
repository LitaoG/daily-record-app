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

/** The period control keeps its inactive labels neutral across both modules. */
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

/**
 * Module-aware glass atmosphere for shared controls.
 *
 * The light tint keeps the surface airy while the primary colour gives the
 * selected slider a restrained module-specific glow (purple for hand brew,
 * deep red for intimacy). Keeping this derived from the module token prevents
 * a shared control from silently falling back to the hand-brew palette.
 */
val RecordModuleColorTokens.periodGlassTint: Color
    get() = soft

val RecordModuleColorTokens.periodGlassGlow: Color
    get() = primary

val HandBrewColorTokens = RecordModuleColorTokens(
    // Brighter, cleaner purple keeps the module expressive without turning
    // the selected surfaces grey or near-black.
    primary = Color(0xFF85569A),
    strong = Color(0xFF603670),
    soft = Color(0xFFEEDAF3),
    medium = Color(0xFFD5AFDF),
)

val SexColorTokens = RecordModuleColorTokens(
    // Use a clear wine red with enough warmth and saturation to avoid a
    // muddy brown cast in selected cards and glass controls.
    primary = Color(0xFFA54658),
    strong = Color(0xFF7A3040),
    soft = Color(0xFFF4D8DD),
    medium = Color(0xFFE4A9B2),
)

/**
 * Shared controls outside a selected record module use the hand-brew purple as
 * the app-level accent. Module-aware screens must pass their own color tokens.
 */
val DailyRecordDefaultAccent = HandBrewColorTokens.primary
val DailyRecordDefaultAccentStrong = HandBrewColorTokens.strong
val DailyRecordDefaultAccentSoft = HandBrewColorTokens.soft
