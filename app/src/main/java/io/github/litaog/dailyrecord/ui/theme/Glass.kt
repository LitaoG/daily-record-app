package io.github.litaog.dailyrecord.ui.theme

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid glass material (ADR-022).
 *
 * Two decoupled layers keep text sharp:
 * 1. [DailyRecordLiquidBackdrop] paints the pearl canvas and one faint module
 *    glow; only that empty layer is blurred, and only on API 31+ where
 *    `RenderEffect` exists. Older devices draw a radial fade that looks the same.
 * 2. [appleLiquidGlass] paints a surface: diffuse shadow, milk-glass fill and a
 *    top-lit rim. It never blurs, so text, icons and controls inside stay crisp.
 *
 * Color restraint: module hues are always flat fills. Gradients only appear as
 * white glass light (fill sheen, rim, slider highlight), the single faint glow
 * and single-hue fades; no surface mixes several hues, to avoid gradient fatigue.
 *
 * These stay modifiers instead of layout composables: callers keep their
 * measurement, padding and semantics while only the paint layer changes.
 */
enum class DailyRecordGlassLevel {
    Base,
    Muted,
    Elevated,
    Emphasis,
}

enum class LiquidGlassElevation(val shadow: Dp) {
    /** Nested glass inside another glass surface; no extra shadow. */
    Flat(0.dp),
    /** Small glass such as chips, capsules and selector tracks. */
    Subtle(3.dp),
    /** Standard cards, the today island and chart containers. */
    Elevated(8.dp),
    /** Focus surfaces such as the popover stepper and dialogs. */
    Emphasis(16.dp),
}

private val GlassNeutralTint = Color(0xFFE2E8F0)
private val LiquidShadowAmbient = Color(0x0A000000)
private val LiquidShadowSpot = Color(0x14000000)
private val LiquidGlowRadius = 220.dp
private val LiquidGlowBlur = 45.dp

// Top-lit rim: bright where the light lands, fading along the lower edge.
private val LiquidRimBrush = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = .90f),
        Color.White.copy(alpha = .35f),
        Color.White.copy(alpha = .15f),
    ),
)

/**
 * Paints a liquid glass surface. [glowColor] tints the shadow and a 6% wash of
 * the milk glass with the module identity; [edgeColor] replaces the rim for
 * selected/outlined states.
 *
 * The fill never drops below 84% white, so an elevation shadow cannot bleed
 * through the surface and text contrast is judged against near-white.
 */
fun Modifier.appleLiquidGlass(
    shape: Shape,
    glowColor: Color = Color.Transparent,
    elevation: LiquidGlassElevation = LiquidGlassElevation.Elevated,
    edgeColor: Color? = null,
): Modifier {
    val shadowed = if (elevation.shadow > 0.dp) {
        shadow(
            elevation = elevation.shadow,
            shape = shape,
            clip = false,
            ambientColor = LiquidShadowAmbient,
            spotColor = if (glowColor.alpha == 0f) LiquidShadowSpot else glowColor.copy(alpha = .16f),
        )
    } else {
        this
    }
    return shadowed
        .clip(shape)
        .background(liquidGlassFill(glowColor, elevation))
        .border(
            if (edgeColor != null) BorderStroke(1.dp, edgeColor) else BorderStroke(1.dp, LiquidRimBrush),
            shape,
        )
}

fun Modifier.dailyRecordGlassBackground(
    moduleColors: RecordModuleColorTokens? = null,
    level: DailyRecordGlassLevel = DailyRecordGlassLevel.Base,
): Modifier = background(
    brush = liquidGlassFill(
        glowColor = moduleColors?.primary ?: GlassNeutralTint,
        elevation = level.toElevation(),
    ),
)

fun Modifier.dailyRecordGlass(
    shape: Shape,
    moduleColors: RecordModuleColorTokens? = null,
    level: DailyRecordGlassLevel = DailyRecordGlassLevel.Base,
    edgeColor: Color? = null,
): Modifier = appleLiquidGlass(
    shape = shape,
    glowColor = moduleColors?.primary ?: Color.Transparent,
    elevation = level.toElevation(),
    edgeColor = edgeColor,
)

