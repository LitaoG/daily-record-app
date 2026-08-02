package io.github.litaog.dailyrecord.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.ui.components.ChevronIcon
import io.github.litaog.dailyrecord.ui.components.DailyRecordDialog
import io.github.litaog.dailyrecord.ui.components.OutlineActionButton
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDivider
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurface
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSurfaceMuted
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

internal enum class DateNavigationSelection { Date, Month, Year }

private enum class NavigationMode { Date, Year }

@Composable
internal fun DateNavigationDialog(
    initialDate: LocalDate,
    earliestDate: LocalDate,
    latestDate: LocalDate,
    colors: RecordModuleColorTokens = HandBrewColorTokens,
    selection: DateNavigationSelection = DateNavigationSelection.Date,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val boundedInitial = initialDate.coerceIn(earliestDate, latestDate)
    var selectedDate by remember(initialDate, earliestDate, latestDate) { mutableStateOf(boundedInitial) }
    var displayedMonth by remember(initialDate, earliestDate, latestDate) {
        mutableStateOf(YearMonth.from(boundedInitial))
    }
    var mode by remember { mutableStateOf(NavigationMode.Date) }

    DailyRecordDialog(
        title = AppCopy.Navigation.title,
        subtitle = when (selection) {
            DateNavigationSelection.Date -> AppCopy.Navigation.subtitle
            DateNavigationSelection.Month -> AppCopy.Navigation.monthSubtitle
            DateNavigationSelection.Year -> AppCopy.Navigation.yearSubtitle
        },
        testTag = "date_navigation_dialog",
        onDismissRequest = onDismiss,
    ) {
        when (selection) {
            DateNavigationSelection.Date -> SelectedDateSummary(selectedDate, colors)
            DateNavigationSelection.Month -> SelectedMonthSummary(YearMonth.from(selectedDate), colors)
            DateNavigationSelection.Year -> SelectedYearSummary(selectedDate.year, colors)
        }
        Spacer(Modifier.height(16.dp))

        when (selection) {
            DateNavigationSelection.Date -> {
                if (mode == NavigationMode.Year) {
                    YearPicker(
                        selectedYear = displayedMonth.year,
                        years = (earliestDate.year..latestDate.year).toList(),
                        colors = colors,
                        onYearSelected = { year ->
                            val newMonth = displayedMonth.withYear(year).coerceIn(
                                YearMonth.from(earliestDate),
                                YearMonth.from(latestDate),
                            )
                            displayedMonth = newMonth
                            selectedDate = newMonth
                                .atDay(selectedDate.dayOfMonth.coerceAtMost(newMonth.lengthOfMonth()))
                                .coerceIn(earliestDate, latestDate)
                            mode = NavigationMode.Date
                        },
                        onBack = { mode = NavigationMode.Date },
                    )
                } else {
                    MonthPicker(
                        displayedMonth = displayedMonth,
                        selectedDate = selectedDate,
                        earliestDate = earliestDate,
                        latestDate = latestDate,
                        colors = colors,
                        onSwitchToYear = { mode = NavigationMode.Year },
                        onMonthChanged = { newMonth ->
                            displayedMonth = newMonth
                            selectedDate = newMonth
                                .atDay(selectedDate.dayOfMonth.coerceAtMost(newMonth.lengthOfMonth()))
                                .coerceIn(earliestDate, latestDate)
                        },
                        onDateSelected = { selectedDate = it },
                    )
                }
            }

            DateNavigationSelection.Month -> MonthSelectionPicker(
                selectedMonth = YearMonth.from(selectedDate),
                earliestMonth = YearMonth.from(earliestDate),
                latestMonth = YearMonth.from(latestDate),
                colors = colors,
                onMonthSelected = { month ->
                    selectedDate = month
                        .atDay(selectedDate.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
                        .coerceIn(earliestDate, latestDate)
                },
            )

            DateNavigationSelection.Year -> YearPicker(
                selectedYear = selectedDate.year,
                years = (earliestDate.year..latestDate.year).toList(),
                colors = colors,
                onYearSelected = { year ->
                    val month = selectedDate.monthValue
                    selectedDate = LocalDate.of(
                        year,
                        month,
                        minOf(selectedDate.dayOfMonth, YearMonth.of(year, month).lengthOfMonth()),
                    ).coerceIn(earliestDate, latestDate)
                },
                onBack = onDismiss,
            )
        }

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlineActionButton(
                AppCopy.Auth.cancel,
                onDismiss,
                Modifier.weight(1f),
                accent = colors.primary,
            )
            PrimaryActionButton(
                label = AppCopy.Navigation.jump,
                onClick = { onDateSelected(selectedDate) },
                modifier = Modifier.weight(1.35f),
                accent = colors.primary,
            )
        }
    }
}

