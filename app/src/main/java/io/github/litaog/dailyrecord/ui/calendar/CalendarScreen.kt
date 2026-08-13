package io.github.litaog.dailyrecord.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.HandBrewModuleSpec
import io.github.litaog.dailyrecord.ui.RecordModule
import io.github.litaog.dailyrecord.ui.RecordModuleUiSpec
import io.github.litaog.dailyrecord.ui.asDailyCountEntry
import io.github.litaog.dailyrecord.core.statistics.EARLIEST_SUPPORTED_DATE
import io.github.litaog.dailyrecord.ui.components.ChevronIcon
import io.github.litaog.dailyrecord.ui.components.RecordModuleSelector
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceDisabled
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.RecordVisualState
import java.time.LocalDate
import java.time.YearMonth

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
    moduleSpec = HandBrewModuleSpec,
    selectedModule = RecordModule.HandBrew,
    availableModules = listOf(HandBrewModuleSpec),
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
) {
    Column {
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

@Composable
private fun MonthlySummary(
    totalCount: Long,
    recordedDays: Int,
) {
    val summary = AppCopy.Calendar.monthSummary(totalCount, recordedDays)
    Text(
        text = summary,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp)
            .testTag("calendar_month_summary")
            .semantics { contentDescription = summary },
        color = DailyRecordText,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun CalendarGuide(
    moduleSpec: RecordModuleUiSpec,
    largeText: Boolean,
) {
    val legendItems = listOf(
        CalendarLegendItem.Unset,
        CalendarLegendItem.Disabled,
        CalendarLegendItem.Zero,
        CalendarLegendItem.Recorded,
    )
    val legendRows = if (largeText) legendItems.chunked(2) else listOf(legendItems)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 2.dp)
            .testTag("calendar_guide")
            .clearAndSetSemantics {
                contentDescription =
                    AppCopy.Calendar.legendDescription(moduleSpec.semanticCountLabel)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DailyRecordDivider),
        )
        Text(
            text = AppCopy.Calendar.recordHint,
            color = DailyRecordTextSecondary,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        legendRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                row.forEach { item ->
                    CalendarLegendEntry(
                        item = item,
                        colors = moduleSpec.colors,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

private enum class CalendarLegendItem(
    val label: String,
) {
    Unset(AppCopy.Calendar.unset),
    Disabled(AppCopy.Calendar.future),
    Zero(AppCopy.Calendar.zero),
    Recorded(AppCopy.Calendar.recorded),
}

@Composable
private fun CalendarLegendEntry(
    item: CalendarLegendItem,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CalendarLegendMarker(item = item, colors = colors)
        Spacer(Modifier.width(5.dp))
        Text(
            text = item.label,
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}

@Composable
private fun CalendarLegendMarker(
    item: CalendarLegendItem,
    colors: RecordModuleColorTokens,
) {
    when (item) {
        CalendarLegendItem.Unset -> Box(
            Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(colors.colorsFor(RecordVisualState.Unset).background),
        )
        CalendarLegendItem.Disabled -> Box(
            Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DailyRecordSurfaceDisabled),
        )
        CalendarLegendItem.Zero -> Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DailyRecordSurface)
                .border(1.dp, colors.primary, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(5.dp)
                    .border(1.dp, colors.primary, CircleShape),
            )
        }
        CalendarLegendItem.Recorded -> Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            listOf(colors.soft, colors.medium, colors.intense, colors.primary).forEach { color ->
                Box(
                    Modifier
                        .size(width = 5.dp, height = 14.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color),
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onOpenDatePicker: () -> Unit,
    colors: RecordModuleColorTokens,
) {
    val fontScale = LocalDensity.current.fontScale
    val largeText = fontScale >= 1.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (largeText) 108.dp else DailyRecordSizes.MinimumTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonthArrow(forward = false, description = AppCopy.Calendar.previousMonth, enabled = canGoPrevious, onClick = onPreviousMonth)
        Column(
            modifier = Modifier
                .weight(1f)
                .sizeIn(minHeight = DailyRecordSizes.MinimumTouchTarget)
                .clip(RoundedCornerShape(14.dp))
                .clickable(role = Role.Button, onClick = onOpenDatePicker)
                .semantics {
                    role = Role.Button
                    contentDescription = AppCopy.Calendar.monthSelectionDescription(month)
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (largeText) {
                    AppCopy.Calendar.monthTitleMultiline(month)
                } else {
                    AppCopy.Calendar.monthTitle(month)
                },
                color = DailyRecordText,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = if (largeText) 2 else 1,
            )
        }
        MonthArrow(forward = true, description = AppCopy.Calendar.nextMonth, enabled = canGoNext, onClick = onNextMonth)
        Box(
            modifier = Modifier
                .padding(start = 6.dp)
                .sizeIn(minWidth = DailyRecordSizes.MinimumTouchTarget, minHeight = DailyRecordSizes.MinimumTouchTarget)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onToday)
                .semantics { role = Role.Button; contentDescription = AppCopy.Calendar.backToToday },
            contentAlignment = Alignment.Center,
        ) {
            Text(AppCopy.today, color = colors.primary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun MonthArrow(
    forward: Boolean,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .sizeIn(minWidth = DailyRecordSizes.MinimumTouchTarget, minHeight = DailyRecordSizes.MinimumTouchTarget)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else .3f)
            .semantics { role = Role.Button; contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        ChevronIcon(forward = forward, color = DailyRecordText)
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    earliestDate: LocalDate,
    today: LocalDate,
    focused: Boolean,
    record: DailyCountEntry?,
    moduleSpec: RecordModuleUiSpec,
    cellHeight: androidx.compose.ui.unit.Dp,
    largeText: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unsupported = date < earliestDate
    val future = date > today
    val count = record?.count
    val visualState = calendarRecordVisualState(
        unsupported = unsupported,
        future = future,
        count = count,
    )
    val visualColors = moduleSpec.colors.colorsFor(visualState)
    val background = visualColors.background
    val contentColor = visualColors.content
    val recordStatus = when {
        unsupported -> AppCopy.Calendar.unavailable
        future || record == null -> null
        count == 0 -> null
        else -> AppCopy.Calendar.countDescription(count ?: 0)
    }
    val visibleStatus = when {
        date == today && record == null -> AppCopy.Calendar.todayShort
        else -> recordStatus
    }
    val semanticStatus = AppCopy.Calendar.statusDescription(
        date = date,
        today = today,
        unsupported = unsupported,
        future = future,
        count = count,
        moduleLabel = moduleSpec.semanticCountLabel,
    )
    val borderColor = when {
        focused -> moduleSpec.colors.strong
        date == today -> moduleSpec.colors.primary
        else -> visualColors.outline
    }
    val showOutline = focused || date == today || visualState == RecordVisualState.ExplicitZero
    val borderWidth = if (focused || date == today) 2.dp else 1.dp
    val cellShape = RoundedCornerShape(9.dp)
    val stateDescription = semanticStatus

    Box(
        modifier = modifier
            .height(cellHeight)
            .testTag("calendar_day_$date")
            .clip(cellShape)
            .clickable(enabled = !future && !unsupported, role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                selected = focused
                contentDescription = AppCopy.Calendar.monthDateDescription(date, stateDescription, focused)
            }
            .padding(3.dp),
        contentAlignment = Alignment.Center,
    ) {
        val dayColor = if (date == today && record == null) moduleSpec.colors.primary else contentColor
        val visualModifier = Modifier
            .fillMaxSize()
            .clip(cellShape)
            .background(background)
            .then(
                if (showOutline) {
                    Modifier.border(borderWidth, borderColor, cellShape)
                } else {
                    Modifier
                },
            )

        Column(
            modifier = visualModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = dayColor,
                style = if (largeText) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            when {
                visualState == RecordVisualState.ExplicitZero -> Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .sizeIn(minWidth = 9.dp, minHeight = 9.dp)
                        .border(1.5.dp, moduleSpec.colors.primary, CircleShape),
                )
                visibleStatus != null -> Text(
                    text = visibleStatus,
                    color = dayColor,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}

internal fun calendarRecordVisualState(
    unsupported: Boolean,
    future: Boolean,
    count: Int?,
): RecordVisualState = when {
    unsupported || future -> RecordVisualState.Disabled
    count == null -> RecordVisualState.Unset
    count == 0 -> RecordVisualState.ExplicitZero
    count == 1 -> RecordVisualState.One
    count == 2 -> RecordVisualState.Two
    count == 3 -> RecordVisualState.Three
    else -> RecordVisualState.FourPlus
}
