package io.github.litaog.dailyrecord.core.sync

import androidx.work.ExistingWorkPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class HandBrewSyncSchedulerTest {
    @Test
    fun localChangeQueuesFollowUpWorkInsteadOfBeingDroppedByRunningWork() {
        assertEquals(
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            HandBrewSyncScheduler.workPolicy,
        )
    }
}
