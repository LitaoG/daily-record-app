package io.github.litaog.dailyrecord.core.sync

import androidx.work.ExistingWorkPolicy
import org.junit.Assert.assertEquals
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
}
