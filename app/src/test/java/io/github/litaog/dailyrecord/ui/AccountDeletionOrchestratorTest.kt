package io.github.litaog.dailyrecord.ui

import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalCleanupPendingException
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

    private fun orchestrator(
        deleteAccount: suspend (
            ownerId: String,
            password: String,
            localData: LocalDataAfterAccountDeletion,
        ) -> Unit = { _, _, _ -> deletionInvoked = true },
    ) = AccountDeletionOrchestrator(
        cancelAndAwait = {},
        performDeletion = deleteAccount,
        markCleanupPending = { cleanupMarked = it },
        onLocalRecordsKept = { localRecordsKept = true },
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
        val result = orchestrator(deleteAccount = { _, _, _ -> deletionInvoked = true }).deleteAccount(
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

        val result = orchestrator(deleteAccount = { _, _, _ -> deletionInvoked = true }).deleteAccount(
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

    override fun writeMarkers(inProgress: Set<String>, cleanupPending: Set<String>) {
        error("disk full")
    }
}
