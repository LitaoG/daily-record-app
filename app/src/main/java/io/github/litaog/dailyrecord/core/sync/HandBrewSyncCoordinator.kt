package io.github.litaog.dailyrecord.core.sync

import kotlinx.coroutines.flow.Flow

internal class HandBrewSyncCoordinator(
    store: RoomHandBrewSyncStore,
    remote: HandBrewRemoteDataSource,
) : AccountSyncOperations {
    private val engine = DailyCountSyncEngine(store, remote)

    override fun observeRemote(ownerId: String): Flow<RemoteSnapshot> = engine.observeRemote(ownerId)

    override fun observePendingCount(ownerId: String): Flow<Int> = engine.observePendingCount(ownerId)

    override suspend fun pendingCount(ownerId: String): Int = engine.pendingCount(ownerId)

    override suspend fun applySnapshot(ownerId: String, snapshot: RemoteSnapshot): Int =
        engine.applySnapshot(ownerId, snapshot)

    suspend fun prepareLocalAccount(ownerId: String): Int = engine.prepareLocalAccount(ownerId)

    override suspend fun syncOnce(ownerId: String): SyncResult = engine.syncOnce(ownerId)
}
