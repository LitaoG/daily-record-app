package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.ui.components.DailyRecordHaptics
import io.github.litaog.dailyrecord.ui.theme.LiquidGlassElevation
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.appleLiquidGlass
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Nearest day (0-based) under [x] on the month pulse chart. Inverts
 * [monthDailyChartXPosition], so the probe always lands on a drawn pulse.
 */
internal fun monthDailyChartIndexAt(
    x: Float,
    dayCount: Int,
    width: Float,
    inset: Float,
): Int {
    if (dayCount <= 1) return 0
    val usable = (width - inset * 2f).coerceAtLeast(0f)
    if (usable <= 0f) return 0
    val fraction = ((x - inset) / usable).coerceIn(0f, 1f)
    return (fraction * (dayCount - 1)).roundToInt().coerceIn(0, dayCount - 1)
}

/** Month slot (0-based) under [x] on the year line chart, which uses equal slots. */
internal fun yearLineChartIndexAt(
    x: Float,
    width: Float,
    monthCount: Int = 12,
): Int {
    if (width <= 0f || monthCount <= 0) return 0
    return floor(x / width * monthCount).toInt().coerceIn(0, monthCount - 1)
}

/** Left edge of a value bubble centered on [anchorX] and kept inside the plot. */
internal fun chartBubbleX(
    anchorX: Float,
    bubbleWidth: Int,
    containerWidth: Int,
): Int = (anchorX.roundToInt() - bubbleWidth / 2)
    .coerceIn(0, (containerWidth - bubbleWidth).coerceAtLeast(0))

/** Which chart point the finger is on, plus the last one while the probe fades out. */
@Stable
internal class ChartScrubState {
    /** Point under the finger, or null when the chart is not touched. */
    var activeIndex by mutableStateOf<Int?>(null)
        private set

    /** Last touched point; kept after release so the probe can fade out in place. */
    var shownIndex by mutableStateOf<Int?>(null)
        private set

    fun update(index: Int?) {
        activeIndex = index
        if (index != null) shownIndex = index
    }
}

@Composable
internal fun rememberChartScrubState(): ChartScrubState = remember { ChartScrubState() }

/**
 * Touch scrubbing for a chart (ADR-022). Pressing shows the probe at the
 * nearest point; dragging sideways moves it point by point with a light tick.
 * A mostly vertical drag ends the scrub and leaves the gesture to the page
 * scroll, while a sideways drag is consumed so the page does not scroll under
 * the finger. TalkBack users keep the chart's full description.
 */
internal fun Modifier.chartScrub(
    key: Any?,
    indexAt: (x: Float, width: Float) -> Int,
    onScrub: (Int?) -> Unit,
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val currentIndexAt by rememberUpdatedState(indexAt)
    val currentOnScrub by rememberUpdatedState(onScrub)
    pointerInput(key) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val width = size.width.toFloat()
            var lastIndex = currentIndexAt(down.position.x, width)
            currentOnScrub(lastIndex)
            val slop = viewConfiguration.touchSlop
            var horizontal = false
            var totalX = 0f
            var totalY = 0f
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (change.changedToUpIgnoreConsumed()) break
                val delta = change.positionChange()
                if (!horizontal) {
                    totalX += delta.x
                    totalY += delta.y
                    // Mostly vertical: hand the gesture back to the page scroll.
                    if (abs(totalY) > slop && abs(totalY) > abs(totalX)) break
                    if (abs(totalX) > slop) horizontal = true
                }
                if (horizontal) change.consume()
                val index = currentIndexAt(change.position.x, width)
                if (index != lastIndex) {
                    lastIndex = index
                    DailyRecordHaptics.tick(haptic)
                    currentOnScrub(index)
                }
            }
            currentOnScrub(null)
        }
    }
}

/** Draws the scrub probe: a soft vertical guide and a ring on the touched point. */
internal fun DrawScope.drawChartProbe(
    x: Float,
    pointY: Float,
    topY: Float,
    bottomY: Float,
    color: Color,
    alpha: Float,
) {
    if (alpha <= 0f) return
    drawLine(
        color = color.copy(alpha = .45f * alpha),
        start = Offset(x, topY),
        end = Offset(x, bottomY),
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = 7.dp.toPx(),
        center = Offset(x, pointY),
        style = Stroke(width = 2.dp.toPx()),
    )
}

private val ChartBubbleShape = RoundedCornerShape(12.dp)

/**
 * Small glass value bubble pinned to the top of the plot, centered on the
 * probe and kept inside the plot width. [alpha] is read in the draw phase, so
 * the fade does not recompose. The bubble is transient touch feedback and is
 * hidden from TalkBack, which reads the full chart description instead.
 */
@Composable
internal fun ChartScrubBubble(
    text: String,
    anchorX: Float,
    colors: RecordModuleColorTokens,
    alpha: () -> Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .testTag("chart_scrub_bubble")
            .clearAndSetSemantics { }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                val containerWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
                val x = chartBubbleX(anchorX, placeable.width, containerWidth)
                layout(containerWidth, placeable.height) { placeable.place(x, 0) }
            }
            .graphicsLayer { this.alpha = alpha() }
            .appleLiquidGlass(
                shape = ChartBubbleShape,
                glowColor = colors.primary,
                elevation = LiquidGlassElevation.Subtle,
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = colors.strong,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}
