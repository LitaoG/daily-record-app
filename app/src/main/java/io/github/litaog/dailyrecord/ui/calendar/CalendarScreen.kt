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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
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
    today: LocalDate,
    records: List<HandBrewRecord>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthRecords = records.filter { YearMonth.from(it.localDate) == month }
    val recordsByDate = monthRecords.associateBy { it.localDate }
    val totalCount = monthRecords.sumOf { it.brewCount }
    val brewDays = monthRecords.count { it.brewCount > 0 }
    val first = month.atDay(1)
    val gridStart = first.minusDays((first.dayOfWeek.value - 1).toLong())
    val gridDates = List(42) { gridStart.plusDays(it.toLong()) }
    val canGoNext = month < YearMonth.from(today)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MonthHeader(month, canGoNext, onPreviousMonth, onNextMonth, onToday)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
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
                            today = today,
                            record = recordsByDate[date],
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
    canGoNext: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonthArrow("‹", "上个月", true, onPreviousMonth)
        Text(
            text = month.year.toString() + "年 " + month.monthValue + "月",
            modifier = Modifier.weight(1f),
            color = Ink900,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
        )
        MonthArrow("›", "下个月", canGoNext, onNextMonth)
        Box(
            modifier = Modifier
                .padding(start = 6.dp)
                .clip(CircleShape)
                .background(Paper0)
                .border(1.dp, Neutral300, CircleShape)
                .clickable(onClick = onToday)
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
    symbol: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else .3f)
            .semantics { role = Role.Button; contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = Ink900, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    month: YearMonth,
    today: LocalDate,
    record: HandBrewRecord?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val future = date > today
    val outsideMonth = YearMonth.from(date) != month
    val count = record?.brewCount
    val background = when {
        future -> Paper100
        record == null -> Paper0
        count == 0 -> Neutral300
        count == 1 -> Terracotta400
        count == 2 -> Terracotta500
        else -> Terracotta600
    }
    val contentColor = if ((count ?: 0) >= 2) White else Ink900
    val status = when {
        future -> "未来"
        record == null -> "未填"
        count == 0 -> "0次"
        count == 1 -> "1次"
        count == 2 -> "2次"
        count in 3..8 -> "${count}次"
        else -> "9+次"
    }
    val semanticStatus = when {
        future -> "未来日期，不可记录"
        record == null -> "未填写"
        count == 0 -> "明确记录 0 次手冲"
        else -> "手冲 $count 次"
    }
    val todayBorder = if (date == today) Terracotta500 else Neutral300

    Column(
        modifier = modifier
            .height(56.dp)
            .alpha(if (outsideMonth) .38f else 1f)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .border(if (date == today) 2.dp else 1.dp, todayBorder, RoundedCornerShape(16.dp))
            .clickable(enabled = !future, onClick = onClick)
            .semantics {
                role = Role.Button
                contentDescription = date.year.toString() + "年" + date.monthValue + "月" +
                    date.dayOfMonth + "日，" + semanticStatus
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = if (date == today && count == null) Terracotta500 else contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = if (date == today && !future) "$status·今" else status,
            color = if (date == today && count == null) Terracotta500 else contentColor,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}
