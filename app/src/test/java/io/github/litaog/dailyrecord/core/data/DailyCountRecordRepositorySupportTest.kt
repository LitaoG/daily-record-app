package io.github.litaog.dailyrecord.core.data

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
    fun detailDeleteIdsAreChunkedBelowSqliteVariableLimit() {
        assertEquals(emptyList<List<String>>(), chunkDetailIdsForDelete(emptyList()))
        assertEquals(listOf(listOf("a")), chunkDetailIdsForDelete(listOf("a")))

        val ids = (1..1000).map { "detail-$it" }
        val chunks = chunkDetailIdsForDelete(ids)

        assertEquals(listOf(400, 400, 200), chunks.map { it.size })
        assertEquals(ids, chunks.flatten())
        assertTrue(chunks.all { it.size <= 999 })
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
