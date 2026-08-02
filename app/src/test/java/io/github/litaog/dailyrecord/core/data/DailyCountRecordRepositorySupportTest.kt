package io.github.litaog.dailyrecord.core.data

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DailyCountRecordRepositorySupportTest {
    @Test
    fun recordRangesAreNonEmptyHalfOpenIntervals() {
        val start = LocalDate.of(2026, 1, 1)
        requireValidRecordRange(start, start.plusDays(1))

        try {
            requireValidRecordRange(start, start)
            fail("Expected an empty range to be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }

    @Test
    fun nextRecordTimestampAdvancesWithoutOverflow() {
        val timestamp = Instant.parse("2026-01-01T00:00:00Z")

        assertEquals(timestamp.plusMillis(1), timestamp.nextRecordTimestamp())
        assertEquals(Instant.MAX, Instant.MAX.nextRecordTimestamp())
    }

    @Test
    fun localChangeCallbackDoesNotTurnBestEffortSchedulingIntoFailure() {
        var invoked = false

        notifyLocalChangeSafely {
            invoked = true
            error("Scheduling is best effort")
        }

        assertTrue(invoked)
    }
}
