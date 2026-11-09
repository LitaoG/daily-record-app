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


internal class DeletionBlockingGate : CloudWriteGate {
    override suspend fun <T> withWrite(ownerId: String, block: suspend () -> T): T =
        throw AccountDeletionInProgressException(ownerId)
}

internal fun testSnapshot(rejectedRecordCount: Int = 0) = RemoteSnapshot(
    sourceCollection = HAND_BREW_RECORDS_COLLECTION,
    records = emptyList(),
    fromCache = false,
    rejectedRecordCount = rejectedRecordCount,
)

internal class CountingApplyOperations : AccountSyncOperations {
    val applyCalls = AtomicInteger()

    override fun observeRemote(ownerId: String): Flow<RemoteSnapshot> = flow {
        emit(testSnapshot())
        awaitCancellation()
    }

    override fun observePendingCount(ownerId: String): Flow<Int> = MutableStateFlow(0)

    override suspend fun pendingCount(ownerId: String): Int = 0

    override suspend fun applySnapshot(ownerId: String, snapshot: RemoteSnapshot): Int {
        applyCalls.incrementAndGet()
        return 0
    }

    override suspend fun syncOnce(ownerId: String): SyncResult =
        SyncResult(uploaded = 0, downloaded = 0, pending = 0)
}

internal class NonRetryableFailingOperations : AccountSyncOperations {
    override fun observeRemote(ownerId: String): Flow<RemoteSnapshot> = flow {
        emit(testSnapshot())
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

internal class FailingApplyOperations : AccountSyncOperations {
    private val applyCalls = AtomicInteger()

    override fun observeRemote(ownerId: String): Flow<RemoteSnapshot> = flow {
        emit(testSnapshot())
        delay(200)
        emit(testSnapshot())
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

internal class RejectedSnapshotOperations : AccountSyncOperations {
    val syncCalls = AtomicInteger()
    val initialSyncCompleted = CompletableDeferred<Unit>()
    val emitRejectedSnapshot = CompletableDeferred<Unit>()
    val snapshotApplied = CompletableDeferred<Unit>()

    override fun observeRemote(ownerId: String): Flow<RemoteSnapshot> = flow {
        emitRejectedSnapshot.await()
        emit(testSnapshot(rejectedRecordCount = 1))
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

internal class BlockingSyncOperations : AccountSyncOperations {
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

/**
 * Parks every sync attempt on its own gate, so a test can queue a follow-up
 * while the mutex is held by an in-flight attempt and release attempts one by
 * one to walk the burst loop to its final attempt.
 */
internal class GatedBurstOperations : AccountSyncOperations {
    val syncCalls = AtomicInteger()
    private val attemptsStarted = List(3) { CompletableDeferred<Unit>() }
    private val attemptsRelease = List(3) { CompletableDeferred<Unit>() }

    suspend fun awaitAttempt(number: Int) = attemptsStarted[number - 1].await()

    fun release(number: Int) {
        attemptsRelease[number - 1].complete(Unit)
    }

    override fun observeRemote(ownerId: String): Flow<RemoteSnapshot> = emptyFlow()

    override fun observePendingCount(ownerId: String): Flow<Int> = MutableStateFlow(0)

    override suspend fun pendingCount(ownerId: String): Int = 0

    override suspend fun applySnapshot(ownerId: String, snapshot: RemoteSnapshot): Int = 0

    override suspend fun syncOnce(ownerId: String): SyncResult {
        val call = syncCalls.incrementAndGet()
        if (call <= 3) {
            attemptsStarted[call - 1].complete(Unit)
            attemptsRelease[call - 1].await()
        }
        return SyncResult(uploaded = 0, downloaded = 0, pending = 0)
    }
}
