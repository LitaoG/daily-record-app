package io.github.litaog.dailyrecord.core.model

import java.time.Instant
import java.time.LocalDate

/**
 * Shared contract for modules that store one non-negative count per local date.
 *
 * Module-specific models keep their own domain names and storage boundaries while the
 * presentation layer can reuse date/count behavior without branching on a module type.
 */
interface DailyCountRecord {
    val id: String
    val localDate: LocalDate
    val count: Int
    val createdAt: Instant
    val updatedAt: Instant
}
