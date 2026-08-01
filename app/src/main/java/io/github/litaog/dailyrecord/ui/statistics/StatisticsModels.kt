package io.github.litaog.dailyrecord.ui.statistics

import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.ui.DailyCountEntry
import io.github.litaog.dailyrecord.ui.asDailyCountEntry
import io.github.litaog.dailyrecord.ui.components.StatisticsPeriod
import java.time.LocalDate
import java.time.YearMonth

data class StatisticsSummary(
    val totalCount: Long,
    val recordedDays: Int,
) {
    val average: Double
        get() = if (recordedDays == 0) 0.0 else totalCount.toDouble() / recordedDays
}

/** One real calendar date in the selected month. Grid padding is not represented here. */
data class StatisticsDay(
    val date: LocalDate,
    val count: Long?,
    val recorded: Boolean,
    val future: Boolean,
)

data class MonthStatistics(
    val month: YearMonth,
    val leadingEmptyCells: Int,
    val days: List<StatisticsDay>,
) {
    init {
        require(leadingEmptyCells in 0..6) { "Month grid offset must be a weekday offset." }
        require(days.size == month.lengthOfMonth()) { "Month statistics must contain every real date exactly once." }
    }

    val gridCellCount: Int
        get() = ((leadingEmptyCells + days.size + 6) / 7) * 7
}

data class YearMonthStatistics(
    val month: YearMonth,
    val count: Long?,
    val recordedDays: Int?,
    val recorded: Boolean,
    val future: Boolean,
    val inProgress: Boolean,
) {
    val complete: Boolean
        get() = !future && !inProgress
}

data class QuarterStatistics(
    val quarter: Int,
    val totalCount: Long,
)

data class YearStatistics(
    val months: List<YearMonthStatistics>,
    val quarters: List<QuarterStatistics>,
    val monthlyAverage: Double,
    val maximumMonths: List<YearMonthStatistics>,
    val minimumMonths: List<YearMonthStatistics>,
) {
    val rankedMonthCount: Int
        get() = months.count { it.complete && it.recorded }
}

data class StatisticsDetail(
    val label: String,
    val count: Long?,
    val days: Int?,
    val future: Boolean = false,
    val recorded: Boolean = true,
)

data class StatisticsUiModel(
    val title: String,
    val status: String,
    val summary: StatisticsSummary,
    val detailsTitle: String,
    val details: List<StatisticsDetail>,
    val month: MonthStatistics? = null,
    val year: YearStatistics? = null,
)

fun buildStatistics(
    period: StatisticsPeriod,
    anchorDate: LocalDate,
    today: LocalDate,
    records: List<HandBrewRecord>,
): StatisticsUiModel = buildDailyCountStatistics(
    period = period,
    anchorDate = anchorDate,
    today = today,
    records = records.map(HandBrewRecord::asDailyCountEntry),
)

