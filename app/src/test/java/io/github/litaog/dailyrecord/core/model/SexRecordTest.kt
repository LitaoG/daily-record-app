package io.github.litaog.dailyrecord.core.model

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SexRecordTest {
    private val now = Instant.parse("2026-07-16T00:00:00Z")

    @Test
    fun positiveCountMeansOccurredAndZeroMeansExplicitNoSex() {
        assertTrue(record(count = 2).occurred)
        assertFalse(record(count = 0).occurred)
    }

    @Test
    fun negativeCountIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { record(count = -1) }
    }

    private fun record(count: Int) = SexRecord(
        id = "sex-record-1",
        localDate = LocalDate.of(2026, 7, 16),
        sexCount = count,
        createdAt = now,
        updatedAt = now,
    )
}
