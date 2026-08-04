package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import kotlin.math.cos
import kotlin.math.sin

private const val WEEK_SEGMENT_COUNT = 7
private const val WEEK_SEGMENT_GAP_DEGREES = 11f
private const val WEEK_SEGMENT_START_DEGREES = -90f

internal enum class WeekRingState {
    Future,
    Unrecorded,
    ExplicitZero,
    Positive,
}

internal fun weekRingState(detail: StatisticsDetail): WeekRingState = when {
    detail.future -> WeekRingState.Future
    !detail.recorded -> WeekRingState.Unrecorded
    (detail.count ?: 0L) <= 0L -> WeekRingState.ExplicitZero
    else -> WeekRingState.Positive
}

internal fun weekRingIntensity(detail: StatisticsDetail, maxCount: Long): Float {
    if (weekRingState(detail) != WeekRingState.Positive || maxCount <= 0L) return 0f
    return ((detail.count ?: 0L).toFloat() / maxCount.toFloat()).coerceIn(0f, 1f)
}

@Composable
internal fun WeekDistributionCard(
    details: List<StatisticsDetail>,
    modifier: Modifier = Modifier,
    colors: RecordModuleColorTokens = HandBrewColorTokens,
) {
    val maxCount = details
        .asSequence()
        .filter { weekRingState(it) == WeekRingState.Positive }
        .mapNotNull(StatisticsDetail::count)
        .maxOrNull()
        ?.coerceAtLeast(1L)
        ?: 1L
    val recordedDays = details.count { it.recorded && !it.future }

    DistributionSurface(
        title = AppCopy.Statistics.dailyDistribution,
        subtitle = AppCopy.Statistics.times,
        modifier = modifier.testTag("week_distribution_card"),
        colors = colors,
    ) {
        WeekRingChart(
            details = details,
            maxCount = maxCount,
            recordedDays = recordedDays,
            colors = colors,
        )
        WeekRingLegend(colors = colors)
    }
}

@Composable
private fun WeekRingChart(
    details: List<StatisticsDetail>,
    maxCount: Long,
    recordedDays: Int,
    colors: RecordModuleColorTokens,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(258.dp)
            .testTag("week_distribution_chart")
            .semantics { contentDescription = weekDistributionDescription(details) },
    ) {
        val centerY = 128.dp
        val labelSize = 78.dp
        val radius = minOf(maxWidth * .34f, 96.dp)
        val centerX = maxWidth / 2

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(x = size.width / 2f, y = centerY.toPx())
            val radiusPx = radius.toPx()
            val segmentSweep = 360f / WEEK_SEGMENT_COUNT - WEEK_SEGMENT_GAP_DEGREES

            details.take(WEEK_SEGMENT_COUNT).forEachIndexed { index, detail ->
                val startAngle = WEEK_SEGMENT_START_DEGREES +
                    index * 360f / WEEK_SEGMENT_COUNT + WEEK_SEGMENT_GAP_DEGREES / 2f
                val state = weekRingState(detail)
                val intensity = weekRingIntensity(detail, maxCount)
                val baseColor = when (state) {
                    WeekRingState.Future -> colors.soft.copy(alpha = .78f)
                    WeekRingState.Unrecorded -> DailyRecordDivider.copy(alpha = .92f)
                    WeekRingState.ExplicitZero -> colors.primary.copy(alpha = .82f)
                    WeekRingState.Positive -> colors.primary.copy(alpha = .18f)
                }
                val baseStroke = when (state) {
                    WeekRingState.Future -> 10.dp.toPx()
                    WeekRingState.Unrecorded -> 4.dp.toPx()
                    WeekRingState.ExplicitZero -> 5.dp.toPx()
                    WeekRingState.Positive -> 13.dp.toPx()
                }
                drawArc(
                    color = baseColor,
                    startAngle = startAngle,
                    sweepAngle = segmentSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radiusPx, center.y - radiusPx),
                    size = androidx.compose.ui.geometry.Size(radiusPx * 2f, radiusPx * 2f),
                    style = Stroke(
                        width = baseStroke,
                        cap = StrokeCap.Round,
                        pathEffect = if (state == WeekRingState.Future) {
                            PathEffect.dashPathEffect(
                                intervals = floatArrayOf(7.dp.toPx(), 6.dp.toPx()),
                            )
                        } else {
                            null
                        },
                    ),
                )
                when (state) {
                    WeekRingState.Positive -> {
                        drawArc(
                            color = colors.primary.copy(alpha = .58f + .38f * intensity),
                            startAngle = startAngle,
                            sweepAngle = segmentSweep,
                            useCenter = false,
                            topLeft = Offset(center.x - radiusPx, center.y - radiusPx),
                            size = androidx.compose.ui.geometry.Size(radiusPx * 2f, radiusPx * 2f),
                            style = Stroke(
                                width = baseStroke + 5.dp.toPx() * intensity,
                                cap = StrokeCap.Round,
                            ),
                        )
                    }
                    WeekRingState.ExplicitZero -> {
                        drawArc(
                            color = DailyRecordSurface,
                            startAngle = startAngle,
                            sweepAngle = segmentSweep,
                            useCenter = false,
                            topLeft = Offset(center.x - radiusPx, center.y - radiusPx),
                            size = androidx.compose.ui.geometry.Size(radiusPx * 2f, radiusPx * 2f),
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                    WeekRingState.Future,
                    WeekRingState.Unrecorded,
                    -> Unit
                }
            }
        }

        details.take(WEEK_SEGMENT_COUNT).forEachIndexed { index, detail ->
            val angle = Math.toRadians(
                (
                    WEEK_SEGMENT_START_DEGREES +
                        index * 360f / WEEK_SEGMENT_COUNT +
                        360f / WEEK_SEGMENT_COUNT / 2f
                    ).toDouble(),
            )
            val labelX = centerX - labelSize / 2 + radius * sin(angle).toFloat()
            val labelY = centerY - labelSize / 2 - radius * cos(angle).toFloat()
            val labelColor = when (weekRingState(detail)) {
                WeekRingState.Future -> colors.primary.copy(alpha = .45f)
                WeekRingState.Unrecorded -> DailyRecordTextMuted
                WeekRingState.ExplicitZero -> colors.primary.copy(alpha = .72f)
                WeekRingState.Positive -> DailyRecordText
            }
            Column(
                modifier = Modifier
                    .offset(x = labelX, y = labelY)
                    .width(labelSize),
                horizontalAlignment = Alignment.CenterHorizontally,
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
        AppCopy.Statistics.weeklyLegendHigh to colors.primary,
        AppCopy.Statistics.weeklyLegendMedium to colors.primary.copy(alpha = .68f),
        AppCopy.Statistics.weeklyLegendLow to colors.primary.copy(alpha = .40f),
        AppCopy.Statistics.weeklyLegendZero to DailyRecordDivider,
        AppCopy.Statistics.weeklyLegendFuture to colors.soft.copy(alpha = .78f),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        legend.forEach { (label, color) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape),
                )
                Text(
                    text = label,
                    color = DailyRecordTextMuted,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
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

private fun weekDistributionDescription(details: List<StatisticsDetail>): String =
    details.joinToString("，") { detail ->
        val state = when (weekRingState(detail)) {
            WeekRingState.Future -> AppCopy.Statistics.future
            WeekRingState.Unrecorded -> AppCopy.Statistics.unset
            WeekRingState.ExplicitZero -> AppCopy.Statistics.countText(0L)
            WeekRingState.Positive -> AppCopy.Statistics.countText(detail.count ?: 0L)
        }
        "${detail.label}，$state"
    }
