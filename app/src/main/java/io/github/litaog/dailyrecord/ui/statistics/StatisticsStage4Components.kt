package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.MetricNumberLarge
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.core.common.AppCopy

@Composable
internal fun StatisticsSummaryCard(
    periodLabel: String,
    moduleLabel: String,
    totalCount: Long,
    recordedDays: Int,
    average: Double,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DailyRecordSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, DailyRecordDivider),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = AppCopy.Statistics.periodCountLabel(periodLabel, moduleLabel),
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = totalCount.toString(),
                    color = colors.primary,
                    style = MetricNumberLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = AppCopy.Statistics.countUnit,
                    color = DailyRecordTextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 5.dp, bottom = 7.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryFact(AppCopy.Statistics.recordedDaysLabel, AppCopy.Statistics.daysText(recordedDays), Modifier.weight(1f))
                SummaryFact(
                    AppCopy.Statistics.averageLabel,
                    AppCopy.Statistics.average(average),
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryFact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = DailyRecordTextMuted, style = MaterialTheme.typography.labelSmall)
        Text(
            value,
            color = DailyRecordText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun MonthWeeklyAnalysisCard(
    month: MonthStatistics,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    val maxCount = month.peakCount?.coerceAtLeast(1L) ?: 1L
    val weeksDescription = month.weeks.joinToString("，") { week ->
        "${week.label} ${AppCopy.Statistics.weekAccessibilityCount(week.count, week.future, week.recorded)}，" +
            AppCopy.Statistics.weekDays(week.recordedDays, week.future, week.recorded)
    }
    StatisticsSurface(
        modifier = modifier.testTag("month_distribution_card"),
        title = AppCopy.Statistics.monthWeeklyAnalysis,
        subtitle = AppCopy.Statistics.monthWeeklySubtitle,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = AppCopy.Statistics.monthWeeklyAccessibility(weeksDescription)
                },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            month.weeks.forEach { week ->
                val fraction = distributionFraction(
                    detail = week.asDetail(),
                    maxCount = maxCount,
                    minNonZeroFraction = .12f,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {
                            contentDescription =
                                "${week.label}，${AppCopy.Statistics.weekAccessibilityCount(week.count, week.future, week.recorded)}，" +
                                    AppCopy.Statistics.weekDays(week.recordedDays, week.future, week.recorded)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = AppCopy.Statistics.weekCount(week.count, week.future, week.recorded),
                        color = if (week.future) DailyRecordTextMuted else DailyRecordTextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(112.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(DailyRecordSurfaceMuted),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (fraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp))
                                    .background(colors.primary),
                            )
                        }
                    }
                    Text(
                        text = AppCopy.Statistics.weekLabel(week.index),
                        color = if (week.future) DailyRecordTextMuted else DailyRecordText,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                    Text(
                        text = AppCopy.Statistics.weekDays(week.recordedDays, week.future, week.recorded),
                        color = DailyRecordTextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SummaryFact(
                label = AppCopy.Statistics.activeWeeks,
                value = AppCopy.Statistics.activeWeeksText(month.activeWeekCount),
                modifier = Modifier.weight(1f),
            )
            SummaryFact(
                label = AppCopy.Statistics.peakWeek,
                value = AppCopy.Statistics.peakWeekText(
                    weeks = month.peakWeeks.joinToString("、") { AppCopy.Statistics.weekLabel(it.index) },
                    count = month.peakCount,
                ),
                modifier = Modifier.weight(1.4f),
            )
        }
        Text(
            text = AppCopy.Statistics.monthWeeklyHint,
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private fun MonthWeekStatistics.asDetail(): StatisticsDetail = StatisticsDetail(
    label = label,
    count = count,
    days = recordedDays,
    future = future,
    recorded = recorded,
)

@Composable
internal fun YearBarChartCard(
    year: YearStatistics,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    val maxCount = year.months.mapNotNull { it.count }.maxOrNull() ?: 0L
    StatisticsSurface(
        modifier = modifier,
        title = AppCopy.Statistics.annualCount,
        subtitle = AppCopy.Statistics.annualAverage(year.monthlyAverage),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = yearBarChartDescription(year) },
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            year.months.forEach { month ->
                val fraction = if (maxCount == 0L || month.count == null) 0f else {
                    (month.count.toDouble() / maxCount.toDouble()).toFloat()
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = when {
                            !month.recorded || month.future -> ""
                            else -> (month.count ?: 0L).toString()
                        },
                        color = if (month.inProgress) colors.primary.copy(alpha = .72f) else colors.primary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        modifier = Modifier
                            .height(142.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(5.dp))
                            .background(DailyRecordSurfaceMuted),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (fraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(
                                        colors.primary.copy(alpha = if (month.inProgress) .65f else 1f),
                                    ),
                            )
                        }
                    }
                    Text(
                        text = AppCopy.Statistics.monthLabel(month.month.monthValue),
                        color = if (month.future) DailyRecordTextMuted else DailyRecordText,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Text(
            text = AppCopy.Statistics.blankBarHint,
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
internal fun QuarterShareCard(
    year: YearStatistics,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    val total = year.quarters.sumOf { it.totalCount }
    val quarterColors = remember(colors) {
        listOf(
            colors.primary,
            colors.primary.copy(alpha = .78f),
            colors.medium,
            colors.soft,
        )
    }
    StatisticsSurface(modifier = modifier, title = AppCopy.Statistics.quarterShare, subtitle = if (total == 0L) AppCopy.Statistics.noPositiveCount else AppCopy.Statistics.byCount) {
        if (total == 0L) {
            Text(AppCopy.Statistics.quarterShareHint, color = DailyRecordTextSecondary, style = MaterialTheme.typography.bodyMedium)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Canvas(
                    modifier = Modifier
                        .size(118.dp)
                        .semantics { contentDescription = quarterSummaryDescription(year, total) },
                ) {
                    var startAngle = -90f
                    year.quarters.forEachIndexed { index, quarter ->
                        if (quarter.totalCount > 0L) {
                            val sweep = 360f * quarter.totalCount.toFloat() / total.toFloat()
                            drawArc(
                                color = quarterColors[index],
                                startAngle = startAngle + 1.5f,
                                sweepAngle = (sweep - 3f).coerceAtLeast(0f),
                                useCenter = false,
                                style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt),
                            )
                            startAngle += sweep
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    year.quarters.forEachIndexed { index, quarter ->
                        val percentage = quarter.totalCount * 100.0 / total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(9.dp).clip(CircleShape).background(quarterColors[index]),
                            )
                            Text(AppCopy.Statistics.quarterLabel(quarter.quarter), color = DailyRecordText, style = MaterialTheme.typography.labelMedium)
                            Text(
                                AppCopy.Statistics.percentage(percentage),
                                color = DailyRecordTextSecondary,
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ExtremesCard(
    year: YearStatistics,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    StatisticsSurface(modifier = modifier, title = AppCopy.Statistics.monthSummary, subtitle = AppCopy.Statistics.fullMonths) {
        if (year.maximumMonths.isEmpty() || year.minimumMonths.isEmpty()) {
            Text(
                AppCopy.Statistics.monthExtremesHint,
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            ExtremesRow(AppCopy.Statistics.maximumMonth, year.maximumMonths, colors.primary)
            Box(Modifier.fillMaxWidth().height(1.dp).background(DailyRecordDivider))
            ExtremesRow(AppCopy.Statistics.minimumMonth, year.minimumMonths, colors.primary)
        }
    }
}

@Composable
private fun ExtremesRow(
    label: String,
    months: List<YearMonthStatistics>,
    accent: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = DailyRecordTextSecondary, style = MaterialTheme.typography.labelMedium)
        Text(
            months.joinToString("、") { AppCopy.Statistics.monthLabel(it.month.monthValue) },
            color = accent,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            AppCopy.Statistics.countText(months.first().count ?: 0L),
            color = DailyRecordText,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun StatisticsSurface(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = DailyRecordSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, DailyRecordDivider),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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

private fun quarterSummaryDescription(year: YearStatistics, total: Long): String =
    AppCopy.Statistics.totalCountAccessibility(total, year.quarters.joinToString("，") { quarter ->
        val percentage = quarter.totalCount * 100.0 / total
        "${AppCopy.Statistics.quarterLabel(quarter.quarter)} ${AppCopy.Statistics.percentage(percentage)}"
    })

private fun yearBarChartDescription(year: YearStatistics): String =
    AppCopy.Statistics.annualChartAccessibility(
        year.months.joinToString("，") { month ->
            AppCopy.Statistics.monthChartLabel(
                month = month.month.monthValue,
                isFuture = month.future,
                recorded = month.recorded,
                count = month.count,
            )
        },
    )
