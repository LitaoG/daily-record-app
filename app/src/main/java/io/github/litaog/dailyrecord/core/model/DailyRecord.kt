package io.github.litaog.dailyrecord.core.model

import java.time.Instant
import java.time.LocalDate

data class DailyRecord(
    val id: String,
    val ownerId: String,
    val activityId: String,
    val localDate: LocalDate,
    val status: RecordStatus,
    val quantity: Long?,
    val note: String?,
    val timezoneId: String,
    val occurredAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
    val revision: Long,
) {
    init {
        require(id.isNotBlank()) { "DailyRecord id must not be blank." }
        require(ownerId.isNotBlank()) { "DailyRecord ownerId must not be blank." }
        require(activityId.isNotBlank()) { "DailyRecord activityId must not be blank." }
        require(timezoneId.isNotBlank()) { "DailyRecord timezoneId must not be blank." }
        require(quantity == null || quantity >= 0) { "DailyRecord quantity must be non-negative." }
        require(status !in setOf(RecordStatus.UNSET, RecordStatus.SKIPPED) || quantity == null) {
            "UNSET and SKIPPED records cannot carry a quantity."
        }
        require(revision >= 0) { "DailyRecord revision must be non-negative." }
    }
}
