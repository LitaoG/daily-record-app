package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.ui.graphics.Color
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.statistics.StatisticsDetail
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceDisabled
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.RecordVisualState
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal const val WEEK_SEGMENT_COUNT = 7
internal const val WEEK_SEGMENT_GAP_DEGREES = 11f
private const val WEEK_LABEL_START_DEGREES = 0f
internal const val WEEK_LABEL_GAP_DP = 12f
private const val WEEK_SIDE_LABEL_EXTRA_GAP_DP = 6f
// Canvas.drawArc uses 0° at 3 o'clock, so its equivalent is 90° clockwise.
private const val WEEK_CANVAS_ANGLE_OFFSET_DEGREES = -90f

internal enum class WeekRingState {
    Future,
    Unrecorded,
    ExplicitZero,
    Positive,
}

internal enum class WeekRingCountBand {
    One,
    Two,
    Three,
    FourPlus,
}

internal enum class WeekRingLegendMarkerStyle {
    Filled,
    HollowPrimary,
    HollowNeutral,
}

internal fun weekRingState(detail: StatisticsDetail): WeekRingState = when {
    detail.future -> WeekRingState.Future
    !detail.recorded -> WeekRingState.Unrecorded
    (detail.count ?: 0L) <= 0L -> WeekRingState.ExplicitZero
    else -> WeekRingState.Positive
}

internal fun weekRingCountBand(detail: StatisticsDetail): WeekRingCountBand? {
    if (weekRingState(detail) != WeekRingState.Positive) return null
    return when (detail.count ?: 0L) {
        1L -> WeekRingCountBand.One
        2L -> WeekRingCountBand.Two
        3L -> WeekRingCountBand.Three
        else -> WeekRingCountBand.FourPlus
    }
}

internal fun weekRingLegendMarkerStyle(state: WeekRingState): WeekRingLegendMarkerStyle = when (state) {
    WeekRingState.ExplicitZero -> WeekRingLegendMarkerStyle.HollowPrimary
    WeekRingState.Unrecorded -> WeekRingLegendMarkerStyle.HollowNeutral
    WeekRingState.Future,
    WeekRingState.Positive -> WeekRingLegendMarkerStyle.Filled
}

internal fun weekRingPositiveDayCount(details: List<StatisticsDetail>): Int =
    details.count { weekRingState(it) == WeekRingState.Positive }

internal fun weekRingColorForBand(
    band: WeekRingCountBand,
    colors: RecordModuleColorTokens,
): Color = colors.colorsFor(
    when (band) {
        WeekRingCountBand.One -> RecordVisualState.One
        WeekRingCountBand.Two -> RecordVisualState.Two
        WeekRingCountBand.Three -> RecordVisualState.Three
        WeekRingCountBand.FourPlus -> RecordVisualState.FourPlus
    },
).background

internal fun weekRingStrokeWidthDp(state: WeekRingState): Float = when (state) {
    WeekRingState.Future -> 10f
    WeekRingState.Unrecorded -> 4f
    WeekRingState.ExplicitZero -> 12f
    WeekRingState.Positive -> 16f
}

internal fun weekRingLabelColor(state: WeekRingState): Color = when (state) {
    WeekRingState.Future,
    WeekRingState.Unrecorded,
    WeekRingState.ExplicitZero -> DailyRecordTextMuted
    WeekRingState.Positive -> DailyRecordText
}

private fun weekSegmentSizeDegrees(): Float = 360f / WEEK_SEGMENT_COUNT

internal fun weekRingLabelAngleDegrees(index: Int): Float =
    WEEK_LABEL_START_DEGREES +
        index * weekSegmentSizeDegrees()

internal fun weekRingCanvasMidpointDegrees(index: Int): Float =
    weekRingLabelAngleDegrees(index) + WEEK_CANVAS_ANGLE_OFFSET_DEGREES

private fun weekRingLabelRadialSupport(
    angleDegrees: Float,
    labelHalfWidth: Float,
    labelHalfHeight: Float,
): Float {
    val angle = Math.toRadians(angleDegrees.toDouble())
    return labelHalfWidth * abs(sin(angle).toFloat()) +
        labelHalfHeight * abs(cos(angle).toFloat())
}

internal fun weekRingLabelRadialDistance(
    angleDegrees: Float,
    ringOuterRadius: Float,
    labelHalfWidth: Float,
    labelHalfHeight: Float,
    gap: Float,
): Float {
    return ringOuterRadius +
        weekRingLabelRadialSupport(angleDegrees, labelHalfWidth, labelHalfHeight) +
        gap
}

internal fun weekRingLabelGapDp(
    segmentIndex: Int,
    sharedGap: Float,
): Float = sharedGap + weekRingSideLabelExtraGapDp(segmentIndex)

