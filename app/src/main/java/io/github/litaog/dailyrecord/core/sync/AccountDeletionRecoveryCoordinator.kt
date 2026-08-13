package io.github.litaog.dailyrecord.core.sync

import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalRecoveryConflictException
import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalStore
import io.github.litaog.dailyrecord.core.auth.AuthAccountPresence
import io.github.litaog.dailyrecord.core.auth.AuthRepository
import io.github.litaog.dailyrecord.core.common.runCatchingPreservingCancellation
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import kotlinx.coroutines.delay

internal data class AccountDeletionRecoverySnapshot(
    val inProgressOwnerIds: Set<String>,
    val cloudDeletionPendingOwnerIds: Set<String>,
    val authDeletionPendingOwnerIds: Set<String>,
    val cleanupPendingOwnerIds: Set<String>,
    val recoveryCopyPendingOwnerIds: Set<String>,
    val recoveryCopyReadyOwnerIds: Set<String>,
    val conflictOwnerIds: Set<String> = emptySet(),
) {
    val hasDurableRecovery: Boolean
        get() = inProgressOwnerIds.isNotEmpty() ||
            cloudDeletionPendingOwnerIds.isNotEmpty() ||
            authDeletionPendingOwnerIds.isNotEmpty() ||
            cleanupPendingOwnerIds.isNotEmpty() ||
            recoveryCopyPendingOwnerIds.isNotEmpty() ||
            recoveryCopyReadyOwnerIds.any { it !in cleanupPendingOwnerIds } ||
            conflictOwnerIds.isNotEmpty()

    val hasPendingRecovery: Boolean
        get() = hasDurableRecovery

    val blocksLocalMode: Boolean
        get() = hasPendingRecovery

    val hasPreAuthRecovery: Boolean
        get() {
            val cloudPending = cloudDeletionPendingOwnerIds - authDeletionPendingOwnerIds
            val recoveryCopies = recoveryCopyPendingOwnerIds + recoveryCopyReadyOwnerIds
            val interrupted = (inProgressOwnerIds + recoveryCopies) -
                authDeletionPendingOwnerIds - cloudPending - cleanupPendingOwnerIds
            return cloudPending.isNotEmpty() ||
                cleanupPendingOwnerIds.isNotEmpty() ||
                interrupted.isNotEmpty()
        }
}

/**
 * Owns account-deletion recovery decisions after the UI process is restarted.
 *
 * It deliberately depends on ports (`AccountDeletionLocalStore` and
 * `AuthRepository`) rather than Compose state. The root only renders the
 * snapshot and decides whether the current Auth screen may continue.
 */
