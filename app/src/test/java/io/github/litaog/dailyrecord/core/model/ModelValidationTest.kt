package io.github.litaog.dailyrecord.core.model

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertThrows
import org.junit.Test

class ModelValidationTest {
    private val now = Instant.parse("2026-07-16T00:00:00Z")

    @Test
    fun activityRequiresConfigurationInsteadOfHardCodedTypes() {
        assertThrows(IllegalArgumentException::class.java) {
            Activity(
                id = "activity-1",
                ownerId = "owner-1",
                name = " ",
                iconKey = "flight",
                colorArgb = 0xFFCC5A35.toInt(),
                measurementType = MeasurementType.COUNT,
                unit = "次",
                sortOrder = 0,
                isArchived = false,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                revision = 0,
            )
        }
    }

    @Test
    fun unsetAndSkippedCannotCarryQuantity() {
        assertThrows(IllegalArgumentException::class.java) {
            DailyRecord(
                id = "record-1",
                ownerId = "owner-1",
                activityId = "activity-1",
                localDate = LocalDate.of(2026, 7, 16),
                status = RecordStatus.UNSET,
                quantity = 1,
                note = null,
                timezoneId = "Asia/Shanghai",
                occurredAt = now,
                createdAt = now,
                updatedAt = now,
                deletedAt = null,
                revision = 0,
            )
        }
    }
}
