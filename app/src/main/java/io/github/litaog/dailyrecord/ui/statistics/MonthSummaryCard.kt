package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.MetricNumberMedium
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlass

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
            .dailyRecordGlass(
                shape = DailyRecordShapes.Large,
                moduleColors = colors,
                level = DailyRecordGlassLevel.Elevated,
            )
            .semantics {
                contentDescription = AppCopy.Statistics.monthSummaryAccessibility(
                    totalCount = totalCount,
                    recordedDays = recordedDays,
                    average = average,
                )
            },
        color = androidx.compose.ui.graphics.Color.Transparent,
        shape = DailyRecordShapes.Large,
        border = null,
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
