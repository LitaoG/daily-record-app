package io.github.litaog.dailyrecord.core.di

import androidx.work.ExistingWorkPolicy
import io.github.litaog.dailyrecord.core.sync.DeletionBarrier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyRecordSyncSchedulerTest {
    @Test
    fun usesDailyRecordWorkNameAfterModuleExpansion() {
        assertEquals("daily-record-cloud-sync", DailyRecordSyncScheduler.workName)
    }

    @Test
    fun localChangeReplacesPendingWorkDuringDebounceWindow() {
        assertEquals(ExistingWorkPolicy.REPLACE, DailyRecordSyncScheduler.workPolicy)
        assertEquals(750L, DailyRecordSyncScheduler.SYNC_DEBOUNCE_MILLIS)
    }

    @Test
    fun deletionBlockStartsBlockedAndEndsUnblocked() {
        assertFalse(DeletionBarrier.isDeletionBlocked)
        try {
            DeletionBarrier.beginDeletionBlock()
            assertTrue(DeletionBarrier.isDeletionBlocked)
            DeletionBarrier.beginDeletionBlock()
            assertTrue(DeletionBarrier.isDeletionBlocked)
        } finally {
            DeletionBarrier.endDeletionBlock()
        }
        assertFalse(DeletionBarrier.isDeletionBlocked)
    }
}
