package io.github.litaog.dailyrecord.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom settings glyph for the quiet private journal: a soft eight-tooth
 * gear with a heart cut out of its center. The gear keeps the top-bar
 * graphite tone while the heart follows the selected module accent,
 * replacing the generic Material settings gear.
 */
@Composable
fun PrivateSettingsIcon(
    contentDescription: String?,
    modifier: Modifier = Modifier,
    gearTint: Color = DailyRecordTextSecondary,
    heartTint: Color = DailyRecordDefaultAccent,
) {
    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    Canvas(modifier.then(semanticsModifier).size(24.dp)) {
        val unit = size.width / ViewportSize
        drawPath(path = privateSettingsGearPath(unit), color = gearTint)
        drawPath(path = privateSettingsHeartPath(unit), color = heartTint)
    }
}

private const val ViewportSize = 24f
private const val GearCenter = 12f
private const val GearTeeth = 8
private const val GearRootRadius = 6.55f
private const val GearTipRadius = 9.45f

/** Within one 45° pitch: root flat, smoothstep rise, tip flat, smoothstep fall. */
private const val RiseStart = 9f
private const val RiseEnd = 16f
private const val FallStart = 27f
private const val FallEnd = 34f

/** Tip flats are centered 21.5° into each pitch; shift so tips sit on the axes. */
private const val ToothPhaseShift = -21.5f
private const val SampleStepDegrees = 0.75f

/** Material "favorite" heart geometry, scaled to fit inside the gear root circle. */
private const val HeartSourceCenterY = 12.175f
private const val HeartCenterY = 12.2f
private const val HeartScale = 0.38f

/** Slightly larger so the overlay heart bleeds cleanly into the gear cutout. */
private const val HeartOverlayScale = 0.4f

private fun privateSettingsGearPath(unit: Float): Path {
    val path = Path()
    path.fillType = PathFillType.EvenOdd
    val pitch = 360f / GearTeeth
    var degree = 0f
    while (degree < 360f) {
        val theta = Math.toRadians((degree + ToothPhaseShift).toDouble())
        val radius = gearRadiusAt(degree % pitch)
        val x = (GearCenter + (radius * cos(theta)).toFloat()) * unit
        val y = (GearCenter + (radius * sin(theta)).toFloat()) * unit
        if (degree == 0f) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
        degree += SampleStepDegrees
    }
    path.close()
    path.addHeart(unit, HeartScale)
    return path
}

private fun privateSettingsHeartPath(unit: Float): Path {
    val path = Path()
    path.addHeart(unit, HeartOverlayScale)
    return path
}

private fun gearRadiusAt(pitchAngle: Float): Float = when {
    pitchAngle < RiseStart -> GearRootRadius
    pitchAngle < RiseEnd -> {
        val t = (pitchAngle - RiseStart) / (RiseEnd - RiseStart)
        GearRootRadius + (GearTipRadius - GearRootRadius) * smoothStep(t)
    }
    pitchAngle < FallStart -> GearTipRadius
    pitchAngle < FallEnd -> {
        val t = (pitchAngle - FallStart) / (FallEnd - FallStart)
        GearTipRadius - (GearTipRadius - GearRootRadius) * smoothStep(t)
    }
    else -> GearRootRadius
}

private fun smoothStep(t: Float): Float = t * t * (3f - 2f * t)

private fun Path.addHeart(unit: Float, scale: Float) {
    fun px(x: Float) = (GearCenter + (x - GearCenter) * scale) * unit
    fun py(y: Float) = (HeartCenterY + (y - HeartSourceCenterY) * scale) * unit
    moveTo(px(12f), py(21.35f))
    lineTo(px(10.55f), py(20.03f))
    cubicTo(px(5.4f), py(15.36f), px(2f), py(12.28f), px(2f), py(8.5f))
    cubicTo(px(2f), py(5.42f), px(4.42f), py(3f), px(7.5f), py(3f))
    cubicTo(px(9.24f), py(3f), px(10.91f), py(3.81f), px(12f), py(5.09f))
    cubicTo(px(13.09f), py(3.81f), px(14.76f), py(3f), px(16.5f), py(3f))
    cubicTo(px(19.58f), py(3f), px(22f), py(5.42f), px(22f), py(8.5f))
    cubicTo(px(22f), py(12.28f), px(18.6f), py(15.36f), px(13.45f), py(20.04f))
    close()
}
