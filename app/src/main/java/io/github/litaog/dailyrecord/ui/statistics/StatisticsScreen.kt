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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.DailyCountEntry
import io.github.litaog.dailyrecord.ui.HandBrewModuleSpec
import io.github.litaog.dailyrecord.ui.RecordModule
import io.github.litaog.dailyrecord.ui.RecordModuleUiSpec
import io.github.litaog.dailyrecord.ui.asDailyCountEntry
import io.github.litaog.dailyrecord.ui.components.ChevronIcon
import io.github.litaog.dailyrecord.ui.components.PeriodTabs
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.components.RecordModuleSelector
import io.github.litaog.dailyrecord.ui.components.StatisticRow
import io.github.litaog.dailyrecord.ui.components.StatisticsPeriod
import io.github.litaog.dailyrecord.ui.navigation.nextPeriodAnchor
import io.github.litaog.dailyrecord.ui.navigation.previousPeriodAnchor
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import java.time.LocalDate

@Composable
fun StatisticsScreen(
    today: LocalDate,
    anchorDate: LocalDate,
    earliestDate: LocalDate,
    records: List<HandBrewRecord>,
    onAnchorDateChanged: (LocalDate) -> Unit,
    onOpenDatePicker: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenPeriodPicker: (StatisticsPeriod) -> Unit = { onOpenDatePicker() },
    onOpenCalendar: () -> Unit = {},
) = DailyCountStatisticsScreen(
    today = today,
    anchorDate = anchorDate,
    earliestDate = earliestDate,
    records = records.map(HandBrewRecord::asDailyCountEntry),
    moduleSpec = HandBrewModuleSpec,
    selectedModule = RecordModule.HandBrew,
    availableModules = listOf(HandBrewModuleSpec),
    onModuleSelected = {},
    onAnchorDateChanged = onAnchorDateChanged,
    onOpenDatePicker = onOpenDatePicker,
    modifier = modifier,
    onOpenPeriodPicker = onOpenPeriodPicker,
    onOpenCalendar = onOpenCalendar,
)

