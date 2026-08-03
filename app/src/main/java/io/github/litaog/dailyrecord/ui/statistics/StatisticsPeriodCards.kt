package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceMuted
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.DailyRecordGlassLevel
import io.github.litaog.dailyrecord.ui.theme.dailyRecordGlass
import io.github.litaog.dailyrecord.core.common.AppCopy

@Composable
internal fun WeekDistributionCard(
    details: List<StatisticsDetail>,
    modifier: Modifier = Modifier,
    colors: RecordModuleColorTokens = HandBrewColorTokens,
) {
    val maxCount = details.mapNotNull { it.count }.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    DistributionSurface(
        title = AppCopy.Statistics.dailyDistribution,
        subtitle = AppCopy.Statistics.times,
        modifier = modifier.testTag("week_distribution_card"),
        colors = colors,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            details.forEach { detail ->
                val display = detail.displayValues()
                val fraction = distributionFraction(detail, maxCount, minNonZeroFraction = .16f)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "${detail.label}，${display.count}，${display.days}"
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = display.chartValue,
                        color = if (detail.future) DailyRecordTextMuted else DailyRecordTextSecondary,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(DailyRecordSurfaceMuted),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (fraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction)
                                    .background(colors.primary),
                            )
                        }
                    }
                    Text(
                        text = detail.label.substringBefore(" ").removePrefix("周"),
                        color = if (detail.future) DailyRecordTextMuted else DailyRecordText,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = detail.label.substringAfter(" "),
                        color = DailyRecordTextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
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
        color = androidx.compose.ui.graphics.Color.Transparent,
        shape = RoundedCornerShape(20.dp),
        border = null,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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

private data class DetailDisplay(
    val count: String,
    val days: String,
    val chartValue: String,
)

internal fun distributionFraction(
    detail: StatisticsDetail,
    maxCount: Long,
    minNonZeroFraction: Float,
): Float {
    val count = detail.count ?: return 0f
    if (detail.future || !detail.recorded || count <= 0L) return 0f
    if (maxCount <= 0L) return 0f
    return (count.toDouble() / maxCount.toDouble())
        .toFloat()
        .coerceIn(minNonZeroFraction, 1f)
}

private fun StatisticsDetail.displayValues(): DetailDisplay = when {
    future -> DetailDisplay(AppCopy.Statistics.future, AppCopy.Statistics.dash, AppCopy.Statistics.future)
    !recorded -> DetailDisplay(AppCopy.Statistics.unset, AppCopy.Statistics.dash, AppCopy.Statistics.unsetShort)
    else -> {
        val safeCount = count ?: 0L
        DetailDisplay(
            AppCopy.Statistics.countText(safeCount),
            AppCopy.Statistics.daysText(days ?: 0),
            safeCount.toString(),
        )
    }
}
