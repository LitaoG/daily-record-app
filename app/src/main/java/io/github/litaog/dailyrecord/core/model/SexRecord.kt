package io.github.litaog.dailyrecord.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * One aggregate sex record per local date.
 *
 * A positive [sexCount] means sex occurred. Zero means the user explicitly
 * recorded that it did not occur. The absence of a row means the date is unset.
 */
data class SexRecord(
    val id: String,
    val localDate: LocalDate,
    val sexCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "SexRecord id must not be blank." }
        require(sexCount >= 0) { "SexRecord sexCount must be non-negative." }
        require(!updatedAt.isBefore(createdAt)) {
            "SexRecord updatedAt must not be before createdAt."
        }
    }

    val occurred: Boolean
        get() = sexCount > 0
}
