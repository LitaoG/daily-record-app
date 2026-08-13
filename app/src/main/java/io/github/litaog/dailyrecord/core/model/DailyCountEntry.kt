package io.github.litaog.dailyrecord.core.model

import java.time.LocalDate

/** One date's aggregated count, used by statistics and calendar projections. */
data class DailyCountEntry(
    val localDate: LocalDate,
    val count: Int,
) {
    init {
        require(count >= 0) { "Daily count must be non-negative." }
    }
}
