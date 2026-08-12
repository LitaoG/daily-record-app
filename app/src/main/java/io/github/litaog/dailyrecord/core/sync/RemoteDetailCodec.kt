package io.github.litaog.dailyrecord.core.sync

import io.github.litaog.dailyrecord.core.model.MAX_RECORD_DETAIL_FEELING_CHARACTERS
import io.github.litaog.dailyrecord.core.model.visibleCharacterCount
import java.time.LocalTime
import java.time.format.DateTimeParseException

/**
 * Wire codec for the occurrence-detail array shared by both daily-count
 * modules. The parsed values are returned through [ParsedRemoteDetail]; each
 * module maps them onto its own typed remote detail.
 */
internal data class ParsedRemoteDetail(
    val id: String,
    val occurrenceIndex: Int,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val feeling: String,
)

internal fun detailToMap(
    id: String,
    occurrenceIndex: Int,
    startTime: LocalTime?,
    endTime: LocalTime?,
    feeling: String,
): Map<String, Any?> = mapOf(
    DETAIL_ID to id,
    DETAIL_OCCURRENCE_INDEX to occurrenceIndex.toLong(),
    DETAIL_START_TIME to startTime?.toString(),
    DETAIL_END_TIME to endTime?.toString(),
    DETAIL_FEELING to feeling,
)

internal fun parseRemoteDetail(value: Any?, label: String): ParsedRemoteDetail {
    val map = value as? Map<*, *> ?: throw MalformedRemoteRecordException(
        IllegalArgumentException("$label detail must be a map"),
    )
    val id = map[DETAIL_ID] as? String
    val occurrenceIndex = parseRemoteOccurrenceIndex(
        map[DETAIL_OCCURRENCE_INDEX],
        "$label detail occurrenceIndex",
    )
    val startTime = parseDetailTime(map[DETAIL_START_TIME])
    val endTime = parseDetailTime(map[DETAIL_END_TIME])
    val feeling = map[DETAIL_FEELING] as? String
    require(!id.isNullOrBlank()) { "$label detail id is missing" }
    require(startTime == null || endTime == null || !endTime.isBefore(startTime)) {
        "$label detail endTime is before startTime"
    }
    require(feeling != null && feeling.visibleCharacterCount() <= MAX_RECORD_DETAIL_FEELING_CHARACTERS) {
        "$label detail feeling is invalid"
    }
    return ParsedRemoteDetail(id!!, occurrenceIndex, startTime, endTime, feeling!!)
}

internal fun parseDetailTime(value: Any?): LocalTime? {
    if (value == null) return null
    val text = value as? String ?: throw IllegalArgumentException("detail time must be a string")
    return try {
        LocalTime.parse(text).also {
            require(it.second == 0 && it.nano == 0) { "detail time must have minute precision" }
        }
    } catch (error: DateTimeParseException) {
        throw IllegalArgumentException("detail time is invalid", error)
    }
}
