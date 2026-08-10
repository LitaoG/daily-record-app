package io.github.litaog.dailyrecord.ui.calendar

import io.github.litaog.dailyrecord.ui.theme.RecordVisualState
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarGridTest {
    @Test
    fun dailyCountColorsUseSeparateThreeAndFourPlusLevels() {
        assertEquals(RecordVisualState.Unset, calendarRecordVisualState(false, false, null))
        assertEquals(RecordVisualState.ExplicitZero, calendarRecordVisualState(false, false, 0))
        assertEquals(RecordVisualState.One, calendarRecordVisualState(false, false, 1))
        assertEquals(RecordVisualState.Two, calendarRecordVisualState(false, false, 2))
        assertEquals(RecordVisualState.Three, calendarRecordVisualState(false, false, 3))
        assertEquals(RecordVisualState.FourPlus, calendarRecordVisualState(false, false, 4))
        assertEquals(RecordVisualState.FourPlus, calendarRecordVisualState(false, false, 12))
        assertEquals(RecordVisualState.Disabled, calendarRecordVisualState(false, true, 3))
    }

    @Test
    fun fiveWeekMonthUsesRealDatesWithoutAdjacentMonthFillers() {
        val cells = calendarGridDates(YearMonth.of(2026, 7))

        assertEquals(35, cells.size)
        assertNull(cells[0])
        assertNull(cells[1])
        assertEquals(LocalDate.of(2026, 7, 1), cells[2])
        assertEquals(LocalDate.of(2026, 7, 31), cells[32])
        assertNull(cells[33])
        assertNull(cells[34])
        assertEquals(31, cells.filterNotNull().distinct().size)
    }

    @Test
    fun sixWeekMonthKeepsAllDatesAndOnlyPadsTheTrailingWeek() {
        val cells = calendarGridDates(YearMonth.of(2026, 8))

        assertEquals(42, cells.size)
        assertEquals(5, cells.takeWhile { it == null }.size)
        assertEquals(LocalDate.of(2026, 8, 1), cells[5])
        assertEquals(LocalDate.of(2026, 8, 31), cells[35])
        assertEquals(31, cells.filterNotNull().distinct().size)
    }

    @Test
    fun leapFebruaryContainsFebruary29ExactlyOnce() {
        val cells = calendarGridDates(YearMonth.of(2028, 2))

        assertEquals(35, cells.size)
        assertEquals(29, cells.filterNotNull().size)
        assertEquals(1, cells.count { it == LocalDate.of(2028, 2, 29) })
    }
}
