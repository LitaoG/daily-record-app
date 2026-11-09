package io.github.litaog.dailyrecord.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.core.statistics.EARLIEST_SUPPORTED_DATE
import io.github.litaog.dailyrecord.core.statistics.StatisticsPeriod
import io.github.litaog.dailyrecord.core.statistics.buildDailyCountStatistics
import io.github.litaog.dailyrecord.ui.RecordModule
import io.github.litaog.dailyrecord.ui.components.PeriodTabs
import io.github.litaog.dailyrecord.ui.statistics.DailyCountStatisticsScreen
import io.github.litaog.dailyrecord.ui.statistics.ExtremesCard
import io.github.litaog.dailyrecord.ui.statistics.MonthCountCompositionCard
import io.github.litaog.dailyrecord.ui.statistics.MonthDailyCountCard
import io.github.litaog.dailyrecord.ui.statistics.MonthSummaryCard
import io.github.litaog.dailyrecord.ui.statistics.QuarterShareCard
import io.github.litaog.dailyrecord.ui.statistics.StatisticsSummaryCard
import io.github.litaog.dailyrecord.ui.statistics.WeekDistributionCard
import io.github.litaog.dailyrecord.ui.statistics.YearLineChartCard
import io.github.litaog.dailyrecord.ui.uiSpec
import java.time.LocalDate

// In-memory sample records generators (zero Room/SQLite dependency)
private fun sampleStatisticsRecords(anchor: LocalDate): List<DailyCountEntry> = listOf(
    DailyCountEntry(anchor.minusDays(25), 1),
    DailyCountEntry(anchor.minusDays(22), 2),
    DailyCountEntry(anchor.minusDays(19), 0),
    DailyCountEntry(anchor.minusDays(15), 3),
    DailyCountEntry(anchor.minusDays(12), 4),
    DailyCountEntry(anchor.minusDays(9), 1),
    DailyCountEntry(anchor.minusDays(6), 2),
    DailyCountEntry(anchor.minusDays(5), 0),
    DailyCountEntry(anchor.minusDays(4), 1),
    DailyCountEntry(anchor.minusDays(3), 2),
    DailyCountEntry(anchor.minusDays(2), 3),
    DailyCountEntry(anchor.minusDays(1), 1),
    DailyCountEntry(anchor, 2),
)

private fun sampleYearStatisticsRecords(today: LocalDate): List<DailyCountEntry> = listOf(
    DailyCountEntry(today.minusMonths(10).withDayOfMonth(5), 2),
    DailyCountEntry(today.minusMonths(9).withDayOfMonth(12), 3),
    DailyCountEntry(today.minusMonths(8).withDayOfMonth(18), 1),
    DailyCountEntry(today.minusMonths(7).withDayOfMonth(22), 4),
    DailyCountEntry(today.minusMonths(6).withDayOfMonth(8), 2),
    DailyCountEntry(today.minusMonths(5).withDayOfMonth(14), 0),
    DailyCountEntry(today.minusMonths(4).withDayOfMonth(20), 5),
    DailyCountEntry(today.minusMonths(3).withDayOfMonth(3), 1),
    DailyCountEntry(today.minusMonths(2).withDayOfMonth(15), 3),
    DailyCountEntry(today.minusMonths(1).withDayOfMonth(7), 2),
    DailyCountEntry(today.minusMonths(1).withDayOfMonth(25), 3),
    DailyCountEntry(today.minusDays(10), 1),
    DailyCountEntry(today.minusDays(5), 2),
    DailyCountEntry(today, 2),
)

// ==========================================
// 1. Full Statistics Screen Previews
// ==========================================

@Preview(name = "Statistics Screen - Solo (Week)", group = "Statistics Screens")
@Composable
fun StatisticsScreenSoloWeekPreview() {
    val today = LocalDate.of(2026, 11, 8)
    val modules = listOf(RecordModule.HandBrew.uiSpec(), RecordModule.Sex.uiSpec())
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        DailyCountStatisticsScreen(
            today = today,
            anchorDate = today,
            earliestDate = EARLIEST_SUPPORTED_DATE,
            records = sampleStatisticsRecords(today),
            moduleSpec = spec,
            selectedModule = RecordModule.HandBrew,
            availableModules = modules,
            onModuleSelected = {},
            onAnchorDateChanged = {},
            onOpenDatePicker = {},
            initialPeriod = StatisticsPeriod.Week,
        )
    }
}

