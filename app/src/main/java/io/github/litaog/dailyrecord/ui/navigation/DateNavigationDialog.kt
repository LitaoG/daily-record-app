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
import androidx.compose.foundation.layout.width
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
import io.github.litaog.dailyrecord.ui.components.HandBrewDialog
import io.github.litaog.dailyrecord.ui.components.OutlineActionButton
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.theme.Ink500
import io.github.litaog.dailyrecord.ui.theme.Ink700
import io.github.litaog.dailyrecord.ui.theme.Ink900
import io.github.litaog.dailyrecord.ui.theme.Neutral300
import io.github.litaog.dailyrecord.ui.theme.Paper0
import io.github.litaog.dailyrecord.ui.theme.Paper100
import io.github.litaog.dailyrecord.ui.theme.Terracotta400
import io.github.litaog.dailyrecord.ui.theme.Terracotta500
import io.github.litaog.dailyrecord.ui.theme.White
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private enum class NavigationMode { Date, Year }

@Composable
internal fun DateNavigationDialog(
    initialDate: LocalDate,
    earliestDate: LocalDate,
    latestDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val boundedInitial = initialDate.coerceIn(earliestDate, latestDate)
    var selectedDate by remember(initialDate, earliestDate, latestDate) { mutableStateOf(boundedInitial) }
    var displayedMonth by remember(initialDate, earliestDate, latestDate) {
        mutableStateOf(YearMonth.from(boundedInitial))
    }
    var mode by remember { mutableStateOf(NavigationMode.Date) }

    HandBrewDialog(
        title = "快速跳转",
        subtitle = "直接选择年份和日期，不必逐月翻找",
        testTag = "date_navigation_dialog",
        onDismissRequest = onDismiss,
    ) {
        SelectedDateSummary(selectedDate)
        Spacer(Modifier.height(16.dp))

        if (mode == NavigationMode.Year) {
            YearPicker(
                selectedYear = displayedMonth.year,
                years = (earliestDate.year..latestDate.year).toList(),
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

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlineActionButton("取消", onDismiss, Modifier.weight(1f))
            PrimaryActionButton(
                label = "跳转到此日",
                onClick = { onDateSelected(selectedDate) },
                modifier = Modifier.weight(1.35f),
            )
        }
    }
}

@Composable
private fun SelectedDateSummary(date: LocalDate) {
    val locale = Locale.SIMPLIFIED_CHINESE
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
    val largeText = LocalDensity.current.fontScale >= 1.4f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Terracotta400.copy(alpha = .22f))
            .border(1.dp, Terracotta400, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text("已选择", color = Ink700, style = MaterialTheme.typography.labelSmall)
        if (largeText) {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("yyyy年M月d日")),
                color = Ink900,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = weekday,
                color = Ink700,
                style = MaterialTheme.typography.labelMedium,
            )
        } else {
            Text(
                text = date.format(DateTimeFormatter.ofPattern("yyyy年M月d日")) + " · " + weekday,
                color = Ink900,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun MonthPicker(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    earliestDate: LocalDate,
    latestDate: LocalDate,
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
                    contentDescription = "切换年份，当前${displayedMonth.year}年"
                }
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "${displayedMonth.year}年 ${displayedMonth.monthValue}月",
                color = Ink900,
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
        listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                color = Ink500,
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
            .background(if (enabled) Paper100 else Color.Transparent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = if (forward) "快速跳转下个月" else "快速跳转上个月"
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        ChevronIcon(forward = forward, color = if (enabled) Ink900 else Neutral300)
    }
}

@Composable
private fun DateCell(
    date: LocalDate,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.SIMPLIFIED_CHINESE)
    Box(
        modifier = modifier
            .height(44.dp)
            .padding(2.dp)
            .clip(CircleShape)
            .background(if (selected) Terracotta500 else Color.Transparent)
            .border(if (selected) 0.dp else 1.dp, if (selected) Color.Transparent else Paper0, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                this.selected = selected
                contentDescription = "${date.year}年${date.monthValue}月${date.dayOfMonth}日，$weekday"
                if (!enabled) disabled()
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = when {
                selected -> White
                enabled -> Ink900
                else -> Neutral300
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
                .semantics { contentDescription = "返回日期选择" },
            contentAlignment = Alignment.Center,
        ) {
            ChevronIcon(forward = false)
        }
        Text(
            "选择年份",
            color = Ink900,
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
                    .background(if (selected) Terracotta500 else Paper100)
                    .border(1.dp, if (selected) Terracotta500 else Neutral300, RoundedCornerShape(14.dp))
                    .clickable(role = Role.Button) { onYearSelected(year) }
                    .semantics {
                        role = Role.Button
                        this.selected = selected
                        contentDescription = "选择${year}年"
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "$year",
                    color = if (selected) White else Ink900,
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