@Composable
internal fun DailyCountStatisticsScreen(
    today: LocalDate,
    anchorDate: LocalDate,
    earliestDate: LocalDate,
    records: List<DailyCountEntry>,
    moduleSpec: RecordModuleUiSpec,
    selectedModule: RecordModule,
    availableModules: List<RecordModuleUiSpec>,
    onModuleSelected: (RecordModule) -> Unit,
    onAnchorDateChanged: (LocalDate) -> Unit,
    onOpenDatePicker: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenPeriodPicker: (StatisticsPeriod) -> Unit = { onOpenDatePicker() },
    onOpenCalendar: () -> Unit = {},
) {
    var periodName by rememberSaveable { mutableStateOf(StatisticsPeriod.Week.name) }
    val period = StatisticsPeriod.entries.firstOrNull { it.name == periodName }
        ?: StatisticsPeriod.Week
    val model = remember(period, anchorDate, today, records) {
        buildDailyCountStatistics(period, anchorDate, today, records)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("statistics_screen"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = DailyRecordSpacing.ScreenHorizontal,
            vertical = DailyRecordSpacing.ScreenVertical,
        ),
        verticalArrangement = Arrangement.spacedBy(DailyRecordSpacing.Content),
    ) {
        item {
            RecordModuleSelector(
                selected = selectedModule,
                specs = availableModules,
                onSelected = onModuleSelected,
            )
        }
        item {
            PeriodTabs(
                selected = period,
                onSelected = { periodName = it.name },
                colors = moduleSpec.colors,
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
                onOpenPeriodPicker = onOpenPeriodPicker,
                colors = moduleSpec.colors,
            )
        }
        item {
            if (period == StatisticsPeriod.Month) {
                MonthSummaryCard(
                    totalCount = model.summary.totalCount,
                    recordedDays = model.summary.recordedDays,
                    average = model.summary.average,
                    colors = moduleSpec.colors,
                )
            } else {
                StatisticsSummaryCard(
                    periodLabel = periodSummaryLabel(period),
                    moduleLabel = moduleSpec.label,
                    totalCount = model.summary.totalCount,
                    recordedDays = model.summary.recordedDays,
                    average = model.summary.average,
                    colors = moduleSpec.colors,
                )
            }
        }
        when (period) {
            StatisticsPeriod.Week -> {
                if (model.details.isEmpty()) {
                    item { EmptyStatistics(moduleSpec.label, moduleSpec.colors, onOpenCalendar) }
                } else {
                    item { WeekDistributionCard(model.details, colors = moduleSpec.colors) }
                }
            }
            StatisticsPeriod.Month -> {
                item {
                    model.month?.let { MonthDailyCountCard(it, colors = moduleSpec.colors) }
                }
                item {
                    model.month?.let { MonthCountCompositionCard(it, colors = moduleSpec.colors) }
                }
                item {
                    model.month?.let { MonthDayExtremesCard(it, colors = moduleSpec.colors) }
                }
            }
            StatisticsPeriod.Year -> {
                item {
                    model.year?.let {
                        YearLineChartCard(
                            year = it,
                            colors = moduleSpec.colors,
                        )
                    }
                }
                item {
                    model.year?.let { QuarterShareCard(it, colors = moduleSpec.colors) }
                }
                item {
                    model.year?.let { ExtremesCard(it, colors = moduleSpec.colors) }
                }
            }
            StatisticsPeriod.All -> {
                if (model.details.isEmpty()) {
                    item { EmptyStatistics(moduleSpec.label, moduleSpec.colors, onOpenCalendar) }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(model.detailsTitle, color = DailyRecordText, style = MaterialTheme.typography.labelMedium)
                            Text(AppCopy.Statistics.countAndDays, color = DailyRecordTextMuted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    items(model.details, key = { it.label }) { detail ->
                        StatisticRow(
                            label = detail.label,
                            countText = when {
                                detail.future -> AppCopy.Statistics.dash
                                !detail.recorded -> AppCopy.Statistics.unset
                                else -> AppCopy.Statistics.detailCount(detail.count)
                            },
                            daysText = when {
                                detail.future || !detail.recorded -> AppCopy.Statistics.dash
                                else -> AppCopy.Statistics.detailDays(detail.days)
                            },
                            future = detail.future,
                        )
                    }
                }
            }
        }
    }
}

private fun periodSummaryLabel(period: StatisticsPeriod): String = when (period) {
    StatisticsPeriod.Week -> AppCopy.Statistics.currentWeek
    StatisticsPeriod.Month -> AppCopy.Statistics.currentMonth
    StatisticsPeriod.Year -> AppCopy.Statistics.currentYear
    StatisticsPeriod.All -> AppCopy.Statistics.allTab
}

@Composable
private fun PeriodNavigator(
    period: StatisticsPeriod,
    model: StatisticsUiModel,
    anchorDate: LocalDate,
    earliestDate: LocalDate,
    today: LocalDate,
    onAnchorDateChanged: (LocalDate) -> Unit,
    onOpenPeriodPicker: (StatisticsPeriod) -> Unit,
    colors: RecordModuleColorTokens,
) {
    if (period == StatisticsPeriod.All) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(model.title, color = DailyRecordText, style = MaterialTheme.typography.labelLarge)
            Text(model.status, color = colors.primary, style = MaterialTheme.typography.labelMedium)
        }
        return
    }

    val previous = previousPeriodAnchor(period, anchorDate, earliestDate)
    val next = nextPeriodAnchor(period, anchorDate, today)
    val periodLabel = when (period) {
        StatisticsPeriod.Week -> AppCopy.Statistics.weekTab
        StatisticsPeriod.Month -> AppCopy.Statistics.monthTab
        StatisticsPeriod.Year -> AppCopy.Statistics.yearTab
        StatisticsPeriod.All -> AppCopy.Statistics.historyPeriod
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
                description = AppCopy.Statistics.periodAction(periodLabel, previous = true),
                enabled = previous != null,
                onClick = { previous?.let(onAnchorDateChanged) },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button, onClick = { onOpenPeriodPicker(period) })
                    .semantics {
                        role = Role.Button
                        contentDescription = AppCopy.Statistics.datePickerDescription(model.title)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = model.title,
                    color = DailyRecordText,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
            PeriodArrow(
                forward = true,
                description = AppCopy.Statistics.periodAction(periodLabel, previous = false),
                enabled = next != null,
                onClick = { next?.let(onAnchorDateChanged) },
            )
        }
        Text(model.status, color = colors.primary, style = MaterialTheme.typography.labelMedium)
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
        ChevronIcon(forward = forward, color = DailyRecordText)
    }
}

@Composable
private fun EmptyStatistics(
    moduleLabel: String,
    colors: RecordModuleColorTokens,
    onOpenCalendar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DailyRecordSurfaceMuted)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            AppCopy.Statistics.emptyTitle(moduleLabel),
            color = DailyRecordText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            AppCopy.Statistics.emptyMessage,
            color = DailyRecordTextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        PrimaryActionButton(
            label = AppCopy.Statistics.calendarAction,
            onClick = onOpenCalendar,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            accent = colors.primary,
        )
    }
}
