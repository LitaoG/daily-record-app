package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlass
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val WEEK_SEGMENT_COUNT = 7
private const val WEEK_SEGMENT_GAP_DEGREES = 11f
private const val WEEK_LABEL_START_DEGREES = 0f
private const val WEEK_LABEL_GAP_DP = 12f
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
): Color = when (band) {
    WeekRingCountBand.One -> colors.soft
    WeekRingCountBand.Two -> colors.medium
    WeekRingCountBand.Three -> colors.primary.copy(alpha = .78f)
    WeekRingCountBand.FourPlus -> colors.strong
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
        val angleSin = abs(sin(angle).toFloat())
        val angleCos = abs(cos(angle).toFloat())
        val horizontalLimit = if (angleSin > .001f) {
            availableHalfWidth / angleSin
        } else {
            Float.POSITIVE_INFINITY
        }
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
 * Keeps a clipped first week aligned with its real weekday angle. Older
 * callers that construct details without a source index retain the list
 * position as a safe fallback.
 */
internal fun weekRingSegmentIndex(detail: StatisticsDetail, fallbackIndex: Int): Int =
    detail.calendarIndex ?: fallbackIndex

/**
 * Single source of truth for ring segment colors. The legend and the ring
 * drawing must resolve colors through this function so the legend can never
 * drift from the chart (for example, an explicit zero must never use the
 * unrecorded color).
 */
internal fun weekRingSegmentColor(
    state: WeekRingState,
    intensity: Float,
    colors: RecordModuleColorTokens,
): Color = when (state) {
    WeekRingState.Future -> colors.soft.copy(alpha = .78f)
    WeekRingState.Unrecorded -> DailyRecordDivider.copy(alpha = .92f)
    WeekRingState.ExplicitZero -> colors.primary
    WeekRingState.Positive ->
        colors.primary.copy(alpha = .58f + .38f * intensity.coerceIn(0f, 1f))
}

@Composable
internal fun WeekDistributionCard(
    details: List<StatisticsDetail>,
    modifier: Modifier = Modifier,
    colors: RecordModuleColorTokens = HandBrewColorTokens,
) {
    val recordedDays = weekRingPositiveDayCount(details)

    DistributionSurface(
        title = AppCopy.Statistics.dailyDistribution,
        subtitle = AppCopy.Statistics.times,
        modifier = modifier.testTag("week_distribution_card"),
        colors = colors,
    ) {
        WeekRingChart(
            details = details,
            recordedDays = recordedDays,
            colors = colors,
        )
        WeekRingLegend(colors = colors)
    }
}

@Composable
private fun WeekRingChart(
    details: List<StatisticsDetail>,
    recordedDays: Int,
    colors: RecordModuleColorTokens,
) {
    val chartHeight = 304.dp
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight)
            .testTag("week_distribution_chart"),
    ) {
        val centerY = chartHeight / 2
        val labelWidth = 68.dp
        val labelHeight = 44.dp
        val radius = minOf(maxWidth * .28f, 84.dp)
        val centerX = maxWidth / 2
        val positiveStroke = 16.dp
        val ringOuterRadius = radius + positiveStroke / 2
        // Wednesday and Saturday are the tightest labels on narrow screens.
        // Reuse their resolved clearance for every weekday so the labels sit
        // on one visually consistent outer ring instead of being clamped one
        // by one at different distances.
        val sharedLabelGap = weekRingSharedLabelGapDp(
            ringOuterRadius = ringOuterRadius.value,
            labelHalfWidth = (labelWidth / 2).value,
            labelHalfHeight = (labelHeight / 2).value,
            availableHalfWidth = (maxWidth / 2 - labelWidth / 2).value,
            availableHalfHeight = (chartHeight / 2 - labelHeight / 2).value,
            preferredGap = WEEK_LABEL_GAP_DP,
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(x = size.width / 2f, y = centerY.toPx())
            val radiusPx = radius.toPx()
            val segmentSweep = 360f / WEEK_SEGMENT_COUNT - WEEK_SEGMENT_GAP_DEGREES

            details.take(WEEK_SEGMENT_COUNT).forEachIndexed { index, detail ->
                val segmentIndex = weekRingSegmentIndex(detail, index)
                val startAngle = weekRingCanvasMidpointDegrees(segmentIndex) -
                    segmentSweep / 2f
                val state = weekRingState(detail)
                when (state) {
                    WeekRingState.Positive -> drawArc(
                        color = weekRingColorForBand(
                            band = requireNotNull(weekRingCountBand(detail)),
                            colors = colors,
                        ),
                        startAngle = startAngle,
                        sweepAngle = segmentSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radiusPx, center.y - radiusPx),
                        size = androidx.compose.ui.geometry.Size(radiusPx * 2f, radiusPx * 2f),
                        style = Stroke(
                            width = positiveStroke.toPx(),
                            cap = StrokeCap.Round,
                        ),
                    )
                    WeekRingState.ExplicitZero -> {
                        drawArc(
                            color = weekRingSegmentColor(WeekRingState.ExplicitZero, 0f, colors),
                            startAngle = startAngle,
                            sweepAngle = segmentSweep,
                            useCenter = false,
                            topLeft = Offset(center.x - radiusPx, center.y - radiusPx),
                            size = androidx.compose.ui.geometry.Size(radiusPx * 2f, radiusPx * 2f),
                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                        )
                        drawArc(
                            color = DailyRecordSurface,
                            startAngle = startAngle,
                            sweepAngle = segmentSweep,
                            useCenter = false,
                            topLeft = Offset(center.x - radiusPx, center.y - radiusPx),
                            size = androidx.compose.ui.geometry.Size(radiusPx * 2f, radiusPx * 2f),
                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                    WeekRingState.Future -> drawArc(
                        color = colors.soft.copy(alpha = .78f),
                        startAngle = startAngle,
                        sweepAngle = segmentSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radiusPx, center.y - radiusPx),
                        size = androidx.compose.ui.geometry.Size(radiusPx * 2f, radiusPx * 2f),
                        style = Stroke(
                            width = 10.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(
                                intervals = floatArrayOf(7.dp.toPx(), 6.dp.toPx()),
                            ),
                        ),
                    )
                    WeekRingState.Unrecorded -> drawArc(
                        color = DailyRecordDivider.copy(alpha = .92f),
                        startAngle = startAngle,
                        sweepAngle = segmentSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radiusPx, center.y - radiusPx),
                        size = androidx.compose.ui.geometry.Size(radiusPx * 2f, radiusPx * 2f),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
        }

        details.take(WEEK_SEGMENT_COUNT).forEachIndexed { index, detail ->
            val segmentIndex = weekRingSegmentIndex(detail, index)
            val angleDegrees = weekRingLabelAngleDegrees(segmentIndex)
            val angle = Math.toRadians(angleDegrees.toDouble())
            val angleSin = abs(sin(angle).toFloat())
            val angleCos = abs(cos(angle).toFloat())
            val desiredRadius = weekRingLabelRadialDistance(
                angleDegrees = angleDegrees,
                ringOuterRadius = ringOuterRadius.value,
                labelHalfWidth = (labelWidth / 2).value,
                labelHalfHeight = (labelHeight / 2).value,
                gap = sharedLabelGap,
            ).dp
            val horizontalLimit = if (angleSin > .001f) {
                ((maxWidth / 2 - labelWidth / 2).value / angleSin).dp
            } else {
                maxWidth
            }
            val verticalSpace = if (cos(angle) >= 0) {
                centerY - labelHeight / 2
            } else {
                chartHeight - centerY - labelHeight / 2
            }
            val verticalLimit = if (angleCos > .001f) {
                (verticalSpace.value / angleCos).dp
            } else {
                chartHeight
            }
            val labelRadius = minOf(desiredRadius, horizontalLimit, verticalLimit)
            val labelX = centerX - labelWidth / 2 + labelRadius * sin(angle).toFloat()
            val labelY = centerY - labelHeight / 2 - labelRadius * cos(angle).toFloat()
            val labelColor = when (weekRingState(detail)) {
                WeekRingState.Future -> colors.primary.copy(alpha = .45f)
                WeekRingState.Unrecorded -> DailyRecordTextMuted
                WeekRingState.ExplicitZero -> DailyRecordTextMuted
                WeekRingState.Positive -> DailyRecordText
            }
            Column(
                modifier = Modifier
                    .offset(x = labelX, y = labelY)
                    .width(labelWidth)
                    .height(labelHeight)
                    .semantics(mergeDescendants = true) {
                        contentDescription = weekDistributionDescription(detail)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                val countLabel = detail.count
                    ?.takeIf { it > 0L && detail.recorded && !detail.future }
                    ?.let(AppCopy.Statistics::weeklyCountSuffix)
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = detail.label.substringBefore(" "),
                        color = labelColor,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    if (countLabel != null) {
                        Text(
                            text = countLabel,
                            color = colors.primary,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    text = detail.label.substringAfter(" ", detail.label),
                    color = labelColor.copy(alpha = .72f),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center).padding(top = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = AppCopy.Statistics.weeklySummaryTitle,
                color = DailyRecordText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = AppCopy.Statistics.weeklyRecordedDays(recordedDays, details.size),
                color = colors.primary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = AppCopy.Statistics.weeklyRecordedLabel,
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun WeekRingLegend(colors: RecordModuleColorTokens) {
    val legend = listOf(
        WeekRingLegendItem(
            label = AppCopy.Statistics.weeklyLegendFourPlus,
            color = weekRingColorForBand(WeekRingCountBand.FourPlus, colors),
        ),
        WeekRingLegendItem(
            label = AppCopy.Statistics.weeklyLegendThree,
            color = weekRingColorForBand(WeekRingCountBand.Three, colors),
        ),
        WeekRingLegendItem(
            label = AppCopy.Statistics.weeklyLegendTwo,
            color = weekRingColorForBand(WeekRingCountBand.Two, colors),
        ),
        WeekRingLegendItem(
            label = AppCopy.Statistics.weeklyLegendOne,
            color = weekRingColorForBand(WeekRingCountBand.One, colors),
        ),
        WeekRingLegendItem(
            label = AppCopy.Statistics.weeklyLegendZero,
            color = weekRingSegmentColor(WeekRingState.ExplicitZero, 0f, colors),
            markerStyle = weekRingLegendMarkerStyle(WeekRingState.ExplicitZero),
        ),
        WeekRingLegendItem(
            label = AppCopy.Statistics.weeklyLegendUnrecorded,
            color = weekRingSegmentColor(WeekRingState.Unrecorded, 0f, colors),
            markerStyle = weekRingLegendMarkerStyle(WeekRingState.Unrecorded),
        ),
        WeekRingLegendItem(
            label = AppCopy.Statistics.weeklyLegendFuture,
            color = weekRingSegmentColor(WeekRingState.Future, 0f, colors),
        ),
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        legend.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .legendMarker(item),
                )
                Text(
                    text = item.label,
                    color = DailyRecordTextMuted,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

private data class WeekRingLegendItem(
    val label: String,
    val color: Color,
    val markerStyle: WeekRingLegendMarkerStyle = WeekRingLegendMarkerStyle.Filled,
)

private fun Modifier.legendMarker(item: WeekRingLegendItem): Modifier = when (item.markerStyle) {
    WeekRingLegendMarkerStyle.Filled -> background(item.color, CircleShape)
    WeekRingLegendMarkerStyle.HollowPrimary,
    WeekRingLegendMarkerStyle.HollowNeutral -> background(DailyRecordSurface, CircleShape)
        .border(
            width = 1.5.dp,
            color = item.color,
            shape = CircleShape,
        )
}

@Composable
private fun DistributionSurface(
    title: String,
    subtitle: String,
    modifier: Modifier,
    colors: RecordModuleColorTokens,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .dailyRecordGlass(
                shape = RoundedCornerShape(20.dp),
                moduleColors = colors,
                level = DailyRecordGlassLevel.Base,
            ),
        color = Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        border = null,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, color = DailyRecordText, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = DailyRecordTextMuted, style = MaterialTheme.typography.labelSmall)
            }
            content()
        }
    }
}

private fun weekDistributionDescription(detail: StatisticsDetail): String = when {
    detail.future -> "${detail.label}，${AppCopy.Statistics.future}，${AppCopy.Statistics.dash}"
    !detail.recorded -> "${detail.label}，${AppCopy.Statistics.unset}，${AppCopy.Statistics.dash}"
    else -> {
        val count = detail.count ?: 0L
        "${detail.label}，${AppCopy.Statistics.countText(count)}，" +
            AppCopy.Statistics.daysText(detail.days ?: 0)
    }
}
