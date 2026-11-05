package io.github.litaog.dailyrecord.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Liquid canvas neutrals (ADR-022): pearl canvas, white milk-glass surfaces and
// graphite text. Every text color keeps WCAG AA on every surface below.
val DailyRecordCanvas = Color(0xFFF8FAFC)
val DailyRecordCanvasEnd = Color(0xFFF1F5F9)
val DailyRecordSurface = Color(0xFFFFFFFF)
val DailyRecordSurfaceMuted = Color(0xFFF1F5F9)
/**
 * Disabled/future cells need to read as unavailable rather than as another
 * empty record. Keep this neutral (not module tinted) and darker than every
 * theme's past-unset fill.
 */
val DailyRecordSurfaceDisabled = Color(0xFFE6EAF0)
val DailyRecordText = Color(0xFF1E293B)
val DailyRecordTextSecondary = Color(0xFF475569)
// b1 proposed #94A3B8 for muted notes, but it measures 2.57:1 on white; this
// slate keeps the quiet tone while staying above 4.5:1 on muted surfaces.
val DailyRecordTextMuted = Color(0xFF586880)
val DailyRecordDivider = Color(0xFFE2E8F0)
val DailyRecordOnAccent = Color(0xFFFFFFFF)
val DailyRecordSuccess = Color(0xFF3F6F5A)
val DailyRecordWarning = Color(0xFF8A6A18)
val DailyRecordDanger = Color(0xFF9B3A32)
val DailyRecordDangerContainer = Color(0xFFF5E3E1)

/** The period control keeps its inactive labels neutral across both modules. */
val DailyRecordPeriodInactiveText = Color(0xFF536078)

enum class RecordVisualState {
    Unset,
    ExplicitZero,
    One,
    Two,
    Three,
    FourPlus,
    Focused,
    Disabled,
}

@Immutable
data class RecordVisualColors(
    val background: Color,
    val content: Color,
    val outline: Color,
)

/**
 * One record module's colors inside the selected theme.
 *
 * [primary] and [strong] are the deep accents that carry white text. The count
 * ladder [unset] > [soft] > [medium] > [intense] > [peak] stays light and airy
 * and always carries graphite text. [gradient] and [glow] are decorative.
 */
@Immutable
data class RecordModuleColorTokens(
    val primary: Color,
    val strong: Color,
    val soft: Color,
    val medium: Color,
    val intense: Color,
    val unset: Color,
    val onPrimary: Color = DailyRecordOnAccent,
    /** Four-or-more count fill. */
    val peak: Color = primary,
    val onPeak: Color = onPrimary,
    /** Multi-stop identity gradient; never placed behind body text. */
    val gradient: List<Color> = listOf(primary, strong),
    /** Ambient diffuse glow for liquid glass shadows and canvas orbs. */
    val glow: Color = primary.copy(alpha = .09f),
) {
    fun colorsFor(state: RecordVisualState): RecordVisualColors = when (state) {
        RecordVisualState.Unset -> RecordVisualColors(
            background = unset,
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
        RecordVisualState.Three -> RecordVisualColors(
            background = intense,
            content = DailyRecordText,
            outline = intense,
        )
        RecordVisualState.FourPlus -> RecordVisualColors(
            background = peak,
            content = onPeak,
            outline = peak,
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
 * Module-aware glass atmosphere for shared controls: the light tint keeps the
 * surface airy while the primary gives the selected slider a module glow.
 */
val RecordModuleColorTokens.periodGlassTint: Color
    get() = soft

val RecordModuleColorTokens.periodGlassGlow: Color
    get() = primary

/**
 * Masturbation module tokens of the selected theme. Read at call time (not
 * cached at class load) so a theme change reaches every screen after the
 * activity is recreated.
 */
val HandBrewColorTokens: RecordModuleColorTokens
    get() = DailyRecordThemeState.current.palette.solo

/** Sex module tokens of the selected theme. */
val SexColorTokens: RecordModuleColorTokens
    get() = DailyRecordThemeState.current.palette.duo

/**
 * Shared controls outside a selected record module use the masturbation
 * module accent of the selected theme. Module-aware screens must pass their
 * own color tokens.
 */
val DailyRecordDefaultAccent: Color
    get() = HandBrewColorTokens.primary
val DailyRecordDefaultAccentStrong: Color
    get() = HandBrewColorTokens.strong
val DailyRecordDefaultAccentSoft: Color
    get() = HandBrewColorTokens.soft
