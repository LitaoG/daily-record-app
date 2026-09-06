package io.github.litaog.dailyrecord.ui.record

import io.github.litaog.dailyrecord.ui.RecordDetailEntry
import java.time.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordDetailRemovalTest {
    @Test
    fun zeroCountNeverConfirms() {
        assertFalse(
            shouldConfirmDetailRemoval(
                count = 0,
                editorRowLimit = 512,
                draftTailHasContent = true,
                storedDetails = listOf(storedDetail(1, feeling = "x")),
            ),
        )
    }

    @Test
    fun draftTailDecidesWithinEditorLimit() {
        assertTrue(
            shouldConfirmDetailRemoval(
                count = 3,
                editorRowLimit = 512,
                draftTailHasContent = true,
                storedDetails = emptyList(),
            ),
        )
        assertFalse(
            shouldConfirmDetailRemoval(
                count = 3,
                editorRowLimit = 512,
                draftTailHasContent = false,
                storedDetails = listOf(storedDetail(3, feeling = "x")),
            ),
        )
    }

    @Test
    fun storedTailWithContentConfirmsAboveEditorLimit() {
        assertTrue(
            shouldConfirmDetailRemoval(
                count = 600,
                editorRowLimit = 512,
                draftTailHasContent = false,
                storedDetails = listOf(
                    storedDetail(599),
                    storedDetail(600, feeling = "remembered"),
                ),
            ),
        )
    }

    @Test
    fun storedTailWithoutContentDecreasesSilentlyAboveEditorLimit() {
        assertFalse(
            shouldConfirmDetailRemoval(
                count = 600,
                editorRowLimit = 512,
                draftTailHasContent = false,
                storedDetails = listOf(storedDetail(600)),
            ),
        )
        assertFalse(
            shouldConfirmDetailRemoval(
                count = 600,
                editorRowLimit = 512,
                draftTailHasContent = false,
                storedDetails = listOf(storedDetail(599, feeling = "older row")),
            ),
        )
    }

    @Test
    fun storedTimeRangeCountsAsContent() {
        assertTrue(
            shouldConfirmDetailRemoval(
                count = 600,
                editorRowLimit = 512,
                draftTailHasContent = false,
                storedDetails = listOf(
                    storedDetail(600, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(9, 30)),
                ),
            ),
        )
    }

    private fun storedDetail(
        occurrenceIndex: Int,
        feeling: String = "",
        startTime: LocalTime? = null,
        endTime: LocalTime? = null,
    ) = RecordDetailEntry(
        occurrenceIndex = occurrenceIndex,
        startTime = startTime,
        endTime = endTime,
        feeling = feeling,
    )
}
