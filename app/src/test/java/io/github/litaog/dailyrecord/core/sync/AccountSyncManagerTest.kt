package io.github.litaog.dailyrecord.core.sync

import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountSyncManagerTest {
    @Test
    fun manualSyncWhileBusyIsQueuedInsteadOfDropped() = runBlocking {
        val operations = BlockingSyncOperations()
        val manager = AccountSyncManager(
            ownerId = "owner",
            coordinator = operations,
            productionConfigured = true,
            syncAttemptTimeoutMillis = 1_000,
        )

        val first = launch { manager.syncNow() }
        operations.started.await()
        // A manual request arriving while the mutex is held must be queued,
        // not silently dropped: it returns immediately and the in-flight
        // attempt's loop runs one more sync once the current one finishes.
        val queued = launch { manager.syncNow() }
        queued.join()
        assertEquals(1, operations.syncCalls.get())
        assertEquals(SyncStatus.Syncing, manager.status.value)

        operations.release.complete(Unit)
        first.join()
        assertEquals(2, operations.syncCalls.get())
        assertEquals(SyncStatus.UpToDate, manager.status.value)
    }

    @Test
    fun malformedRemoteSnapshotDoesNotTriggerAutomaticPendingFlush() = runBlocking {
        val operations = RejectedSnapshotOperations()
        val manager = AccountSyncManager(
            ownerId = "owner",
            coordinator = operations,
            productionConfigured = true,
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        manager.start(scope)

        // The network observer performs one ordinary startup sync. Hold the
        // malformed snapshot until that baseline is complete, then verify that
        // the rejected snapshot itself does not schedule another automatic
        // pending flush.
        withTimeout(5_000) { operations.initialSyncCompleted.await() }
        val syncCallsBeforeRejectedSnapshot = operations.syncCalls.get()
        operations.emitRejectedSnapshot.complete(Unit)
        withTimeout(5_000) { operations.snapshotApplied.await() }
        // A malformed server document must not cause the app to resubmit local
        // pending rows on every subsequent snapshot. Explicit user retry remains
        // available through syncNow().
        assertEquals(syncCallsBeforeRejectedSnapshot, operations.syncCalls.get())
        scope.cancel()
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

    @Test
    fun nonRetryableListenerErrorPublishesFailureWithoutCrashing() = runBlocking {
        val manager = AccountSyncManager(
            ownerId = "owner",
            coordinator = NonRetryableFailingOperations(),
            productionConfigured = true,
        )
        val uncaught = CompletableDeferred<Throwable>()
        val scope = CoroutineScope(
            SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, error ->
                uncaught.complete(error)
            },
        )
        val job = manager.start(scope).first()
        withTimeout(10_000) {
            while (manager.status.value !is SyncStatus.Failed) delay(10)
        }
        withTimeout(5_000) { job.join() }
        // The non-retryable listener error ended the job with a sanitized
        // status; it must never reach the uncaught exception handler.
        assertFalse(uncaught.isCompleted)
        scope.cancel()
    }

    @Test
    fun applySnapshotFailureSurfacesStatusButKeepsTheChannelAlive() = runBlocking {
        val manager = AccountSyncManager(
            ownerId = "owner",
            coordinator = FailingApplyOperations(),
            productionConfigured = true,
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val job = manager.start(scope).first()
        withTimeout(10_000) {
            while (manager.status.value !is SyncStatus.Failed) delay(10)
        }
        assertFalse(job.isCompleted)
        withTimeout(10_000) {
            while (manager.status.value !is SyncStatus.UpToDate) delay(10)
        }
        assertFalse(job.isCompleted)
        scope.cancel()
    }
}

private class NonRetryableFailingOperations : AccountSyncOperations {
    override fun observeRemote(ownerId: String): Flow<RemoteSnapshot> = flow {
        emit(RemoteSnapshot(records = emptyList(), fromCache = false, rejectedRecordCount = 0))
        throw IllegalArgumentException("malformed snapshot")
    }

    override fun observePendingCount(ownerId: String): Flow<Int> = MutableStateFlow(0)

    override suspend fun pendingCount(ownerId: String): Int = 0

    override suspend fun applySnapshot(ownerId: String, snapshot: RemoteSnapshot): Int = 0

    // The startup network job runs an immediate sync attempt; in production a
    // permanent remote error fails that attempt too, so the fake mirrors the
    // same behaviour instead of clobbering the failed status with UpToDate.
    override suspend fun syncOnce(ownerId: String): SyncResult =
        throw IllegalArgumentException("malformed snapshot")
}

private class FailingApplyOperations : AccountSyncOperations {
    private val applyCalls = AtomicInteger()

    override fun observeRemote(ownerId: String): Flow<RemoteSnapshot> = flow {
        emit(RemoteSnapshot(records = emptyList(), fromCache = false, rejectedRecordCount = 0))
        delay(200)
        emit(RemoteSnapshot(records = emptyList(), fromCache = false, rejectedRecordCount = 0))
        awaitCancellation()
    }

    override fun observePendingCount(ownerId: String): Flow<Int> = MutableStateFlow(0)

    override suspend fun pendingCount(ownerId: String): Int = 0

    override suspend fun applySnapshot(ownerId: String, snapshot: RemoteSnapshot): Int {
        if (applyCalls.getAndIncrement() == 0) throw IllegalStateException("Room unavailable")
        return 0
    }

    // Mirrors production: the same Room failure that makes the observer's
    // applySnapshot fail also fails the startup network-job sync attempt, so
    // both paths publish the failed status and cannot race each other into
    // overwriting it with a transient UpToDate.
    override suspend fun syncOnce(ownerId: String): SyncResult =
        throw IllegalStateException("Room unavailable")
}

private class RejectedSnapshotOperations : AccountSyncOperations {
    val syncCalls = AtomicInteger()
    val initialSyncCompleted = CompletableDeferred<Unit>()
    val emitRejectedSnapshot = CompletableDeferred<Unit>()
    val snapshotApplied = CompletableDeferred<Unit>()

    override fun observeRemote(ownerId: String): Flow<RemoteSnapshot> = flow {
        emitRejectedSnapshot.await()
        emit(RemoteSnapshot(records = emptyList(), fromCache = false, rejectedRecordCount = 1))
    }

    override fun observePendingCount(ownerId: String): Flow<Int> = MutableStateFlow(1)

    override suspend fun pendingCount(ownerId: String): Int = 1

    override suspend fun applySnapshot(ownerId: String, snapshot: RemoteSnapshot): Int {
        snapshotApplied.complete(Unit)
        return 0
    }

    override suspend fun syncOnce(ownerId: String): SyncResult {
        syncCalls.incrementAndGet()
        initialSyncCompleted.complete(Unit)
        return SyncResult(uploaded = 0, downloaded = 0, pending = 1)
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
