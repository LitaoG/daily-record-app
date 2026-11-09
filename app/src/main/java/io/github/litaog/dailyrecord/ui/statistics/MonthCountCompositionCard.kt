package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.statistics.MonthStatistics
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens

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
        colors = colors,
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
