package io.github.litaog.dailyrecord.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.statistics.EARLIEST_SUPPORTED_DATE
import io.github.litaog.dailyrecord.ui.RecordModule
import io.github.litaog.dailyrecord.ui.RecordModuleUiSpec
import io.github.litaog.dailyrecord.ui.asDailyCountEntry
import io.github.litaog.dailyrecord.ui.components.RecordModuleSelector
import io.github.litaog.dailyrecord.ui.handBrewUiSpec
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import java.time.LocalDate
import java.time.YearMonth

// Legacy single-module adapter retained for tests; production uses DailyCountCalendarScreen.
@Composable
internal fun CalendarScreen(
    month: YearMonth,
    focusedDate: LocalDate,
    today: LocalDate,
    records: List<HandBrewRecord>,
    modifier: Modifier = Modifier,
    earliestMonth: YearMonth = YearMonth.from(EARLIEST_SUPPORTED_DATE),
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) = DailyCountCalendarScreen(
    month = month,
    focusedDate = focusedDate,
    today = today,
    records = records.map(HandBrewRecord::asDailyCountEntry),
    moduleSpec = handBrewUiSpec(),
    selectedModule = RecordModule.HandBrew,
    availableModules = listOf(handBrewUiSpec()),
    modifier = modifier,
    earliestMonth = earliestMonth,
    onModuleSelected = {},
    onPreviousMonth = onPreviousMonth,
    onNextMonth = onNextMonth,
    onToday = onToday,
    onOpenDatePicker = onOpenDatePicker,
    onDateSelected = onDateSelected,
)

@Composable
internal fun DailyCountCalendarScreen(
    month: YearMonth,
    focusedDate: LocalDate,
    today: LocalDate,
    records: List<DailyCountEntry>,
    moduleSpec: RecordModuleUiSpec,
    selectedModule: RecordModule,
    availableModules: List<RecordModuleUiSpec>,
    modifier: Modifier = Modifier,
    earliestMonth: YearMonth = YearMonth.from(EARLIEST_SUPPORTED_DATE),
    /** Today's stored count of the current module; null means not filled. */
    todayCount: Int? = null,
    /**
     * Applies a quick-log action to one date (ADR-022). When set, the today
     * island is shown and past/today cells offer the long-press stepper.
     */
    onQuickLog: ((LocalDate, QuickLogAction) -> Unit)? = null,
    showOnboardingCard: Boolean = false,
    onDismissOnboarding: () -> Unit = {},
    onModuleSelected: (RecordModule) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val monthRecords = remember(records, month) {
        records.filter { YearMonth.from(it.localDate) == month }
    }
    val recordsByDate = remember(monthRecords) { monthRecords.associateBy { it.localDate } }
    val totalCount = remember(monthRecords) { monthRecords.sumOf { it.count.toLong() } }
    val recordedDays = remember(monthRecords) { monthRecords.count { it.count > 0 } }
    val gridDates = remember(month) { calendarGridDates(month) }
    // The long-press stepper closes when the month or the module changes, so
    // it never adjusts a date or a module the user is no longer looking at.
    val quickAdjustDate = remember(month, selectedModule) {
        androidx.compose.runtime.mutableStateOf<LocalDate?>(null)
    }
    val canGoPrevious = month > earliestMonth
    val canGoNext = month < YearMonth.from(today)
    val fontScale = LocalDensity.current.fontScale
    val largeText = fontScale >= 1.4f
    val dayCellHeight = if (largeText) 76.dp else DailyRecordSizes.MinimumTouchTarget

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("calendar_screen"),
    ) {
        val horizontalPadding = if (maxWidth < 376.dp) 12.dp else DailyRecordSpacing.ScreenHorizontal
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = horizontalPadding,
                vertical = 12.dp,
            ),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RecordModuleSelector(
                        selected = selectedModule,
                        specs = availableModules,
                        onSelected = onModuleSelected,
                    )
                    if (showOnboardingCard) {
                        IntimacyOnboardingCard(
                            colors = moduleSpec.colors,
                            onDismiss = onDismissOnboarding,
                        )
                    }
                    if (onQuickLog != null) {
                        TodayQuickLogIsland(
                            count = todayCount,
                            moduleSpec = moduleSpec,
                            onAction = { action -> onQuickLog(today, action) },
                        )
                    }
                    MonthHeader(
                        month = month,
                        canGoPrevious = canGoPrevious,
                        canGoNext = canGoNext,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onToday = onToday,
                        onOpenDatePicker = onOpenDatePicker,
                        colors = moduleSpec.colors,
                    )
                    MonthlySummary(
                        totalCount = totalCount,
                        recordedDays = recordedDays,
                    )
                    CalendarWeekdayHeader()
                    CalendarMonthGrid(
                        gridDates = gridDates,
                        earliestDate = earliestMonth.atDay(1),
                        today = today,
                        focusedDate = focusedDate,
                        recordsByDate = recordsByDate,
                        moduleSpec = moduleSpec,
                        cellHeight = dayCellHeight,
                        largeText = largeText,
                        onDateSelected = onDateSelected,
                        // Swipe left / right to change month; arrows stay (ADR-022).
                        modifier = Modifier.monthSwipe(
                            month = month,
                            canGoPrevious = canGoPrevious,
                            canGoNext = canGoNext,
                            onPrevious = onPreviousMonth,
                            onNext = onNextMonth,
                        ),
                        quickAdjustDate = quickAdjustDate.value,
                        onLongPress = if (onQuickLog != null) {
                            { date -> quickAdjustDate.value = date }
                        } else {
                            null
                        },
                        quickAdjustPopup = { date ->
                            if (onQuickLog != null) {
                                CalendarQuickAdjustPopup(
                                    date = date,
                                    count = recordsByDate[date]?.count,
                                    moduleSpec = moduleSpec,
                                    onAction = { action -> onQuickLog(date, action) },
                                    onDismiss = { quickAdjustDate.value = null },
                                )
                            }
                        },
                    )
                }
            }
            item {
                CalendarGuide(
                    moduleSpec = moduleSpec,
                    largeText = largeText,
                )
            }
        }
    }
}

