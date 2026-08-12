package io.github.litaog.dailyrecord.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordFactoryTest {
    private val date = LocalDate.of(2026, 7, 17)
    private val now = Instant.parse("2026-07-17T10:00:00Z")

    @Test
    fun createRecordKeepsIdAndCreatedAtOnEditAndGeneratesNewIdOnCreate() {
        val existing = HandBrewRecord(
            id = "existing-id",
            localDate = date,
            brewCount = 3,
            createdAt = now.minusSeconds(3600),
            updatedAt = now.minusSeconds(60),
        )

        val edited = RecordFactory.createHandBrewRecord(
            existing = existing,
            localDate = date,
            count = 5,
            createdAt = existing.createdAt,
            updatedAt = now,
        )
        assertEquals("existing-id", edited.id)
        assertEquals(existing.createdAt, edited.createdAt)
        assertEquals(5, edited.brewCount)

        val fresh = RecordFactory.createHandBrewRecord(
            existing = null,
            localDate = date,
            count = 1,
            createdAt = now,
            updatedAt = now,
        )
        assertNotEquals("existing-id", fresh.id)
        assertTrue(fresh.id.isNotBlank())
        assertEquals(now, fresh.createdAt)
    }

    @Test
    fun createDetailKeepsSlotIdentityAndRewritesNothingOnEdit() {
        val existing = HandBrewRecordDetail(
            id = "slot-id",
            localDate = date,
            occurrenceIndex = 2,
            startTime = LocalTime.of(21, 0),
            endTime = LocalTime.of(21, 30),
            feeling = "relaxed",
            createdAt = now.minusSeconds(3600),
            updatedAt = now.minusSeconds(60),
        )

        val edited = RecordFactory.createHandBrewDetail(
            existing = existing,
            localDate = date,
            occurrenceIndex = 2,
            startTime = LocalTime.of(22, 0),
            endTime = LocalTime.of(22, 30),
            feeling = "calm",
            createdAt = existing.createdAt,
            updatedAt = now,
        )
        assertEquals("slot-id", edited.id)
        assertEquals(existing.createdAt, edited.createdAt)
        assertEquals(LocalTime.of(22, 0), edited.startTime)
        assertEquals("calm", edited.feeling)

        val fresh = RecordFactory.createHandBrewDetail(
            existing = null,
            localDate = date,
            occurrenceIndex = 1,
            startTime = null,
            endTime = null,
            feeling = "",
            createdAt = now,
            updatedAt = now,
        )
        assertNotEquals("slot-id", fresh.id)
        assertEquals(1, fresh.occurrenceIndex)
    }

    @Test
    fun sexModuleMirrorsRecordAndDetailIdentityRules() {
        val existing = SexRecord(
            id = "sex-id",
            localDate = date,
            sexCount = 2,
            createdAt = now.minusSeconds(3600),
            updatedAt = now.minusSeconds(60),
        )
        val edited = RecordFactory.createSexRecord(
            existing = existing,
            localDate = date,
            count = 4,
            createdAt = existing.createdAt,
            updatedAt = now,
        )
        assertEquals("sex-id", edited.id)
        assertEquals(4, edited.sexCount)

        val detail = RecordFactory.createSexDetail(
            existing = null,
            localDate = date,
            occurrenceIndex = 3,
            startTime = LocalTime.of(20, 0),
            endTime = null,
            feeling = "tired",
            createdAt = now,
            updatedAt = now,
        )
        assertTrue(detail.id.isNotBlank())
        assertEquals(3, detail.occurrenceIndex)
    }

    @Test
    fun resolveUpdatedAtIsStrictlyMonotonicAndSurvivesClockRollback() {
        val earlier = Instant.parse("2026-07-17T09:00:00Z")
        assertEquals(now, RecordFactory.resolveUpdatedAt(null, now))

        val first = RecordFactory.resolveUpdatedAt(earlier, now)
        assertEquals(now, first)

        // Same clock tick: strictly after the previous value.
        val second = RecordFactory.resolveUpdatedAt(first, now)
        assertTrue(second.isAfter(first))
        assertEquals(first.plusMillis(1), second)

        // Clock rolled back before the previous write: stays strictly after it.
        val rolledBackNow = Instant.parse("2026-07-17T08:00:00Z")
        val third = RecordFactory.resolveUpdatedAt(second, rolledBackNow)
        assertTrue(third.isAfter(second))
        assertEquals(second.plusMillis(1), third)
    }

    @Test
    fun nextRecordTimestampAdvancesWithoutOverflow() {
        val timestamp = Instant.parse("2026-01-01T00:00:00Z")

        assertEquals(timestamp.plusMillis(1), timestamp.nextRecordTimestamp())
        assertEquals(Instant.MAX, Instant.MAX.nextRecordTimestamp())
        assertSame(Instant.MAX, Instant.MAX.nextRecordTimestamp())
    }
}
