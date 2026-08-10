package io.github.litaog.dailyrecord.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyColumnItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

internal enum class DateNavigationSelection { Date, Month, Year }

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

    DailyRecordDialog(
        title = if (selection == DateNavigationSelection.Year) {
            AppCopy.Navigation.selectYear
        } else {
            AppCopy.Navigation.title
        },
        subtitle = when (selection) {
            DateNavigationSelection.Date -> AppCopy.Navigation.dateWheelSubtitle
            DateNavigationSelection.Month -> AppCopy.Navigation.monthSubtitle
            DateNavigationSelection.Year -> null
        },
        testTag = "date_navigation_dialog",
        onDismissRequest = onDismiss,
    ) {
        when (selection) {
            DateNavigationSelection.Date -> SelectedDateSummary(selectedDate, colors)
            DateNavigationSelection.Month -> SelectedMonthSummary(YearMonth.from(selectedDate), colors)
            DateNavigationSelection.Year -> Unit
        }
        Spacer(Modifier.height(if (selection == DateNavigationSelection.Year) 8.dp else 16.dp))

        when (selection) {
            DateNavigationSelection.Date -> {
                DateWheelPicker(
                    selectedDate = selectedDate,
                    earliestDate = earliestDate,
                    latestDate = latestDate,
                    colors = colors,
                    onDateSelected = { selectedDate = it },
                )
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

            DateNavigationSelection.Year -> YearWheelPicker(
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
                label = navigationJumpLabel(selection),
                onClick = { onDateSelected(selectedDate) },
                modifier = Modifier.weight(1.35f),
                accent = colors.primary,
            )
        }
    }
}

