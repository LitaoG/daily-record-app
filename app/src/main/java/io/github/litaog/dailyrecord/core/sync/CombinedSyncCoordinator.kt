package io.github.litaog.dailyrecord.core.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge

/**
 * Account-level orchestration only. Each module keeps its own typed store,
 * remote mapping, collection and conflict logic.
 */
internal class CombinedSyncCoordinator(
    private val modules: List<ModuleSyncCoordinator>,
) : AccountSyncOperations {
    constructor(
        handBrew: HandBrewSyncCoordinator,
        sex: SexSyncCoordinator,
    ) : this(listOf(handBrew, sex))

    init {
        require(modules.isNotEmpty()) { "At least one sync module is required." }
    }

    override fun observeRemote(ownerId: String): Flow<RemoteSnapshot> =
        merge(*modules.map { it.observeRemote(ownerId) }.toTypedArray())

    override fun observePendingCount(ownerId: String): Flow<Int> = combine(
        modules.map { it.observePendingCount(ownerId) },
    ) { counts -> counts.sum() }

    override suspend fun pendingCount(ownerId: String): Int =
        modules.sumOfSuspend { it.pendingCount(ownerId) }

    override suspend fun applySnapshot(ownerId: String, snapshot: RemoteSnapshot): Int =
        modules.sumOfSuspend { it.applySnapshot(ownerId, snapshot) }

    suspend fun prepareLocalAccount(ownerId: String): Int =
        modules.sumOfSuspend { it.prepareLocalAccount(ownerId) }

    override suspend fun syncOnce(ownerId: String): SyncResult {
        return modules.fold(
            SyncResult(uploaded = 0, downloaded = 0, pending = 0),
        ) { combined, module ->
            combined + module.syncOnce(ownerId)
        }
    }
}

internal interface ModuleSyncCoordinator : AccountSyncOperations {
    suspend fun prepareLocalAccount(ownerId: String): Int
}

private suspend inline fun <T> Iterable<T>.sumOfSuspend(
    crossinline value: suspend (T) -> Int,
): Int {
    var total = 0
    for (item in this) total += value(item)
    return total
}

private operator fun SyncResult.plus(other: SyncResult) = SyncResult(
    uploaded = uploaded + other.uploaded,
    downloaded = downloaded + other.downloaded,
    pending = pending + other.pending,
    rejectedRemoteRecords = rejectedRemoteRecords + other.rejectedRemoteRecords,
)
