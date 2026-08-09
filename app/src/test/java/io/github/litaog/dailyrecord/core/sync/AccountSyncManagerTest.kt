package io.github.litaog.dailyrecord.core.sync

import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountSyncManagerTest {
    @Test
    fun concurrentSyncTriggersShareTheInFlightAttempt() = runBlocking {
        val operations = BlockingSyncOperations()
        val manager = AccountSyncManager(
            ownerId = "owner",
            coordinator = operations,
            productionConfigured = true,
            syncAttemptTimeoutMillis = 1_000,
        )

        val first = launch { manager.syncNow() }
        operations.started.await()
        manager.syncNow()

        assertEquals(1, operations.syncCalls.get())
        assertEquals(SyncStatus.Syncing, manager.status.value)

        operations.release.complete(Unit)
        first.join()
        assertEquals(SyncStatus.UpToDate, manager.status.value)
    }

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
    fun onlyTransientFirebaseAuthFailuresAreRetryable() {
        // FirebaseAuthException invokes Android TextUtils in its constructor,
        // so the JVM test targets the pure code classifier directly. The
        // extension still walks the real exception chain in production.
        assertTrue(isRetryableFirebaseAuthCode("ERROR_USER_TOKEN_EXPIRED"))
        assertFalse(isRetryableFirebaseAuthCode("ERROR_INVALID_CREDENTIAL"))
        assertFalse(isRetryableFirebaseAuthCode("ERROR_USER_DISABLED"))
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
    fun firebaseAuthNetworkFailureIsNotMisclassifiedAsExpiredLogin() {
        assertEquals(
            SyncFailureKind.Network,
            syncFailureKindForFirebaseAuthCode("ERROR_NETWORK_REQUEST_FAILED"),
        )
    }

    @Test
    fun unknownFailureStopsAutomaticRetry() {
        assertFalse(IllegalArgumentException("malformed snapshot").isRetryableRemoteObservation())
    }
}

private class BlockingSyncOperations : AccountSyncOperations {
    val syncCalls = AtomicInteger()
    val started = CompletableDeferred<Unit>()
    val release = CompletableDeferred<Unit>()

    override fun observeRemote(ownerId: String): Flow<RemoteSnapshot> = emptyFlow()

    override fun observePendingCount(ownerId: String): Flow<Int> = MutableStateFlow(0)

    override suspend fun pendingCount(ownerId: String): Int = 0

    override suspend fun applySnapshot(ownerId: String, snapshot: RemoteSnapshot): Int = 0

    override suspend fun syncOnce(ownerId: String): SyncResult {
        syncCalls.incrementAndGet()
        started.complete(Unit)
        release.await()
        return SyncResult(uploaded = 0, downloaded = 0, pending = 0)
    }
}
