package io.github.litaog.dailyrecord.core.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.merge

/**
 * Account-level orchestration only. Each module keeps its own typed store,
 * remote mapping, collection and conflict logic.
 *
 * Module operations are isolated: one module failing must never skip the other
 * module's work. Failures are collected and the first one is rethrown after
 * every module has run, so retries still target the failing module while the
 * successful module stays idempotent.
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
        modules.runEachIsolated { it.pendingCount(ownerId) }

    override suspend fun applySnapshot(ownerId: String, snapshot: RemoteSnapshot): Int =
        modules.runEachIsolated { it.applySnapshot(ownerId, snapshot) }

    suspend fun prepareLocalAccount(ownerId: String): Int =
        modules.runEachIsolated { it.prepareLocalAccount(ownerId) }

    override suspend fun syncOnce(ownerId: String): SyncResult {
        var combined = SyncResult(uploaded = 0, downloaded = 0, pending = 0)
        var primary: Exception? = null
        for (module in modules) {
            try {
                combined = combined + module.syncOnce(ownerId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (primary == null) primary = error else primary.addSuppressed(error)
            }
        }
        primary?.let { throw it }
        return combined
    }
}

internal interface ModuleSyncCoordinator : AccountSyncOperations {
    suspend fun prepareLocalAccount(ownerId: String): Int
}

private suspend inline fun Iterable<ModuleSyncCoordinator>.runEachIsolated(
    crossinline operation: suspend (ModuleSyncCoordinator) -> Int,
): Int {
    var total = 0
    var primary: Exception? = null
    for (module in this) {
        try {
            total += operation(module)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (primary == null) primary = error else primary.addSuppressed(error)
        }
    }
    primary?.let { throw it }
    return total
}

private operator fun SyncResult.plus(other: SyncResult) = SyncResult(
    uploaded = uploaded + other.uploaded,
    downloaded = downloaded + other.downloaded,
    pending = pending + other.pending,
    rejectedRemoteRecords = rejectedRemoteRecords + other.rejectedRemoteRecords,
)
