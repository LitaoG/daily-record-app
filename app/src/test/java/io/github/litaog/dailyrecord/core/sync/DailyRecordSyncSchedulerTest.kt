package io.github.litaog.dailyrecord.core.sync

import androidx.work.ExistingWorkPolicy
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
    fun localChangeQueuesFollowUpWorkInsteadOfBeingDroppedByRunningWork() {
        assertEquals(
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            DailyRecordSyncScheduler.workPolicy,
        )
    }

    @Test
    fun deletionBlockStartsBlockedAndEndsUnblocked() {
        assertFalse(DailyRecordSyncScheduler.isDeletionBlocked)
        try {
            DailyRecordSyncScheduler.beginDeletionBlock()
            assertTrue(DailyRecordSyncScheduler.isDeletionBlocked)
            DailyRecordSyncScheduler.beginDeletionBlock()
            assertTrue(DailyRecordSyncScheduler.isDeletionBlocked)
        } finally {
            DailyRecordSyncScheduler.endDeletionBlock()
        }
        assertFalse(DailyRecordSyncScheduler.isDeletionBlocked)
    }
}
