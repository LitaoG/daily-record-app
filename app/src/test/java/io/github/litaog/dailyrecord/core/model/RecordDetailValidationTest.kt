package io.github.litaog.dailyrecord.core.model

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RecordDetailValidationTest {
    @Test
    fun emojiCountsAsOneVisibleCharacter() {
        assertEquals(2, "好🙂".visibleCharacterCount())
        assertEquals("好🙂", "好🙂了".truncateVisibleCharacters(2))
    }

    @Test
    fun feelingAndTimeRangeAreValidatedAtDomainBoundary() {
        assertThrows(IllegalArgumentException::class.java) {
            HandBrewRecordDetail(
                id = "detail",
                localDate = LocalDate.of(2026, 8, 9),
                occurrenceIndex = 1,
                startTime = LocalTime.of(22, 0),
                endTime = LocalTime.of(21, 0),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            SexRecordDetail(
                id = "detail",
                localDate = LocalDate.of(2026, 8, 9),
                occurrenceIndex = 1,
                feeling = "字".repeat(101),
            )
        }
    }
}
