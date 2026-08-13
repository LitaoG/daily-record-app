package io.github.litaog.dailyrecord.ui.account

import io.github.litaog.dailyrecord.core.account.AccountDeletionAuthPendingException
import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalCleanupPendingException
import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalRecoveryPendingException
import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalRecoveryConflictException
import io.github.litaog.dailyrecord.core.common.AppCopy
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountDeletionErrorMessageTest {
    @Test
    fun knownFailuresHaveActionableMessages() {
        assertEquals(
            AppCopy.Deletion.wrongPassword,
            accountDeletionErrorMessageForCode("ERROR_WRONG_PASSWORD"),
        )
        assertEquals(
            AppCopy.Deletion.networkAuthError,
            accountDeletionErrorMessageForCode("ERROR_NETWORK_REQUEST_FAILED"),
        )
        assertEquals(
            AppCopy.Deletion.authError,
            accountDeletionErrorMessageForCode("ERROR_REQUIRES_RECENT_LOGIN"),
        )
    }

    @Test
    fun unknownFailureDoesNotClaimThatDeletionSucceeded() {
        assertEquals(
            AppCopy.Deletion.unknownError,
            accountDeletionErrorMessageForCode("UNKNOWN"),
        )
    }

    @Test
    fun networkFailureExplainsThatLocalRecordsRemainSafe() {
        assertEquals(
            AppCopy.Deletion.networkError,
            accountDeletionErrorMessage(IOException("offline")),
        )
    }

    @Test
    fun localCleanupPendingExplainsThatAccountIsAlreadyDeleted() {
        assertEquals(
            AppCopy.Deletion.localCleanupPending,
            accountDeletionErrorMessage(
                AccountDeletionLocalCleanupPendingException("owner", IOException("db")),
            ),
        )
    }

    @Test
    fun authDeletionPendingExplainsThatSyncIsPausedUntilResolution() {
        assertEquals(
            AppCopy.Deletion.authDeletionPending,
            accountDeletionErrorMessage(
                AccountDeletionAuthPendingException("owner", IOException("response lost")),
            ),
        )
    }

    @Test
    fun definitiveAuthFailureDoesNotUseTheUnknownOutcomeMessage() {
        assertEquals(
            AppCopy.Deletion.unknownError,
            accountDeletionErrorMessage(IllegalStateException("recent login required")),
        )
    }

    @Test
    fun localRecoveryPendingExplainsThatSyncIsPausedUntilCleanup() {
        assertEquals(
            AppCopy.Deletion.localRecoveryPending,
            accountDeletionErrorMessage(
                AccountDeletionLocalRecoveryPendingException("owner", IOException("local copy unavailable")),
            ),
        )
    }

    @Test
    fun localRecoveryConflictExplainsThatExistingLocalDataWasProtected() {
        assertEquals(
            AppCopy.Deletion.recoveryConflict,
            accountDeletionErrorMessage(
                AccountDeletionLocalRecoveryConflictException(
                    "owner",
                    IllegalStateException("local data exists"),
                ),
            ),
        )
    }
}