internal fun weekRingSideLabelExtraGapDp(segmentIndex: Int): Float =
    if (segmentIndex == 2 || segmentIndex == 5) WEEK_SIDE_LABEL_EXTRA_GAP_DP else 0f

/**
 * Returns the radial limit for a label while allowing a small amount of the
 * surrounding card padding to be used by the Wednesday/Saturday labels.
 */
internal fun weekRingLabelHorizontalLimitDp(
    angleDegrees: Float,
    availableHalfWidth: Float,
    boundaryAllowance: Float,
): Float {
    val angleSin = abs(sin(Math.toRadians(angleDegrees.toDouble()).toFloat()))
    return if (angleSin > .001f) {
        (availableHalfWidth + boundaryAllowance) / angleSin
    } else {
        Float.POSITIVE_INFINITY
    }
}

internal fun weekRingSharedLabelGapDp(
    ringOuterRadius: Float,
    labelHalfWidth: Float,
    labelHalfHeight: Float,
    availableHalfWidth: Float,
    availableHalfHeight: Float,
    preferredGap: Float,
): Float {
    var sharedGap = preferredGap
    repeat(WEEK_SEGMENT_COUNT) { index ->
        val angleDegrees = weekRingLabelAngleDegrees(index)
        val angle = Math.toRadians(angleDegrees.toDouble())
        val angleCos = abs(cos(angle).toFloat())
        val horizontalLimit = weekRingLabelHorizontalLimitDp(
            angleDegrees = angleDegrees,
            availableHalfWidth = availableHalfWidth,
            boundaryAllowance = 0f,
        )
        val verticalLimit = if (angleCos > .001f) {
            availableHalfHeight / angleCos
        } else {
            Float.POSITIVE_INFINITY
        }
        val maximumRadius = minOf(horizontalLimit, verticalLimit)
        val zeroGapRadius = weekRingLabelRadialDistance(
            angleDegrees = angleDegrees,
            ringOuterRadius = ringOuterRadius,
            labelHalfWidth = labelHalfWidth,
            labelHalfHeight = labelHalfHeight,
            gap = 0f,
        )
        sharedGap = minOf(sharedGap, maximumRadius - zeroGapRadius)
    }
    return sharedGap.coerceAtLeast(0f)
}

/**
 * Keeps a clipped first week aligned with its real weekday angle. Details
 * without a source index fall back to the list position (used by tests).
 */
internal fun weekRingSegmentIndex(detail: StatisticsDetail, fallbackIndex: Int): Int =
    detail.calendarIndex ?: fallbackIndex

internal data class WeekRingLabelParts(
    val weekday: String,
    val day: String,
)

/**
 * Derive the two visual labels from the source date rather than splitting the
 * localized summary label.  A localized weekday may contain spaces or use a
 * different ordering, so string parsing would silently corrupt the ring
 * labels as soon as another locale is added.
 */
internal fun weekRingLabelParts(detail: StatisticsDetail): WeekRingLabelParts =
    detail.date?.let { date ->
        WeekRingLabelParts(
            weekday = AppCopy.weekdayName(date.dayOfWeek.value),
            day = AppCopy.Statistics.dayLabel(date.dayOfMonth),
        )
    } ?: WeekRingLabelParts(
        weekday = detail.label,
        day = "",
    )

/**
 * Single source of truth for the non-positive ring segment colors (future,
 * unrecorded and explicit zero). Positive bands resolve through
 * [weekRingColorForBand]; each state resolves through exactly one function so
 * the legend can never drift from the chart (for example, an explicit zero
 * must never use the unrecorded color). The positive branch below is kept for
 * symmetry and test assertions; production renders positive bands via
 * [weekRingColorForBand].
 */
internal fun weekRingSegmentColor(
    state: WeekRingState,
    intensity: Float,
    colors: RecordModuleColorTokens,
): Color = when (state) {
    // Future ring segments must match the homepage's future date cells
    // exactly: both use the same module-independent warm grey-brown
    // (DailyRecordSurfaceDisabled), not the pale unset fill that the
    // homepage reserves for past unfilled dates. The weekday/date copy
    // stays muted separately.
    WeekRingState.Future -> DailyRecordSurfaceDisabled
    // Past-unfilled (unrecorded) segments use the same module-specific pale
    // fill as the homepage's unset cells, so the ring and its legend read
    // identically to the calendar. The weekday/date copy stays muted.
    WeekRingState.Unrecorded -> colors.colorsFor(RecordVisualState.Unset).background
    WeekRingState.ExplicitZero -> colors.primary
    WeekRingState.Positive ->
        colors.primary.copy(alpha = .58f + .38f * intensity.coerceIn(0f, 1f))
}
