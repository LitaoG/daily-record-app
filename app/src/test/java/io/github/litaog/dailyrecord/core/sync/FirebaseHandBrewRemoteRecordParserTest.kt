package io.github.litaog.dailyrecord.core.sync

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FirebaseHandBrewRemoteRecordParserTest {
    @Test
    fun acceptsMaximumSupportedTimestamp() {
        val record = parseRemoteHandBrewRecord(
            documentId = DATE,
            values = validValues(
                createdAtMillis = MAX_SUPPORTED_EPOCH_MILLIS,
                updatedAtMillis = MAX_SUPPORTED_EPOCH_MILLIS,
            ),
        )

        assertEquals(LocalDate.parse(DATE), record.localDate)
        assertEquals(Instant.ofEpochMilli(MAX_SUPPORTED_EPOCH_MILLIS), record.createdAt)
        assertEquals(Instant.ofEpochMilli(MAX_SUPPORTED_EPOCH_MILLIS), record.clientUpdatedAt)
    }

    @Test
    fun rejectsCreatedTimestampAboveSupportedCalendarRange() {
        assertThrows(IllegalArgumentException::class.java) {
            parseRemoteHandBrewRecord(
                documentId = DATE,
                values = validValues(
                    createdAtMillis = MAX_SUPPORTED_EPOCH_MILLIS + 1,
                    updatedAtMillis = MAX_SUPPORTED_EPOCH_MILLIS + 1,
                ),
            )
        }
    }

    @Test
    fun rejectsUpdatedTimestampAboveSupportedCalendarRange() {
        assertThrows(IllegalArgumentException::class.java) {
            parseRemoteHandBrewRecord(
                documentId = DATE,
                values = validValues(updatedAtMillis = MAX_SUPPORTED_EPOCH_MILLIS + 1),
            )
        }
    }

    @Test
    fun rejectsUpdatedTimestampBeforeCreation() {
        assertThrows(IllegalArgumentException::class.java) {
            parseRemoteHandBrewRecord(
                documentId = DATE,
                values = validValues(createdAtMillis = 2, updatedAtMillis = 1),
            )
        }
    }

    @Test
    fun rejectsDocumentIdThatDoesNotMatchDate() {
        assertThrows(IllegalArgumentException::class.java) {
            parseRemoteHandBrewRecord(
                documentId = "2026-07-17",
                values = validValues(),
            )
        }
    }

    private fun validValues(
        createdAtMillis: Long = 1,
        updatedAtMillis: Long = createdAtMillis,
    ): Map<String, Any?> = mapOf(
        "id" to "record-id",
        "localDate" to DATE,
        "brewCount" to 2L,
        "createdAtMillis" to createdAtMillis,
        "clientUpdatedAtMillis" to updatedAtMillis,
        "deleted" to false,
        "revision" to 1L,
    )

    private companion object {
        const val DATE = "2026-07-18"
    }
}
