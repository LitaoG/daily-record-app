package io.github.litaog.dailyrecord.ui

import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalCleanupPendingException
import io.github.litaog.dailyrecord.core.account.AccountDeletionAuthPendingException
import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalRecoveryPendingException
import io.github.litaog.dailyrecord.core.account.AccountDeletionResult
import io.github.litaog.dailyrecord.core.account.LocalDataAfterAccountDeletion
import io.github.litaog.dailyrecord.core.common.runCatchingPreservingCancellation
import io.github.litaog.dailyrecord.core.sync.AccountDeletionOutcome
import io.github.litaog.dailyrecord.core.sync.DeletionBarrier
import kotlinx.coroutines.CancellationException

/**
 * The account deletion state machine, extracted from the composable so it can
 * be unit-tested and survives configuration changes at the call site.
 *
 * Contract (unchanged from the previous in-composable flow):
 * - the durable barrier is persisted before workers are cancelled,
 * - an interrupted deletion keeps its in-progress marker,
 * - a retryable failure re-enables background sync,
 * - a cleanup-pending outcome keeps a durable cleanup marker.
 */
internal class AccountDeletionOrchestrator(
    private val cancelAndAwait: suspend () -> Unit,
    private val performDeletion: suspend (
        ownerId: String,
        password: String,
        localData: LocalDataAfterAccountDeletion,
    ) -> AccountDeletionResult,
    private val markCleanupPending: (ownerId: String) -> Unit,
    private val onLocalRecordsKept: () -> Unit,
    private val onScheduleSync: (ownerId: String) -> Unit,
) {
    /** Returns the deletion result; throws [CancellationException] when interrupted. */
    suspend fun deleteAccount(
        ownerId: String,
        password: String,
        localData: LocalDataAfterAccountDeletion,
    ): Result<Unit> {
        var began = false
        var outcome = AccountDeletionOutcome.Interrupted
        var result: Result<Unit>? = null
        var barrierFailure: Exception? = null
        var nonCriticalCallbackFailure: Exception? = null
        try {
            // Persist the deletion marker before cancelling any producer.
            // This closes the schedule-vs-delete race and survives process
            // death.
            DeletionBarrier.beginDeletionBlock(ownerId)
            began = true
            DeletionBarrier.awaitDeletionWriters()
            val deletionResult = runCatchingPreservingCancellation {
                cancelAndAwait()
                performDeletion(ownerId, password, localData)
            }
            val cleanupPending = deletionResult.exceptionOrNull() is
                AccountDeletionLocalCleanupPendingException
            val localRecoveryPending = deletionResult.exceptionOrNull() is
                AccountDeletionLocalRecoveryPendingException
            val authPending = deletionResult.getOrNull() as?
                AccountDeletionResult.AuthDeletionPending
            outcome = when {
                cleanupPending -> AccountDeletionOutcome.CleanupPending
                localRecoveryPending -> AccountDeletionOutcome.LocalRecoveryPending
                authPending != null -> AccountDeletionOutcome.AuthDeletionPending
                deletionResult.isSuccess -> AccountDeletionOutcome.Completed
                else -> AccountDeletionOutcome.RetryableFailure
            }
            if (cleanupPending) {
                // Keep both the legacy marker and the durable marker;
                // either one can recover local cleanup after a restart.
                try {
                    markCleanupPending(ownerId)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    // The durable barrier is the source of truth. A failure in
                    // the legacy compatibility preference must not turn a
                    // confirmed Auth deletion into a retryable account
                    // deletion, which could never re-authenticate.
                    deletionResult.exceptionOrNull()?.addSuppressed(error)
                }
            }
            if (((deletionResult.isSuccess && authPending == null && !localRecoveryPending) || cleanupPending) &&
                localData == LocalDataAfterAccountDeletion.Keep
            ) {
                try {
                    onLocalRecordsKept()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    // Local-mode navigation is a UI side effect. A failure to
                    // flip that preference must not turn a confirmed Auth
                    // deletion into a retryable deletion or schedule a write
                    // under an account that no longer exists.
                    nonCriticalCallbackFailure = error
                }
            }
            result = when {
                cleanupPending -> Result.success(Unit)
                localRecoveryPending -> Result.failure(
                    deletionResult.exceptionOrNull()!!,
                )
                authPending != null -> Result.failure(
                    AccountDeletionAuthPendingException(ownerId, authPending.cause),
                )
                else -> deletionResult.map { Unit }
            }
            nonCriticalCallbackFailure?.let { error ->
                result?.exceptionOrNull()?.addSuppressed(error)
            }
        } catch (error: CancellationException) {
            // Do not clear an interrupted marker: a future sync must stay
            // blocked until the user retries or finishes deletion.
            throw error
        } catch (error: Exception) {
            outcome = AccountDeletionOutcome.RetryableFailure
            result = Result.failure(error)
        } finally {
            if (began) {
                try {
                    DeletionBarrier.endDeletionBlock(ownerId, outcome)
                } catch (error: Exception) {
                    // Do not resume scheduling when the durable marker could
                    // not be committed. The barrier keeps a conservative
                    // in-process block until restart.
                    barrierFailure = error
                }
            }
            if (barrierFailure != null) {
                result = Result.failure(barrierFailure!!)
            } else if (outcome == AccountDeletionOutcome.RetryableFailure) {
                // A failed deletion must not leave pending local records
                // stuck: re-enable the normal background sync path.
                try {
                    onScheduleSync(ownerId)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    // Scheduling is best effort; keep the original deletion
                    // failure as the public result and let the next local
                    // change or app start schedule the retry.
                    result?.exceptionOrNull()?.addSuppressed(error)
                }
            }
        }
        return requireNotNull(result)
    }
}