internal fun navigationJumpLabel(selection: DateNavigationSelection): String = when (selection) {
    DateNavigationSelection.Date -> AppCopy.Navigation.jumpToDate
    DateNavigationSelection.Month -> AppCopy.Navigation.jumpToMonth
    DateNavigationSelection.Year -> AppCopy.Navigation.jumpToYear
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
            .clip(RoundedCornerShape(14.dp))
            .background(colors.soft.copy(alpha = .54f))
            .border(1.dp, colors.primary.copy(alpha = .20f), RoundedCornerShape(14.dp))
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
private fun SelectionSummary(
    value: String,
    colors: RecordModuleColorTokens,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.soft.copy(alpha = .54f))
            .border(1.dp, colors.primary.copy(alpha = .20f), RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Text(AppCopy.Navigation.selected, color = colors.primary, style = MaterialTheme.typography.labelSmall)
        Text(
            text = value,
            color = DailyRecordText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * Date navigation deliberately uses a different mental model from the home
 * calendar: three compact wheels answer "which date?" without putting a
 * second month grid on top of the first one.
 */
@Composable
private fun DateWheelPicker(
    selectedDate: LocalDate,
    earliestDate: LocalDate,
    latestDate: LocalDate,
    colors: RecordModuleColorTokens,
    onDateSelected: (LocalDate) -> Unit,
) {
    val years = (earliestDate.year..latestDate.year).toList()
    val firstMonth = if (selectedDate.year == earliestDate.year) earliestDate.monthValue else 1
    val lastMonth = if (selectedDate.year == latestDate.year) latestDate.monthValue else 12
    val months = (firstMonth..lastMonth).toList()
    val days = (1..YearMonth.of(selectedDate.year, selectedDate.monthValue).lengthOfMonth())
        .filter { day ->
            LocalDate.of(selectedDate.year, selectedDate.monthValue, day) in earliestDate..latestDate
        }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DateWheelColumn(
                values = years,
                selectedValue = selectedDate.year,
                label = AppCopy.Navigation.yearUnit,
                valueLabel = AppCopy.Navigation::yearTitle,
                optionEnabled = { true },
                onValueSelected = { year ->
                    onDateSelected(
                        clampWheelDate(
                            year = year,
                            month = selectedDate.monthValue,
                            day = selectedDate.dayOfMonth,
                            earliestDate = earliestDate,
                            latestDate = latestDate,
                        ),
                    )
                },
                colors = colors,
                modifier = Modifier.weight(1.16f).testTag("date_wheel_year"),
            )
            DateWheelColumn(
                values = months,
                selectedValue = selectedDate.monthValue,
                label = AppCopy.Navigation.monthUnit,
                valueLabel = AppCopy.Navigation::monthLabel,
                optionEnabled = { month ->
                    YearMonth.of(selectedDate.year, month) in
                        YearMonth.from(earliestDate)..YearMonth.from(latestDate)
                },
                onValueSelected = { month ->
                    onDateSelected(
                        clampWheelDate(
                            year = selectedDate.year,
                            month = month,
                            day = selectedDate.dayOfMonth,
                            earliestDate = earliestDate,
                            latestDate = latestDate,
                        ),
                    )
                },
                colors = colors,
                modifier = Modifier.weight(1f).testTag("date_wheel_month"),
            )
            DateWheelColumn(
                values = days,
                selectedValue = selectedDate.dayOfMonth,
                label = AppCopy.Navigation.dayUnit,
                valueLabel = AppCopy.Navigation::dayLabel,
                optionEnabled = { day ->
                    LocalDate.of(selectedDate.year, selectedDate.monthValue, day) in earliestDate..latestDate
                },
                onValueSelected = { day ->
                    onDateSelected(
                        clampWheelDate(
                            year = selectedDate.year,
                            month = selectedDate.monthValue,
                            day = day,
                            earliestDate = earliestDate,
                            latestDate = latestDate,
                        ),
                    )
                },
                colors = colors,
                modifier = Modifier.weight(.84f).testTag("date_wheel_day"),
            )
        }
        Text(
            text = AppCopy.Navigation.dateWheelHint,
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DateWheelColumn(
    values: List<Int>,
    selectedValue: Int,
    label: String,
    valueLabel: (Int) -> String,
    optionEnabled: (Int) -> Boolean,
    onValueSelected: (Int) -> Unit,
    colors: RecordModuleColorTokens,
    modifier: Modifier = Modifier,
) {
    val initialIndex = values.indexOf(selectedValue).coerceAtLeast(0)
    var selectedIndex by remember(values) { mutableIntStateOf(initialIndex) }
    var dragOffsetPx by remember(values) { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val latestOnValueSelected = rememberUpdatedState(onValueSelected)
    val latestOptionEnabled = rememberUpdatedState(optionEnabled)
    val settleAnimation = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val rowHeightPx = with(LocalDensity.current) { 44.dp.toPx() }
    val shape = RoundedCornerShape(14.dp)

    /**
     * Keep the local wheel position in sync with changes made by another wheel
     * (for example, changing the year can clamp the day). During a drag the
     * local position is authoritative so recomposition cannot snap the content
     * back under the user's finger.
     */
    LaunchedEffect(selectedValue, values) {
        if (!isDragging) {
            selectedIndex = values.indexOf(selectedValue).coerceAtLeast(0)
            dragOffsetPx = 0f
        }
    }

    fun commitIndex(index: Int): Boolean {
        val candidate = values.getOrNull(index) ?: return false
        if (!latestOptionEnabled.value(candidate)) return false
        selectedIndex = index
        latestOnValueSelected.value(candidate)
        return true
    }

    fun cancelSettle() {
        settleJob?.cancel()
        settleJob = null
        coroutineScope.launch { settleAnimation.stop() }
        isDragging = false
    }

    fun settleWheel() {
        val currentIndex = selectedIndex.coerceIn(0, values.lastIndex.coerceAtLeast(0))
        var targetIndex = currentIndex
        if (dragOffsetPx <= -rowHeightPx / 2f && currentIndex < values.lastIndex) {
            targetIndex += 1
        } else if (dragOffsetPx >= rowHeightPx / 2f && currentIndex > 0) {
            targetIndex -= 1
        }

        if (targetIndex != currentIndex && commitIndex(targetIndex)) {
            // Re-basing the three rendered rows keeps the visual position
            // continuous when the centered item changes at the end of a drag.
            dragOffsetPx += if (targetIndex > currentIndex) rowHeightPx else -rowHeightPx
        }

        val startOffset = dragOffsetPx
        settleJob?.cancel()
        settleJob = coroutineScope.launch {
            try {
                settleAnimation.snapTo(startOffset)
                settleAnimation.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(dampingRatio = 0.92f, stiffness = 500f),
                ) {
                    dragOffsetPx = value
                }
                dragOffsetPx = 0f
                isDragging = false
            } catch (_: CancellationException) {
                // A new drag or a row tap takes ownership of the wheel.
            }
        }
    }

    val draggableState = rememberDraggableState { dragAmount ->
        var nextOffset = dragOffsetPx + dragAmount

        // Rebase the rendered three-row window whenever a full row passes the
        // center. The content therefore follows the finger instead of waiting
        // for a threshold before visibly changing.
        while (nextOffset <= -rowHeightPx && selectedIndex < values.lastIndex) {
            if (!commitIndex(selectedIndex + 1)) break
            nextOffset += rowHeightPx
        }
        while (nextOffset >= rowHeightPx && selectedIndex > 0) {
            if (!commitIndex(selectedIndex - 1)) break
            nextOffset -= rowHeightPx
        }

        // Add a small edge resistance instead of exposing an empty row when
        // the wheel is already at its first or last value.
        dragOffsetPx = when {
            selectedIndex == 0 && nextOffset > 0f ->
                (nextOffset - dragAmount + dragAmount * 0.24f)
                    .coerceAtMost(rowHeightPx * 0.65f)
            selectedIndex == values.lastIndex && nextOffset < 0f ->
                (nextOffset - dragAmount + dragAmount * 0.24f)
                    .coerceAtLeast(-rowHeightPx * 0.65f)
            else -> nextOffset
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = DailyRecordTextMuted,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(shape)
                .background(DailyRecordSurfaceMuted)
                .border(1.dp, DailyRecordDivider, shape)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    onDragStarted = {
                        cancelSettle()
                        isDragging = true
                    },
                    onDragStopped = { settleWheel() },
                ),
        ) {
            Canvas(Modifier.fillMaxWidth().height(148.dp)) {
                val bandHeight = 44.dp.toPx()
                val top = (size.height - bandHeight) / 2f
                drawRect(
                    color = colors.soft.copy(alpha = .78f),
                    topLeft = androidx.compose.ui.geometry.Offset(0f, top),
                    size = androidx.compose.ui.geometry.Size(size.width, bandHeight),
                )
                drawLine(
                    color = colors.primary.copy(alpha = .48f),
                    start = androidx.compose.ui.geometry.Offset(0f, top),
                    end = androidx.compose.ui.geometry.Offset(size.width, top),
                    strokeWidth = 1.dp.toPx(),
                )
                drawLine(
                    color = colors.primary.copy(alpha = .48f),
                    start = androidx.compose.ui.geometry.Offset(0f, top + bandHeight),
                    end = androidx.compose.ui.geometry.Offset(size.width, top + bandHeight),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .graphicsLayer { translationY = dragOffsetPx },
            ) {
                WheelValueRow(
                    value = values.getOrNull(selectedIndex - 1),
                    selected = false,
                    valueLabel = valueLabel,
                    enabled = values.getOrNull(selectedIndex - 1)
                        ?.let(latestOptionEnabled.value)
                        ?: false,
                    onClick = {
                        cancelSettle()
                        if (commitIndex(selectedIndex - 1)) {
                            dragOffsetPx = 0f
                        }
                    },
                )
                WheelValueRow(
                    value = values.getOrNull(selectedIndex),
                    selected = true,
                    valueLabel = valueLabel,
                    enabled = values.getOrNull(selectedIndex)
                        ?.let(latestOptionEnabled.value)
                        ?: false,
                    onClick = {
                        cancelSettle()
                        if (commitIndex(selectedIndex)) {
                            dragOffsetPx = 0f
                        }
                    },
                )
                WheelValueRow(
                    value = values.getOrNull(selectedIndex + 1),
                    selected = false,
                    valueLabel = valueLabel,
                    enabled = values.getOrNull(selectedIndex + 1)
                        ?.let(latestOptionEnabled.value)
                        ?: false,
                    onClick = {
                        cancelSettle()
                        if (commitIndex(selectedIndex + 1)) {
                            dragOffsetPx = 0f
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun WheelValueRow(
    value: Int?,
    selected: Boolean,
    valueLabel: (Int) -> String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(enabled = value != null && enabled, role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                this.selected = selected
                if (value != null) contentDescription = valueLabel(value)
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value?.let(valueLabel).orEmpty(),
            color = when {
                selected -> DailyRecordText
                !enabled -> DailyRecordDivider
                else -> DailyRecordTextSecondary.copy(alpha = .72f)
            },
            style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

internal fun clampWheelDate(
    year: Int,
    month: Int,
    day: Int,
    earliestDate: LocalDate,
    latestDate: LocalDate,
): LocalDate {
    val safeMonth = month.coerceIn(1, 12)
    val safeDay = day.coerceIn(1, YearMonth.of(year, safeMonth).lengthOfMonth())
    return LocalDate.of(year, safeMonth, safeDay).coerceIn(earliestDate, latestDate)
}

@Composable
private fun YearWheelPicker(
    selectedYear: Int,
    years: List<Int>,
    colors: RecordModuleColorTokens,
    onYearSelected: (Int) -> Unit,
) {
    val firstYear = years.firstOrNull() ?: selectedYear
    val lastYear = years.lastOrNull() ?: selectedYear
    val visibleYears = ((firstYear - 3)..(lastYear + 3)).toList()
    val selectedIndex = visibleYears.indexOf(selectedYear).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (selectedIndex - 2).coerceAtLeast(0))
    LaunchedEffect(selectedYear) {
        // Keep the selected row and its neighbours in the semantics tree immediately.
        // An animated first scroll can briefly expose only the selected row, making the
        // adjacent years impossible to discover through TalkBack or UI tests.
        listState.scrollToItem((visibleYears.indexOf(selectedYear) - 2).coerceAtLeast(0))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(270.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DailyRecordSurfaceMuted)
            .border(1.dp, DailyRecordDivider, RoundedCornerShape(14.dp)),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 42.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            lazyColumnItems(visibleYears, key = { it }) { year ->
                val selected = year == selectedYear
                val enabled = year in years
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) colors.primary.copy(alpha = .16f) else Color.Transparent,
                        )
                        .border(
                            width = if (selected) 1.dp else 0.dp,
                            color = if (selected) colors.primary.copy(alpha = .72f) else Color.Transparent,
                            shape = RoundedCornerShape(16.dp),
                        )
                        .clickable(enabled = enabled, role = Role.Button) {
                            onYearSelected(year)
                        }
                        .semantics {
                            role = Role.Button
                            this.selected = selected
                            contentDescription = AppCopy.Navigation.selectYearDescription(year)
                            if (!enabled) disabled()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = AppCopy.Navigation.yearTitle(year),
                        color = when {
                            selected -> colors.primary
                            enabled -> DailyRecordTextSecondary
                            else -> DailyRecordDivider
                        },
                        style = if (selected) {
                            MaterialTheme.typography.titleLarge
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
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
        YearWheelPicker(
            selectedYear = displayedYear,
            years = (earliestMonth.year..latestMonth.year).toList(),
            colors = colors,
            onYearSelected = { year ->
                displayedYear = year
                val candidate = YearMonth.of(year, selectedMonth.monthValue).coerceIn(earliestMonth, latestMonth)
                onMonthSelected(candidate)
                showYears = false
            },
        )
        return
    }

    val canGoBack = displayedYear > earliestMonth.year
    val canGoForward = displayedYear < latestMonth.year
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            YearArrow(
                forward = false,
                enabled = canGoBack,
                colors = colors,
                onClick = { displayedYear-- },
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button, onClick = { showYears = true })
                    .semantics {
                        role = Role.Button
                        contentDescription = AppCopy.Navigation.switchYearDescription(displayedYear)
                    }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = AppCopy.Navigation.yearTitle(displayedYear),
                        color = DailyRecordText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    DownChevronIcon(color = DailyRecordTextSecondary)
                }
            }
            YearArrow(
                forward = true,
                enabled = canGoForward,
                colors = colors,
                onClick = { displayedYear++ },
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DailyRecordDivider.copy(alpha = .42f)),
        )
        Spacer(Modifier.height(10.dp))
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().height(252.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items((1..12).map { YearMonth.of(displayedYear, it) }, key = { it.toString() }) { month ->
            val enabled = month in earliestMonth..latestMonth
            val selected = month == selectedMonth
            Box(
                modifier = Modifier
                    .height(54.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (selected) colors.primary.copy(alpha = .92f) else Color.Transparent)
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
                        enabled -> DailyRecordTextSecondary
                        else -> DailyRecordTextMuted
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun YearArrow(
    forward: Boolean,
    enabled: Boolean,
    colors: RecordModuleColorTokens,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (enabled) colors.soft.copy(alpha = .16f) else Color.Transparent)
            .border(
                width = if (enabled) 1.dp else 0.dp,
                color = if (enabled) colors.soft.copy(alpha = .60f) else Color.Transparent,
                shape = CircleShape,
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = AppCopy.Navigation.nextYearDescription(forward)
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        ChevronIcon(forward = forward, color = if (enabled) colors.primary else DailyRecordDivider)
    }
}

@Composable
private fun DownChevronIcon(color: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val stroke = 2.dp.toPx()
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width * .18f, size.height * .35f),
            end = androidx.compose.ui.geometry.Offset(size.width * .50f, size.height * .68f),
            strokeWidth = stroke,
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.width * .50f, size.height * .68f),
            end = androidx.compose.ui.geometry.Offset(size.width * .82f, size.height * .35f),
            strokeWidth = stroke,
        )
    }
}


private fun YearMonth.coerceIn(minimum: YearMonth, maximum: YearMonth): YearMonth = when {
    this < minimum -> minimum
    this > maximum -> maximum
    else -> this
}

