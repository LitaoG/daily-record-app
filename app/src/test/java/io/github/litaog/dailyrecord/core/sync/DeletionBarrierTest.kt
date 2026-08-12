package io.github.litaog.dailyrecord.core.sync

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.runBlocking

class DeletionBarrierTest {
    private val store = InMemoryDeletionStateStore()
    
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
    fun ownerBlockPersistsDurablyAndSurvivesUnrelatedOwners() {
        val owner = "owner-a"
        DeletionBarrier.beginDeletionBlock(owner)

        assertTrue(DeletionBarrier.isDeletionBlocked(owner))
        assertTrue(store.readOwners("in_progress_owner_ids").contains(owner))
        assertFalse(DeletionBarrier.isDeletionBlocked("owner-b"))
    }

    @Test
    fun completedOutcomeClearsBothMarkers() = runBlocking {
        val owner = "owner-a"
        DeletionBarrier.beginDeletionBlock(owner)
        store.writeOwners("cleanup_pending_owner_ids", setOf(owner))

        DeletionBarrier.endDeletionBlock(owner, AccountDeletionOutcome.Completed)

        assertFalse(DeletionBarrier.isDeletionBlocked(owner))
        assertFalse(store.readOwners("in_progress_owner_ids").contains(owner))
        assertFalse(store.readOwners("cleanup_pending_owner_ids").contains(owner))
        assertNull(DeletionBarrier.pendingCleanupOwnerIds().firstOrNull())
    }

    @Test
    fun cleanupPendingOutcomeKeepsDurableCleanupMarker() = runBlocking {
        val owner = "owner-a"
        DeletionBarrier.beginDeletionBlock(owner)

        DeletionBarrier.endDeletionBlock(owner, AccountDeletionOutcome.CleanupPending)

        assertFalse(store.readOwners("in_progress_owner_ids").contains(owner))
        assertTrue(store.readOwners("cleanup_pending_owner_ids").contains(owner))
        assertEquals(setOf(owner), DeletionBarrier.pendingCleanupOwnerIds())
        // A cleanup-pending owner stays blocked until the cleanup completes.
        assertTrue(DeletionBarrier.isDeletionBlocked(owner))
    }

    @Test
    fun interruptedOutcomeKeepsInProgressMarkerAndReleasesProcessLock() = runBlocking {
        val owner = "owner-a"
        DeletionBarrier.beginDeletionBlock(owner)

        DeletionBarrier.endDeletionBlock(owner, AccountDeletionOutcome.Interrupted)

        // Durable marker survives process death; the process lock is released
        // so the user can retry deletion in the same session.
        assertTrue(store.readOwners("in_progress_owner_ids").contains(owner))
        assertFalse(DeletionBarrier.isDeletionBlocked)
        assertFalse(DeletionBarrier.isLegacyDeletionBlocked)
        assertTrue(DeletionBarrier.isDeletionBlocked(owner))
    }

    @Test
    fun completeDeletionCleanupUnblocksOwner() = runBlocking {
        val owner = "owner-a"
        DeletionBarrier.beginDeletionBlock(owner)
        DeletionBarrier.endDeletionBlock(owner, AccountDeletionOutcome.CleanupPending)
        assertTrue(DeletionBarrier.isDeletionBlocked(owner))

        DeletionBarrier.completeDeletionCleanup(owner)

        assertFalse(DeletionBarrier.isDeletionBlocked(owner))
        assertTrue(DeletionBarrier.pendingCleanupOwnerIds().isEmpty())
    }

    @Test
    fun markerTransitionsUseOneStoreCommit() = runBlocking {
        val owner = "owner-a"

        store.markerWriteCount = 0
        DeletionBarrier.beginDeletionBlock(owner)
        assertEquals(1, store.markerWriteCount)

        DeletionBarrier.endDeletionBlock(owner, AccountDeletionOutcome.Completed)
        assertEquals(2, store.markerWriteCount)
    }
}

internal class InMemoryDeletionStateStore : DeletionStateStore {
    private val values = mutableMapOf<String, Set<String>>()
    var markerWriteCount: Int = 0

    override fun readOwners(key: String): Set<String> = values[key].orEmpty()

    override fun writeOwners(key: String, owners: Set<String>) {
        values[key] = owners
    }

    override fun writeMarkers(inProgress: Set<String>, cleanupPending: Set<String>) {
        markerWriteCount += 1
        values["in_progress_owner_ids"] = inProgress
        values["cleanup_pending_owner_ids"] = cleanupPending
    }
}
