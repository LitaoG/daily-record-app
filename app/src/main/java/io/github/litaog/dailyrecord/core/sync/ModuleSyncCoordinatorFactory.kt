package io.github.litaog.dailyrecord.core.sync

import kotlinx.coroutines.flow.Flow

/**
 * Wires one module's typed store and remote adapter into the shared conflict
 * engine as an account-level [ModuleSyncCoordinator]. Modules stay isolated:
 * each keeps its own store, remote adapter, collection and record type.
 */
internal fun <L, R> moduleSyncCoordinator(
    store: DailyCountSyncStore<L, R>,
    remote: DailyCountRemoteDataSource<L, R>,
): ModuleSyncCoordinator = object : ModuleSyncCoordinator {
    private val engine = DailyCountSyncEngine(store, remote)

    override fun observeRemote(ownerId: String): Flow<RemoteSnapshot> = engine.observeRemote(ownerId)

    override fun observePendingCount(ownerId: String): Flow<Int> = engine.observePendingCount(ownerId)

    override suspend fun pendingCount(ownerId: String): Int = engine.pendingCount(ownerId)

    override suspend fun applySnapshot(ownerId: String, snapshot: RemoteSnapshot): Int =
        engine.applySnapshot(ownerId, snapshot)

    override suspend fun prepareLocalAccount(ownerId: String): Int =
        engine.prepareLocalAccount(ownerId)

    override suspend fun syncOnce(ownerId: String): SyncResult = engine.syncOnce(ownerId)
}
