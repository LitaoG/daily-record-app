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
    override val id: String,
    override val localDate: LocalDate,
    val sexCount: Int,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : DailyCountRecord {
    init {
        require(id.isNotBlank()) { "SexRecord id must not be blank." }
        require(sexCount >= 0) { "SexRecord sexCount must be non-negative." }
        require(!updatedAt.isBefore(createdAt)) {
            "SexRecord updatedAt must not be before createdAt."
        }
    }

    val occurred: Boolean
        get() = sexCount > 0

    override val count: Int
        get() = sexCount
}
