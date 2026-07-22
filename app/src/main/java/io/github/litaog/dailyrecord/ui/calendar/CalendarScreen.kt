package io.github.litaog.dailyrecord.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.ui.components.ChevronIcon
import io.github.litaog.dailyrecord.ui.theme.Ink500
import io.github.litaog.dailyrecord.ui.theme.Ink700
import io.github.litaog.dailyrecord.ui.theme.Ink900
import io.github.litaog.dailyrecord.ui.theme.Neutral300
import io.github.litaog.dailyrecord.ui.theme.Paper0
import io.github.litaog.dailyrecord.ui.theme.Paper100
import io.github.litaog.dailyrecord.ui.theme.Terracotta400
import io.github.litaog.dailyrecord.ui.theme.Terracotta500
import io.github.litaog.dailyrecord.ui.theme.Terracotta600
import io.github.litaog.dailyrecord.ui.theme.White
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    month: YearMonth,
    focusedDate: LocalDate,
    today: LocalDate,
    records: List<HandBrewRecord>,
    modifier: Modifier = Modifier,
    earliestMonth: YearMonth = YearMonth.of(1970, 1),
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val monthRecords = records.filter { YearMonth.from(it.localDate) == month }
    val recordsByDate = monthRecords.associateBy { it.localDate }
    val totalCount = monthRecords.sumOf { it.brewCount.toLong() }
    val brewDays = monthRecords.count { it.brewCount > 0 }
    val first = month.atDay(1)
    val gridStart = first.minusDays((first.dayOfWeek.value - 1).toLong())
    val gridDates = List(42) { gridStart.plusDays(it.toLong()) }
    val canGoPrevious = month > earliestMonth
    val canGoNext = month < YearMonth.from(today)
    val fontScale = LocalDensity.current.fontScale
    val largeText = fontScale >= 1.4f
    val dayCellHeight = if (largeText) 68.dp else 56.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("calendar_screen")
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MonthHeader(
            month = month,
            canGoPrevious = canGoPrevious,
            canGoNext = canGoNext,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onToday = onToday,
            onOpenDatePicker = onOpenDatePicker,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (largeText) 56.dp else 44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Terracotta400)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("本月手冲", color = Ink900, style = MaterialTheme.typography.labelMedium)
            Text(
                "$totalCount 次 · $brewDays 天",
                color = Ink900,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { weekday ->
                Text(
                    text = weekday,
                    modifier = Modifier.weight(1f),
                    color = Ink500,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            gridDates.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    week.forEach { date ->
                        CalendarDayCell(
                            date = date,
                            month = month,
                            earliestDate = earliestMonth.atDay(1),
                            today = today,
                            focused = date == focusedDate,
                            record = recordsByDate[date],
                            cellHeight = dayCellHeight,
                            largeText = largeText,
                            modifier = Modifier.weight(1f),
                            onClick = { onDateSelected(date) },
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Paper0)
                .border(1.dp, Neutral300, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text("选择日期记录手冲次数", color = Ink900, style = MaterialTheme.typography.labelLarge)
            Text(
                "0 次表示明确没冲；清除记录才回到未填写",
                color = Ink700,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Spacer(Modifier.height(2.dp))
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
) {
    val fontScale = LocalDensity.current.fontScale
    val largeText = fontScale >= 1.4f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (largeText) 108.dp else 52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonthArrow(forward = false, description = "上个月", enabled = canGoPrevious, onClick = onPreviousMonth)
        Column(
            modifier = Modifier
                .weight(1f)
                .sizeIn(minHeight = 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(role = Role.Button, onClick = onOpenDatePicker)
                .semantics {
                    role = Role.Button
                    contentDescription = "选择年份和日期，当前${month.year}年${month.monthValue}月"
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (largeText) {
                    month.year.toString() + "年\n" + month.monthValue + "月"
                } else {
                    month.year.toString() + "年 " + month.monthValue + "月"
                },
                color = Ink900,
                style = if (largeText) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
                maxLines = if (largeText) 2 else 1,
            )
            Text(
                text = "点此快速跳转",
                color = Terracotta500,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
            )
        }
        MonthArrow(forward = true, description = "下个月", enabled = canGoNext, onClick = onNextMonth)
        Box(
            modifier = Modifier
                .padding(start = 6.dp)
                .clip(CircleShape)
                .background(Paper0)
                .border(1.dp, Neutral300, CircleShape)
                .clickable(role = Role.Button, onClick = onToday)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics { role = Role.Button; contentDescription = "回到今天" },
            contentAlignment = Alignment.Center,
        ) {
            Text("今天", color = Terracotta500, style = MaterialTheme.typography.labelSmall)
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
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else .3f)
            .semantics { role = Role.Button; contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        ChevronIcon(forward = forward, color = Ink900)
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    month: YearMonth,
    earliestDate: LocalDate,
    today: LocalDate,
    focused: Boolean,
    record: HandBrewRecord?,
    cellHeight: androidx.compose.ui.unit.Dp,
    largeText: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unsupported = date < earliestDate
    val future = date > today
    val outsideMonth = YearMonth.from(date) != month
    val count = record?.brewCount
    val background = when {
        unsupported || future -> Paper100
        outsideMonth -> Paper100
        record == null -> Paper0
        count == 0 -> Neutral300
        count == 1 -> Terracotta400
        count == 2 -> Terracotta500
        else -> Terracotta600
    }
    val contentColor = when {
        outsideMonth -> Ink700
        (count ?: 0) >= 2 -> White
        else -> Ink900
    }
    val status = when {
        unsupported -> "不可用"
        future -> "未来"
        record == null -> "未填"
        count == 0 -> "0次"
        count == 1 -> "1次"
        count == 2 -> "2次"
        count in 3..8 -> "${count}次"
        else -> "9+次"
    }
    val semanticStatus = when {
        unsupported -> "超出支持范围，不可记录"
        future -> "未来日期，不可记录"
        record == null -> "未填写"
        count == 0 -> "明确记录 0 次手冲"
        else -> "手冲 $count 次"
    }
    val borderColor = when {
        focused -> Terracotta600
        date == today -> Terracotta500
        else -> Neutral300
    }
    val borderWidth = if (focused || date == today) 2.dp else 1.dp

    Column(
        modifier = modifier
            .height(cellHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = !future && !unsupported, role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                selected = focused
                contentDescription = date.year.toString() + "年" + date.monthValue + "月" +
                    date.dayOfMonth + "日，" + semanticStatus + if (focused) "，已选择" else ""
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = if (date == today && count == null && !outsideMonth) Terracotta500 else contentColor,
            style = if (largeText) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = if (date == today && !future && !largeText) "$status·今" else status,
            color = if (date == today && count == null && !outsideMonth) Terracotta500 else contentColor,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}
