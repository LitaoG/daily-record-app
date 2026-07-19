package io.github.litaog.dailyrecord.ui.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.ui.components.ChevronIcon
import io.github.litaog.dailyrecord.ui.components.MetricCard
import io.github.litaog.dailyrecord.ui.components.PeriodTabs
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.components.StatisticRow
import io.github.litaog.dailyrecord.ui.components.StatisticsPeriod
import io.github.litaog.dailyrecord.ui.navigation.nextPeriodAnchor
import io.github.litaog.dailyrecord.ui.navigation.previousPeriodAnchor
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
    anchorDate: LocalDate,
    earliestDate: LocalDate,
    records: List<HandBrewRecord>,
    onAnchorDateChanged: (LocalDate) -> Unit,
    onOpenDatePicker: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenCalendar: () -> Unit = {},
) {
    var periodName by rememberSaveable { mutableStateOf(StatisticsPeriod.Week.name) }
    val period = StatisticsPeriod.entries.firstOrNull { it.name == periodName }
        ?: StatisticsPeriod.Week
    val model = remember(period, anchorDate, today, records) {
        buildStatistics(period, anchorDate, today, records)
    }
    val useHorizontalMetrics = LocalDensity.current.fontScale < 1.4f

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
            PeriodNavigator(
                period = period,
                model = model,
                anchorDate = anchorDate,
                earliestDate = earliestDate,
                today = today,
                onAnchorDateChanged = onAnchorDateChanged,
                onOpenDatePicker = onOpenDatePicker,
            )
        }
        item {
            if (useHorizontalMetrics) {
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
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("手冲总次数", model.summary.totalCount.toString(), "次", Modifier.fillMaxWidth())
                    MetricCard("手冲天数", model.summary.brewDays.toString(), "天", Modifier.fillMaxWidth())
                    MetricCard(
                        "记录日均",
                        String.format(Locale.US, "%.1f", model.summary.average),
                        "次/天",
                        Modifier.fillMaxWidth(),
                    )
                }
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
            item { EmptyStatistics(onOpenCalendar) }
        } else {
            items(model.details, key = { it.label }) { detail ->
                StatisticRow(
                    label = detail.label,
                    countText = when {
                        detail.future -> "—"
                        !detail.recorded -> "未填写"
                        else -> "${detail.count} 次"
                    },
                    daysText = when {
                        detail.future || !detail.recorded -> "—"
                        else -> "${detail.days} 天"
                    },
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
                        "首次记录：" + records.filter { it.localDate <= today }.minOfOrNull { it.localDate }
                            .orEmptyDate(),
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
private fun PeriodNavigator(
    period: StatisticsPeriod,
    model: StatisticsUiModel,
    anchorDate: LocalDate,
    earliestDate: LocalDate,
    today: LocalDate,
    onAnchorDateChanged: (LocalDate) -> Unit,
    onOpenDatePicker: () -> Unit,
) {
    if (period == StatisticsPeriod.All) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(model.title, color = Ink900, style = MaterialTheme.typography.labelLarge)
            Text(model.status, color = Terracotta500, style = MaterialTheme.typography.labelMedium)
        }
        return
    }

    val previous = previousPeriodAnchor(period, anchorDate, earliestDate)
    val next = nextPeriodAnchor(period, anchorDate, today)
    val periodLabel = when (period) {
        StatisticsPeriod.Week -> "周"
        StatisticsPeriod.Month -> "月"
        StatisticsPeriod.Year -> "年"
        StatisticsPeriod.All -> "历史"
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PeriodArrow(
                forward = false,
                description = "上一个${periodLabel}",
                enabled = previous != null,
                onClick = { previous?.let(onAnchorDateChanged) },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button, onClick = onOpenDatePicker)
                    .semantics {
                        role = Role.Button
                        contentDescription = "选择统计日期，当前${model.title}"
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = model.title,
                        color = Ink900,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = "点此快速跳转",
                        color = Terracotta500,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            PeriodArrow(
                forward = true,
                description = "下一个${periodLabel}",
                enabled = next != null,
                onClick = { next?.let(onAnchorDateChanged) },
            )
        }
        Text(model.status, color = Terracotta500, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun PeriodArrow(
    forward: Boolean,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else .3f)
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        ChevronIcon(forward = forward, color = Ink900)
    }
}

@Composable
private fun EmptyStatistics(onOpenCalendar: () -> Unit) {
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
        PrimaryActionButton(
            label = "去日历记录",
            onClick = onOpenCalendar,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
    }
}

private fun LocalDate?.orEmptyDate(): String = this?.toString() ?: "暂无记录"
