package io.github.litaog.dailyrecord.ui.record

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BrewCountDraftTest {
    @Test
    fun firstRoomValueInitializesDraftAndBaseline() {
        val draft = BrewCountDraft().reconcile(2)

        assertEquals(2, draft.count)
        assertEquals(2, draft.baseline)
        assertTrue(draft.initialized)
        assertFalse(draft.hasChanges)
    }

    @Test
    fun cleanDraftFollowsNewRoomValue() {
        val draft = BrewCountDraft().reconcile(2).reconcile(4)

        assertEquals(4, draft.count)
        assertEquals(4, draft.baseline)
        assertFalse(draft.hasChanges)
    }

    @Test
    fun dirtyDraftSurvivesRemoteUpdate() {
        val draft = BrewCountDraft().reconcile(2).increase().reconcile(5)

        assertEquals(3, draft.count)
        assertEquals(5, draft.baseline)
        assertTrue(draft.hasChanges)
    }

    @Test
    fun matchingRemoteUpdateMakesDraftClean() {
        val draft = BrewCountDraft().reconcile(2).increase().reconcile(3)

        assertEquals(3, draft.count)
        assertEquals(3, draft.baseline)
        assertFalse(draft.hasChanges)
    }

    @Test
    fun countNeverUnderflowsOrOverflows() {
        assertEquals(0, BrewCountDraft().decrease().count)

        val maximum = BrewCountDraft(count = Int.MAX_VALUE)
        assertSame(maximum, maximum.increase())
    }
}
