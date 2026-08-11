package io.github.litaog.dailyrecord.ui.statistics

import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.common.AppCopy
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

data class MonthDayStatistics(
    val date: LocalDate,
    val count: Long?,
    val recorded: Boolean,
    val future: Boolean,
)

data class MonthDayExtreme(
    val count: Long,
)

data class MonthStatistics(
    val month: YearMonth,
    val days: List<MonthDayStatistics>,
    val explicitZeroDays: Int,
    val oneCountDays: Int,
    val twoCountDays: Int,
    val threePlusCountDays: Int,
    val unfilledElapsedDays: Int,
    val futureDays: Int,
    val maximum: MonthDayExtreme?,
    val minimumPositive: MonthDayExtreme?,
) {
    init {
        require(days.size == month.lengthOfMonth()) {
            "Month statistics must contain every date in the selected month."
        }
    }

    val savedDays: Int
        get() = explicitZeroDays + oneCountDays + twoCountDays + threePlusCountDays
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
    /**
     * Week position in the Monday-based week (week view only); null falls
     * back to the caller-supplied list index.
     */
    val calendarIndex: Int? = null,
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
    earliestDate: LocalDate = LocalDate.of(1970, 1, 1),
): StatisticsUiModel = buildDailyCountStatistics(
    period = period,
    anchorDate = anchorDate,
    today = today,
    records = records.map(HandBrewRecord::asDailyCountEntry),
    earliestDate = earliestDate,
)

internal fun buildDailyCountStatistics(
    period: StatisticsPeriod,
    anchorDate: LocalDate,
    today: LocalDate,
    records: List<DailyCountEntry>,
    earliestDate: LocalDate = LocalDate.of(1970, 1, 1),
): StatisticsUiModel {
    val completedRecords = records.filter { it.localDate <= today }
    val safeAnchor = anchorDate.coerceIn(earliestDate, today)
    return when (period) {
        StatisticsPeriod.Week -> buildWeek(safeAnchor, today, earliestDate, completedRecords)
        StatisticsPeriod.Month -> buildMonth(safeAnchor, today, completedRecords)
        StatisticsPeriod.Year -> buildYear(safeAnchor, today, completedRecords)
        StatisticsPeriod.All -> buildAll(today, completedRecords)
    }
}