@Preview(name = "Statistics Screen - Duo (Month)", group = "Statistics Screens")
@Composable
fun StatisticsScreenDuoMonthPreview() {
    val today = LocalDate.of(2026, 11, 8)
    val modules = listOf(RecordModule.HandBrew.uiSpec(), RecordModule.Sex.uiSpec())
    DailyRecordPreviewTheme(module = RecordModule.Sex) { spec ->
        DailyCountStatisticsScreen(
            today = today,
            anchorDate = today,
            earliestDate = EARLIEST_SUPPORTED_DATE,
            records = sampleStatisticsRecords(today),
            moduleSpec = spec,
            selectedModule = RecordModule.Sex,
            availableModules = modules,
            onModuleSelected = {},
            onAnchorDateChanged = {},
            onOpenDatePicker = {},
            initialPeriod = StatisticsPeriod.Month,
        )
    }
}

@Preview(name = "Statistics Screen - Year View", group = "Statistics Screens")
@Composable
fun StatisticsScreenYearPreview() {
    val today = LocalDate.of(2026, 11, 8)
    val modules = listOf(RecordModule.HandBrew.uiSpec(), RecordModule.Sex.uiSpec())
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        DailyCountStatisticsScreen(
            today = today,
            anchorDate = today,
            earliestDate = EARLIEST_SUPPORTED_DATE,
            records = sampleYearStatisticsRecords(today),
            moduleSpec = spec,
            selectedModule = RecordModule.HandBrew,
            availableModules = modules,
            onModuleSelected = {},
            onAnchorDateChanged = {},
            onOpenDatePicker = {},
            initialPeriod = StatisticsPeriod.Year,
        )
    }
}

@Preview(name = "Statistics Screen - All View", group = "Statistics Screens")
@Composable
fun StatisticsScreenAllPreview() {
    val today = LocalDate.of(2026, 11, 8)
    val modules = listOf(RecordModule.HandBrew.uiSpec(), RecordModule.Sex.uiSpec())
    DailyRecordPreviewTheme(module = RecordModule.Sex) { spec ->
        DailyCountStatisticsScreen(
            today = today,
            anchorDate = today,
            earliestDate = EARLIEST_SUPPORTED_DATE,
            records = sampleYearStatisticsRecords(today),
            moduleSpec = spec,
            selectedModule = RecordModule.Sex,
            availableModules = modules,
            onModuleSelected = {},
            onAnchorDateChanged = {},
            onOpenDatePicker = {},
            initialPeriod = StatisticsPeriod.All,
        )
    }
}

@Preview(name = "Statistics Screen - Empty State", group = "Statistics Screens")
@Composable
fun StatisticsScreenEmptyPreview() {
    val today = LocalDate.of(2026, 11, 8)
    val modules = listOf(RecordModule.HandBrew.uiSpec(), RecordModule.Sex.uiSpec())
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        DailyCountStatisticsScreen(
            today = today,
            anchorDate = today,
            earliestDate = EARLIEST_SUPPORTED_DATE,
            records = emptyList(),
            moduleSpec = spec,
            selectedModule = RecordModule.HandBrew,
            availableModules = modules,
            onModuleSelected = {},
            onAnchorDateChanged = {},
            onOpenDatePicker = {},
            initialPeriod = StatisticsPeriod.Week,
        )
    }
}

@PreviewModules
@Composable
fun StatisticsScreenModulesPreview() {
    val today = LocalDate.of(2026, 11, 8)
    val modules = listOf(RecordModule.HandBrew.uiSpec(), RecordModule.Sex.uiSpec())
    DailyRecordPreviewTheme { spec ->
        DailyCountStatisticsScreen(
            today = today,
            anchorDate = today,
            earliestDate = EARLIEST_SUPPORTED_DATE,
            records = sampleStatisticsRecords(today),
            moduleSpec = spec,
            selectedModule = RecordModule.HandBrew,
            availableModules = modules,
            onModuleSelected = {},
            onAnchorDateChanged = {},
            onOpenDatePicker = {},
            initialPeriod = StatisticsPeriod.Week,
        )
    }
}

