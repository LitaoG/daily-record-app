package io.github.litaog.dailyrecord.core.account

import io.github.litaog.dailyrecord.core.sync.RoomHandBrewSyncStore
import io.github.litaog.dailyrecord.core.sync.RoomSexSyncStore
import kotlinx.coroutines.CancellationException

internal class CombinedAccountDeletionLocalStore(
    private val stores: List<AccountDeletionLocalStore>,
) : AccountDeletionLocalStore {
    constructor(
        handBrew: RoomHandBrewSyncStore,
        sex: RoomSexSyncStore,
    ) : this(listOf(handBrew, sex))

    init {
        require(stores.isNotEmpty()) { "At least one local account store is required." }
    }

    override suspend fun stageLocalRecoveryCopy(ownerId: String) {
        try {
            stores.forEach { it.stageLocalRecoveryCopy(ownerId) }
        } catch (error: CancellationException) {
            discardAllSafely(error)
            throw error
        } catch (error: Exception) {
            discardAllSafely(error)
            throw error
        }
    }

    override suspend fun discardLocalRecoveryCopy() {
        runForAll { it.discardLocalRecoveryCopy() }
    }

    override suspend fun deleteOwnerCache(ownerId: String) {
        runForAll { it.deleteOwnerCache(ownerId) }
    }

    override suspend fun markOwnerPendingForResync(ownerId: String) {
        runForAll { it.markOwnerPendingForResync(ownerId) }
    }

    private suspend fun discardAllSafely(primary: Throwable) {
        stores.forEach { store ->
            try {
                store.discardLocalRecoveryCopy()
            } catch (error: CancellationException) {
                // Cancellation must remain cooperative; otherwise a cancelled
                // deletion can report completion while cleanup is incomplete.
                throw error
            } catch (error: Exception) {
                primary.addSuppressed(error)
            }
        }
    }

    private suspend fun runForAll(operation: suspend (AccountDeletionLocalStore) -> Unit) {
        var primary: Exception? = null
        stores.forEach { store ->
            try {
                operation(store)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (primary == null) primary = error else primary?.addSuppressed(error)
            }
        }
        primary?.let { throw it }
    }
}
