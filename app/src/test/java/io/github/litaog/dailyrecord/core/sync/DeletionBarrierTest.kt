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
        store.writeMarkers(
            inProgress = emptySet(),
            cleanupPending = emptySet(),
            cloudDeletionPending = emptySet(),
            authDeletionStarted = emptySet(),
            authDeletionComplete = emptySet(),
            localRecoveryCopyPending = emptySet(),
            localRecoveryCopyReady = emptySet(),
        )
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
    fun staleInProgressMarkerPreventsOverwritingDeletionJournal() {
        val owner = "owner-a"
        store.writeOwners("in_progress_owner_ids", setOf(owner))

        try {
            DeletionBarrier.beginDeletionBlock(owner)
            error("Expected the interrupted deletion to remain durable")
        } catch (_: IllegalStateException) {
            // Expected: startup recovery must resolve the marker first.
        }
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
    fun cloudDeletionPhaseIsSeparateFromInProgressMarker() = runBlocking {
        val owner = "owner-a"
        DeletionBarrier.beginDeletionBlock(owner)
        DeletionBarrier.markCloudDeletionComplete(owner)

        assertTrue(store.readOwners("in_progress_owner_ids").contains(owner))
        assertTrue(store.readOwners("cloud_deletion_pending_owner_ids").contains(owner))
        assertFalse(DeletionBarrier.pendingCleanupOwnerIds().contains(owner))
        assertTrue(DeletionBarrier.cloudDeletionPendingOwnerIds().contains(owner))
        DeletionBarrier.endDeletionBlock(owner, AccountDeletionOutcome.CleanupPending)
    }

    @Test
    fun authDeletionIntentRemainsBlockedButIsNotTreatedAsCleanup() = runBlocking {
        val owner = "owner-a"
        DeletionBarrier.beginDeletionBlock(owner)
        DeletionBarrier.markAuthDeletionStarted(owner)

        assertTrue(DeletionBarrier.isDeletionBlocked(owner))
        assertTrue(store.readOwners("auth_deletion_started_owner_ids").contains(owner))
        assertFalse(DeletionBarrier.pendingCleanupOwnerIds().contains(owner))
    }

    @Test
    fun authDeletionPendingIsNotCleanedUntilPresenceIsResolved() = runBlocking {
        val owner = "owner-a"
        DeletionBarrier.beginDeletionBlock(owner)
        DeletionBarrier.markCloudDeletionComplete(owner)
        DeletionBarrier.markAuthDeletionStarted(owner)
        DeletionBarrier.endDeletionBlock(owner, AccountDeletionOutcome.AuthDeletionPending)

        assertTrue(DeletionBarrier.isDeletionBlocked(owner))
        assertTrue(DeletionBarrier.authDeletionStartedOwnerIds().contains(owner))
        assertTrue(DeletionBarrier.cloudDeletionPendingOwnerIds().contains(owner))
        assertTrue(DeletionBarrier.pendingCleanupOwnerIds().isEmpty())

        DeletionBarrier.promoteAuthDeletionCleanup(owner)

        assertTrue(DeletionBarrier.pendingCleanupOwnerIds().contains(owner))
        assertTrue(store.readOwners("auth_deletion_complete_owner_ids").contains(owner))
        assertTrue(DeletionBarrier.isDeletionBlocked(owner))
    }

    @Test
    fun authPresenceStillExistsClearsBlockAfterResyncIsQueued() = runBlocking {
        val owner = "owner-a"
        DeletionBarrier.beginDeletionBlock(owner)
        DeletionBarrier.markCloudDeletionComplete(owner)
        DeletionBarrier.markAuthDeletionStarted(owner)
        DeletionBarrier.endDeletionBlock(owner, AccountDeletionOutcome.AuthDeletionPending)

        DeletionBarrier.resolveAuthDeletionAccountStillExists(owner)

        assertFalse(DeletionBarrier.isDeletionBlocked(owner))
        assertTrue(DeletionBarrier.authDeletionStartedOwnerIds().isEmpty())
        assertTrue(DeletionBarrier.cloudDeletionPendingOwnerIds().isEmpty())
    }

    @Test
    fun recoveryCopyPendingAndReadyMarkersAreDistinct() {
        val owner = "owner-a"
        DeletionBarrier.beginDeletionBlock(owner)
        DeletionBarrier.markLocalRecoveryCopyPending(owner)

        assertTrue(DeletionBarrier.localRecoveryCopyPendingOwnerIds().contains(owner))
        assertFalse(DeletionBarrier.localRecoveryCopyReadyOwnerIds().contains(owner))

        DeletionBarrier.markLocalRecoveryCopyReady(owner)

        assertFalse(DeletionBarrier.localRecoveryCopyPendingOwnerIds().contains(owner))
        assertTrue(DeletionBarrier.localRecoveryCopyReadyOwnerIds().contains(owner))
    }

    @Test
    fun recoveryCopyMarkerAloneKeepsOwnerBlockedAndPreventsOverwrite() {
        val owner = "owner-a"
        DeletionBarrier.markLocalRecoveryCopyReady(owner)

        assertTrue(DeletionBarrier.isDeletionBlocked(owner))
        try {
            DeletionBarrier.beginDeletionBlock(owner)
            error("Expected the unresolved recovery copy to block a new deletion")
        } catch (_: IllegalStateException) {
            // Expected: the recovery copy must be resolved before it can be
            // overwritten by another account deletion.
        }
        DeletionBarrier.completeDeletionCleanup(owner)
    }

    @Test
    fun recoveryCopyMarkerBlocksEveryOwnerBecauseLocalSpaceIsGlobal() {
        val pendingOwner = "owner-a"
        val otherOwner = "owner-b"
        DeletionBarrier.markLocalRecoveryCopyPending(pendingOwner)

        assertTrue(DeletionBarrier.isDeletionBlocked(otherOwner))
        DeletionBarrier.clearLocalRecoveryCopyPending(pendingOwner)
        assertFalse(DeletionBarrier.isDeletionBlocked(otherOwner))
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

    override fun writeMarkers(
        inProgress: Set<String>,
        cleanupPending: Set<String>,
        cloudDeletionPending: Set<String>,
        authDeletionStarted: Set<String>,
        authDeletionComplete: Set<String>,
        localRecoveryCopyPending: Set<String>,
        localRecoveryCopyReady: Set<String>,
    ) {
        markerWriteCount += 1
        values["in_progress_owner_ids"] = inProgress
        values["cleanup_pending_owner_ids"] = cleanupPending
        values["cloud_deletion_pending_owner_ids"] = cloudDeletionPending
        values["auth_deletion_started_owner_ids"] = authDeletionStarted
        values["auth_deletion_complete_owner_ids"] = authDeletionComplete
        values["local_recovery_copy_pending_owner_ids"] = localRecoveryCopyPending
        values["local_recovery_copy_ready_owner_ids"] = localRecoveryCopyReady
    }
}