@PreviewThemes
@Composable
fun StatisticsScreenThemesPreview() {
    val today = LocalDate.of(2026, 11, 8)
    val modules = listOf(RecordModule.HandBrew.uiSpec(), RecordModule.Sex.uiSpec())
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        DailyCountStatisticsScreen(
            today = today,
            anchorDate = today,
            earliestDate = EARLIEST_SUPPORTED_DATE,
            records = sampleStatisticsRecords(today),
            moduleSpec = spec,
            selectedModule = RecordModule.HandBrew,
            availableModules = modules,
            onModuleSelected = {},
            onAnchorDateChanged = {},
            onOpenDatePicker = {},
            initialPeriod = StatisticsPeriod.Week,
        )
    }
}

@PreviewFontScales
@Composable
fun StatisticsScreenFontScalesPreview() {
    val today = LocalDate.of(2026, 11, 8)
    val modules = listOf(RecordModule.HandBrew.uiSpec(), RecordModule.Sex.uiSpec())
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        DailyCountStatisticsScreen(
            today = today,
            anchorDate = today,
            earliestDate = EARLIEST_SUPPORTED_DATE,
            records = sampleStatisticsRecords(today),
            moduleSpec = spec,
            selectedModule = RecordModule.HandBrew,
            availableModules = modules,
            onModuleSelected = {},
            onAnchorDateChanged = {},
            onOpenDatePicker = {},
            initialPeriod = StatisticsPeriod.Week,
        )
    }
}

// ==========================================
// 2. Isolated Components Previews
// ==========================================

@Preview(name = "Period Switcher - Tabs", group = "Statistics Components")
@Composable
fun PeriodTabsPreview() {
    DailyRecordPreviewTheme { _ ->
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PeriodTabs(
                selected = StatisticsPeriod.Week,
                onSelected = {},
                colors = RecordModule.HandBrew.uiSpec().colors,
            )
            PeriodTabs(
                selected = StatisticsPeriod.Month,
                onSelected = {},
                colors = RecordModule.Sex.uiSpec().colors,
            )
        }
    }
}

@Preview(name = "KPI Summary Cards", group = "Statistics Components")
@Composable
fun KpiSummaryCardsPreview() {
    DailyRecordPreviewTheme { spec ->
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatisticsSummaryCard(
                periodLabel = AppCopy.Statistics.currentWeek,
                moduleLabel = spec.label,
                totalCount = 12,
                recordedDays = 5,
                average = 2.4,
                colors = spec.colors,
            )
            MonthSummaryCard(
                totalCount = 28,
                recordedDays = 18,
                average = 1.55,
                colors = spec.colors,
            )
        }
    }
}

@Preview(name = "Weekly Radial Ring (7 Segments)", group = "Statistics Components")
@Composable
fun WeeklyRadialRingPreview() {
    val today = LocalDate.of(2026, 11, 8)
    val model = buildDailyCountStatistics(
        period = StatisticsPeriod.Week,
        anchorDate = today,
        today = today,
        records = sampleStatisticsRecords(today),
    )
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        WeekDistributionCard(
            details = model.details,
            colors = spec.colors,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Monthly Pulse & Composition Cards", group = "Statistics Components")
@Composable
fun MonthlyPulseAndCompositionPreview() {
    val today = LocalDate.of(2026, 11, 8)
    val model = buildDailyCountStatistics(
        period = StatisticsPeriod.Month,
        anchorDate = today,
        today = today,
        records = sampleStatisticsRecords(today),
    )
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            model.month?.let { MonthDailyCountCard(month = it, colors = spec.colors) }
            model.month?.let { MonthCountCompositionCard(month = it, colors = spec.colors) }
        }
    }
}

@Preview(name = "Annual Chart & Breakdown Cards", group = "Statistics Components")
@Composable
fun AnnualChartAndBreakdownPreview() {
    val today = LocalDate.of(2026, 11, 8)
    val model = buildDailyCountStatistics(
        period = StatisticsPeriod.Year,
        anchorDate = today,
        today = today,
        records = sampleYearStatisticsRecords(today),
    )
    DailyRecordPreviewTheme(module = RecordModule.Sex) { spec ->
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            model.year?.let {
                YearLineChartCard(year = it, colors = spec.colors, modifier = Modifier.fillMaxWidth())
                QuarterShareCard(year = it, colors = spec.colors)
                ExtremesCard(year = it, colors = spec.colors)
            }
        }
    }
}
