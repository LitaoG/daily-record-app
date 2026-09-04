package io.github.litaog.dailyrecord.core.sync

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class RemoteDetailCodecTest {
    @Test
    fun parseDetailTimeAcceptsMinutePrecisionAndNull() {
        assertNull(parseDetailTime(null))
        assertEquals(LocalTime.of(22, 30), parseDetailTime("22:30"))
    }

    @Test
    fun parseDetailTimeRejectsSecondsNanosAndNonStrings() {
        assertThrows(IllegalArgumentException::class.java) { parseDetailTime("22:30:30") }
        assertThrows(IllegalArgumentException::class.java) { parseDetailTime("22:30.5") }
        assertThrows(IllegalArgumentException::class.java) { parseDetailTime(22_30) }
        assertThrows(IllegalArgumentException::class.java) { parseDetailTime("not-a-time") }
    }

    @Test
    fun parseRemoteDetailRejectsNonMapAndInvalidShape() {
        assertThrows(MalformedRemoteRecordException::class.java) {
            parseRemoteDetail("nope", "handBrew")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseRemoteDetail(mapOf("id" to "  "), "handBrew")
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseRemoteDetail(
                mapOf(
                    "id" to "abc",
                    "occurrenceIndex" to 1L,
                    "startTime" to "23:00",
                    "endTime" to "22:00",
                    "feeling" to "ok",
                ),
                "handBrew",
            )
        }
    }

    @Test
    fun parseRemoteDetailAcceptsEmptyFeelingAndOptionalTimes() {
        val parsed = parseRemoteDetail(
            mapOf(
                "id" to "abc",
                "occurrenceIndex" to 3L,
                "startTime" to null,
                "endTime" to null,
                "feeling" to "",
            ),
            "handBrew",
        )
        assertEquals("abc", parsed.id)
        assertEquals(3, parsed.occurrenceIndex)
        assertNull(parsed.startTime)
        assertNull(parsed.endTime)
        assertEquals("", parsed.feeling)
    }

    @Test
    fun detailToMapRoundTripsThroughParseRemoteDetail() {
        val map = detailToMap(
            id = "abc",
            occurrenceIndex = 2,
            startTime = LocalTime.of(9, 5),
            endTime = LocalTime.of(9, 20),
            feeling = "note",
        )
        val parsed = parseRemoteDetail(map, "sex")
        assertEquals("abc", parsed.id)
        assertEquals(2, parsed.occurrenceIndex)
        assertEquals(LocalTime.of(9, 5), parsed.startTime)
        assertEquals(LocalTime.of(9, 20), parsed.endTime)
        assertEquals("note", parsed.feeling)
    }
}