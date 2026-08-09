package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
// The reference layout puts Monday in the upper-left gap and Tuesday at 12
// o'clock. Labels are gap anchors; each day's arc is the segment immediately
// before that day's label.
private const val WEEK_LABEL_START_DEGREES = -51.42857f
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
    weekRingLabelAngleDegrees(index) -
        weekSegmentSizeDegrees() / 2f +
        WEEK_CANVAS_ANGLE_OFFSET_DEGREES

internal fun weekRingLabelRadialDistance(
    angleDegrees: Float,
    ringOuterRadius: Float,
    labelHalfWidth: Float,
    labelHalfHeight: Float,
    gap: Float,
): Float {
    val angle = Math.toRadians(angleDegrees.toDouble())
    val horizontalSupport = labelHalfWidth * abs(sin(angle).toFloat())
    val verticalSupport = labelHalfHeight * abs(cos(angle).toFloat())
    return ringOuterRadius + horizontalSupport + verticalSupport + gap
}

@Composable
internal fun WeekDistributionCard(
    details: List<StatisticsDetail>,
    modifier: Modifier = Modifier,
    colors: RecordModuleColorTokens = HandBrewColorTokens,
) {
    val recordedDays = details.count { it.recorded && !it.future }

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
    val chartHeight = 292.dp
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(chartHeight)
            .testTag("week_distribution_chart"),
    ) {
        val centerY = chartHeight / 2
        val labelWidth = 68.dp
        val labelHeight = 44.dp
        val labelGap = 8.dp
        val radius = minOf(maxWidth * .28f, 84.dp)
        val centerX = maxWidth / 2
        val positiveStroke = 16.dp
        val ringOuterRadius = radius + positiveStroke / 2

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(x = size.width / 2f, y = centerY.toPx())
            val radiusPx = radius.toPx()
            val segmentSweep = 360f / WEEK_SEGMENT_COUNT - WEEK_SEGMENT_GAP_DEGREES

            details.take(WEEK_SEGMENT_COUNT).forEachIndexed { index, detail ->
                val startAngle = weekRingCanvasMidpointDegrees(index) -
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
                            color = DailyRecordDivider.copy(alpha = .78f),
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
            val angleDegrees = weekRingLabelAngleDegrees(index)
            val angle = Math.toRadians(angleDegrees.toDouble())
            val angleSin = abs(sin(angle).toFloat())
            val angleCos = abs(cos(angle).toFloat())
            val desiredRadius = weekRingLabelRadialDistance(
                angleDegrees = angleDegrees,
                ringOuterRadius = ringOuterRadius.value,
                labelHalfWidth = (labelWidth / 2).value,
                labelHalfHeight = (labelHeight / 2).value,
                gap = labelGap.value +
                    if (index == 3) 8.dp.value else 0.dp.value,
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
        AppCopy.Statistics.weeklyLegendFourPlus to
            weekRingColorForBand(WeekRingCountBand.FourPlus, colors),
        AppCopy.Statistics.weeklyLegendThree to
            weekRingColorForBand(WeekRingCountBand.Three, colors),
        AppCopy.Statistics.weeklyLegendTwo to
            weekRingColorForBand(WeekRingCountBand.Two, colors),
        AppCopy.Statistics.weeklyLegendOne to
            weekRingColorForBand(WeekRingCountBand.One, colors),
        AppCopy.Statistics.weeklyLegendZero to DailyRecordDivider,
        AppCopy.Statistics.weeklyLegendFuture to colors.soft.copy(alpha = .78f),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        legend.forEachIndexed { index, (label, color) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .then(
                            if (index == 4) {
                                Modifier
                                    .background(DailyRecordSurface, CircleShape)
                                    .border(
                                        width = 1.dp,
                                        color = DailyRecordDivider,
                                        shape = CircleShape,
                                    )
                            } else {
                                Modifier.background(color, CircleShape)
                            },
                        ),
                )
                Text(
                    text = label,
                    color = DailyRecordTextMuted,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    maxLines = 1,
                )
            }
        }
    }
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
