package io.github.litaog.dailyrecord.core.account

import io.github.litaog.dailyrecord.core.auth.AuthDeletionResult
import io.github.litaog.dailyrecord.core.auth.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

enum class LocalDataAfterAccountDeletion {
    Keep,
    Delete,
}

sealed interface AccountDeletionResult {
    data object Completed : AccountDeletionResult

    /**
     * Auth accepted the delete request, but the client did not receive a
     * definitive result. Local recovery data and the deletion barrier must be
     * retained until the account's state is explicitly resolved.
     */
    data class AuthDeletionPending(val cause: Throwable) : AccountDeletionResult
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

internal class AccountDeletionAuthPendingException(
    val ownerId: String,
    cause: Throwable,
) : IllegalStateException("Account deletion result is unknown; synchronization remains blocked", cause)

internal class AccountDeletionLocalRecoveryPendingException(
    val ownerId: String,
    cause: Throwable,
) : IllegalStateException("Local recovery copy cleanup is pending", cause)

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
    /** Persist the post-cloud phase before local cache cleanup starts. */
    private val markCloudDeletionComplete: (ownerId: String) -> Unit = {},
    /** Persist auth-delete intent immediately before invoking Firebase Auth. */
    private val markAuthDeletionStarted: (ownerId: String) -> Unit = {},
    /** Persist the optional local recovery-copy intent before touching Room. */
    private val markLocalRecoveryCopyPending: (ownerId: String) -> Unit = {},
    /** Persist that the optional local recovery copy is complete. */
    private val markLocalRecoveryCopyReady: (ownerId: String) -> Unit = {},
    /** Persist definitive Auth completion before local owner cleanup. */
    private val markAuthDeletionComplete: (ownerId: String) -> Unit = {},
) {
    suspend fun deleteAccount(
        ownerId: String,
        password: String,
        localData: LocalDataAfterAccountDeletion,
    ): AccountDeletionResult {
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

        try {
            // Cross-system deletion has no transaction. Persist the cloud phase
            // before starting Auth so an unknown Auth response cannot be
            // mistaken for a safe pre-deletion failure after a process death.
            markCloudDeletionComplete(ownerId)
        } catch (error: CancellationException) {
            localStore.markOwnerPendingForResyncSafely(ownerId, error)
            throw error
        } catch (error: Exception) {
            localStore.markOwnerPendingForResyncSafely(ownerId, error)
            throw error
        }

        var localRecoveryCopyIntent = false
        var localRecoveryCopyCleanupConfirmed = false
        var authDeletionStarted = false
        var authDeleted = false
        try {
            if (localData == LocalDataAfterAccountDeletion.Keep) {
                try {
                    markLocalRecoveryCopyPending(ownerId)
                    localRecoveryCopyIntent = true
                    localStore.stageLocalRecoveryCopy(ownerId)
                    markLocalRecoveryCopyReady(ownerId)
                } catch (error: CancellationException) {
                    if (!localRecoveryCopyIntent || localStore.discardLocalRecoveryCopySafely(error)) {
                        localRecoveryCopyCleanupConfirmed = true
                    } else {
                        throw AccountDeletionLocalRecoveryPendingException(ownerId, error)
                    }
                    throw error
                } catch (error: Exception) {
                    if (!localRecoveryCopyIntent || localStore.discardLocalRecoveryCopySafely(error)) {
                        localRecoveryCopyCleanupConfirmed = true
                    } else {
                        throw AccountDeletionLocalRecoveryPendingException(ownerId, error)
                    }
                    throw error
                }
            }
            markAuthDeletionStarted(ownerId)
            authDeletionStarted = true
            when (val authResult = authRepository.deleteCurrentAccount()) {
                AuthDeletionResult.Completed -> {
                    authDeleted = true
                    // This marker is the durable proof that the Auth account is
                    // gone. If the process dies after this point, startup can
                    // clean local data without guessing from a signed-out SDK.
                    markAuthDeletionComplete(ownerId)
                }
                is AuthDeletionResult.Unknown -> {
                    // Do not discard the staged copy or mark rows pending. The
                    // Firebase Task may have completed remotely after its
                    // response was lost, so resyncing here could resurrect
                    // data under an account that was already deleted.
                    return AccountDeletionResult.AuthDeletionPending(authResult.cause)
                }
            }
        } catch (error: CancellationException) {
            if (authDeleted) {
                // Auth is gone; do not re-mark cloud data for resync or discard
                // the recovery copy. Startup must finish local cleanup.
                throw AccountDeletionLocalCleanupPendingException(ownerId, error)
            }
            if (authDeletionStarted) {
                // Cancellation after the Auth request was launched is an
                // unknown remote outcome, not proof that the account remains.
                // Keep both the recovery copy and the owner block.
                return AccountDeletionResult.AuthDeletionPending(error)
            }
            if (localRecoveryCopyIntent && !localRecoveryCopyCleanupConfirmed) {
                if (localStore.discardLocalRecoveryCopySafely(error)) {
                    localRecoveryCopyCleanupConfirmed = true
                } else {
                    throw AccountDeletionLocalRecoveryPendingException(ownerId, error)
                }
            }
            // Cloud deletion has already completed, but cancellation means the auth
            // deletion may not have reached Firebase. Preserve the recovery path for
            // a still-existing account even though the coroutine is being cancelled.
            localStore.markOwnerPendingForResyncSafely(ownerId, error)
            throw error
        } catch (error: Exception) {
            if (authDeleted) {
                throw AccountDeletionLocalCleanupPendingException(ownerId, error)
            }
            if (authDeletionStarted && error !is AccountDeletionAuthPendingException) {
                return AccountDeletionResult.AuthDeletionPending(error)
            }
            if (error is AccountDeletionLocalRecoveryPendingException) throw error
            if (localRecoveryCopyIntent && !localRecoveryCopyCleanupConfirmed) {
                if (localStore.discardLocalRecoveryCopySafely(error)) {
                    localRecoveryCopyCleanupConfirmed = true
                } else {
                    throw AccountDeletionLocalRecoveryPendingException(ownerId, error)
                }
            }
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
        return AccountDeletionResult.Completed
    }
}

private suspend fun AccountDeletionLocalStore.discardLocalRecoveryCopySafely(primary: Throwable): Boolean {
    return try {
        withContext(NonCancellable) {
            discardLocalRecoveryCopy()
        }
        true
    } catch (error: CancellationException) {
        throw error
    } catch (cleanupError: Exception) {
        primary.addSuppressed(cleanupError)
        false
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