private fun buildWeek(
    anchorDate: LocalDate,
    today: LocalDate,
    earliestDate: LocalDate,
    records: List<DailyCountEntry>,
): StatisticsUiModel {
    val weekStart = anchorDate.minusDays((anchorDate.dayOfWeek.value - 1).toLong())
    val end = weekStart.plusDays(6)
    // The week still starts on Monday for statistics; only the display range is
    // clipped so the earliest supported week never shows pre-1970 dates.
    val start = maxOf(weekStart, earliestDate)
    val rangeRecords = records.filter { it.localDate in start..end }
    val details = (0L..6L).mapNotNull { offset ->
        val date = weekStart.plusDays(offset)
        if (date < earliestDate) return@mapNotNull null
        if (date > today) {
            StatisticsDetail(
                label = AppCopy.Statistics.weekdayDateLabel(weekdayName(date), date),
                count = null,
                days = null,
                future = true,
                recorded = false,
                calendarIndex = offset.toInt(),
            )
        } else {
            val record = rangeRecords.firstOrNull { it.localDate == date }
            StatisticsDetail(
                label = AppCopy.Statistics.weekdayDateLabel(weekdayName(date), date),
                count = record?.count?.toLong() ?: 0L,
                days = if ((record?.count ?: 0) > 0) 1 else 0,
                recorded = record != null,
                calendarIndex = offset.toInt(),
            )
        }
    }
    return StatisticsUiModel(
        title = dateRangeTitle(start, end),
        status = AppCopy.Statistics.periodStatus(end, today),
        summary = summaryOf(rangeRecords),
        detailsTitle = AppCopy.Statistics.dailyDetails,
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
    val recordsByDate = rangeRecords.associateBy(DailyCountEntry::localDate)
    val days = (1..month.lengthOfMonth()).map { dayOfMonth ->
        val date = month.atDay(dayOfMonth)
        val future = date > today
        val record = recordsByDate[date].takeUnless { future }
        MonthDayStatistics(
            date = date,
            count = record?.count?.toLong(),
            recorded = record != null,
            future = future,
        )
    }
    val positiveDays = days.filter { it.recorded && (it.count ?: 0L) > 0L }
    val maximumCount = positiveDays.maxOfOrNull { requireNotNull(it.count) }
    val minimumCount = positiveDays.minOfOrNull { requireNotNull(it.count) }
    val monthStatistics = MonthStatistics(
        month = month,
        days = days,
        explicitZeroDays = days.count { it.recorded && it.count == 0L },
        oneCountDays = days.count { it.recorded && it.count == 1L },
        twoCountDays = days.count { it.recorded && it.count == 2L },
        threePlusCountDays = days.count { it.recorded && (it.count ?: 0L) >= 3L },
        unfilledElapsedDays = days.count { !it.recorded && !it.future },
        futureDays = days.count(MonthDayStatistics::future),
        maximum = maximumCount?.let { count ->
            MonthDayExtreme(count = count)
        },
        minimumPositive = minimumCount?.let { count ->
            MonthDayExtreme(count = count)
        },
    )
    val details = days.map { day ->
        StatisticsDetail(
            label = AppCopy.Statistics.dayLabel(day.date.dayOfMonth),
            count = day.count,
            days = day.count?.let { if (it > 0L) 1 else 0 },
            future = day.future,
            recorded = day.recorded,
        )
    }
    return StatisticsUiModel(
        title = AppCopy.Statistics.monthTitle(month.year, month.monthValue),
        status = AppCopy.Statistics.periodStatus(end, today),
        summary = summaryOf(rangeRecords),
        detailsTitle = AppCopy.Statistics.dailyCount,
        details = details,
        month = monthStatistics,
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
                label = AppCopy.Statistics.monthLabel(monthNumber),
                count = null,
                days = null,
                future = true,
                recorded = false,
            )
        } else {
            val monthRecords = rangeRecords.filter { YearMonth.from(it.localDate) == month }
            val summary = summaryOf(monthRecords)
            StatisticsDetail(
                label = AppCopy.Statistics.monthLabel(monthNumber),
                count = summary.totalCount,
                days = summary.recordedDays,
                recorded = monthRecords.isNotEmpty(),
            )
        }
    }
    return StatisticsUiModel(
        title = AppCopy.Statistics.yearTitle(anchorDate.year),
        status = AppCopy.Statistics.yearStatus(end, today),
        summary = summaryOf(rangeRecords),
        detailsTitle = AppCopy.Statistics.monthlyDetails,
        details = details,
        year = buildYearStatistics(anchorDate.year, today, rangeRecords),
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
        // A month is only complete after its last calendar day has elapsed.  In
        // particular, 12/31 remains the in-progress month until the clock moves
        // to 1/1, at which point December is eligible for extrema ranking.
        val complete = month.atEndOfMonth() < today
        val inProgress = !future && !complete
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
        StatisticsDetail(AppCopy.Statistics.yearTitle(year), summary.totalCount, summary.recordedDays)
    }
    val status = AppCopy.Statistics.historyStatus(records.minOfOrNull { it.localDate }, today)
    return StatisticsUiModel(
        title = AppCopy.Statistics.allHistory,
        status = status,
        summary = summaryOf(records),
        detailsTitle = AppCopy.Statistics.yearlyDetails,
        details = details,
    )
}

private fun summaryOf(records: List<DailyCountEntry>) = StatisticsSummary(
    totalCount = records.sumOf { it.count.toLong() },
    recordedDays = records.count { it.count > 0 },
)

private fun weekdayName(date: LocalDate): String = AppCopy.weekdayName(date.dayOfWeek.value)

private fun dateRangeTitle(start: LocalDate, end: LocalDate): String =
    AppCopy.Statistics.dateRangeTitle(start, end)
