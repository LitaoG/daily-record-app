package io.github.litaog.dailyrecord.core.database

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class DatabaseConvertersTest {
    private val converters = DatabaseConverters()

    @Test
    fun instantRoundTripPreservesValue() {
        val instant = Instant.ofEpochMilli(1_700_000_000_000L)
        assertEquals(instant, converters.epochMillisecondsToInstant(converters.instantToEpochMilliseconds(instant)))
        assertNull(converters.instantToEpochMilliseconds(null))
        assertNull(converters.epochMillisecondsToInstant(null))
    }

    @Test
    fun instantBoundariesRoundTrip() {
        assertEquals(Instant.EPOCH, converters.epochMillisecondsToInstant(0L))
        assertEquals(0L, converters.instantToEpochMilliseconds(Instant.EPOCH))
        assertEquals(
            Instant.ofEpochMilli(Long.MAX_VALUE),
            converters.epochMillisecondsToInstant(Long.MAX_VALUE),
        )
    }

    @Test
    fun localDateRoundTripPreservesValue() {
        val date = LocalDate.of(2026, 8, 16)
        assertEquals(date, converters.isoToLocalDate(converters.localDateToIso(date)))
        assertNull(converters.localDateToIso(null))
        assertNull(converters.isoToLocalDate(null))
    }

    @Test
    fun isoToLocalDateRejectsGarbage() {
        assertThrows(java.time.format.DateTimeParseException::class.java) {
            converters.isoToLocalDate("garbage")
        }
    }

    @Test
    fun localTimeRoundTripPreservesValue() {
        val time = LocalTime.of(23, 59)
        assertEquals(time, converters.isoToLocalTime(converters.localTimeToIso(time)))
        assertNull(converters.localTimeToIso(null))
        assertNull(converters.isoToLocalTime(null))
    }

    @Test
    fun isoToLocalTimeParsesSecondsAndNanos() {
        assertEquals(LocalTime.of(22, 30, 30), converters.isoToLocalTime("22:30:30"))
        assertEquals(LocalTime.of(22, 30, 30, 500_000_000), converters.isoToLocalTime("22:30:30.5"))
    }
}