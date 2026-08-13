package io.github.litaog.dailyrecord.core.statistics

import java.time.LocalDate
import java.time.YearMonth

internal fun shiftMonthAnchor(
    anchorDate: LocalDate,
    months: Long,
    earliestDate: LocalDate,
    latestDate: LocalDate,
): LocalDate {
    val targetMonth = YearMonth.from(anchorDate).plusMonths(months)
    val target = targetMonth.atDay(minOf(anchorDate.dayOfMonth, targetMonth.lengthOfMonth()))
    return target.coerceIn(earliestDate, latestDate)
}

internal fun previousPeriodAnchor(
    period: StatisticsPeriod,
    anchorDate: LocalDate,
    earliestDate: LocalDate,
): LocalDate? {
    return when (period) {
        StatisticsPeriod.Week -> anchorDate.minusWeeks(1).let { candidate ->
            // The first supported week may be a clipped partial week. Once the
            // displayed week is already that clipped partial week, there is no
            // earlier period to navigate to.  Comparing only the selected day
            // with the lower bound is insufficient: Jan 2-4, 1970 still belong
            // to the clipped Dec 29-Jan 4 week, so returning `earliestDate`
            // would enable an arrow that appears to do nothing.
            val currentWeekStart = anchorDate.minusDays((anchorDate.dayOfWeek.value - 1).toLong())
            if (currentWeekStart <= earliestDate && candidate < earliestDate) null
            else candidate.coerceAtLeast(earliestDate)
        }
        StatisticsPeriod.Month -> {
            val targetMonth = YearMonth.from(anchorDate).minusMonths(1)
            if (targetMonth < YearMonth.from(earliestDate)) return null
            targetMonth
                .atDay(minOf(anchorDate.dayOfMonth, targetMonth.lengthOfMonth()))
                .coerceAtLeast(earliestDate)
        }
        StatisticsPeriod.Year -> {
            val year = anchorDate.year - 1
            if (year < earliestDate.year) return null
            LocalDate.of(
                year,
                anchorDate.month,
                minOf(anchorDate.dayOfMonth, YearMonth.of(year, anchorDate.month).lengthOfMonth()),
            ).coerceAtLeast(earliestDate)
        }
        StatisticsPeriod.All -> return null
    }
}

internal fun nextPeriodAnchor(
    period: StatisticsPeriod,
    anchorDate: LocalDate,
    latestDate: LocalDate,
): LocalDate? {
    return when (period) {
        StatisticsPeriod.Week -> anchorDate.plusWeeks(1)
            .takeIf { it <= latestDate }
        StatisticsPeriod.Month -> {
            val targetMonth = YearMonth.from(anchorDate).plusMonths(1)
            if (targetMonth > YearMonth.from(latestDate)) return null
            targetMonth
                .atDay(minOf(anchorDate.dayOfMonth, targetMonth.lengthOfMonth()))
                .coerceAtMost(latestDate)
        }
        StatisticsPeriod.Year -> {
            val year = anchorDate.year + 1
            if (year > latestDate.year) return null
            LocalDate.of(
                year,
                anchorDate.month,
                minOf(anchorDate.dayOfMonth, YearMonth.of(year, anchorDate.month).lengthOfMonth()),
            ).coerceAtMost(latestDate)
        }
        StatisticsPeriod.All -> return null
    }
}
