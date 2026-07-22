package io.github.litaog.dailyrecord.core.sync

import kotlinx.coroutines.flow.Flow

internal class HandBrewSyncCoordinator(
    private val store: RoomHandBrewSyncStore,
    private val remote: HandBrewRemoteDataSource,
) {
    fun observeRemote(ownerId: String): Flow<RemoteSnapshot> = remote.observe(ownerId)

    fun observePendingCount(ownerId: String): Flow<Int> = store.observePendingCount(ownerId)

    suspend fun pendingCount(ownerId: String): Int = store.pendingCount(ownerId)

    suspend fun applySnapshot(ownerId: String, snapshot: RemoteSnapshot): Int =
        store.applyRemote(ownerId, snapshot.records)

    suspend fun prepareLocalAccount(ownerId: String): Int {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        return store.adoptLocalRecords(ownerId)
    }

    suspend fun syncOnce(ownerId: String): SyncResult {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        store.adoptLocalRecords(ownerId)
        val initial = remote.fetch(ownerId)
        var downloaded = store.applyRemote(ownerId, initial.records)
        var uploaded = 0

        store.pending(ownerId).forEach { local ->
            val committed = remote.commit(ownerId, local)
            if (store.applyCommitIfUnchanged(ownerId, local, committed)) {
                if (committed.matches(local)) {
                    uploaded += 1
                } else {
                    downloaded += 1
                }
            }
        }

        val confirmed = remote.fetch(ownerId)
        downloaded += store.applyRemote(ownerId, confirmed.records)
        return SyncResult(
            uploaded = uploaded,
            downloaded = downloaded,
            pending = store.pendingCount(ownerId),
        )
    }
}

private fun RemoteHandBrewRecord.matches(local: io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity): Boolean =
    localDate == local.localDate &&
        brewCount == local.brewCount &&
        clientUpdatedAt == local.updatedAt &&
        deleted == local.isDeleted
