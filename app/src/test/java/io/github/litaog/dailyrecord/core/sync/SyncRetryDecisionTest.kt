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
    fun pendingWithRejectedRemoteRecordsStillRetries() {
        // A malformed cloud document must not starve healthy pending rows:
        // the engine quarantines only locally refused dates.
        assertTrue(
            SyncResult(uploaded = 0, downloaded = 0, pending = 1, rejectedRemoteRecords = 1)
                .workerShouldRetry(),
        )
    }

    @Test
    fun locallyQuarantinedRowsStopRetrying() {
        // The server refused this date with a data error; retrying without a
        // user edit can never succeed, so the worker stops burning backoff.
        assertFalse(
            SyncResult(uploaded = 0, downloaded = 1, pending = 2, quarantinedLocalRecords = 1)
                .workerShouldRetry(),
        )
    }

    @Test
    fun fullySyncedResultDoesNotRetry() {
        assertFalse(SyncResult(uploaded = 1, downloaded = 0, pending = 0).workerShouldRetry())
    }
}