@Composable
private fun SelectedDateSummary(
    date: LocalDate,
    colors: RecordModuleColorTokens,
) {
    val locale = Locale.SIMPLIFIED_CHINESE
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    val largeText = LocalDensity.current.fontScale >= 1.4f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.soft.copy(alpha = .22f))
            .border(1.dp, colors.soft, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(AppCopy.Navigation.selected, color = DailyRecordTextSecondary, style = MaterialTheme.typography.labelSmall)
        if (largeText) {
            Text(
                text = AppCopy.Navigation.dateText(date),
                color = DailyRecordText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = weekday,
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
        } else {
            Text(
                text = AppCopy.Navigation.dateLabel(date, weekday),
                color = DailyRecordText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SelectedMonthSummary(
    month: YearMonth,
    colors: RecordModuleColorTokens,
) {
    SelectionSummary(
        value = AppCopy.Navigation.monthTitle(month),
        colors = colors,
    )
}

@Composable
private fun SelectedYearSummary(
    year: Int,
    colors: RecordModuleColorTokens,
) {
    SelectionSummary(
        value = AppCopy.Navigation.yearTitle(year),
        colors = colors,
    )
}

@Composable
private fun SelectionSummary(
    value: String,
    colors: RecordModuleColorTokens,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.soft.copy(alpha = .22f))
            .border(1.dp, colors.soft, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(AppCopy.Navigation.selected, color = DailyRecordTextSecondary, style = MaterialTheme.typography.labelSmall)
        Text(
            text = value,
            color = DailyRecordText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MonthSelectionPicker(
    selectedMonth: YearMonth,
    earliestMonth: YearMonth,
    latestMonth: YearMonth,
    colors: RecordModuleColorTokens,
    onMonthSelected: (YearMonth) -> Unit,
) {
    var displayedYear by remember(selectedMonth.year) { mutableStateOf(selectedMonth.year) }
    var showYears by remember(selectedMonth.year) { mutableStateOf(false) }

    if (showYears) {
        YearPicker(
            selectedYear = displayedYear,
            years = (earliestMonth.year..latestMonth.year).toList(),
            colors = colors,
            onYearSelected = { year ->
                displayedYear = year
                val candidate = YearMonth.of(year, selectedMonth.monthValue).coerceIn(earliestMonth, latestMonth)
                onMonthSelected(candidate)
                showYears = false
            },
            onBack = { showYears = false },
        )
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(role = Role.Button, onClick = { showYears = true })
                .semantics {
                    role = Role.Button
                    contentDescription = AppCopy.Navigation.switchYearDescription(displayedYear)
                }
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = AppCopy.Navigation.yearTitle(displayedYear),
                color = DailyRecordText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = AppCopy.Navigation.selectMonth,
            color = DailyRecordTextSecondary,
            style = MaterialTheme.typography.labelMedium,
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().height(238.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items((1..12).map { YearMonth.of(displayedYear, it) }, key = { it.toString() }) { month ->
            val enabled = month in earliestMonth..latestMonth
            val selected = month == selectedMonth
            Box(
                modifier = Modifier
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) colors.primary else DailyRecordSurfaceMuted)
                    .border(1.dp, if (selected) colors.primary else DailyRecordDivider, RoundedCornerShape(14.dp))
                    .clickable(enabled = enabled, role = Role.Button) { onMonthSelected(month) }
                    .semantics {
                        role = Role.Button
                        this.selected = selected
                        contentDescription = AppCopy.Navigation.monthDescription(month)
                        if (!enabled) disabled()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = AppCopy.Navigation.monthLabel(month.monthValue),
                    color = when {
                        selected -> colors.onPrimary
                        enabled -> DailyRecordText
                        else -> DailyRecordDivider
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun MonthPicker(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    earliestDate: LocalDate,
    latestDate: LocalDate,
    colors: RecordModuleColorTokens,
    onSwitchToYear: () -> Unit,
    onMonthChanged: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val firstMonth = YearMonth.from(earliestDate)
    val lastMonth = YearMonth.from(latestDate)
    val canGoBack = displayedMonth > firstMonth
    val canGoForward = displayedMonth < lastMonth

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        MonthArrow(
            forward = false,
            enabled = canGoBack,
            onClick = { onMonthChanged(displayedMonth.minusMonths(1)) },
        )
        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(role = Role.Button, onClick = onSwitchToYear)
                .semantics {
                    role = Role.Button
                    contentDescription = AppCopy.Navigation.switchYearDescription(displayedMonth.year)
                }
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = AppCopy.Calendar.monthTitle(displayedMonth),
                color = DailyRecordText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        MonthArrow(
            forward = true,
            enabled = canGoForward,
            onClick = { onMonthChanged(displayedMonth.plusMonths(1)) },
        )
    }

    Row(Modifier.fillMaxWidth()) {
        AppCopy.Navigation.weekdays.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                color = DailyRecordTextMuted,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
    }

    val leadingBlanks = displayedMonth.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value
    val cells = leadingBlanks + displayedMonth.lengthOfMonth()
    val rowCount = (cells + 6) / 7
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(rowCount) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { column ->
                    val day = row * 7 + column - leadingBlanks + 1
                    if (day in 1..displayedMonth.lengthOfMonth()) {
                        val date = displayedMonth.atDay(day)
                        DateCell(
                            date = date,
                            selected = date == selectedDate,
                            enabled = date in earliestDate..latestDate,
                            onClick = { onDateSelected(date) },
                            colors = colors,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f).height(44.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthArrow(forward: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (enabled) DailyRecordSurfaceMuted else Color.Transparent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = AppCopy.Navigation.nextMonthDescription(forward)
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        ChevronIcon(forward = forward, color = if (enabled) DailyRecordText else DailyRecordDivider)
    }
}

@Composable
private fun DateCell(
    date: LocalDate,
    selected: Boolean,
    enabled: Boolean,
    colors: RecordModuleColorTokens,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.SIMPLIFIED_CHINESE)
    Box(
        modifier = modifier
            .height(44.dp)
            .padding(2.dp)
            .clip(CircleShape)
            .background(if (selected) colors.primary else Color.Transparent)
            .border(if (selected) 0.dp else 1.dp, if (selected) Color.Transparent else DailyRecordSurface, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                this.selected = selected
                contentDescription = AppCopy.Navigation.dateDescription(date, weekday)
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = when {
                selected -> colors.onPrimary
                enabled -> DailyRecordText
                else -> DailyRecordDivider
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun YearPicker(
    selectedYear: Int,
    years: List<Int>,
    colors: RecordModuleColorTokens,
    onYearSelected: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val selectedIndex = years.indexOf(selectedYear).coerceAtLeast(0)
    val state = rememberLazyGridState(initialFirstVisibleItemIndex = (selectedIndex - 4).coerceAtLeast(0))
    LaunchedEffect(selectedYear) {
        state.animateScrollToItem((years.indexOf(selectedYear) - 4).coerceAtLeast(0))
    }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onBack)
                .semantics { contentDescription = AppCopy.Navigation.returnToDatePicker },
            contentAlignment = Alignment.Center,
        ) {
            ChevronIcon(forward = false)
        }
        Text(
            AppCopy.Navigation.selectYear,
            color = DailyRecordText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = state,
        modifier = Modifier.fillMaxWidth().height(290.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(years, key = { it }) { year ->
            val selected = year == selectedYear
            Box(
                modifier = Modifier
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) colors.primary else DailyRecordSurfaceMuted)
                    .border(1.dp, if (selected) colors.primary else DailyRecordDivider, RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button) { onYearSelected(year) }
                    .semantics {
                        role = Role.Button
                        this.selected = selected
                        contentDescription = AppCopy.Navigation.selectYearDescription(year)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$year",
                    color = if (selected) colors.onPrimary else DailyRecordText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

private fun YearMonth.coerceIn(minimum: YearMonth, maximum: YearMonth): YearMonth = when {
    this < minimum -> minimum
    this > maximum -> maximum
    else -> this
}
