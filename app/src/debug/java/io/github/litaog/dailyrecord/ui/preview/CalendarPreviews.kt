package io.github.litaog.dailyrecord.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.core.statistics.EARLIEST_SUPPORTED_DATE
import io.github.litaog.dailyrecord.ui.RecordModule
import io.github.litaog.dailyrecord.ui.calendar.CalendarDayCell
import io.github.litaog.dailyrecord.ui.calendar.DailyCountCalendarScreen
import io.github.litaog.dailyrecord.ui.calendar.IntimacyOnboardingCard
import io.github.litaog.dailyrecord.ui.calendar.QuickAdjustContent
import io.github.litaog.dailyrecord.ui.calendar.QuickLogAction
import io.github.litaog.dailyrecord.ui.calendar.TodayQuickLogIsland
import io.github.litaog.dailyrecord.ui.navigation.DateNavigationDialog
import io.github.litaog.dailyrecord.ui.navigation.DateNavigationSelection
import io.github.litaog.dailyrecord.ui.uiSpec
import java.time.LocalDate
import java.time.YearMonth

private fun sampleMonthRecords(month: YearMonth): List<DailyCountEntry> = listOf(
    DailyCountEntry(month.atDay(1), 1),
    DailyCountEntry(month.atDay(3), 0),
    DailyCountEntry(month.atDay(5), 2),
    DailyCountEntry(month.atDay(8), 3),
    DailyCountEntry(month.atDay(12), 4),
    DailyCountEntry(month.atDay(15), 1),
)

// ==========================================
// 1. Full Calendar Screen Previews
// ==========================================

@Preview(name = "Calendar Screen - Solo Active", group = "Calendar")
@Composable
fun CalendarScreenSoloPreview() {
    val today = LocalDate.of(2026, 11, 8)
    val month = YearMonth.of(2026, 11)
    val modules = listOf(RecordModule.HandBrew.uiSpec(), RecordModule.Sex.uiSpec())
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        DailyCountCalendarScreen(
            month = month,
            focusedDate = today,
            today = today,
            records = sampleMonthRecords(month),
            moduleSpec = spec,
            selectedModule = RecordModule.HandBrew,
            availableModules = modules,
            todayCount = 2,
            onQuickLog = { _, _ -> },
            showOnboardingCard = true,
            onDismissOnboarding = {},
            onModuleSelected = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onToday = {},
            onOpenDatePicker = {},
            onDateSelected = {},
        )
    }
}

@Preview(name = "Calendar Screen - Duo Active", group = "Calendar")
@Composable
fun CalendarScreenDuoPreview() {
    val today = LocalDate.of(2026, 11, 8)
    val month = YearMonth.of(2026, 11)
    val modules = listOf(RecordModule.HandBrew.uiSpec(), RecordModule.Sex.uiSpec())
    DailyRecordPreviewTheme(module = RecordModule.Sex) { spec ->
        DailyCountCalendarScreen(
            month = month,
            focusedDate = today,
            today = today,
            records = sampleMonthRecords(month),
            moduleSpec = spec,
            selectedModule = RecordModule.Sex,
            availableModules = modules,
            todayCount = null,
            onQuickLog = { _, _ -> },
            onModuleSelected = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onToday = {},
            onOpenDatePicker = {},
            onDateSelected = {},
        )
    }
}

@PreviewThemes
@Composable
fun CalendarScreenThemesPreview() {
    val today = LocalDate.of(2026, 11, 8)
    val month = YearMonth.of(2026, 11)
    val modules = listOf(RecordModule.HandBrew.uiSpec(), RecordModule.Sex.uiSpec())
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        DailyCountCalendarScreen(
            month = month,
            focusedDate = today,
            today = today,
            records = sampleMonthRecords(month),
            moduleSpec = spec,
            selectedModule = RecordModule.HandBrew,
            availableModules = modules,
            todayCount = 1,
            onQuickLog = { _, _ -> },
            onModuleSelected = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onToday = {},
            onOpenDatePicker = {},
            onDateSelected = {},
        )
    }
}

@PreviewFontScales
@Composable
fun CalendarScreenFontScalesPreview() {
    val today = LocalDate.of(2026, 11, 8)
    val month = YearMonth.of(2026, 11)
    val modules = listOf(RecordModule.HandBrew.uiSpec(), RecordModule.Sex.uiSpec())
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        DailyCountCalendarScreen(
            month = month,
            focusedDate = today,
            today = today,
            records = sampleMonthRecords(month),
            moduleSpec = spec,
            selectedModule = RecordModule.HandBrew,
            availableModules = modules,
            todayCount = 3,
            onQuickLog = { _, _ -> },
            onModuleSelected = {},
            onPreviousMonth = {},
            onNextMonth = {},
            onToday = {},
            onOpenDatePicker = {},
            onDateSelected = {},
        )
    }
}

