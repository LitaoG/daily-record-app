package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.statistics.MonthDayExtreme
import io.github.litaog.dailyrecord.core.statistics.MonthStatistics
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens

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
        colors = colors,
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
    }
}

@Composable
private fun MonthExtremeRow(
    label: String,
    extreme: MonthDayExtreme,
    colors: RecordModuleColorTokens,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = AppCopy.Statistics.monthExtremeAccessibility(
                    label = label,
                    count = extreme.count,
                )
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
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
}
