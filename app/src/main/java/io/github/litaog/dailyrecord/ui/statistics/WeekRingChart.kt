package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import io.github.litaog.dailyrecord.core.statistics.StatisticsDetail
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun WeekRingChart(
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
        val positiveStroke = weekRingStrokeWidthDp(WeekRingState.Positive).dp
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
                            style = Stroke(
                                width = weekRingStrokeWidthDp(WeekRingState.ExplicitZero).dp.toPx(),
                                cap = StrokeCap.Round,
                            ),
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
                        color = weekRingSegmentColor(WeekRingState.Future, 0f, colors),
                        startAngle = startAngle,
                        sweepAngle = segmentSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radiusPx, center.y - radiusPx),
                        size = androidx.compose.ui.geometry.Size(radiusPx * 2f, radiusPx * 2f),
                        style = Stroke(
                            width = weekRingStrokeWidthDp(WeekRingState.Future).dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(
                                intervals = floatArrayOf(7.dp.toPx(), 6.dp.toPx()),
                            ),
                        ),
                    )
                    WeekRingState.Unrecorded -> drawArc(
                        // Must resolve through weekRingSegmentColor (same as
                        // the legend marker) so the ring can never drift from
                        // the legend: past-unfilled segments use the homepage
                        // unset fill, not the neutral divider grey.
                        color = weekRingSegmentColor(WeekRingState.Unrecorded, 0f, colors),
                        startAngle = startAngle,
                        sweepAngle = segmentSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radiusPx, center.y - radiusPx),
                        size = androidx.compose.ui.geometry.Size(radiusPx * 2f, radiusPx * 2f),
                        style = Stroke(
                            width = weekRingStrokeWidthDp(WeekRingState.Unrecorded).dp.toPx(),
                            cap = StrokeCap.Round,
                        ),
                    )
                }
            }
        }

        details.take(WEEK_SEGMENT_COUNT).forEachIndexed { index, detail ->
            val segmentIndex = weekRingSegmentIndex(detail, index)
            val angleDegrees = weekRingLabelAngleDegrees(segmentIndex)
            val angle = Math.toRadians(angleDegrees.toDouble())
            val angleCos = abs(cos(angle).toFloat())
            val desiredRadius = weekRingLabelRadialDistance(
                angleDegrees = angleDegrees,
                ringOuterRadius = ringOuterRadius.value,
                labelHalfWidth = (labelWidth / 2).value,
                labelHalfHeight = (labelHeight / 2).value,
                gap = weekRingLabelGapDp(segmentIndex, sharedLabelGap),
            ).dp
            // DistributionSurface keeps 16dp of horizontal padding around the
            // chart. Let the side labels use 6dp of that padding; otherwise
            // their requested extra gap is clamped back to the chart edge.
            val horizontalLimit = weekRingLabelHorizontalLimitDp(
                angleDegrees = angleDegrees,
                availableHalfWidth = (maxWidth / 2 - labelWidth / 2).value,
                boundaryAllowance = weekRingSideLabelExtraGapDp(segmentIndex),
            ).dp
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
            val labelColor = weekRingLabelColor(weekRingState(detail))
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
                val labelParts = weekRingLabelParts(detail)
                // The day label must carry a visible status word so the state
                // is not encoded by the ring style alone: unrecorded days show
                // the unset word, explicit zero its count text, future days the
                // future word, and positive counts keep the count suffix
                // (docs/STATISTICS.md).
                val statusLabel = when {
                    detail.future -> AppCopy.Statistics.future
                    !detail.recorded -> AppCopy.Statistics.unsetShort
                    detail.count == 0L -> AppCopy.Statistics.countText(0L)
                    else -> detail.count?.let(AppCopy.Statistics::weeklyCountSuffix)
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = labelParts.weekday,
                        color = labelColor,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                    if (statusLabel != null) {
                        Text(
                            text = statusLabel,
                            color = if (detail.recorded && (detail.count ?: 0) > 0) {
                                colors.primary
                            } else {
                                labelColor.copy(alpha = .72f)
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
                Text(
                    text = labelParts.day,
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
