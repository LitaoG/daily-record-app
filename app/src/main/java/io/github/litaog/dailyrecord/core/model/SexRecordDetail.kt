package io.github.litaog.dailyrecord.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Optional, per-occurrence context for one sex count on a local date. */
data class SexRecordDetail(
    val id: String,
    val localDate: LocalDate,
    val occurrenceIndex: Int,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val feeling: String = "",
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
) {
    init {
        require(id.isNotBlank()) { "SexRecordDetail id must not be blank." }
        requireValidRecordDetail(occurrenceIndex, startTime, endTime, feeling)
        require(!updatedAt.isBefore(createdAt)) {
            "SexRecordDetail updatedAt must not be before createdAt."
        }
    }
}
