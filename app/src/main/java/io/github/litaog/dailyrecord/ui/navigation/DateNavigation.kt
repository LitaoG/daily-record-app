package io.github.litaog.dailyrecord.ui.navigation

import io.github.litaog.dailyrecord.ui.components.StatisticsPeriod
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

internal fun LocalDate.toUtcDateMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun utcDateMillisToLocalDate(value: Long): LocalDate =
    Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC).toLocalDate()

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
        StatisticsPeriod.Week -> anchorDate.minusWeeks(1)
            .takeIf { it >= earliestDate }
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
