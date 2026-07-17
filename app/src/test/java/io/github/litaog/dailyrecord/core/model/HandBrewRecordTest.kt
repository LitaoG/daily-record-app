package io.github.litaog.dailyrecord.core.model

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HandBrewRecordTest {
    private val now = Instant.parse("2026-07-16T00:00:00Z")

    @Test
    fun positiveCountMeansBrewedAndZeroMeansExplicitNoBrew() {
        assertTrue(record(count = 2).wasBrewed)
        assertFalse(record(count = 0).wasBrewed)
    }

    @Test
    fun negativeCountIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { record(count = -1) }
    }

    @Test
    fun summaryAverageUsesOnlyBrewDaysAndHandlesEmptyData() {
        assertEquals(1.7, HandBrewSummary(128, 74).averagePerBrewDay, 0.05)
        assertEquals(0.0, HandBrewSummary(0, 0).averagePerBrewDay, 0.0)
    }

    private fun record(count: Int) = HandBrewRecord(
        id = "record-1",
        localDate = LocalDate.of(2026, 7, 16),
        brewCount = count,
        createdAt = now,
        updatedAt = now,
    )
}
