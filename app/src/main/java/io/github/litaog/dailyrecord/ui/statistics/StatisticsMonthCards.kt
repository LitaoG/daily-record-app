package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.MetricNumberMedium
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens

internal data class MonthDailyChartScale(
    val maximum: Long,
    val ticks: List<Long>,
)

internal fun monthDailyChartScale(month: MonthStatistics): MonthDailyChartScale {
    val maximumCount = month.days
        .asSequence()
        .filter { it.recorded && !it.future }
        .mapNotNull(MonthDayStatistics::count)
        .maxOrNull()
        ?.coerceAtLeast(1L) ?: 1L
    var step = 1L
    var phase = 0
    while ((maximumCount + step - 1L) / step > 4L) {
        step = when (phase) {
            0 -> step * 2L
            1 -> step * 5L / 2L
            else -> step * 2L
        }
        phase = (phase + 1) % 3
    }
    val maximum = ((maximumCount + step - 1L) / step) * step
    return MonthDailyChartScale(
        maximum = maximum,
        ticks = (0L..maximum step step).toList(),
    )
}

@Composable
internal fun MonthSummaryCard(
    totalCount: Long,
    recordedDays: Int,
    average: Double,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    val metrics = listOf(
        MonthSummaryMetric(AppCopy.Statistics.monthTotalCount, totalCount.toString(), AppCopy.Statistics.countUnit),
        MonthSummaryMetric(AppCopy.Statistics.recordedDaysLabel, recordedDays.toString(), AppCopy.Statistics.dayUnit),
        MonthSummaryMetric(AppCopy.Statistics.averageLabel, AppCopy.Statistics.averageNumber(average), AppCopy.Statistics.perDayUnit),
    )
    val useStackedLayout = LocalDensity.current.fontScale >= 1.6f
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = AppCopy.Statistics.monthSummaryAccessibility(
                    totalCount = totalCount,
                    recordedDays = recordedDays,
                    average = average,
                )
            },
        color = DailyRecordSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, DailyRecordDivider),
    ) {
        if (useStackedLayout) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                metrics.forEach { metric ->
                    MonthSummaryMetricCell(metric = metric, accent = colors.primary)
                }
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                metrics.forEachIndexed { index, metric ->
                    MonthSummaryMetricCell(
                        metric = metric,
                        accent = colors.primary,
                        modifier = Modifier.weight(1f),
                        centered = true,
                    )
                    if (index < metrics.lastIndex) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(52.dp)
                                .background(DailyRecordDivider),
                        )
                    }
                }
            }
        }
    }
}

private data class MonthSummaryMetric(
    val label: String,
    val value: String,
    val unit: String,
)

@Composable
private fun MonthSummaryMetricCell(
    metric: MonthSummaryMetric,
    accent: Color,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = metric.label,
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.labelSmall,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = metric.value,
                color = accent,
                style = MetricNumberMedium,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                maxLines = 1,
            )
            Text(
                text = metric.unit,
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 3.dp, bottom = 3.dp),
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun MonthDailyCountCard(
    month: MonthStatistics,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    val scale = remember(month) { monthDailyChartScale(month) }
    val description = remember(month) {
        AppCopy.Statistics.monthDailyChartAccessibility(
            month.days.joinToString("，") { day ->
                AppCopy.Statistics.dayChartValue(
                    day = day.date.dayOfMonth,
                    count = day.count,
                    future = day.future,
                    recorded = day.recorded,
                )
            },
        )
    }
    StatisticsSurface(
        modifier = modifier.testTag("month_daily_count_card"),
        title = AppCopy.Statistics.dailyCount,
        subtitle = AppCopy.Statistics.byDate,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(174.dp)
                .semantics { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MonthDailyChartAxis(scale)
            MonthDailyChartPlot(
                month = month,
                scale = scale,
                colors = colors,
                modifier = Modifier.weight(1f),
            )
        }
        MonthDailyXAxis(dayCount = month.days.size)
    }
}

