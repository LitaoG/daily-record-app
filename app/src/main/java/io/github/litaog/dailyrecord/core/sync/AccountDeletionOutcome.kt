package io.github.litaog.dailyrecord.core.sync

internal enum class AccountDeletionOutcome {
    Completed,
    CleanupPending,
    /** Auth deletion was requested but its final result is unknown. */
    AuthDeletionPending,
    /** The temporary local recovery copy could not be safely discarded. */
    LocalRecoveryPending,
    RetryableFailure,
    Interrupted,
}

internal class AccountDeletionInProgressException(
    ownerId: String,
) : IllegalStateException("Cloud writes are blocked while deleting account $ownerId")
