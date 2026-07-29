package io.github.litaog.dailyrecord.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * One aggregate hand-brew record per local date.
 *
 * A positive [brewCount] means hand-brew occurred. Zero means the user explicitly
 * recorded that no hand-brew occurred. The absence of a row means the date is unset.
 */
data class HandBrewRecord(
    override val id: String,
    override val localDate: LocalDate,
    val brewCount: Int,
    override val createdAt: Instant,
    override val updatedAt: Instant,
) : DailyCountRecord {
    init {
        require(id.isNotBlank()) { "HandBrewRecord id must not be blank." }
        require(brewCount >= 0) { "HandBrewRecord brewCount must be non-negative." }
        require(!updatedAt.isBefore(createdAt)) {
            "HandBrewRecord updatedAt must not be before createdAt."
        }
    }

    val wasBrewed: Boolean
        get() = brewCount > 0

    override val count: Int
        get() = brewCount
}
