package io.github.litaog.dailyrecord.core.statistics

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatisticsPeriodAnchorsTest {
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
        assertNull(previousPeriodAnchor(StatisticsPeriod.Week, earliest, earliest))
        assertNull(
            previousPeriodAnchor(StatisticsPeriod.Week, LocalDate.of(1970, 1, 2), earliest),
        )
        assertEquals(
            earliest,
            previousPeriodAnchor(StatisticsPeriod.Week, LocalDate.of(1970, 1, 5), earliest),
        )
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
    fun crossYearWeekAndMonthNavigation() {
        assertEquals(
            LocalDate.of(2025, 12, 29),
            previousPeriodAnchor(StatisticsPeriod.Week, LocalDate.of(2026, 1, 5), earliest),
        )
        assertEquals(
            LocalDate.of(2025, 12, 15),
            previousPeriodAnchor(StatisticsPeriod.Month, LocalDate.of(2026, 1, 15), earliest),
        )
        assertEquals(
            today,
            nextPeriodAnchor(StatisticsPeriod.Week, LocalDate.of(2026, 7, 10), today),
        )
        assertEquals(
            LocalDate.of(2026, 7, 7),
            nextPeriodAnchor(StatisticsPeriod.Week, LocalDate.of(2026, 6, 30), today),
        )
    }

    @Test
    fun nextMonthClampsToValidDayOfTargetMonth() {
        assertEquals(
            LocalDate.of(2026, 2, 28),
            nextPeriodAnchor(StatisticsPeriod.Month, LocalDate.of(2026, 1, 31), today),
        )
        assertNull(nextPeriodAnchor(StatisticsPeriod.All, today, today))
        assertNull(previousPeriodAnchor(StatisticsPeriod.All, today, earliest))
    }

    @Test
    fun shiftMonthAnchorHandlesNegativeOffsetsAndCoercion() {
        assertEquals(
            LocalDate.of(2025, 6, 30),
            shiftMonthAnchor(LocalDate.of(2025, 7, 31), -1, earliest, today),
        )
        assertEquals(
            earliest,
            shiftMonthAnchor(LocalDate.of(1970, 2, 1), -1, earliest, today),
        )
        assertEquals(
            today,
            shiftMonthAnchor(today, 3, earliest, today),
        )
    }
}
