package io.github.litaog.dailyrecord.ui

import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalCleanupPendingException
import io.github.litaog.dailyrecord.core.account.AccountDeletionAuthPendingException
import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalRecoveryPendingException
import io.github.litaog.dailyrecord.core.account.AccountDeletionResult
import io.github.litaog.dailyrecord.core.account.LocalDataAfterAccountDeletion
import io.github.litaog.dailyrecord.core.sync.DeletionBarrier
import io.github.litaog.dailyrecord.core.sync.DeletionStateStore
import io.github.litaog.dailyrecord.core.sync.InMemoryDeletionStateStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AccountDeletionOrchestratorTest {
    private val store = InMemoryDeletionStateStore()
        private val ownerId = "owner-a"
    private var deletionInvoked = false
    private var cleanupMarked: String? = null
    private var localRecordsKept = false
    private var syncRescheduled: String? = null
    private var cancelAndAwait: suspend () -> Unit = {}

    private fun orchestrator(
        deleteAccount: suspend (
            ownerId: String,
            password: String,
            localData: LocalDataAfterAccountDeletion,
        ) -> AccountDeletionResult = { _, _, _ ->
            deletionInvoked = true
            AccountDeletionResult.Completed
        },
        markCleanupPending: (String) -> Unit = { cleanupMarked = it },
        onLocalRecordsKept: () -> Unit = { localRecordsKept = true },
    ) = AccountDeletionOrchestrator(
        cancelAndAwait = { cancelAndAwait() },
        performDeletion = deleteAccount,
        markCleanupPending = markCleanupPending,
        onLocalRecordsKept = onLocalRecordsKept,
        onScheduleSync = { syncRescheduled = it },
    )

    @Before
    fun installMemoryStore() {
        DeletionBarrier.installStateStoreForTest(store)
        DeletionBarrier.endDeletionBlock()
    }

    @After
    fun reset() {
        DeletionBarrier.endDeletionBlock()
    }

    @Test
    fun successfulDeletionClearsBarrierAndKeepsLocalDataFlag() = runBlocking {
        val result = orchestrator(deleteAccount = { _, _, _ ->
            deletionInvoked = true
            AccountDeletionResult.Completed
        }).deleteAccount(
            ownerId = ownerId,
            password = "pw",
            localData = LocalDataAfterAccountDeletion.Keep,
        )

        assertTrue(deletionInvoked)
        assertTrue(localRecordsKept)
        assertNull(cleanupMarked)
        assertNull(syncRescheduled)
        assertFalse(DeletionBarrier.isDeletionBlocked(ownerId))
        assertTrue(store.readOwners("in_progress_owner_ids").isEmpty())
    }

    @Test
    fun cancellationRunsAfterDurableBarrierIsWritten() = runBlocking {
        var barrierWasBlocked = false
        cancelAndAwait = { barrierWasBlocked = DeletionBarrier.isDeletionBlocked(ownerId) }

        orchestrator().deleteAccount(
            ownerId = ownerId,
            password = "pw",
            localData = LocalDataAfterAccountDeletion.Delete,
        )

        assertTrue(barrierWasBlocked)
    }

    @Test
    fun cleanupPendingMarksCleanupAndTreatsResultAsSuccess() = runBlocking {
        val result = orchestrator(
            deleteAccount = { _, _, _ ->
                throw AccountDeletionLocalCleanupPendingException(ownerId = ownerId, cause = RuntimeException("cleanup pending"))
            },
        ).deleteAccount(
            ownerId = ownerId,
            password = "pw",
            localData = LocalDataAfterAccountDeletion.Delete,
        )

        assertTrue(result.isSuccess)
        assertEquals(ownerId, cleanupMarked)
        assertFalse(localRecordsKept)
        // A cleanup-pending owner stays blocked until the cleanup completes.
        assertTrue(DeletionBarrier.isDeletionBlocked(ownerId))
        assertTrue(store.readOwners("cleanup_pending_owner_ids").contains(ownerId))
    }

    @Test
    fun authPendingKeepsBarrierAndDoesNotScheduleSync() = runBlocking {
        val cause = IllegalStateException("response lost")
        val result = orchestrator(
            deleteAccount = { _, _, _ -> AccountDeletionResult.AuthDeletionPending(cause) },
        ).deleteAccount(
            ownerId = ownerId,
            password = "pw",
            localData = LocalDataAfterAccountDeletion.Keep,
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is AccountDeletionAuthPendingException)
        assertTrue(DeletionBarrier.isDeletionBlocked(ownerId))
        assertTrue(store.readOwners("auth_deletion_started_owner_ids").contains(ownerId))
        assertNull(syncRescheduled)
        assertFalse(localRecordsKept)
    }

    @Test
    fun localRecoveryPendingKeepsGlobalRecoveryBarrier() = runBlocking {
        val result = orchestrator(
            deleteAccount = { _, _, _ ->
                throw AccountDeletionLocalRecoveryPendingException(
                    ownerId = ownerId,
                    cause = IllegalStateException("local copy cleanup pending"),
                )
            },
        ).deleteAccount(
            ownerId = ownerId,
            password = "pw",
            localData = LocalDataAfterAccountDeletion.Keep,
        )

        assertTrue(result.isFailure)
        assertTrue(DeletionBarrier.isDeletionBlocked(ownerId))
        assertTrue(DeletionBarrier.cloudDeletionPendingOwnerIds().contains(ownerId))
        assertNull(syncRescheduled)
    }

    @Test
    fun cleanupMarkerFailureDoesNotUnlockDurableCleanup() = runBlocking {
        val result = orchestrator(
            deleteAccount = { _, _, _ ->
                throw AccountDeletionLocalCleanupPendingException(
                    ownerId = ownerId,
                    cause = RuntimeException("cleanup pending"),
                )
            },
            markCleanupPending = { error("legacy preference unavailable") },
        ).deleteAccount(
            ownerId = ownerId,
            password = "pw",
            localData = LocalDataAfterAccountDeletion.Delete,
        )

        assertTrue(result.isSuccess)
        assertTrue(DeletionBarrier.isDeletionBlocked(ownerId))
        assertTrue(store.readOwners("cleanup_pending_owner_ids").contains(ownerId))
        assertNull(syncRescheduled)
    }

    @Test
    fun localModeCallbackFailureDoesNotTurnCompletedDeletionIntoRetry() = runBlocking {
        val result = orchestrator(
            onLocalRecordsKept = { error("local preference unavailable") },
        ).deleteAccount(
            ownerId = ownerId,
            password = "pw",
            localData = LocalDataAfterAccountDeletion.Keep,
        )

        assertTrue(result.isSuccess)
        assertFalse(DeletionBarrier.isDeletionBlocked(ownerId))
        assertNull(syncRescheduled)
    }

    @Test
    fun retryableFailureReschedulesBackgroundSync() = runBlocking {
        val result = orchestrator(
            deleteAccount = { _, _, _ -> error("cloud unavailable") },
        ).deleteAccount(
            ownerId = ownerId,
            password = "pw",
            localData = LocalDataAfterAccountDeletion.Delete,
        )

        assertTrue(result.isFailure)
        assertEquals(ownerId, syncRescheduled)
        assertFalse(DeletionBarrier.isDeletionBlocked(ownerId))
    }

    @Test
    fun interruptedDeletionKeepsDurableMarkerAndRethrowsCancellation() = runBlocking {
        try {
            orchestrator(
                deleteAccount = { _, _, _ -> throw CancellationException("user aborted") },
            ).deleteAccount(
                ownerId = ownerId,
                password = "pw",
                localData = LocalDataAfterAccountDeletion.Delete,
            )
            fail("Expected the cancellation to propagate")
        } catch (_: CancellationException) {
            // Expected.
        }

        assertTrue(store.readOwners("in_progress_owner_ids").contains(ownerId))
        assertFalse(DeletionBarrier.isLegacyDeletionBlocked)
        assertNull(syncRescheduled)
    }

    @Test
    fun barrierPersistenceFailureBecomesDeletionFailure() = runBlocking {
        val store = FailingDeletionStateStore()
        DeletionBarrier.installStateStoreForTest(store)

        val result = orchestrator(deleteAccount = { _, _, _ ->
            deletionInvoked = true
            AccountDeletionResult.Completed
        }).deleteAccount(
            ownerId = ownerId,
            password = "pw",
            localData = LocalDataAfterAccountDeletion.Delete,
        )

        assertTrue(result.isFailure)
    }
}

private class FailingDeletionStateStore : DeletionStateStore {
    private val delegate = InMemoryDeletionStateStore()

    override fun readOwners(key: String): Set<String> = delegate.readOwners(key)

    override fun writeOwners(key: String, owners: Set<String>) {
        error("disk full")
    }

    override fun writeMarkers(
        inProgress: Set<String>,
        cleanupPending: Set<String>,
        cloudDeletionPending: Set<String>,
        authDeletionStarted: Set<String>,
        authDeletionComplete: Set<String>,
        localRecoveryCopyPending: Set<String>,
        localRecoveryCopyReady: Set<String>,
    ) {
        error("disk full")
    }
}
