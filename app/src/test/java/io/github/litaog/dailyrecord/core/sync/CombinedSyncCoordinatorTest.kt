package io.github.litaog.dailyrecord.core.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CombinedSyncCoordinatorTest {
    @Test
    fun aggregatesAnyNumberOfRecordModules() = runBlocking {
        val modules = listOf(
            FakeModule(pending = 1, prepared = 2, applied = 3, uploaded = 4),
            FakeModule(pending = 10, prepared = 20, applied = 30, uploaded = 40),
            FakeModule(pending = 100, prepared = 200, applied = 300, uploaded = 400),
        )
        val coordinator = CombinedSyncCoordinator(modules)
        val snapshot = RemoteSnapshot(fromCache = false)

        assertEquals(111, coordinator.observePendingCount("owner").first())
        assertEquals(111, coordinator.pendingCount("owner"))
        assertEquals(222, coordinator.prepareLocalAccount("owner"))
        assertEquals(333, coordinator.applySnapshot("owner", snapshot))
        assertEquals(
            SyncResult(
                uploaded = 444,
                downloaded = 333,
                pending = 111,
                rejectedRemoteRecords = 6,
            ),
            coordinator.syncOnce("owner"),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAnEmptyModuleList() {
        CombinedSyncCoordinator(emptyList())
    }

    @Test
    fun failingFirstModuleDoesNotSkipTheSecondModuleSync() = runBlocking {
        val failure = IllegalStateException("hand brew sync failed")
        val failing = FakeModule(1, 1, 1, 1, syncFailure = failure)
        val healthy = FakeModule(2, 2, 2, 2)
        val coordinator = CombinedSyncCoordinator(listOf(failing, healthy))

        val result = runCatching { coordinator.syncOnce("owner") }

        assertSame(failure, result.exceptionOrNull())
        assertEquals(1, failing.syncCalls)
        assertEquals(1, healthy.syncCalls)
        assertEquals(2, healthy.lastSyncResult.uploaded)
    }

    @Test
    fun failingSecondModuleStillAggregatesFirstModuleResultBeforeThrowing() = runBlocking {
        val failure = IllegalStateException("sex sync failed")
        val healthy = FakeModule(1, 1, 1, 1)
        val failing = FakeModule(2, 2, 2, 2, syncFailure = failure)
        val coordinator = CombinedSyncCoordinator(listOf(healthy, failing))

        val result = runCatching { coordinator.syncOnce("owner") }

        assertSame(failure, result.exceptionOrNull())
        assertEquals(1, healthy.syncCalls)
        assertEquals(1, failing.syncCalls)
        assertEquals(1, healthy.lastSyncResult.uploaded)
    }

    @Test
    fun failingModuleDoesNotSkipOtherModuleSnapshotApplication() = runBlocking {
        val failure = IllegalStateException("snapshot apply failed")
        val failing = FakeModule(1, 1, 1, 1, applyFailure = failure)
        val healthy = FakeModule(2, 2, 2, 2)
        val coordinator = CombinedSyncCoordinator(listOf(failing, healthy))
        val snapshot = RemoteSnapshot(fromCache = false)

        val result = runCatching { coordinator.applySnapshot("owner", snapshot) }

        assertSame(failure, result.exceptionOrNull())
        assertEquals(1, failing.applyCalls)
        assertEquals(1, healthy.applyCalls)
    }

    @Test
    fun failingModuleDoesNotSkipOtherModuleLocalAdoption() = runBlocking {
        val failure = IllegalStateException("local adoption failed")
        val failing = FakeModule(1, 1, 1, 1, prepareFailure = failure)
        val healthy = FakeModule(2, 2, 2, 2)
        val coordinator = CombinedSyncCoordinator(listOf(failing, healthy))

        val result = runCatching { coordinator.prepareLocalAccount("owner") }

        assertSame(failure, result.exceptionOrNull())
        assertEquals(1, failing.prepareCalls)
        assertEquals(1, healthy.prepareCalls)
    }

    @Test
    fun failingModuleDoesNotSkipOtherModulePendingCount() = runBlocking {
        val failure = IllegalStateException("pending count failed")
        val failing = FakeModule(1, 1, 1, 1, pendingFailure = failure)
        val healthy = FakeModule(2, 2, 2, 2)
        val coordinator = CombinedSyncCoordinator(listOf(failing, healthy))

        val result = runCatching { coordinator.pendingCount("owner") }

        assertSame(failure, result.exceptionOrNull())
        assertEquals(1, failing.pendingCalls)
        assertEquals(1, healthy.pendingCalls)
    }

    @Test
    fun cancellationFromOneModulePropagatesImmediatelyWithoutSkippingOtherWork() = runBlocking {
        val cancellation = CancellationException("cancelled")
        val cancelling = FakeModule(1, 1, 1, 1, syncFailure = cancellation)
        val healthy = FakeModule(2, 2, 2, 2)
        val coordinator = CombinedSyncCoordinator(listOf(cancelling, healthy))

        val result = runCatching { coordinator.syncOnce("owner") }

        assertSame(cancellation, result.exceptionOrNull())
        assertEquals(1, cancelling.syncCalls)
        // Cancellation propagates immediately; the second module must not be
        // started in the same batch, matching coroutine cancellation semantics.
        assertEquals(0, healthy.syncCalls)
    }
}

private class FakeModule(
    private val pending: Int,
    private val prepared: Int,
    private val applied: Int,
    private val uploaded: Int,
    private val syncFailure: Exception? = null,
    private val applyFailure: Exception? = null,
    private val prepareFailure: Exception? = null,
    private val pendingFailure: Exception? = null,
) : ModuleSyncCoordinator {
    private val remote = MutableStateFlow(RemoteSnapshot(fromCache = false))
    private val pendingFlow = MutableStateFlow(pending)
    var syncCalls = 0
        private set
    var applyCalls = 0
        private set
    var prepareCalls = 0
        private set
    var pendingCalls = 0
        private set
    var lastSyncResult = SyncResult(0, 0, 0)
        private set

    override fun observeRemote(ownerId: String): Flow<RemoteSnapshot> = remote

    override fun observePendingCount(ownerId: String): Flow<Int> = pendingFlow

    override suspend fun pendingCount(ownerId: String): Int {
        pendingCalls += 1
        pendingFailure?.let { throw it }
        return pending
    }

    override suspend fun applySnapshot(ownerId: String, snapshot: RemoteSnapshot): Int {
        applyCalls += 1
        applyFailure?.let { throw it }
        return applied
    }

    override suspend fun prepareLocalAccount(ownerId: String): Int {
        prepareCalls += 1
        prepareFailure?.let { throw it }
        return prepared
    }

    override suspend fun syncOnce(ownerId: String): SyncResult {
        syncCalls += 1
        syncFailure?.let { throw it }
        lastSyncResult = SyncResult(
            uploaded = uploaded,
            downloaded = applied,
            pending = pending,
            rejectedRemoteRecords = 2,
        )
        return lastSyncResult
    }
}