private fun DailyRecordGlassLevel.toElevation(): LiquidGlassElevation = when (this) {
    DailyRecordGlassLevel.Muted -> LiquidGlassElevation.Flat
    DailyRecordGlassLevel.Base -> LiquidGlassElevation.Subtle
    DailyRecordGlassLevel.Elevated -> LiquidGlassElevation.Elevated
    DailyRecordGlassLevel.Emphasis -> LiquidGlassElevation.Emphasis
}

private fun liquidGlassFill(glowColor: Color, elevation: LiquidGlassElevation): Brush {
    val (top, middle) = when (elevation) {
        LiquidGlassElevation.Flat -> .86f to .84f
        LiquidGlassElevation.Subtle -> .90f to .86f
        LiquidGlassElevation.Elevated -> .94f to .88f
        LiquidGlassElevation.Emphasis -> .97f to .92f
    }
    // A 6% wash of the module hue sits in the lower part of the milk glass;
    // the sheen itself is white, so the surface never reads as a hue gradient.
    val wash = if (glowColor.alpha == 0f) {
        Color.White
    } else {
        lerp(Color.White, glowColor.copy(alpha = 1f), .06f)
    }
    return Brush.verticalGradient(
        colors = listOf(
            Color.White.copy(alpha = top),
            Color.White.copy(alpha = middle),
            wash.copy(alpha = middle),
        ),
    )
}

/** Flat accent fill; white text on it reaches WCAG AA in every theme. */
fun dailyRecordAccentBrush(colors: RecordModuleColorTokens): Brush = SolidColor(colors.primary)

fun dailyRecordAccentBrush(accent: Color): Brush = SolidColor(accent)

/** Period capsule track: one translucent tint of the current module. */
fun dailyRecordPeriodTrackBrush(colors: RecordModuleColorTokens): Brush =
    SolidColor(lerp(Color.White, colors.soft, .55f).copy(alpha = .92f))

/** Floating period slider: white glass with a soft top highlight only. */
fun dailyRecordPeriodSliderBrush(@Suppress("UNUSED_PARAMETER") colors: RecordModuleColorTokens): Brush =
    Brush.verticalGradient(
        colors = listOf(Color.White, Color.White.copy(alpha = .94f)),
    )

/**
 * Pearl canvas with a barely visible wash of the current module at the top
 * (10%), fading into neutral pearl so color never competes with content.
 */
fun dailyRecordBackdropBrush(colors: RecordModuleColorTokens): Brush =
    Brush.verticalGradient(
        colors = listOf(
            lerp(DailyRecordCanvas, colors.soft, .10f),
            DailyRecordCanvas,
            DailyRecordCanvasEnd,
        ),
    )

/**
 * Full-screen backdrop layer. Place it as the first child of a screen root so
 * every glass surface floats above it. Only this empty layer is ever blurred.
 */
@Composable
fun DailyRecordLiquidBackdrop(
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    val supportsBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Box(modifier.fillMaxSize().background(dailyRecordBackdropBrush(colors))) {
        Box(
            Modifier
                .fillMaxSize()
                .then(
                    if (supportsBlur) {
                        Modifier.blur(LiquidGlowBlur, BlurredEdgeTreatment.Unbounded)
                    } else {
                        Modifier
                    },
                )
                .drawBehind {
                    // One faint module glow behind the top of the screen.
                    val radius = LiquidGlowRadius.toPx()
                    val center = Offset(size.width * .86f, size.height * .04f)
                    val glow = colors.glow.copy(alpha = .12f)
                    if (supportsBlur) {
                        drawCircle(color = glow, radius = radius, center = center)
                    } else {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(glow, Color.Transparent),
                                center = center,
                                radius = radius,
                            ),
                            radius = radius,
                            center = center,
                        )
                    }
                },
        )
    }
}
