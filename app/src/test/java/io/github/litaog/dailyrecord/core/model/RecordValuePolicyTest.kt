package io.github.litaog.dailyrecord.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RecordValuePolicyTest {
    @Test
    fun countAboveZeroBecomesDone() {
        val value = RecordValuePolicy.normalize(
            measurementType = MeasurementType.COUNT,
            requestedStatus = RecordStatus.MISSED,
            quantity = 3,
        )

        assertEquals(RecordStatus.DONE, value.status)
        assertEquals(3L, value.quantity)
    }

    @Test
    fun countZeroBecomesMissed() {
        val value = RecordValuePolicy.normalize(
            measurementType = MeasurementType.COUNT,
            requestedStatus = RecordStatus.DONE,
            quantity = 0,
        )

        assertEquals(RecordStatus.MISSED, value.status)
        assertEquals(0L, value.quantity)
    }

    @Test
    fun durationUsesTheSameZeroRule() {
        assertEquals(
            NormalizedRecordValue(RecordStatus.DONE, 45),
            RecordValuePolicy.normalize(MeasurementType.DURATION, RecordStatus.DONE, 45),
        )
        assertEquals(
            NormalizedRecordValue(RecordStatus.MISSED, 0),
            RecordValuePolicy.normalize(MeasurementType.DURATION, RecordStatus.DONE, 0),
        )
    }

    @Test
    fun clearAndSkippedNeverCarryQuantity() {
        assertEquals(
            NormalizedRecordValue(RecordStatus.UNSET, null),
            RecordValuePolicy.normalize(MeasurementType.COUNT, RecordStatus.UNSET, null),
        )
        assertEquals(
            NormalizedRecordValue(RecordStatus.SKIPPED, null),
            RecordValuePolicy.normalize(MeasurementType.DURATION, RecordStatus.SKIPPED, null),
        )
    }

    @Test
    fun booleanUsesStatusAsTheBusinessValue() {
        RecordStatus.entries.forEach { status ->
            assertEquals(
                NormalizedRecordValue(status, null),
                RecordValuePolicy.normalize(MeasurementType.BOOLEAN, status, null),
            )
        }
    }

    @Test
    fun invalidQuantitiesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            RecordValuePolicy.normalize(MeasurementType.COUNT, RecordStatus.DONE, -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RecordValuePolicy.normalize(MeasurementType.BOOLEAN, RecordStatus.DONE, 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RecordValuePolicy.normalize(MeasurementType.COUNT, RecordStatus.DONE, null)
        }
    }
}
