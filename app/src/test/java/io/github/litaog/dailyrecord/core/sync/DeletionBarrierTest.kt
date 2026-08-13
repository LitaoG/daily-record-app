package io.github.litaog.dailyrecord.core.sync

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DeletionBarrierTest {
    private val store = InMemoryDeletionStateStore()

    @Before
    fun installMemoryStore() {
        DeletionBarrier.installStateStoreForTest(store)
        store.writeJournal(emptyMap())
        DeletionBarrier.endDeletionBlock()
    }

    @After
    fun reset() {
        DeletionBarrier.endDeletionBlock()
    }

    @Test
    fun processBlockStartsBlockedAndEndsUnblocked() {
        assertFalse(DeletionBarrier.isDeletionBlocked)
        DeletionBarrier.beginDeletionBlock()
        assertTrue(DeletionBarrier.isDeletionBlocked)
        assertTrue(DeletionBarrier.isLegacyDeletionBlocked)
        DeletionBarrier.endDeletionBlock()
        assertFalse(DeletionBarrier.isDeletionBlocked)
        assertFalse(DeletionBarrier.isLegacyDeletionBlocked)
    }

    @Test
    fun ownerBlockPersistsOneJournalEntryAndSurvivesUnrelatedOwners() {
        val owner = "owner-a"

        DeletionBarrier.beginDeletionBlock(owner)

        assertEquals(
            DeletionJournalEntry(phase = DeletionJournalPhase.InProgress),
            store.readJournal()[owner],
        )
        assertTrue(DeletionBarrier.isDeletionBlocked(owner))
        assertFalse(DeletionBarrier.isDeletionBlocked("owner-b"))
    }

    @Test
    fun completedOutcomeRemovesTheJournalEntry() = runBlocking {
        val owner = "owner-a"
        DeletionBarrier.beginDeletionBlock(owner)

        DeletionBarrier.endDeletionBlock(owner, AccountDeletionOutcome.Completed)

        assertTrue(store.readJournal().isEmpty())
        assertFalse(DeletionBarrier.isDeletionBlocked(owner))
    }

    @Test
    fun cleanupPendingOutcomeKeepsAuthDeletedJournalEntry() = runBlocking {
        val owner = "owner-a"
        moveToAuthDeleted(owner)

        DeletionBarrier.endDeletionBlock(owner, AccountDeletionOutcome.CleanupPending)

        assertEquals(
            DeletionJournalPhase.AuthDeletedCleanupPending,
            store.readJournal()[owner]?.phase,
        )
        assertEquals(setOf(owner), DeletionBarrier.pendingCleanupOwnerIds())
        assertTrue(DeletionBarrier.isDeletionBlocked(owner))
    }

    @Test
    fun interruptedPreCloudDeletionCanRetryOnlyInTheSameProcess() = runBlocking {
        val owner = "owner-a"
        DeletionBarrier.beginDeletionBlock(owner)

        DeletionBarrier.endDeletionBlock(owner, AccountDeletionOutcome.Interrupted)

        assertEquals(
            DeletionJournalPhase.InProgress,
            store.readJournal()[owner]?.phase,
        )
        assertFalse(DeletionBarrier.isDeletionBlocked)
        assertTrue(DeletionBarrier.isDeletionBlocked(owner))

        DeletionBarrier.beginDeletionBlock(owner)
        DeletionBarrier.endDeletionBlock(owner, AccountDeletionOutcome.Completed)
        assertTrue(store.readJournal().isEmpty())
    }

    @Test
    fun staleInProgressEntryPreventsOverwritingDeletionJournal() {
        val owner = "owner-a"
        store.writeJournal(
            mapOf(owner to DeletionJournalEntry(phase = DeletionJournalPhase.InProgress)),
        )

        try {
            DeletionBarrier.beginDeletionBlock(owner)
            error("Expected the interrupted deletion to remain durable")
        } catch (_: IllegalStateException) {
            // Expected: a fresh process must resolve the entry first.
        }
    }

    @Test
    fun completeDeletionCleanupUnblocksOwner() = runBlocking {
        val owner = "owner-a"
        moveToAuthDeleted(owner)
        DeletionBarrier.endDeletionBlock(owner, AccountDeletionOutcome.CleanupPending)

        DeletionBarrier.completeDeletionCleanup(owner)

        assertTrue(store.readJournal().isEmpty())
        assertFalse(DeletionBarrier.isDeletionBlocked(owner))
    }

    @Test
    fun cloudDeletionPhaseIsSeparateFromInProgressPhase() {
        val owner = "owner-a"
        DeletionBarrier.beginDeletionBlock(owner)
        DeletionBarrier.markCloudDeletionComplete(owner)

        assertEquals(
            DeletionJournalPhase.CloudDeleted,
            store.readJournal()[owner]?.phase,
        )
        assertTrue(DeletionBarrier.cloudDeletionPendingOwnerIds().contains(owner))
        assertFalse(DeletionBarrier.pendingCleanupOwnerIds().contains(owner))
    }

    @Test
    fun authDeletionIntentRemainsBlockedButIsNotTreatedAsCleanup() {
        val owner = "owner-a"
        moveToAuthPending(owner)

        assertEquals(
            DeletionJournalPhase.AuthDeletionPending,
            store.readJournal()[owner]?.phase,
        )
        assertTrue(DeletionBarrier.isDeletionBlocked(owner))
        assertTrue(DeletionBarrier.authDeletionStartedOwnerIds().contains(owner))
        assertTrue(DeletionBarrier.cloudDeletionPendingOwnerIds().contains(owner))
        assertFalse(DeletionBarrier.pendingCleanupOwnerIds().contains(owner))
    }

    @Test
    fun authDeletionPendingIsNotCleanedUntilPresenceIsResolved() = runBlocking {
        val owner = "owner-a"
        moveToAuthPending(owner)
        DeletionBarrier.endDeletionBlock(owner, AccountDeletionOutcome.AuthDeletionPending)

        DeletionBarrier.promoteAuthDeletionCleanup(owner)

        assertEquals(
            DeletionJournalPhase.AuthDeletedCleanupPending,
            store.readJournal()[owner]?.phase,
        )
        assertTrue(DeletionBarrier.pendingCleanupOwnerIds().contains(owner))
        assertTrue(DeletionBarrier.isDeletionBlocked(owner))
    }

    @Test
    fun authPresenceStillExistsRemovesTheEntryAfterResyncIsQueued() = runBlocking {
        val owner = "owner-a"
        moveToAuthPending(owner)
        DeletionBarrier.endDeletionBlock(owner, AccountDeletionOutcome.AuthDeletionPending)

        DeletionBarrier.resolveAuthDeletionAccountStillExists(owner)

        assertTrue(store.readJournal().isEmpty())
        assertFalse(DeletionBarrier.isDeletionBlocked(owner))
    }

    @Test
    fun recoveryCopyPendingAndReadyAreFieldsOfOneEntry() {
        val owner = "owner-a"
        DeletionBarrier.beginDeletionBlock(owner)
        DeletionBarrier.markCloudDeletionComplete(owner)
        DeletionBarrier.markLocalRecoveryCopyPending(owner)

        assertEquals(
            RecoveryCopyState.Pending,
            store.readJournal()[owner]?.recoveryCopy,
        )

        DeletionBarrier.markLocalRecoveryCopyReady(owner)

        assertEquals(
            RecoveryCopyState.Ready,
            store.readJournal()[owner]?.recoveryCopy,
        )
    }

    @Test
    fun invalidPhaseTransitionsAreRejected() {
        val owner = "owner-a"
        DeletionBarrier.beginDeletionBlock(owner)

        assertIllegalState { DeletionBarrier.markAuthDeletionStarted(owner) }
        assertIllegalState { DeletionBarrier.markLocalRecoveryCopyReady(owner) }
    }

    @Test
    fun recoveryEntryBlocksOnlyItsOwner() {
        val owner = "owner-a"
        DeletionBarrier.beginDeletionBlock(owner)
        DeletionBarrier.markCloudDeletionComplete(owner)
        DeletionBarrier.markLocalRecoveryCopyPending(owner)

        assertTrue(DeletionBarrier.isDeletionBlocked(owner))
        assertFalse(DeletionBarrier.isDeletionBlocked("owner-b"))
        DeletionBarrier.clearLocalRecoveryCopyPending(owner)
    }

    @Test
    fun journalTransitionsUseOneStoreWritePerTransition() = runBlocking {
        val owner = "owner-a"
        store.writeCount = 0

        DeletionBarrier.beginDeletionBlock(owner)
        DeletionBarrier.markCloudDeletionComplete(owner)
        DeletionBarrier.endDeletionBlock(owner, AccountDeletionOutcome.Completed)

        assertEquals(3, store.writeCount)
    }

    private fun moveToAuthPending(owner: String) {
        DeletionBarrier.beginDeletionBlock(owner)
        DeletionBarrier.markCloudDeletionComplete(owner)
        DeletionBarrier.markAuthDeletionStarted(owner)
    }

    private fun moveToAuthDeleted(owner: String) {
        moveToAuthPending(owner)
        DeletionBarrier.markAuthDeletionComplete(owner)
    }

    private fun assertIllegalState(block: () -> Unit) {
        try {
            block()
            error("Expected an illegal state")
        } catch (_: IllegalStateException) {
            // Expected.
        }
    }
}

internal class InMemoryDeletionStateStore : DeletionStateStore {
    private var entries: Map<String, DeletionJournalEntry> = emptyMap()
    var writeCount: Int = 0

    override fun readJournal(): Map<String, DeletionJournalEntry> = entries

    override fun writeJournal(entries: Map<String, DeletionJournalEntry>) {
        writeCount += 1
        this.entries = entries.toMap()
    }
}
