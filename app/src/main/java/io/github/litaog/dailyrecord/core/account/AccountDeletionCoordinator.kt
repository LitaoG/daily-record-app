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
 * local owner cache could not be cleared. This is not a retryable account
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
        remoteDataSource.deleteAll(ownerId)

        val stagedLocalCopy = localData == LocalDataAfterAccountDeletion.Keep
        if (stagedLocalCopy) {
            localStore.stageLocalRecoveryCopy(ownerId)
        }
        try {
            authRepository.deleteCurrentAccount()
        } catch (error: CancellationException) {
            if (stagedLocalCopy) localStore.discardLocalRecoveryCopySafely(error)
            throw error
        } catch (error: Exception) {
            if (stagedLocalCopy) localStore.discardLocalRecoveryCopySafely(error)
            // The account still exists but its cloud data is already cleared.
            // Re-mark the owner rows pending so the next successful sync
            // rebuilds the cloud instead of leaving the account permanently
            // desynced from this device.
            localStore.markOwnerPendingForResync(ownerId)
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
