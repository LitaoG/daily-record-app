package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.statistics.StatisticsDetail
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.DailyRecordShapes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlass

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
                shape = DailyRecordShapes.Large,
                moduleColors = colors,
                level = DailyRecordGlassLevel.Base,
            ),
        color = Color.Transparent,
        shape = DailyRecordShapes.Large,
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

internal fun weekDistributionDescription(detail: StatisticsDetail): String = when {
    detail.future -> AppCopy.Components.joinSemantics(
        detail.label,
        AppCopy.Statistics.future,
        AppCopy.Statistics.dash,
    )
    !detail.recorded -> AppCopy.Components.joinSemantics(
        detail.label,
        AppCopy.Statistics.unset,
        AppCopy.Statistics.dash,
    )
    else -> {
        val count = detail.count ?: 0L
        AppCopy.Components.joinSemantics(
            detail.label,
            AppCopy.Statistics.countText(count),
            AppCopy.Statistics.daysText(detail.days ?: 0),
        )
    }
}
