package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.ui.components.StatisticsPeriod
import io.github.litaog.dailyrecord.ui.theme.Ink500
import io.github.litaog.dailyrecord.ui.theme.Ink700
import io.github.litaog.dailyrecord.ui.theme.Ink900
import io.github.litaog.dailyrecord.ui.theme.Neutral300
import io.github.litaog.dailyrecord.ui.theme.Paper0
import io.github.litaog.dailyrecord.ui.theme.Paper100
import io.github.litaog.dailyrecord.ui.theme.Terracotta500
import io.github.litaog.dailyrecord.ui.theme.White
import java.util.Locale

@Composable
internal fun CompactPeriodSummary(
    period: StatisticsPeriod,
    summary: StatisticsSummary,
    horizontal: Boolean,
    modifier: Modifier = Modifier,
) {
    val periodLabel = if (period == StatisticsPeriod.Week) "本周" else "本月"
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Terracotta500,
        shape = RoundedCornerShape(20.dp),
    ) {
        if (horizontal) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                PeriodTotal(periodLabel, summary, Modifier.weight(1.2f))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SummaryFact("手冲天数", "${summary.brewDays} 天")
                    SummaryFact(
                        "记录日均",
                        String.format(Locale.US, "%.1f 次/天", summary.average),
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PeriodTotal(periodLabel, summary, Modifier.fillMaxWidth())
                SummaryFact("手冲天数", "${summary.brewDays} 天")
                SummaryFact(
                    "记录日均",
                    String.format(Locale.US, "%.1f 次/天", summary.average),
                )
            }
        }
    }
}

@Composable
private fun PeriodTotal(
    periodLabel: String,
    summary: StatisticsSummary,
    modifier: Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "$periodLabel · 手冲次数",
            color = White,
            style = MaterialTheme.typography.labelMedium,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = summary.totalCount.toString(),
                color = White,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "次",
                color = White,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(start = 4.dp, bottom = 5.dp),
            )
        }
    }
}

@Composable
private fun SummaryFact(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = White, style = MaterialTheme.typography.labelSmall)
        Text(
            value,
            color = White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
internal fun WeekDistributionCard(
    details: List<StatisticsDetail>,
    modifier: Modifier = Modifier,
) {
    val maxCount = details.mapNotNull { it.count }.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    DistributionSurface(
        title = "每日分布",
        subtitle = "次数",
        modifier = modifier.testTag("week_distribution_card"),
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
                        color = if (detail.future) Ink500 else Ink700,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(76.dp)
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Paper100),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        if (fraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction)
                                    .background(Terracotta500),
                            )
                        }
                    }
                    Text(
                        text = detail.label.substringBefore(" ").removePrefix("周"),
                        color = if (detail.future) Ink500 else Ink900,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = detail.label.substringAfter(" "),
                        color = Ink500,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
internal fun MonthDistributionCard(
    details: List<StatisticsDetail>,
    modifier: Modifier = Modifier,
) {
    val maxCount = details.mapNotNull { it.count }.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    DistributionSurface(
        title = "每周分布",
        subtitle = "次数 · 天数",
        modifier = modifier.testTag("month_distribution_card"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            details.forEach { detail ->
                val display = detail.displayValues()
                val fraction = distributionFraction(detail, maxCount, minNonZeroFraction = .08f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            contentDescription = "${detail.label}，${display.count}，${display.days}"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = detail.label,
                        color = if (detail.future) Ink500 else Ink900,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1.15f),
                    )
                    Box(
                        modifier = Modifier
                            .weight(1.35f)
                            .height(12.dp)
                            .clip(CircleShape)
                            .background(Paper100),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (fraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(Terracotta500),
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.width(68.dp),
                        horizontalAlignment = Alignment.End,
                    ) {
                        Text(
                            display.count,
                            color = if (detail.future) Ink500 else Ink900,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                        Text(
                            display.days,
                            color = Ink500,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                        )
                    }
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
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Paper0,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Neutral300),
        shadowElevation = 2.dp,
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
                Text(title, color = Ink900, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, color = Ink500, style = MaterialTheme.typography.labelSmall)
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
    future -> DetailDisplay("未来", "—", "未来")
    !recorded -> DetailDisplay("未填写", "—", "未填")
    else -> DetailDisplay("${count ?: 0L} 次", "${days ?: 0} 天", (count ?: 0L).toString())
}