internal class AccountDeletionRecoveryCoordinator(
    private val localStore: AccountDeletionLocalStore,
) {
    fun snapshot(conflictOwnerIds: Set<String> = emptySet()): AccountDeletionRecoverySnapshot {
        return AccountDeletionRecoverySnapshot(
            inProgressOwnerIds = DeletionBarrier.inProgressOwnerIds(),
            cloudDeletionPendingOwnerIds = DeletionBarrier.cloudDeletionPendingOwnerIds(),
            authDeletionPendingOwnerIds = DeletionBarrier.authDeletionStartedOwnerIds(),
            cleanupPendingOwnerIds = DeletionBarrier.pendingCleanupOwnerIds(),
            recoveryCopyPendingOwnerIds = DeletionBarrier.localRecoveryCopyPendingOwnerIds(),
            recoveryCopyReadyOwnerIds = DeletionBarrier.localRecoveryCopyReadyOwnerIds(),
            conflictOwnerIds = conflictOwnerIds,
        )
    }

    fun isDeletionBlocked(ownerId: String): Boolean =
        DeletionBarrier.isDeletionBlocked(ownerId)

    fun isAnyDeletionBlocked(): Boolean = DeletionBarrier.isDeletionBlocked(null)

    /**
     * Resolves only phases for which Auth was not invoked, then retries until
     * no such durable work remains. This path never initializes Firebase.
     */
    suspend fun resolvePreAuthUntilSettled(
        onAttemptCompleted: (AccountDeletionRecoverySnapshot) -> Unit = {},
        shouldContinue: () -> Boolean = { true },
        delayMillis: (attempt: Int) -> Long = ::deletionRecoveryRetryDelayMillis,
        delayBlock: suspend (Long) -> Unit = ::delay,
    ) {
        var retryAttempt = 0
        while (true) {
            val conflicts = retryPendingOwnerCleanup()
            resolvePreAuthMarkers()
            val snapshot = snapshot(conflicts)
            onAttemptCompleted(snapshot)
            if (!shouldContinue() || !snapshot.hasPreAuthRecovery) return
            delayBlock(delayMillis(retryAttempt++))
        }
    }

    /**
     * Resolves Auth-pending entries and retries local cleanup until the caller
     * says that the current signed-in session should stop waiting.
     */
    suspend fun resolveUntilSettled(
        authRepository: AuthRepository,
        onAttemptCompleted: (AccountDeletionRecoverySnapshot) -> Unit = {},
        shouldContinue: () -> Boolean = { true },
        delayMillis: (attempt: Int) -> Long = ::deletionRecoveryRetryDelayMillis,
        delayBlock: suspend (Long) -> Unit = ::delay,
    ) {
        var retryAttempt = 0
        while (true) {
            val conflicts = resolvePendingAuthDeletions(authRepository)
            val snapshot = snapshot(conflicts)
            onAttemptCompleted(snapshot)
            if (!shouldContinue() || !snapshot.hasPendingRecovery) return
            delayBlock(delayMillis(retryAttempt++))
        }
    }

    suspend fun resolvePendingAuthDeletions(authRepository: AuthRepository): Set<String> {
        resolvePreAuthMarkers()
        val conflicts = mutableSetOf<String>()
        DeletionBarrier.authDeletionStartedOwnerIds().forEach { ownerId ->
            val hasPendingCopy = ownerId in DeletionBarrier.localRecoveryCopyPendingOwnerIds()
            val hasReadyCopy = ownerId in DeletionBarrier.localRecoveryCopyReadyOwnerIds()
            when (val presence = authRepository.inspectAccountPresence(ownerId)) {
                AuthAccountPresence.Absent -> {
                    val recoveryCopyReady = if (hasPendingCopy && !hasReadyCopy) {
                        runCatchingPreservingCancellation {
                            localStore.stageLocalRecoveryCopy(ownerId)
                            DeletionBarrier.markLocalRecoveryCopyReady(ownerId)
                        }.isSuccess
                    } else {
                        true
                    }
                    if (recoveryCopyReady) {
                        runCatchingPreservingCancellation {
                            localStore.promoteLocalRecoveryCopy(ownerId)
                            DeletionBarrier.promoteAuthDeletionCleanup(ownerId)
                        }.onFailure { error ->
                            if (error is AccountDeletionLocalRecoveryConflictException) {
                                conflicts += ownerId
                            }
                        }
                    }
                }

                AuthAccountPresence.SignedOut,
                is AuthAccountPresence.Unknown,
                -> Unit

                AuthAccountPresence.Exists -> {
                    runCatchingPreservingCancellation {
                        if (hasPendingCopy && !hasReadyCopy) {
                            localStore.stageLocalRecoveryCopy(ownerId)
                            DeletionBarrier.markLocalRecoveryCopyReady(ownerId)
                        }
                        if (hasPendingCopy || hasReadyCopy) {
                            localStore.discardLocalRecoveryCopy(ownerId)
                        }
                        localStore.markOwnerPendingForResync(ownerId)
                        DeletionBarrier.clearLocalRecoveryCopyPending(ownerId)
                        DeletionBarrier.resolveAuthDeletionAccountStillExists(ownerId)
                    }
                }
            }
        }
        conflicts += retryPendingOwnerCleanup()
        return conflicts
    }

    suspend fun resolveRecoveryConflict(ownerId: String) {
        localStore.deleteOwnerCache(LOCAL_OWNER_ID)
        localStore.promoteLocalRecoveryCopy(ownerId)
        DeletionBarrier.completeDeletionCleanup(ownerId)
    }

    private suspend fun retryPendingOwnerCleanup(): Set<String> {
        val conflicts = mutableSetOf<String>()
        val owners = DeletionBarrier.pendingCleanupOwnerIds()
        owners.forEach { ownerId ->
            runCatchingPreservingCancellation {
                if (ownerId in DeletionBarrier.localRecoveryCopyReadyOwnerIds()) {
                    localStore.promoteLocalRecoveryCopy(ownerId)
                }
                localStore.deleteOwnerCache(ownerId)
            }.onSuccess {
                runCatching {
                    DeletionBarrier.completeDeletionCleanup(ownerId)
                }
            }.onFailure { error ->
                if (error is AccountDeletionLocalRecoveryConflictException) {
                    conflicts += ownerId
                }
            }
        }
        return conflicts
    }

    private suspend fun resolvePreAuthMarkers() {
        val current = snapshot()
        val authPendingOwners = current.authDeletionPendingOwnerIds
        val cloudPendingOwners = current.cloudDeletionPendingOwnerIds - authPendingOwners
        val cleanupPendingOwners = current.cleanupPendingOwnerIds
        val recoveryCopyOwners = current.recoveryCopyPendingOwnerIds +
            current.recoveryCopyReadyOwnerIds
        val interruptedOwners = (
            current.inProgressOwnerIds + recoveryCopyOwners
            ) - authPendingOwners - cloudPendingOwners - cleanupPendingOwners
        if (cloudPendingOwners.isEmpty() && interruptedOwners.isEmpty()) return

        cloudPendingOwners.forEach { ownerId ->
            runCatchingPreservingCancellation {
                discardRecoveryCopyIfPresent(ownerId)
                localStore.markOwnerPendingForResync(ownerId)
                DeletionBarrier.clearLocalRecoveryCopyPending(ownerId)
                DeletionBarrier.resolveAuthDeletionAccountStillExists(ownerId)
            }
        }

        interruptedOwners.forEach { ownerId ->
            runCatchingPreservingCancellation {
                discardRecoveryCopyIfPresent(ownerId)
                localStore.markOwnerPendingForResync(ownerId)
                DeletionBarrier.resolveInterruptedDeletion(ownerId)
            }
        }
    }

    private suspend fun discardRecoveryCopyIfPresent(ownerId: String) {
        if (ownerId in DeletionBarrier.localRecoveryCopyPendingOwnerIds() ||
            ownerId in DeletionBarrier.localRecoveryCopyReadyOwnerIds()
        ) {
            localStore.discardLocalRecoveryCopy(ownerId)
        }
    }
}

internal fun deletionRecoveryRetryDelayMillis(attempt: Int): Long =
    (DELETION_RECOVERY_RETRY_DELAY_MILLIS * (1L shl attempt.coerceIn(0, 4)))
        .coerceAtMost(60_000L)

private const val DELETION_RECOVERY_RETRY_DELAY_MILLIS = 5_000L
