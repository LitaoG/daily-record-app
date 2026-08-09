package io.github.litaog.dailyrecord.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Optional, per-occurrence context for one hand-brew count on a local date. */
data class HandBrewRecordDetail(
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
        require(id.isNotBlank()) { "HandBrewRecordDetail id must not be blank." }
        requireValidRecordDetail(occurrenceIndex, startTime, endTime, feeling)
        require(!updatedAt.isBefore(createdAt)) {
            "HandBrewRecordDetail updatedAt must not be before createdAt."
        }
    }
}
