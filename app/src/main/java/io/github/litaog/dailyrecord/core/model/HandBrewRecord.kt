package io.github.litaog.dailyrecord.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * The only record type in the product: one aggregate hand-brew record per local date.
 *
 * A positive [brewCount] means hand-brew occurred. Zero means the user explicitly
 * recorded that no hand-brew occurred. The absence of a row means the date is unset.
 */
data class HandBrewRecord(
    val id: String,
    val localDate: LocalDate,
    val brewCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(id.isNotBlank()) { "HandBrewRecord id must not be blank." }
        require(brewCount >= 0) { "HandBrewRecord brewCount must be non-negative." }
        require(!updatedAt.isBefore(createdAt)) {
            "HandBrewRecord updatedAt must not be before createdAt."
        }
    }

    val wasBrewed: Boolean
        get() = brewCount > 0

}
