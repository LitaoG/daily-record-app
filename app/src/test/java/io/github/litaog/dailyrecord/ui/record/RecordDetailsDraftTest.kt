package io.github.litaog.dailyrecord.ui.record

import io.github.litaog.dailyrecord.ui.RecordDetailEntry
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordDetailsDraftTest {
    @Test
    fun reconcileAddsOneBlankEntryPerCount() {
        val draft = RecordDetailsDraft().reconcile(
            latest = listOf(RecordDetailEntry(1, LocalTime.of(9, 20), LocalTime.of(9, 35), "")),
            count = 2,
        )

        assertEquals(2, draft.entries.size)
        assertEquals(9 * 60 + 20, draft.entries[0].startMinutes)
        assertEquals(null, draft.entries[1].startMinutes)
        assertFalse(draft.hasChanges)
    }

    @Test
    fun dirtyDetailsSurviveAConcurrentRoomRefreshButResizeWithCount() {
        val draft = RecordDetailsDraft()
            .reconcile(emptyList(), 2)
            .update(0) { it.withFeeling("今天更平静") }
            .reconcile(
                latest = listOf(RecordDetailEntry(1, feeling = "云端版本")),
                count = 3,
            )

        assertTrue(draft.hasChanges)
        assertEquals("今天更平静", draft.entries[0].feeling)
        assertEquals(3, draft.entries.size)
    }

    @Test
    fun changeToZeroClearsEntryDraftsForNextExpansion() {
        val draft = RecordDetailsDraft()
            .reconcile(emptyList(), 2)
            .update(1) { it.withFeeling("需要移除") }
            .resize(0)

        assertEquals(emptyList<RecordDetailDraft>(), draft.entries)
    }
}
