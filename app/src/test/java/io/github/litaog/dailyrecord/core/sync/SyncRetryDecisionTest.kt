package io.github.litaog.dailyrecord.core.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncRetryDecisionTest {
    @Test
    fun pendingWithoutRejectionsIsRetried() {
        assertTrue(
            SyncResult(uploaded = 1, downloaded = 0, pending = 1).workerShouldRetry(),
        )
    }

    @Test
    fun pendingWithRejectedRemoteRecordsStopsRetrying() {
        // A permanently malformed cloud document keeps its row pending but
        // counts as rejected; retrying would never make progress.
        assertFalse(
            SyncResult(uploaded = 0, downloaded = 0, pending = 1, rejectedRemoteRecords = 1)
                .workerShouldRetry(),
        )
    }

    @Test
    fun rejectedCommitsAlsoStopRetrying() {
        // The engine counts malformed commit results in the same counter.
        assertFalse(
            SyncResult(uploaded = 0, downloaded = 1, pending = 2, rejectedRemoteRecords = 1)
                .workerShouldRetry(),
        )
    }

    @Test
    fun fullySyncedResultDoesNotRetry() {
        assertFalse(SyncResult(uploaded = 1, downloaded = 0, pending = 0).workerShouldRetry())
    }
}