// ==========================================
// 2. Today Quick-Log Island Previews
// ==========================================

@Preview(name = "Today Quick Log - Unset State", group = "Calendar Components")
@Composable
fun TodayQuickLogUnsetPreview() {
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        TodayQuickLogIsland(
            count = null,
            moduleSpec = spec,
            onAction = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Today Quick Log - Recorded State", group = "Calendar Components")
@Composable
fun TodayQuickLogRecordedPreview() {
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        TodayQuickLogIsland(
            count = 3,
            moduleSpec = spec,
            onAction = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Today Quick Log - Explicit Zero", group = "Calendar Components")
@Composable
fun TodayQuickLogZeroPreview() {
    DailyRecordPreviewTheme(module = RecordModule.Sex) { spec ->
        TodayQuickLogIsland(
            count = 0,
            moduleSpec = spec,
            onAction = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

// ==========================================
// 3. Calendar Day Cell Matrix Previews
// ==========================================

@Preview(name = "Calendar Day Cells - State Matrix", group = "Calendar Components")
@Composable
fun CalendarDayCellMatrixPreview() {
    val today = LocalDate.of(2026, 11, 8)
    val earliest = EARLIEST_SUPPORTED_DATE
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CalendarDayCell(
                date = today.minusDays(3),
                earliestDate = earliest,
                today = today,
                focused = false,
                record = null,
                moduleSpec = spec,
                cellHeight = 48.dp,
                largeText = false,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
            CalendarDayCell(
                date = today.minusDays(2),
                earliestDate = earliest,
                today = today,
                focused = false,
                record = DailyCountEntry(today.minusDays(2), 0),
                moduleSpec = spec,
                cellHeight = 48.dp,
                largeText = false,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
            CalendarDayCell(
                date = today.minusDays(1),
                earliestDate = earliest,
                today = today,
                focused = false,
                record = DailyCountEntry(today.minusDays(1), 1),
                moduleSpec = spec,
                cellHeight = 48.dp,
                largeText = false,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
            CalendarDayCell(
                date = today,
                earliestDate = earliest,
                today = today,
                focused = true,
                record = DailyCountEntry(today, 3),
                moduleSpec = spec,
                cellHeight = 48.dp,
                largeText = false,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
            CalendarDayCell(
                date = today.plusDays(1),
                earliestDate = earliest,
                today = today,
                focused = false,
                record = null,
                moduleSpec = spec,
                cellHeight = 48.dp,
                largeText = false,
                onClick = {},
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ==========================================
// 4. Intimacy Guidance Card Preview
// ==========================================

@Preview(name = "Intimacy Onboarding Card - Solo", group = "Calendar Components")
@Composable
fun IntimacyOnboardingCardSoloPreview() {
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        IntimacyOnboardingCard(
            colors = spec.colors,
            onDismiss = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Intimacy Onboarding Card - Duo", group = "Calendar Components")
@Composable
fun IntimacyOnboardingCardDuoPreview() {
    DailyRecordPreviewTheme(module = RecordModule.Sex) { spec ->
        IntimacyOnboardingCard(
            colors = spec.colors,
            onDismiss = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

// ==========================================
// 5. Calendar Quick Adjust Preview
// ==========================================

@Preview(name = "Calendar Quick Adjust - Recorded", group = "Calendar Components")
@Composable
fun CalendarQuickAdjustPreview() {
    val date = LocalDate.of(2026, 11, 8)
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        QuickAdjustContent(
            date = date,
            count = 2,
            moduleSpec = spec,
            onAction = {},
            onDismiss = {},
        )
    }
}

// ==========================================
// 6. Date Navigation Dialog Previews
// ==========================================

@Preview(name = "Date Navigation Dialog - Date Wheel", group = "Dialogs")
@Composable
fun DateNavigationDialogDatePreview() {
    val date = LocalDate.of(2026, 11, 8)
    DailyRecordPreviewTheme(module = RecordModule.HandBrew) { spec ->
        DateNavigationDialog(
            initialDate = date,
            earliestDate = LocalDate.of(2020, 1, 1),
            latestDate = date,
            colors = spec.colors,
            selection = DateNavigationSelection.Date,
            onDismiss = {},
            onDateSelected = {},
        )
    }
}

@Preview(name = "Date Navigation Dialog - Month Grid", group = "Dialogs")
@Composable
fun DateNavigationDialogMonthPreview() {
    val date = LocalDate.of(2026, 11, 8)
    DailyRecordPreviewTheme(module = RecordModule.Sex) { spec ->
        DateNavigationDialog(
            initialDate = date,
            earliestDate = LocalDate.of(2020, 1, 1),
            latestDate = date,
            colors = spec.colors,
            selection = DateNavigationSelection.Month,
            onDismiss = {},
            onDateSelected = {},
        )
    }
}
