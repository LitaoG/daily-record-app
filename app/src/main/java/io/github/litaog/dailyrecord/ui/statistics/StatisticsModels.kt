package io.github.litaog.dailyrecord.ui.statistics

import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.ui.components.StatisticsPeriod
import java.time.LocalDate
import java.time.YearMonth

data class StatisticsSummary(
    val totalCount: Int,
    val brewDays: Int,
) {
    val average: Double
        get() = if (brewDays == 0) 0.0 else totalCount.toDouble() / brewDays
}

data class StatisticsDetail(
    val label: String,
    val count: Int?,
    val days: Int?,
) {
    val future: Boolean
        get() = count == null || days == null
}

data class StatisticsUiModel(
    val title: String,
    val status: String,
    val summary: StatisticsSummary,
    val detailsTitle: String,
    val details: List<StatisticsDetail>,
)

fun buildStatistics(
    period: StatisticsPeriod,
    anchorDate: LocalDate,
    today: LocalDate,
    records: List<HandBrewRecord>,
): StatisticsUiModel {
    val completedRecords = records.filter { it.localDate <= today }
    val safeAnchor = anchorDate.coerceAtMost(today)
    return when (period) {
        StatisticsPeriod.Week -> buildWeek(safeAnchor, today, completedRecords)
        StatisticsPeriod.Month -> buildMonth(safeAnchor, today, completedRecords)
        StatisticsPeriod.Year -> buildYear(safeAnchor, today, completedRecords)
        StatisticsPeriod.All -> buildAll(today, completedRecords)
    }
}

private fun buildWeek(
    anchorDate: LocalDate,
    today: LocalDate,
    records: List<HandBrewRecord>,
): StatisticsUiModel {
    val start = anchorDate.minusDays((anchorDate.dayOfWeek.value - 1).toLong())
    val end = start.plusDays(6)
    val rangeRecords = records.filter { it.localDate in start..end }
    val details = (0L..6L).map { offset ->
        val date = start.plusDays(offset)
        if (date > today) {
            StatisticsDetail(weekdayName(date) + " " + date.dayOfMonth + "日", null, null)
        } else {
            val record = rangeRecords.firstOrNull { it.localDate == date }
            StatisticsDetail(
                label = weekdayName(date) + " " + date.dayOfMonth + "日",
                count = record?.brewCount ?: 0,
                days = if ((record?.brewCount ?: 0) > 0) 1 else 0,
            )
        }
    }
    return StatisticsUiModel(
        title = dateRangeTitle(start, end),
        status = if (end < today) "已结束" else "进行中",
        summary = summaryOf(rangeRecords),
        detailsTitle = "每日明细",
        details = details,
    )
}

private fun buildMonth(
    anchorDate: LocalDate,
    today: LocalDate,
    records: List<HandBrewRecord>,
): StatisticsUiModel {
    val month = YearMonth.from(anchorDate)
    val start = month.atDay(1)
    val end = month.atEndOfMonth()
    val rangeRecords = records.filter { it.localDate in start..end }
    val gridStart = start.minusDays((start.dayOfWeek.value - 1).toLong())
    val details = buildList {
        var weekStart = gridStart
        var index = 1
        while (weekStart <= end) {
            val weekEnd = weekStart.plusDays(6)
            val bucketStart = maxOf(weekStart, start)
            val bucketEnd = minOf(weekEnd, end)
            if (bucketStart > today) {
                add(StatisticsDetail(monthWeekLabel(index, bucketStart, bucketEnd), null, null))
            } else {
                val bucketRecords = rangeRecords.filter { it.localDate in bucketStart..minOf(bucketEnd, today) }
                val summary = summaryOf(bucketRecords)
                add(StatisticsDetail(monthWeekLabel(index, bucketStart, bucketEnd), summary.totalCount, summary.brewDays))
            }
            weekStart = weekStart.plusDays(7)
            index += 1
        }
    }
    return StatisticsUiModel(
        title = month.year.toString() + "年 " + month.monthValue + "月",
        status = if (end < today) "已结束" else "进行中",
        summary = summaryOf(rangeRecords),
        detailsTitle = "周明细",
        details = details,
    )
}

private fun buildYear(
    anchorDate: LocalDate,
    today: LocalDate,
    records: List<HandBrewRecord>,
): StatisticsUiModel {
    val start = LocalDate.of(anchorDate.year, 1, 1)
    val end = LocalDate.of(anchorDate.year, 12, 31)
    val rangeRecords = records.filter { it.localDate in start..end }
    val details = (1..12).map { monthNumber ->
        val month = YearMonth.of(anchorDate.year, monthNumber)
        if (month.atDay(1) > today) {
            StatisticsDetail(monthNumber.toString() + "月", null, null)
        } else {
            val monthRecords = rangeRecords.filter { YearMonth.from(it.localDate) == month }
            val summary = summaryOf(monthRecords)
            StatisticsDetail(monthNumber.toString() + "月", summary.totalCount, summary.brewDays)
        }
    }
    return StatisticsUiModel(
        title = anchorDate.year.toString() + "年",
        status = if (anchorDate.year < today.year) {
            "已结束"
        } else {
            "截至 " + today.monthValue + "月" + today.dayOfMonth + "日"
        },
        summary = summaryOf(rangeRecords),
        detailsTitle = "月份明细",
        details = details,
    )
}

private fun buildAll(today: LocalDate, records: List<HandBrewRecord>): StatisticsUiModel {
    val years = records.map { it.localDate.year }.distinct().sortedDescending()
    val details = years.map { year ->
        val yearRecords = records.filter { it.localDate.year == year }
        val summary = summaryOf(yearRecords)
        StatisticsDetail(year.toString() + "年", summary.totalCount, summary.brewDays)
    }
    val status = records.minOfOrNull { it.localDate }?.let { first ->
        first.year.toString() + "." + first.monthValue.toString().padStart(2, '0') +
            "–" + today.year + "." + today.monthValue.toString().padStart(2, '0')
    } ?: "暂无记录"
    return StatisticsUiModel(
        title = "全部历史",
        status = status,
        summary = summaryOf(records),
        detailsTitle = "年度明细",
        details = details,
    )
}

private fun summaryOf(records: List<HandBrewRecord>) = StatisticsSummary(
    totalCount = records.sumOf { it.brewCount },
    brewDays = records.count { it.brewCount > 0 },
)

private fun weekdayName(date: LocalDate): String = when (date.dayOfWeek.value) {
    1 -> "周一"
    2 -> "周二"
    3 -> "周三"
    4 -> "周四"
    5 -> "周五"
    6 -> "周六"
    else -> "周日"
}

private fun monthWeekLabel(
    index: Int,
    start: LocalDate,
    end: LocalDate,
): String = "第${index}周 ${start.dayOfMonth}–${end.dayOfMonth}日"

private fun dateRangeTitle(start: LocalDate, end: LocalDate): String = if (start.year == end.year) {
    start.year.toString() + "年 " + start.monthValue + "月" + start.dayOfMonth + "日–" +
        end.monthValue + "月" + end.dayOfMonth + "日"
} else {
    start.year.toString() + "年" + start.monthValue + "月" + start.dayOfMonth + "日–" +
        end.year + "年" + end.monthValue + "月" + end.dayOfMonth + "日"
}
