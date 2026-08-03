package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.MetricNumberLarge
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlass
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
        modifier = modifier
            .fillMaxWidth()
            .dailyRecordGlass(
                shape = RoundedCornerShape(18.dp),
                moduleColors = colors,
                level = DailyRecordGlassLevel.Elevated,
            ),
        color = androidx.compose.ui.graphics.Color.Transparent,
        shape = RoundedCornerShape(18.dp),
        border = null,
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
    StatisticsSurface(
        modifier = modifier,
        title = AppCopy.Statistics.quarterShare,
        subtitle = if (total == 0L) AppCopy.Statistics.noPositiveCount else AppCopy.Statistics.byCount,
        colors = colors,
    ) {
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
                        .size(104.dp)
                        .semantics { contentDescription = quarterSummaryDescription(year, total) },
                ) {
                    // Separators only make sense when the ring contains more than
                    // one non-zero segment. A single 100% quarter must close the
                    // circle instead of leaving a misleading white seam.
                    val gapDegrees = quarterShareGapDegrees(
                        positiveQuarterCount = year.quarters.count { it.totalCount > 0L },
                    )
                    var startAngle = -90f
                    year.quarters.forEachIndexed { index, quarter ->
                        if (quarter.totalCount > 0L) {
                            val sweep = 360f * quarter.totalCount.toFloat() / total.toFloat()
                            drawArc(
                                color = quarterColors[index],
                                startAngle = startAngle + gapDegrees / 2f,
                                sweepAngle = (sweep - gapDegrees).coerceAtLeast(0f),
                                useCenter = false,
                                style = Stroke(width = 21.dp.toPx(), cap = StrokeCap.Butt),
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

internal fun quarterShareGapDegrees(positiveQuarterCount: Int): Float =
    if (positiveQuarterCount > 1) 3f else 0f

@Composable
internal fun ExtremesCard(
    year: YearStatistics,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    StatisticsSurface(
        modifier = modifier,
        title = AppCopy.Statistics.monthSummary,
        subtitle = AppCopy.Statistics.fullMonths,
        colors = colors,
    ) {
        when {
            year.maximumMonths.isEmpty() && year.minimumMonths.isEmpty() -> {
                Text(
                    AppCopy.Statistics.monthExtremesHint,
                    color = DailyRecordTextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            else -> {
                if (year.maximumMonths.isNotEmpty()) {
                    ExtremesRow(AppCopy.Statistics.maximumMonth, year.maximumMonths, colors.primary)
                }
                if (year.maximumMonths.isNotEmpty() && year.minimumMonths.isNotEmpty()) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(DailyRecordDivider))
                }
                if (year.minimumMonths.isNotEmpty()) {
                    ExtremesRow(AppCopy.Statistics.minimumMonth, year.minimumMonths, colors.primary)
                }
            }
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(label, color = DailyRecordTextSecondary, style = MaterialTheme.typography.labelMedium)
            Text(
                months.joinToString("、") { AppCopy.Statistics.monthLabel(it.month.monthValue) },
                color = accent,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Text(
            AppCopy.Statistics.countText(months.first().count ?: 0L),
            color = DailyRecordText,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
internal fun StatisticsSurface(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    colors: RecordModuleColorTokens? = null,
    content: @Composable () -> Unit,
) {
    val useStackedHeader = LocalConfiguration.current.fontScale >= 1.6f
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .dailyRecordGlass(
                shape = RoundedCornerShape(18.dp),
                moduleColors = colors,
                level = DailyRecordGlassLevel.Base,
            ),
        color = androidx.compose.ui.graphics.Color.Transparent,
        shape = RoundedCornerShape(18.dp),
        border = null,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (useStackedHeader) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, color = DailyRecordText, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, color = DailyRecordTextMuted, style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(title, color = DailyRecordText, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, color = DailyRecordTextMuted, style = MaterialTheme.typography.labelSmall)
                }
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
