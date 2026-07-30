package io.github.litaog.dailyrecord.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import io.github.litaog.dailyrecord.ui.DailyCountEntry
import io.github.litaog.dailyrecord.ui.HandBrewModuleSpec
import io.github.litaog.dailyrecord.ui.RecordModule
import io.github.litaog.dailyrecord.ui.RecordModuleUiSpec
import io.github.litaog.dailyrecord.ui.asDailyCountEntry
import io.github.litaog.dailyrecord.ui.components.ChevronIcon
import io.github.litaog.dailyrecord.ui.components.RecordModuleSelector
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextMuted
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSpacing
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.RecordVisualState
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

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
    earliestMonth: YearMonth = YearMonth.of(1970, 1),
    onModuleSelected: (RecordModule) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val monthRecords = records.filter { YearMonth.from(it.localDate) == month }
    val recordsByDate = monthRecords.associateBy { it.localDate }
    val totalCount = monthRecords.sumOf { it.count.toLong() }
    val recordedDays = monthRecords.count { it.count > 0 }
    val averagePerRecordedDay = if (recordedDays == 0) 0.0 else totalCount.toDouble() / recordedDays
    val gridDates = calendarGridDates(month)
    val canGoPrevious = month > earliestMonth
    val canGoNext = month < YearMonth.from(today)
    val fontScale = LocalDensity.current.fontScale
    val largeText = fontScale >= 1.4f
    val dayCellHeight = if (largeText) 76.dp else 48.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("calendar_screen"),
    ) {
        val horizontalPadding = if (maxWidth < 376.dp) 12.dp else DailyRecordSpacing.ScreenHorizontal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = horizontalPadding,
                    vertical = 12.dp,
                ),
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
                averagePerRecordedDay = averagePerRecordedDay,
                largeText = largeText,
            )
            Row(Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { weekday ->
                    Text(
                        text = weekday,
                        modifier = Modifier.weight(1f),
                        color = DailyRecordTextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Column {
                gridDates.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            if (date == null) {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(dayCellHeight),
                                )
                            } else {
                                CalendarDayCell(
                                    date = date,
                                    earliestDate = earliestMonth.atDay(1),
                                    today = today,
                                    focused = date == focusedDate,
                                    record = recordsByDate[date],
                                    moduleSpec = moduleSpec,
                                    cellHeight = dayCellHeight,
                                    largeText = largeText,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onDateSelected(date) },
                                )
                            }
                        }
                    }
                }
            }
            Text(
                text = "点击日期记录",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .semantics {
                        contentDescription = "点击日期记录${moduleSpec.semanticCountLabel}次数"
                    },
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(2.dp))
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
    averagePerRecordedDay: Double,
    largeText: Boolean,
) {
    val summary = "本月 $totalCount 次 · $recordedDays 天"
    val average = String.format(Locale.SIMPLIFIED_CHINESE, "%.1f次/天", averagePerRecordedDay)

    if (largeText) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("calendar_month_summary")
                .semantics { contentDescription = "$summary，记录日均 $average" },
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = summary,
                color = DailyRecordText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = average,
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 32.dp)
                .testTag("calendar_month_summary")
                .semantics { contentDescription = "$summary，记录日均 $average" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = summary,
                color = DailyRecordText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = average,
                color = DailyRecordTextSecondary,
                style = MaterialTheme.typography.labelMedium,
            )
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
            .heightIn(min = if (largeText) 108.dp else 48.dp),
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
                color = DailyRecordText,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = if (largeText) 2 else 1,
            )
        }
        MonthArrow(forward = true, description = "下个月", enabled = canGoNext, onClick = onNextMonth)
        Box(
            modifier = Modifier
                .padding(start = 6.dp)
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClick = onToday)
                .semantics { role = Role.Button; contentDescription = "回到今天" },
            contentAlignment = Alignment.Center,
        ) {
            Text("今天", color = colors.primary, style = MaterialTheme.typography.labelMedium)
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
    val visualState = when {
        unsupported || future -> RecordVisualState.Disabled
        record == null -> RecordVisualState.Unset
        count == 0 -> RecordVisualState.ExplicitZero
        count == 1 -> RecordVisualState.One
        count == 2 -> RecordVisualState.Two
        else -> RecordVisualState.ThreePlus
    }
    val visualColors = moduleSpec.colors.colorsFor(visualState)
    val background = visualColors.background
    val contentColor = visualColors.content
    val recordStatus = when {
        unsupported -> "不可用"
        future || record == null -> null
        count == 0 -> null
        count == 1 -> "1次"
        count == 2 -> "2次"
        count in 3..8 -> "${count}次"
        else -> "9+次"
    }
    val visibleStatus = when {
        date == today && record == null -> "今"
        else -> recordStatus
    }
    val semanticStatus = when {
        unsupported -> "超出支持范围，不可记录"
        future -> "未来日期，不可记录"
        record == null -> "未填写"
        count == 0 -> "明确记录 0 次${moduleSpec.semanticCountLabel}"
        else -> "${moduleSpec.semanticCountLabel} $count 次"
    }
    val borderColor = when {
        focused -> moduleSpec.colors.strong
        date == today -> moduleSpec.colors.primary
        else -> visualColors.outline
    }
    val showOutline = focused || date == today || visualState == RecordVisualState.ExplicitZero
    val borderWidth = if (focused || date == today) 2.dp else 1.dp
    val cellShape = RoundedCornerShape(9.dp)
    val stateDescription = when {
        date == today -> "$semanticStatus，今天"
        else -> semanticStatus
    }

    Box(
        modifier = modifier
            .height(cellHeight)
            .testTag("calendar_day_$date")
            .clip(cellShape)
            .clickable(enabled = !future && !unsupported, role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Button
                selected = focused
                contentDescription = date.year.toString() + "年" + date.monthValue + "月" +
                    date.dayOfMonth + "日，" + stateDescription + if (focused) "，已选择" else ""
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
