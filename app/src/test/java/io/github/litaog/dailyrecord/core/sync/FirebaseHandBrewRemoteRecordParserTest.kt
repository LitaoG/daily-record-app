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

    @Test
    fun isolatesMalformedRecordAndKeepsValidRecordsInSameSnapshot() {
        val parsed = parseRemoteHandBrewRecords(
            listOf(
                DATE to validValues(),
                "2026-07-19" to validValues(),
                "2026-07-20" to null,
            ),
        )

        assertEquals(1, parsed.records.size)
        assertEquals(LocalDate.parse(DATE), parsed.records.single().localDate)
        assertEquals(2, parsed.rejectedRecordCount)
    }

    @Test
    fun parsesOptionalDetailsAndAcceptsNumericOccurrenceIndexes() {
        val record = parseRemoteHandBrewRecord(
            documentId = DATE,
            values = validValues() + (
                "details" to listOf(
                    mapOf(
                        "id" to "detail-id",
                        "occurrenceIndex" to 1,
                        "startTime" to "22:30",
                        "endTime" to "22:45",
                        "feeling" to "平静",
                    ),
                )
            ),
        )

        assertEquals(1, record.details.single().occurrenceIndex)
        assertEquals("平静", record.details.single().feeling)
    }

    @Test
    fun rejectsDetailsWithMoreThanOneHundredVisibleCharacters() {
        assertThrows(IllegalArgumentException::class.java) {
            parseRemoteHandBrewRecord(
                documentId = DATE,
                values = validValues() + (
                    "details" to listOf(
                        mapOf(
                            "id" to "detail-id",
                            "occurrenceIndex" to 1L,
                            "startTime" to null,
                            "endTime" to null,
                            "feeling" to "字".repeat(101),
                        ),
                    )
                ),
            )
        }
    }

    @Test
    fun rejectsFractionalOrOverflowingOccurrenceIndexesBeforeConversion() {
        assertThrows(IllegalArgumentException::class.java) {
            parseRemoteHandBrewRecord(
                documentId = DATE,
                values = validValues() + ("details" to listOf(detail("detail-fraction", 1.9))),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseRemoteHandBrewRecord(
                documentId = DATE,
                values = validValues() + (
                    "details" to listOf(detail("detail-overflow", 4_294_967_297L))
                ),
            )
        }
    }

    @Test
    fun rejectsDuplicateDetailIdsAndOccurrenceIndexes() {
        assertThrows(IllegalArgumentException::class.java) {
            parseRemoteHandBrewRecord(
                documentId = DATE,
                values = validValues() + (
                    "details" to listOf(
                        detail("detail-a", 1),
                        detail("detail-b", 1),
                    )
                ),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseRemoteHandBrewRecord(
                documentId = DATE,
                values = validValues() + (
                    "details" to listOf(
                        detail("same-id", 1),
                        detail("same-id", 2),
                    )
                ),
            )
        }
    }

    private fun detail(id: String, occurrenceIndex: Any): Map<String, Any?> = mapOf(
        "id" to id,
        "occurrenceIndex" to occurrenceIndex,
        "startTime" to null,
        "endTime" to null,
        "feeling" to "平静",
    )

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
