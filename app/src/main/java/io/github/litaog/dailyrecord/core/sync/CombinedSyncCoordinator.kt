package io.github.litaog.dailyrecord.core.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge

/**
 * Account-level orchestration only. Each module keeps its own typed store,
 * remote mapping, collection and conflict logic.
 */
internal class CombinedSyncCoordinator(
    private val handBrew: HandBrewSyncCoordinator,
    private val sex: SexSyncCoordinator,
) : AccountSyncOperations {
    override fun observeRemote(ownerId: String): Flow<RemoteSnapshot> = merge(
        handBrew.observeRemote(ownerId),
        sex.observeRemote(ownerId),
    )

    override fun observePendingCount(ownerId: String): Flow<Int> = combine(
        handBrew.observePendingCount(ownerId),
        sex.observePendingCount(ownerId),
    ) { handBrewCount, sexCount -> handBrewCount + sexCount }

    override suspend fun pendingCount(ownerId: String): Int =
        handBrew.pendingCount(ownerId) + sex.pendingCount(ownerId)

    override suspend fun applySnapshot(ownerId: String, snapshot: RemoteSnapshot): Int =
        handBrew.applySnapshot(ownerId, snapshot) + sex.applySnapshot(ownerId, snapshot)

    suspend fun prepareLocalAccount(ownerId: String): Int =
        handBrew.prepareLocalAccount(ownerId) + sex.prepareLocalAccount(ownerId)

    override suspend fun syncOnce(ownerId: String): SyncResult {
        val handBrewResult = handBrew.syncOnce(ownerId)
        val sexResult = sex.syncOnce(ownerId)
        return SyncResult(
            uploaded = handBrewResult.uploaded + sexResult.uploaded,
            downloaded = handBrewResult.downloaded + sexResult.downloaded,
            pending = handBrewResult.pending + sexResult.pending,
            rejectedRemoteRecords =
                handBrewResult.rejectedRemoteRecords + sexResult.rejectedRemoteRecords,
        )
    }
}
