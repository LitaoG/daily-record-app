package io.github.litaog.dailyrecord.ui.navigation

import io.github.litaog.dailyrecord.ui.components.StatisticsPeriod
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DateNavigationTest {
    private val earliest = LocalDate.of(1970, 1, 1)
    private val today = LocalDate.of(2026, 7, 17)

    @Test
    fun monthShiftClampsDayForLeapAndCommonYears() {
        assertEquals(
            LocalDate.of(2024, 2, 29),
            shiftMonthAnchor(LocalDate.of(2024, 1, 31), 1, earliest, today),
        )
        assertEquals(
            LocalDate.of(2025, 2, 28),
            shiftMonthAnchor(LocalDate.of(2025, 1, 31), 1, earliest, today),
        )
    }

    @Test
    fun periodNavigationStopsAtSupportedAndCurrentBoundaries() {
        assertNull(previousPeriodAnchor(StatisticsPeriod.Month, earliest, earliest))
        assertNull(previousPeriodAnchor(StatisticsPeriod.Year, earliest, earliest))
        assertNull(nextPeriodAnchor(StatisticsPeriod.Month, today, today))
        assertNull(nextPeriodAnchor(StatisticsPeriod.Year, today, today))
        assertNull(nextPeriodAnchor(StatisticsPeriod.Week, today, today))
    }

    @Test
    fun navigationIntoCurrentMonthAndYearClampsToToday() {
        assertEquals(
            today,
            nextPeriodAnchor(StatisticsPeriod.Month, LocalDate.of(2026, 6, 30), today),
        )
        assertEquals(
            today,
            nextPeriodAnchor(StatisticsPeriod.Year, LocalDate.of(2025, 12, 31), today),
        )
    }

    @Test
    fun previousYearClampsLeapDay() {
        assertEquals(
            LocalDate.of(2023, 2, 28),
            previousPeriodAnchor(StatisticsPeriod.Year, LocalDate.of(2024, 2, 29), earliest),
        )
    }

    @Test
    fun utcDateConversionRoundTripsWithoutTimezoneShift() {
        listOf(earliest, LocalDate.of(2024, 2, 29), today).forEach { date ->
            assertEquals(date, utcDateMillisToLocalDate(date.toUtcDateMillis()))
        }
    }
}
