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
        assertNull(previousPeriodAnchor(StatisticsPeriod.Week, earliest, earliest))
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
    fun utcDateConversionRoundTripsWithoutTimezoneShift() {
        listOf(earliest, LocalDate.of(2024, 2, 29), today).forEach { date ->
            assertEquals(date, utcDateMillisToLocalDate(date.toUtcDateMillis()))
        }
    }

    @Test
    fun jumpButtonLabelMatchesSelectionGranularity() {
        assertEquals("跳转到此日", navigationJumpLabel(DateNavigationSelection.Date))
        assertEquals("跳转到此月", navigationJumpLabel(DateNavigationSelection.Month))
        assertEquals("跳转到此年", navigationJumpLabel(DateNavigationSelection.Year))
    }

    @Test
    fun wheelDateClampsMonthDayAndSupportedRange() {
        assertEquals(
            LocalDate.of(2025, 2, 28),
            clampWheelDate(
                year = 2025,
                month = 2,
                day = 31,
                earliestDate = earliest,
                latestDate = today,
            ),
        )
        assertEquals(
            earliest,
            clampWheelDate(
                year = 1969,
                month = 12,
                day = 31,
                earliestDate = earliest,
                latestDate = today,
            ),
        )
        assertEquals(
            today,
            clampWheelDate(
                year = 2026,
                month = 12,
                day = 31,
                earliestDate = earliest,
                latestDate = today,
            ),
        )
    }
}
