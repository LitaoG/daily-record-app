package io.github.litaog.dailyrecord.core.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
}

private class FakeModule(
    private val pending: Int,
    private val prepared: Int,
    private val applied: Int,
    private val uploaded: Int,
) : ModuleSyncCoordinator {
    private val remote = MutableStateFlow(RemoteSnapshot(fromCache = false))
    private val pendingFlow = MutableStateFlow(pending)

    override fun observeRemote(ownerId: String): Flow<RemoteSnapshot> = remote

    override fun observePendingCount(ownerId: String): Flow<Int> = pendingFlow

    override suspend fun pendingCount(ownerId: String): Int = pending

    override suspend fun applySnapshot(ownerId: String, snapshot: RemoteSnapshot): Int = applied

    override suspend fun prepareLocalAccount(ownerId: String): Int = prepared

    override suspend fun syncOnce(ownerId: String): SyncResult = SyncResult(
        uploaded = uploaded,
        downloaded = applied,
        pending = pending,
        rejectedRemoteRecords = 2,
    )
}
