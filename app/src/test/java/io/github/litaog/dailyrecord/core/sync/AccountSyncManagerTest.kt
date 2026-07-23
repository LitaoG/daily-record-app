package io.github.litaog.dailyrecord.core.sync

import java.io.IOException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountSyncManagerTest {
    @Test
    fun networkFailureIsRetryable() {
        assertTrue(IOException("temporary network failure").isRetryableRemoteObservation())
    }

    @Test
    fun wrappedNetworkFailureIsRetryable() {
        assertTrue(
            IllegalStateException(
                "listener failed",
                IOException("connection closed"),
            ).isRetryableRemoteObservation(),
        )
    }

    @Test
    fun networkFailuresAreMarkedForVpnGuidance() {
        assertTrue(IOException("firebase unreachable").isNetworkRelatedSyncFailure())
        assertTrue(
            IllegalStateException(
                "listener failed",
                IOException("connection closed"),
            ).isNetworkRelatedSyncFailure(),
        )
        assertFalse(IllegalArgumentException("invalid data").isNetworkRelatedSyncFailure())
    }

    @Test
    fun unknownFailureStopsAutomaticRetry() {
        assertFalse(IllegalArgumentException("malformed snapshot").isRetryableRemoteObservation())
    }
}
