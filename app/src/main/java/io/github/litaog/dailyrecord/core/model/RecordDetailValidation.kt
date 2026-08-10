package io.github.litaog.dailyrecord.core.model

import java.time.LocalTime

internal const val MAX_RECORD_DETAIL_FEELING_CHARACTERS = 100

/** Counts user-visible Unicode code points instead of UTF-16 code units. */
internal fun String.visibleCharacterCount(): Int = codePointCount(0, length)

internal fun String.truncateVisibleCharacters(maxCharacters: Int): String {
    require(maxCharacters >= 0) { "maxCharacters must be non-negative." }
    if (visibleCharacterCount() <= maxCharacters) return this
    return substring(0, offsetByCodePoints(0, maxCharacters))
}

internal fun requireValidRecordDetail(
    occurrenceIndex: Int,
    startTime: LocalTime?,
    endTime: LocalTime?,
    feeling: String,
) {
    require(occurrenceIndex >= 1) { "Record detail occurrenceIndex must be positive." }
    require(startTime == null || startTime.second == 0 && startTime.nano == 0) {
        "Record detail startTime must have minute precision."
    }
    require(endTime == null || endTime.second == 0 && endTime.nano == 0) {
        "Record detail endTime must have minute precision."
    }
    require(startTime == null || endTime == null || !endTime.isBefore(startTime)) {
        "Record detail endTime must not be before startTime."
    }
    require(feeling.visibleCharacterCount() <= MAX_RECORD_DETAIL_FEELING_CHARACTERS) {
        "Record detail feeling must be at most $MAX_RECORD_DETAIL_FEELING_CHARACTERS visible characters."
    }
}