internal fun buildDailyCountStatistics(
    period: StatisticsPeriod,
    anchorDate: LocalDate,
    today: LocalDate,
    records: List<DailyCountEntry>,
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
    records: List<DailyCountEntry>,
): StatisticsUiModel {
    val start = anchorDate.minusDays((anchorDate.dayOfWeek.value - 1).toLong())
    val end = start.plusDays(6)
    val rangeRecords = records.filter { it.localDate in start..end }
    val details = (0L..6L).map { offset ->
        val date = start.plusDays(offset)
        if (date > today) {
            StatisticsDetail(
                label = weekdayName(date) + " " + date.dayOfMonth + "日",
                count = null,
                days = null,
                future = true,
                recorded = false,
            )
        } else {
            val record = rangeRecords.firstOrNull { it.localDate == date }
            StatisticsDetail(
                label = weekdayName(date) + " " + date.dayOfMonth + "日",
                count = record?.count?.toLong() ?: 0L,
                days = if ((record?.count ?: 0) > 0) 1 else 0,
                recorded = record != null,
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
    records: List<DailyCountEntry>,
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
                add(
                    StatisticsDetail(
                        label = monthWeekLabel(index, bucketStart, bucketEnd),
                        count = null,
                        days = null,
                        future = true,
                        recorded = false,
                    ),
                )
            } else {
                val bucketRecords = rangeRecords.filter { it.localDate in bucketStart..minOf(bucketEnd, today) }
                val summary = summaryOf(bucketRecords)
                add(
                    StatisticsDetail(
                        label = monthWeekLabel(index, bucketStart, bucketEnd),
                        count = summary.totalCount,
                        days = summary.recordedDays,
                        recorded = bucketRecords.isNotEmpty(),
                    ),
                )
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
        month = buildMonthStatistics(month, today, rangeRecords),
    )
}

private fun buildYear(
    anchorDate: LocalDate,
    today: LocalDate,
    records: List<DailyCountEntry>,
): StatisticsUiModel {
    val start = LocalDate.of(anchorDate.year, 1, 1)
    val end = LocalDate.of(anchorDate.year, 12, 31)
    val rangeRecords = records.filter { it.localDate in start..end }
    val details = (1..12).map { monthNumber ->
        val month = YearMonth.of(anchorDate.year, monthNumber)
        if (month.atDay(1) > today) {
            StatisticsDetail(
                label = monthNumber.toString() + "月",
                count = null,
                days = null,
                future = true,
                recorded = false,
            )
        } else {
            val monthRecords = rangeRecords.filter { YearMonth.from(it.localDate) == month }
            val summary = summaryOf(monthRecords)
            StatisticsDetail(
                label = monthNumber.toString() + "月",
                count = summary.totalCount,
                days = summary.recordedDays,
                recorded = monthRecords.isNotEmpty(),
            )
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
        year = buildYearStatistics(anchorDate.year, today, rangeRecords),
    )
}

private fun buildMonthStatistics(
    month: YearMonth,
    today: LocalDate,
    records: List<DailyCountEntry>,
): MonthStatistics {
    val recordsByDate = records.associateBy { it.localDate }
    val days = (1..month.lengthOfMonth()).map { dayOfMonth ->
        val date = month.atDay(dayOfMonth)
        val future = date > today
        val record = recordsByDate[date]
        StatisticsDay(
            date = date,
            count = if (future || record == null) null else record.count.toLong(),
            recorded = !future && record != null,
            future = future,
        )
    }
    return MonthStatistics(
        month = month,
        leadingEmptyCells = month.atDay(1).dayOfWeek.value - 1,
        days = days,
    )
}

private fun buildYearStatistics(
    year: Int,
    today: LocalDate,
    records: List<DailyCountEntry>,
): YearStatistics {
    val months = (1..12).map { monthNumber ->
        val month = YearMonth.of(year, monthNumber)
        val future = month.atDay(1) > today
        val inProgress = !future && month.atEndOfMonth() >= today
        val monthRecords = records.filter { it.localDate in month.atDay(1)..month.atEndOfMonth() }
        val recorded = !future && monthRecords.isNotEmpty()
        YearMonthStatistics(
            month = month,
            count = if (recorded) monthRecords.sumOf { it.count.toLong() } else null,
            recordedDays = if (recorded) monthRecords.count { it.count > 0 } else null,
            recorded = recorded,
            future = future,
            inProgress = inProgress,
        )
    }
    val elapsedMonthCount = when {
        year < today.year -> 12
        year == today.year -> today.monthValue
        else -> 0
    }
    val totalCount = months.sumOf { it.count ?: 0L }
    val rankableMonths = months.filter { it.complete && it.recorded }
    val maximumValue = rankableMonths.maxOfOrNull { it.count ?: 0L }
    val minimumValue = rankableMonths.minOfOrNull { it.count ?: 0L }
    return YearStatistics(
        months = months,
        quarters = (1..4).map { quarter ->
            QuarterStatistics(
                quarter = quarter,
                totalCount = months
                    .filter { ((it.month.monthValue - 1) / 3) + 1 == quarter }
                    .sumOf { it.count ?: 0L },
            )
        },
        monthlyAverage = if (elapsedMonthCount == 0) 0.0 else totalCount.toDouble() / elapsedMonthCount,
        maximumMonths = maximumValue?.let { value -> rankableMonths.filter { it.count == value } }.orEmpty(),
        minimumMonths = minimumValue?.let { value -> rankableMonths.filter { it.count == value } }.orEmpty(),
    )
}

private fun buildAll(today: LocalDate, records: List<DailyCountEntry>): StatisticsUiModel {
    val years = records.map { it.localDate.year }.distinct().sortedDescending()
    val details = years.map { year ->
        val yearRecords = records.filter { it.localDate.year == year }
        val summary = summaryOf(yearRecords)
        StatisticsDetail(year.toString() + "年", summary.totalCount, summary.recordedDays)
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

private fun summaryOf(records: List<DailyCountEntry>) = StatisticsSummary(
    totalCount = records.sumOf { it.count.toLong() },
    recordedDays = records.count { it.count > 0 },
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
