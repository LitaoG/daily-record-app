package io.github.litaog.dailyrecord.core.sync

import java.math.BigDecimal

/** Raised when a cloud record or detail cannot be interpreted. */
internal class MalformedRemoteRecordException(
    cause: Throwable,
) : IllegalArgumentException("Cloud record is malformed", cause)

/**
 * Parses a Firestore detail index without allowing Number.toInt() truncation or overflow.
 * Firestore normally returns a Long, but accepting other integral Number implementations keeps
 * the boundary safe for emulator fixtures and future serializers too.
 */
internal fun parseRemoteOccurrenceIndex(value: Any?, fieldName: String): Int {
    val number = value as? Number
        ?: throw IllegalArgumentException("$fieldName must be a number")
    val integer = try {
        BigDecimal(number.toString()).toBigIntegerExact()
    } catch (error: ArithmeticException) {
        throw IllegalArgumentException("$fieldName must be an integer", error)
    } catch (error: NumberFormatException) {
        throw IllegalArgumentException("$fieldName must be a finite integer", error)
    }
    require(
        integer.signum() > 0 &&
            integer <= BigDecimal.valueOf(Int.MAX_VALUE.toLong()).toBigInteger(),
    ) {
        "$fieldName is out of range"
    }
    return integer.toInt()
}

internal fun requireUniqueRemoteDetailIdentity(
    ids: List<String>,
    occurrenceIndexes: List<Int>,
    label: String,
) {
    require(ids.toSet().size == ids.size) {
        "$label detail ids must be unique"
    }
    require(occurrenceIndexes.toSet().size == occurrenceIndexes.size) {
        "$label detail occurrenceIndexes must be unique"
    }
}
