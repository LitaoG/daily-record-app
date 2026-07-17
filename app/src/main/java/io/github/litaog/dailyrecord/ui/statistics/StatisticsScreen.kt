package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.ui.components.MetricCard
import io.github.litaog.dailyrecord.ui.components.PeriodTabs
import io.github.litaog.dailyrecord.ui.components.StatisticRow
import io.github.litaog.dailyrecord.ui.components.StatisticsPeriod
import io.github.litaog.dailyrecord.ui.theme.Ink500
import io.github.litaog.dailyrecord.ui.theme.Ink700
import io.github.litaog.dailyrecord.ui.theme.Ink900
import io.github.litaog.dailyrecord.ui.theme.Neutral300
import io.github.litaog.dailyrecord.ui.theme.Paper0
import io.github.litaog.dailyrecord.ui.theme.Paper100
import io.github.litaog.dailyrecord.ui.theme.Terracotta500
import java.time.LocalDate
import java.util.Locale

@Composable
fun StatisticsScreen(
    today: LocalDate,
    records: List<HandBrewRecord>,
    modifier: Modifier = Modifier,
) {
    var periodName by rememberSaveable { mutableStateOf(StatisticsPeriod.Week.name) }
    val period = StatisticsPeriod.valueOf(periodName)
    val model = remember(period, today, records) { buildStatistics(period, today, records) }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("statistics_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 15.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("统计", color = Ink900, style = MaterialTheme.typography.headlineLarge)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Paper0)
                        .border(1.dp, Neutral300, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text("手冲", color = Terracotta500, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        item {
            PeriodTabs(
                selected = period,
                onSelected = { periodName = it.name },
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(model.title, color = Ink900, style = MaterialTheme.typography.labelLarge)
                Text(model.status, color = Terracotta500, style = MaterialTheme.typography.labelMedium)
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                MetricCard("手冲总次数", model.summary.totalCount.toString(), "次", Modifier.weight(1f))
                MetricCard("手冲天数", model.summary.brewDays.toString(), "天", Modifier.weight(1f))
                MetricCard(
                    "记录日均",
                    String.format(Locale.US, "%.1f", model.summary.average),
                    "次/天",
                    Modifier.weight(1f),
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(model.detailsTitle, color = Ink900, style = MaterialTheme.typography.labelMedium)
                Text("次数 · 天数", color = Ink500, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (model.details.isEmpty()) {
            item { EmptyStatistics() }
        } else {
            items(model.details, key = { it.label }) { detail ->
                StatisticRow(
                    label = detail.label,
                    countText = detail.count?.let { "$it 次" } ?: "—",
                    daysText = detail.days?.let { "$it 天" } ?: "—",
                    future = detail.future,
                )
            }
        }
        if (period == StatisticsPeriod.All && records.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Paper0)
                        .border(1.dp, Terracotta500, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("历史事实", color = Ink900, style = MaterialTheme.typography.labelLarge)
                    Text(
                        "首次记录：" + records.minOf { it.localDate },
                        color = Ink700,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        "只展示已发生数据，不预测未来趋势",
                        color = Ink500,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStatistics() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Paper100)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "还没有可统计的手冲记录",
            color = Ink900,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "回到日历选择日期，保存第一条记录。",
            color = Ink700,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
