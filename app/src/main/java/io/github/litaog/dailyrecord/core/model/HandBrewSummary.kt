package io.github.litaog.dailyrecord.core.model

data class HandBrewSummary(
    val totalCount: Long,
    val brewDays: Int,
) {
    init {
        require(totalCount >= 0) { "HandBrewSummary totalCount must be non-negative." }
        require(brewDays >= 0) { "HandBrewSummary brewDays must be non-negative." }
    }

    val averagePerBrewDay: Double
        get() = if (brewDays == 0) 0.0 else totalCount.toDouble() / brewDays
}
