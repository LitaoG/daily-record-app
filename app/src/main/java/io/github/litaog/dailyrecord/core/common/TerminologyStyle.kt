package io.github.litaog.dailyrecord.core.common

/**
 * Visual phrasing style for intimacy modules (ADR-025).
 *
 * [ELEGANT] is the default style using elegant paired terms.
 * [DIRECT] offers direct terminology for users who prefer explicit wording.
 */
enum class TerminologyStyle {
    ELEGANT,
    DIRECT;

    companion object {
        val Elegant get() = ELEGANT
        val Direct get() = DIRECT
    }
}