@Composable
private fun MonthDailyChartAxis(scale: MonthDailyChartScale) {
    Box(
        modifier = Modifier
            .width(24.dp)
            .height(174.dp),
    ) {
        val density = LocalDensity.current
        val plotTopPx = with(density) { 24.dp.toPx() }
        val plotBottomPx = with(density) { 154.dp.toPx() }
        scale.ticks.forEach { tick ->
            val fraction = tick.toFloat() / scale.maximum.toFloat()
            val y = plotBottomPx - (plotBottomPx - plotTopPx) * fraction
            Text(
                text = tick.toString(),
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .offset(y = (with(density) { y.toDp() } - 6.dp).coerceAtLeast(0.dp))
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MonthDailyChartPlot(
    month: MonthStatistics,
    scale: MonthDailyChartScale,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .height(174.dp)
            .testTag("month_daily_count_chart"),
    ) {
        val density = LocalDensity.current
        val plotTop = 24.dp
        val plotBottom = 154.dp
        val horizontalInset = 5.dp
        val plotTopPx = with(density) { plotTop.toPx() }
        val plotBottomPx = with(density) { plotBottom.toPx() }
        val horizontalInsetPx = with(density) { horizontalInset.toPx() }
        val plotWidthPx = (constraints.maxWidth.toFloat() - horizontalInsetPx * 2f).coerceAtLeast(0f)
        val slotWidth = maxWidth / month.days.size
        val offsets = month.days.mapIndexed { index, day ->
            val xFraction = if (month.days.size == 1) 0f else index.toFloat() / (month.days.size - 1).toFloat()
            val count = day.count?.takeIf { day.recorded && !day.future }
            Offset(
                x = horizontalInsetPx + plotWidthPx * xFraction,
                y = count?.let { value ->
                    val fraction = (value.toFloat() / scale.maximum.toFloat()).coerceIn(0f, 1f)
                    plotBottomPx - (plotBottomPx - plotTopPx) * fraction
                } ?: plotBottomPx,
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
            scale.ticks.forEach { tick ->
                val fraction = tick.toFloat() / scale.maximum.toFloat()
                val y = plotBottomPx - (plotBottomPx - plotTopPx) * fraction
                drawLine(
                    color = DailyRecordDivider.copy(alpha = if (tick == 0L) .62f else .38f),
                    start = Offset(horizontalInsetPx, y),
                    end = Offset(size.width - horizontalInsetPx, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dash,
                )
            }
            month.days.forEachIndexed { index, day ->
                val offset = offsets[index]
                when {
                    day.future -> drawCircle(
                        color = DailyRecordDivider.copy(alpha = .32f),
                        radius = 2.35.dp.toPx(),
                        center = Offset(offset.x, plotBottomPx),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                    !day.recorded -> drawCircle(
                        color = DailyRecordDivider.copy(alpha = .82f),
                        radius = 2.35.dp.toPx(),
                        center = Offset(offset.x, plotBottomPx),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                    day.count == 0L -> {
                        drawCircle(
                            color = colors.primary.copy(alpha = .16f),
                            radius = 5.dp.toPx(),
                            center = Offset(offset.x, plotBottomPx),
                        )
                        drawCircle(
                            color = colors.primary,
                            radius = 2.6.dp.toPx(),
                            center = Offset(offset.x, plotBottomPx),
                        )
                    }
                    else -> {
                        drawLine(
                            color = colors.primary,
                            start = Offset(offset.x, plotBottomPx),
                            end = offset,
                            strokeWidth = 1.6.dp.toPx(),
                            cap = StrokeCap.Round,
                        )
                        drawCircle(
                            color = colors.primary.copy(alpha = .15f),
                            radius = 6.dp.toPx(),
                            center = offset,
                        )
                        drawCircle(color = DailyRecordSurface, radius = 4.4.dp.toPx(), center = offset)
                        drawCircle(color = colors.primary, radius = 3.dp.toPx(), center = offset)
                    }
                }
            }
        }

        month.days.forEachIndexed { index, day ->
            val count = day.count?.takeIf { day.recorded && !day.future && it > 0L } ?: return@forEachIndexed
            val labelY = with(density) { offsets[index].y.toDp() } - 23.dp
            Box(
                modifier = Modifier
                    .offset(x = slotWidth * index, y = labelY.coerceAtLeast(0.dp))
                    .width(slotWidth)
                    .height(18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = count.toString(),
                    color = colors.primary,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun MonthDailyXAxis(dayCount: Int) {
    val tickDays = listOf(1, 5, 10, 15, 20, 25, dayCount).distinct()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp)
            .height(18.dp),
    ) {
        val labelWidth = 24.dp
        val plotInset = 5.dp
        val availableWidth = (maxWidth - plotInset * 2).coerceAtLeast(0.dp)
        tickDays.forEach { day ->
            val fraction = if (dayCount == 1) 0f else (day - 1).toFloat() / (dayCount - 1).toFloat()
            val center = plotInset + availableWidth * fraction
            val x = (center - labelWidth / 2).coerceIn(0.dp, (maxWidth - labelWidth).coerceAtLeast(0.dp))
            Text(
                text = day.toString(),
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.offset(x = x).width(labelWidth),
            )
        }
    }
}

@Composable
internal fun MonthCountCompositionCard(
    month: MonthStatistics,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    val segments = listOf(
        MonthCompositionSegment(AppCopy.Statistics.explicitZero, month.explicitZeroDays, colors.soft.copy(alpha = .42f), colors.strong, true),
        MonthCompositionSegment(AppCopy.Statistics.once, month.oneCountDays, colors.soft, colors.strong),
        MonthCompositionSegment(AppCopy.Statistics.twice, month.twoCountDays, colors.medium, colors.strong),
        MonthCompositionSegment(AppCopy.Statistics.threePlus, month.threePlusCountDays, colors.strong, colors.onPrimary),
    )
    StatisticsSurface(
        modifier = modifier
            .testTag("month_composition_card")
            .semantics {
                contentDescription = AppCopy.Statistics.monthCompositionAccessibility(
                    savedDays = month.savedDays,
                    explicitZeroDays = month.explicitZeroDays,
                    oneCountDays = month.oneCountDays,
                    twoCountDays = month.twoCountDays,
                    threePlusCountDays = month.threePlusCountDays,
                    unfilledDays = month.unfilledElapsedDays,
                    futureDays = month.futureDays,
                )
            },
        title = AppCopy.Statistics.countComposition,
        subtitle = AppCopy.Statistics.savedDaysSubtitle(month.savedDays),
    ) {
        if (month.savedDays == 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(DailyRecordSurfaceMuted, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = AppCopy.Statistics.noSavedDays,
                    color = DailyRecordTextMuted,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(DailyRecordSurfaceMuted, RoundedCornerShape(12.dp))
                    .padding(1.dp),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                segments.filter { it.days > 0 }.forEach { segment ->
                    val percentage = segment.days * 100.0 / month.savedDays
                    Box(
                        modifier = Modifier
                            .weight(segment.days.toFloat())
                            .fillMaxHeight()
                            .background(segment.color, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = AppCopy.Statistics.percentage(percentage),
                            color = segment.contentColor,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            segments.forEach { segment ->
                MonthCompositionLegendItem(
                    segment = segment,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            MonthRemainderFact(
                label = AppCopy.Statistics.unfilledDays,
                days = month.unfilledElapsedDays,
                modifier = Modifier.weight(1f),
            )
            if (month.futureDays > 0) {
                MonthRemainderFact(
                    label = AppCopy.Statistics.futureDays,
                    days = month.futureDays,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private data class MonthCompositionSegment(
    val label: String,
    val days: Int,
    val color: Color,
    val contentColor: Color,
    val outlined: Boolean = false,
)

@Composable
private fun MonthCompositionLegendItem(
    segment: MonthCompositionSegment,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(segment.color, RoundedCornerShape(3.dp))
                    .then(
                        if (segment.outlined) {
                            Modifier.border(1.dp, segment.contentColor.copy(alpha = .55f), RoundedCornerShape(3.dp))
                        } else {
                            Modifier
                        },
                    ),
            )
            Text(
                text = segment.label,
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
        Text(
            text = AppCopy.Statistics.categoryDays(segment.days),
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun MonthRemainderFact(
    label: String,
    days: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            text = AppCopy.Statistics.categoryDays(days),
            color = DailyRecordTextSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}

@Composable
internal fun MonthDayExtremesCard(
    month: MonthStatistics,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    StatisticsSurface(
        modifier = modifier.testTag("month_extremes_card"),
        title = AppCopy.Statistics.singleDayExtremes,
        subtitle = AppCopy.Statistics.byPositiveCount,
    ) {
        if (month.maximum == null || month.minimumPositive == null) {
            Text(
                text = AppCopy.Statistics.noPositiveDay,
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            MonthExtremeRow(
                label = AppCopy.Statistics.maximumDay,
                extreme = month.maximum,
                colors = colors,
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(DailyRecordDivider))
            MonthExtremeRow(
                label = AppCopy.Statistics.minimumPositiveDay,
                extreme = month.minimumPositive,
                colors = colors,
            )
        }
        Text(
            text = AppCopy.Statistics.monthDayExtremesHint,
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun MonthExtremeRow(
    label: String,
    extreme: MonthDayExtreme,
    colors: RecordModuleColorTokens,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = AppCopy.Statistics.monthExtremeAccessibility(
                    label = label,
                    count = extreme.count,
                    dates = extreme.dates,
                )
            },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = label,
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = AppCopy.Statistics.countText(extreme.count),
                color = colors.primary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            text = AppCopy.Statistics.dayList(extreme.dates),
            color = DailyRecordText,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
