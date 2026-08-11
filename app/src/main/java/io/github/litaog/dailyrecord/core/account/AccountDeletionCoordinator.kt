package io.github.litaog.dailyrecord.core.account

import io.github.litaog.dailyrecord.core.auth.AuthRepository
import io.github.litaog.dailyrecord.core.sync.AccountRemoteDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

enum class LocalDataAfterAccountDeletion {
    Keep,
    Delete,
}

/**
 * Thrown when the Firebase account and cloud data were already deleted but the
 * owner cache could not be cleared. This is not a retryable account
 * deletion failure: re-authentication is impossible because the account no
 * longer exists. The owner id is kept so a later startup can retry the local
 * cleanup.
 */
internal class AccountDeletionLocalCleanupPendingException(
    val ownerId: String,
    cause: Throwable,
) : IllegalStateException("Account deleted but local owner cache cleanup is pending", cause)

internal interface AccountDeletionLocalStore {
    suspend fun stageLocalRecoveryCopy(ownerId: String)

    suspend fun discardLocalRecoveryCopy()

    suspend fun deleteOwnerCache(ownerId: String)

    /**
     * Re-marks the owner's rows pending for sync after a failed deletion left
     * the cloud cleared while the account still exists. The next successful
     * sync then rebuilds the cloud data instead of leaving the account
     * permanently desynced from the device.
     */
    suspend fun markOwnerPendingForResync(ownerId: String)
}

internal class AccountDeletionCoordinator(
    private val authRepository: AuthRepository,
    private val remoteDataSource: AccountRemoteDataStore,
    private val localStore: AccountDeletionLocalStore,
) {
    suspend fun deleteAccount(
        ownerId: String,
        password: String,
        localData: LocalDataAfterAccountDeletion,
    ) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        require(password.isNotBlank()) { "password must not be blank" }

        authRepository.reauthenticate(password)
        try {
            remoteDataSource.deleteAll(ownerId)
        } catch (error: CancellationException) {
            // A batch delete may have completed before cancellation was observed.
            // Keep the owner pending so a still-live account can rebuild any
            // partially deleted cloud data on the next successful sync.
            localStore.markOwnerPendingForResyncSafely(ownerId, error)
            throw error
        } catch (error: Exception) {
            // deleteAll can span several batches/modules, so an error does not
            // prove that no cloud records were removed. Re-queue the owner before
            // returning the original failure; retrying deletion remains safe.
            localStore.markOwnerPendingForResyncSafely(ownerId, error)
            throw error
        }

        var stagedLocalCopy = false
        try {
            if (localData == LocalDataAfterAccountDeletion.Keep) {
                localStore.stageLocalRecoveryCopy(ownerId)
                stagedLocalCopy = true
            }
            authRepository.deleteCurrentAccount()
        } catch (error: CancellationException) {
            if (stagedLocalCopy) localStore.discardLocalRecoveryCopySafely(error)
            // Cloud deletion has already completed, but cancellation means the auth
            // deletion may not have reached Firebase. Preserve the recovery path for
            // a still-existing account even though the coroutine is being cancelled.
            localStore.markOwnerPendingForResyncSafely(ownerId, error)
            throw error
        } catch (error: Exception) {
            if (stagedLocalCopy) localStore.discardLocalRecoveryCopySafely(error)
            // The account still exists but its cloud data is already cleared.
            // Re-mark the owner rows pending so the next successful sync
            // rebuilds the cloud instead of leaving the account permanently
            // desynced from this device. A failed re-mark must not mask the
            // original deletion failure.
            try {
                localStore.markOwnerPendingForResync(ownerId)
            } catch (markError: Exception) {
                error.addSuppressed(markError)
            }
            throw error
        }
        try {
            localStore.deleteOwnerCache(ownerId)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // The account and cloud data are already gone. Report a dedicated
            // recoverable cleanup state instead of a retryable deletion failure,
            // because re-authentication is impossible once the account is deleted.
            throw AccountDeletionLocalCleanupPendingException(ownerId, error)
        }
    }
}

private suspend fun AccountDeletionLocalStore.discardLocalRecoveryCopySafely(primary: Throwable) {
    try {
        withContext(NonCancellable) {
            discardLocalRecoveryCopy()
        }
    } catch (cleanupError: Exception) {
        primary.addSuppressed(cleanupError)
    }
}

private suspend fun AccountDeletionLocalStore.markOwnerPendingForResyncSafely(
    ownerId: String,
    primary: Throwable,
) {
    try {
        withContext(NonCancellable) {
            markOwnerPendingForResync(ownerId)
        }
    } catch (cleanupError: Exception) {
        primary.addSuppressed(cleanupError)
    }
}

