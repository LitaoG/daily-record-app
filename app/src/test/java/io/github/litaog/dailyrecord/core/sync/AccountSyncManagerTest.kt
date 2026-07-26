package io.github.litaog.dailyrecord.core.sync

import java.io.IOException
import org.junit.Assert.assertEquals
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
    fun firestoreFailuresAreClassifiedForActionableGuidance() {
        assertEquals(
            SyncFailureKind.Authentication,
            syncFailureKindForFirestoreCode(16),
        )
        assertEquals(
            SyncFailureKind.Permission,
            syncFailureKindForFirestoreCode(7),
        )
        assertEquals(
            SyncFailureKind.Quota,
            syncFailureKindForFirestoreCode(8),
        )
        assertEquals(
            SyncFailureKind.Network,
            syncFailureKindForFirestoreCode(14),
        )
        assertEquals(
            SyncFailureKind.Service,
            syncFailureKindForFirestoreCode(13),
        )
        assertEquals(
            SyncFailureKind.Data,
            syncFailureKindForFirestoreCode(15),
        )
        assertEquals(
            SyncFailureKind.Unknown,
            syncFailureKindForFirestoreCode(2),
        )
    }

    @Test
    fun localArgumentFailureIsClassifiedAsDataFailure() {
        assertEquals(
            SyncFailureKind.Data,
            IllegalArgumentException("invalid local record").syncFailureKind(),
        )
    }

    @Test
    fun unknownFailureStopsAutomaticRetry() {
        assertFalse(IllegalArgumentException("malformed snapshot").isRetryableRemoteObservation())
    }
}
