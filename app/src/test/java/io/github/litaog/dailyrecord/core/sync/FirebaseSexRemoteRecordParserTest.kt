package io.github.litaog.dailyrecord.core.sync

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FirebaseSexRemoteRecordParserTest {
    @Test
    fun validRecordParsesWithIndependentCountField() {
        val record = parseRemoteSexRecord(DATE, validValues())

        assertEquals(LocalDate.parse(DATE), record.localDate)
        assertEquals(2, record.sexCount)
    }

    @Test
    fun brewCountCannotSubstituteForSexCount() {
        assertThrows(IllegalArgumentException::class.java) {
            parseRemoteSexRecord(
                DATE,
                validValues() - "sexCount" + ("brewCount" to 2L),
            )
        }
    }

    @Test
    fun negativeCountAndMismatchedDocumentIdAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            parseRemoteSexRecord(DATE, validValues() + ("sexCount" to -1L))
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseRemoteSexRecord("2026-07-19", validValues())
        }
    }

    @Test
    fun malformedRecordsAreIsolatedWithinSnapshot() {
        val parsed = parseRemoteSexRecords(
            listOf(
                DATE to validValues(),
                "2026-07-19" to validValues(),
                "2026-07-20" to null,
            ),
        )

        assertEquals(1, parsed.records.size)
        assertEquals(2, parsed.rejectedRecordCount)
    }

    private fun validValues(): Map<String, Any?> = mapOf(
        "id" to "sex-record-id",
        "localDate" to DATE,
        "sexCount" to 2L,
        "createdAtMillis" to 1L,
        "clientUpdatedAtMillis" to 1L,
        "deleted" to false,
        "revision" to 1L,
    )

    private companion object {
        const val DATE = "2026-07-18"
    }
}
