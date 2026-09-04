package io.github.litaog.dailyrecord.core.common

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    @Test
    fun minuteOfDayBoundaries() {
        assertEquals(0, LocalTime.of(0, 0).toMinutesOfDay())
        assertEquals(1439, LocalTime.of(23, 59).toMinutesOfDay())
    }

    @Test
    fun hoursRollOverAtSixtyMinutes() {
        assertEquals(60, LocalTime.of(1, 0).toMinutesOfDay())
        assertEquals(719, LocalTime.of(11, 59).toMinutesOfDay())
    }

    @Test
    fun formatsAlwaysTwoDigits() {
        assertEquals("00:00", formatMinutesOfDay(0))
        assertEquals("01:00", formatMinutesOfDay(60))
        assertEquals("23:59", formatMinutesOfDay(1439))
        assertEquals("12:05", formatMinutesOfDay(725))
    }

    @Test
    fun roundTripMinutesToTime() {
        for (minutes in intArrayOf(0, 1, 59, 60, 725, 1439)) {
            assertEquals(minutes, minutes.toLocalTime().toMinutesOfDay())
        }
    }
}