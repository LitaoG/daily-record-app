package io.github.litaog.dailyrecord.core.account

import kotlinx.coroutines.CancellationException

internal class CombinedAccountDeletionLocalStore(
    private val stores: List<AccountDeletionLocalStore>,
    private val transactionRunner: suspend (suspend () -> Unit) -> Unit = { operation ->
        operation()
    },
) : AccountDeletionLocalStore {
    init {
        require(stores.isNotEmpty()) { "At least one local account store is required." }
    }

    override suspend fun stageLocalRecoveryCopy(ownerId: String) {
        try {
            stores.forEach { it.stageLocalRecoveryCopy(ownerId) }
        } catch (error: CancellationException) {
            discardAllSafely(ownerId, error)
            throw error
        } catch (error: Exception) {
            discardAllSafely(ownerId, error)
            throw error
        }
    }

    override suspend fun discardLocalRecoveryCopy(ownerId: String) {
        runForAll { it.discardLocalRecoveryCopy(ownerId) }
    }

    override suspend fun promoteLocalRecoveryCopy(ownerId: String) {
        if (stores.any { it.hasLocalRecoveryConflict(ownerId) }) {
            throw AccountDeletionLocalRecoveryConflictException(
                ownerId,
                IllegalStateException("Local records already exist; recovery promotion requires explicit resolution"),
            )
        }
        transactionRunner {
            runForAll { it.promoteLocalRecoveryCopy(ownerId) }
        }
    }

    override suspend fun hasLocalRecoveryConflict(ownerId: String): Boolean =
        stores.any { it.hasLocalRecoveryConflict(ownerId) }

    override suspend fun deleteOwnerCache(ownerId: String) {
        runForAll { it.deleteOwnerCache(ownerId) }
    }

    override suspend fun markOwnerPendingForResync(ownerId: String) {
        runForAll { it.markOwnerPendingForResync(ownerId) }
    }

    private suspend fun discardAllSafely(ownerId: String, primary: Throwable) {
        stores.forEach { store ->
            try {
                store.discardLocalRecoveryCopy(ownerId)
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
