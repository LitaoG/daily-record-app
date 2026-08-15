package io.github.litaog.dailyrecord.ui.record

import androidx.compose.runtime.saveable.SaverScope
import io.github.litaog.dailyrecord.ui.RecordDetailEntry
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordDetailsDraftTest {
    @Test
    fun reconcilePreservesSparseOccurrenceIndexes() {
        val draft = RecordDetailsDraft().reconcile(
            latest = listOf(
                RecordDetailEntry(2, feeling = "second"),
                RecordDetailEntry(4, feeling = "fourth"),
            ),
            count = 4,
        )

        assertEquals(4, draft.entries.size)
        assertEquals(2, draft.entries[1].occurrenceIndex)
        assertEquals("second", draft.entries[1].feeling)
        assertEquals(4, draft.entries[3].occurrenceIndex)
        assertEquals("fourth", draft.entries[3].feeling)
    }

    @Test
    fun hugeAggregateCountDoesNotMaterializeDetailRows() {
        val draft = RecordDetailsDraft().reconcile(
            latest = listOf(RecordDetailEntry(Int.MAX_VALUE, feeling = "kept remotely")),
            count = Int.MAX_VALUE,
        )

        assertEquals(Int.MAX_VALUE, draft.count)
        assertTrue(draft.entries.isEmpty())
        assertFalse(draft.expanded)
        assertFalse(draft.hasChanges)
    }

    @Test
    fun hugeAggregateCountKeepsExistingDetailsInsideTheEditorBoundary() {
        val draft = RecordDetailsDraft().reconcile(
            latest = listOf(RecordDetailEntry(1, feeling = "first")),
            count = Int.MAX_VALUE,
        )

        assertEquals(listOf(1), draft.entries.map(RecordDetailDraft::occurrenceIndex))
        assertEquals("first", draft.entries.single().feeling)
    }

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
    fun dirtyDetailsSurviveAConcurrentRemoteShrinkWithoutTruncation() {
        // The screen passes the reconciled draft count (2): the user's dirty
        // count keeps its size, so a remote shrink to 1 must not truncate the
        // entry the user is still editing.
        val draft = RecordDetailsDraft()
            .reconcile(emptyList(), 2)
            .update(0) { it.withFeeling("今天更平静") }
            .reconcile(
                latest = listOf(RecordDetailEntry(1, feeling = "云端版本")),
                count = 2,
            )

        assertTrue(draft.hasChanges)
        assertEquals("今天更平静", draft.entries[0].feeling)
        assertEquals(2, draft.entries.size)
        assertEquals("云端版本", draft.baseline[0].feeling)
    }

    @Test
    fun dirtyDetailsNormalizeToTheDraftCountWhenTheRemoteGrows() {
        // Remote count 2 -> 3 with a clean count: countDraft follows the
        // server, so the row list must normalize to 3 (blank tail) instead of
        // staying at 2 and leaving the displayed count inconsistent.
        val draft = RecordDetailsDraft()
            .reconcile(emptyList(), 2)
            .update(0) { it.withFeeling("保留") }
            .reconcile(
                latest = listOf(
                    RecordDetailEntry(1, feeling = "云端一"),
                    RecordDetailEntry(2, feeling = "云端二"),
                ),
                count = 3,
            )

        assertTrue(draft.hasChanges)
        assertEquals(3, draft.entries.size)
        assertEquals("保留", draft.entries[0].feeling)
        assertNull(draft.entries[2].startMinutes)
    }

    @Test
    fun changeToZeroClearsEntryDraftsForNextExpansion() {
        val draft = RecordDetailsDraft()
            .reconcile(emptyList(), 2)
            .update(1) { it.withFeeling("需要移除") }
            .resize(0)

        assertEquals(emptyList<RecordDetailDraft>(), draft.entries)
    }

    @Test
    fun saverRoundTripsExpandedDraftForProcessRecreation() {
        val draft = RecordDetailsDraft()
            .reconcile(emptyList(), 1)
            .update(0) {
                it.copy(startMinutes = 8 * 60 + 30, endMinutes = 9 * 60, feeling = "保留这段感受")
            }
            .copy(expanded = true)
        val scope = object : SaverScope {
            override fun canBeSaved(value: Any): Boolean = true
        }

        val saved = with(RecordDetailsDraft.Saver) { scope.save(draft) }
            ?: error("draft saver returned null")
        val restored = RecordDetailsDraft.Saver.restore(saved)

        assertEquals(draft, restored)
    }
}