@Composable
private fun CalendarWeekdayHeader() {
    Row(Modifier.fillMaxWidth()) {
        AppCopy.Calendar.weekdays.forEach { weekday ->
            Text(
                text = weekday,
                modifier = Modifier.weight(1f),
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    gridDates: List<LocalDate?>,
    earliestDate: LocalDate,
    today: LocalDate,
    focusedDate: LocalDate,
    recordsByDate: Map<LocalDate, DailyCountEntry>,
    moduleSpec: RecordModuleUiSpec,
    cellHeight: androidx.compose.ui.unit.Dp,
    largeText: Boolean,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    /** Date whose quick-adjust stepper is open, if any. */
    quickAdjustDate: LocalDate? = null,
    onLongPress: ((LocalDate) -> Unit)? = null,
    quickAdjustPopup: (@Composable (LocalDate) -> Unit)? = null,
) {
    Column(modifier = modifier) {
        gridDates.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    if (date == null) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(cellHeight),
                        )
                    } else {
                        // Only today and past supported dates can be adjusted.
                        val canQuickAdjust = onLongPress != null && date <= today && date >= earliestDate
                        CalendarDayCell(
                            date = date,
                            earliestDate = earliestDate,
                            today = today,
                            focused = date == focusedDate,
                            record = recordsByDate[date],
                            moduleSpec = moduleSpec,
                            cellHeight = cellHeight,
                            largeText = largeText,
                            modifier = Modifier.weight(1f),
                            onClick = { onDateSelected(date) },
                            onLongClick = if (canQuickAdjust) {
                                { onLongPress?.invoke(date) }
                            } else {
                                null
                            },
                            quickAdjustPopup = if (canQuickAdjust && date == quickAdjustDate && quickAdjustPopup != null) {
                                { quickAdjustPopup(date) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
    }
}

internal fun calendarGridDates(month: YearMonth): List<LocalDate?> {
    val leadingEmptyCells = month.atDay(1).dayOfWeek.value - 1
    val visibleCellCount = ((leadingEmptyCells + month.lengthOfMonth() + 6) / 7) * 7
    return List(visibleCellCount) { index ->
        val dayOfMonth = index - leadingEmptyCells + 1
        if (dayOfMonth in 1..month.lengthOfMonth()) month.atDay(dayOfMonth) else null
    }
}
