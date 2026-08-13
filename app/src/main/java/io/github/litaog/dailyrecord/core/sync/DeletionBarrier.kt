package io.github.litaog.dailyrecord.core.sync

import android.content.Context
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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

internal interface CloudWriteGate {
    suspend fun <T> withWrite(ownerId: String, block: suspend () -> T): T
}

internal object NoOpCloudWriteGate : CloudWriteGate {
    override suspend fun <T> withWrite(ownerId: String, block: suspend () -> T): T = block()
}

/**
 * Process-wide deletion barrier: serializes cloud writes against account
 * deletion and persists one versioned journal entry per owner.
 *
 * This is the single owner of the deletion state machine. The WorkManager
 * adapter is [io.github.litaog.dailyrecord.core.di.DailyRecordSyncScheduler].
 * It only schedules WorkManager work and consults this barrier before enqueuing.
 */
internal object DeletionBarrier {
    private val cloudWriteMutex = Mutex()
    private val stateLock = Any()
    private var inProcessDeletionOwner: String? = null
    private var deletionLockHeld = false
    private var legacyDeletionBlocked = false

    /** Allows only a same-process retry of an interruption before any phase advanced. */
    private val sameProcessInterruptedOwners = mutableSetOf<String>()

    /** Invalidates writers that observed an open gate but were still queued. */
    private var cloudWriteGeneration = 0L

    @Volatile
    private var deletionBlocked = false

    @Volatile
    private var stateStore: DeletionStateStore? = null

    /** Binds the durable journal; called once at process start. */
    internal fun configure(context: Context) {
        synchronized(stateLock) {
            stateStore = SharedPreferencesDeletionStateStore(context)
            sameProcessInterruptedOwners.clear()
        }
    }

    /** Test hook: swaps the durable journal for an in-memory one. */
    internal fun installStateStoreForTest(store: DeletionStateStore) {
        synchronized(stateLock) {
            stateStore = store
            sameProcessInterruptedOwners.clear()
        }
    }

    internal val isDeletionBlocked: Boolean
        get() = deletionBlocked

    internal val isLegacyDeletionBlocked: Boolean
        get() = legacyDeletionBlocked

    // Account deletion must be the only writer of the user's cloud paths. While
    // blocked, local change callbacks must not create new work and running
    // workers must exit before any remote write.
    internal fun beginDeletionBlock() {
        synchronized(stateLock) {
            legacyDeletionBlocked = true
            deletionBlocked = true
            cloudWriteGeneration += 1
        }
    }

    internal fun endDeletionBlock() {
        synchronized(stateLock) {
            legacyDeletionBlocked = false
            inProcessDeletionOwner = null
            sameProcessInterruptedOwners.clear()
            deletionBlocked = false
        }
    }

    /**
     * Persists the barrier before the caller cancels workers or touches
     * Firestore. A durable entry survives process death and is keyed by owner.
     */
    internal fun beginDeletionBlock(ownerId: String) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        synchronized(stateLock) {
            check(inProcessDeletionOwner == null) {
                "An account deletion is already active in this process."
            }
            val sameProcessRetry = ownerId in sameProcessInterruptedOwners
            val existing = readJournal()[ownerId]
            check(
                existing == null ||
                    (sameProcessRetry && existing.isSafeSameProcessRetry),
            ) {
                "Account deletion for $ownerId is awaiting durable recovery."
            }
            sameProcessInterruptedOwners.remove(ownerId)
            inProcessDeletionOwner = ownerId
            deletionBlocked = true
            cloudWriteGeneration += 1
            try {
                writeJournalEntry(
                    ownerId,
                    DeletionJournalEntry(phase = DeletionJournalPhase.InProgress),
                )
                // A previous failed commit may have conservatively blocked the
                // process; a successful durable journal clears that fallback.
                legacyDeletionBlocked = false
            } catch (error: Exception) {
                // A failed commit means we cannot rely on a durable marker.
                // Keep a conservative in-process block so callbacks cannot
                // resurrect cloud data while the caller reports the failure.
                legacyDeletionBlocked = true
                inProcessDeletionOwner = null
                deletionBlocked = true
                throw error
            }
        }
    }

    /** Records that all cloud collections have been deleted. */
    internal fun markCloudDeletionComplete(ownerId: String) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        synchronized(stateLock) {
            transitionJournal(ownerId) { current ->
                current.copy(phase = DeletionJournalPhase.CloudDeleted)
            }
        }
    }

    /** Persists Auth-delete intent immediately before invoking Firebase Auth. */
    internal fun markAuthDeletionStarted(ownerId: String) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        synchronized(stateLock) {
            transitionJournal(ownerId) { current ->
                check(current.phase == DeletionJournalPhase.CloudDeleted) {
                    "Auth deletion can only start after cloud deletion"
                }
                current.copy(phase = DeletionJournalPhase.AuthDeletionPending)
            }
        }
    }

    /** Persists intent before staging the optional local recovery copy. */
    internal fun markLocalRecoveryCopyPending(ownerId: String) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        synchronized(stateLock) {
            transitionJournal(ownerId) { current ->
                check(current.phase == DeletionJournalPhase.CloudDeleted) {
                    "Recovery copy can only be staged after cloud deletion"
                }
                current.copy(recoveryCopy = RecoveryCopyState.Pending)
            }
        }
    }

    internal fun localRecoveryCopyPendingOwnerIds(): Set<String> = synchronized(stateLock) {
        readJournal().filterValues { it.recoveryCopy == RecoveryCopyState.Pending }.keys
    }

    /** Persists that the local recovery copy is complete and protected. */
    internal fun markLocalRecoveryCopyReady(ownerId: String) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        synchronized(stateLock) {
            transitionJournal(ownerId) { current ->
                check(current.recoveryCopy == RecoveryCopyState.Pending) {
                    "Recovery copy must be pending before it can be ready"
                }
                current.copy(recoveryCopy = RecoveryCopyState.Ready)
            }
        }
    }

    internal fun localRecoveryCopyReadyOwnerIds(): Set<String> = synchronized(stateLock) {
        readJournal().filterValues { it.recoveryCopy == RecoveryCopyState.Ready }.keys
    }

    /** Clears a recovery-copy marker after the copy has been discarded. */
    internal fun clearLocalRecoveryCopyPending(ownerId: String) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        synchronized(stateLock) {
            transitionJournal(ownerId) { current ->
                current.copy(recoveryCopy = RecoveryCopyState.None)
            }
        }
    }

    /** Records definitive Auth success before local owner-cache cleanup begins. */
    internal fun markAuthDeletionComplete(ownerId: String) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        synchronized(stateLock) {
            transitionJournal(ownerId) { current ->
                check(current.phase == DeletionJournalPhase.AuthDeletionPending) {
                    "Auth deletion cannot complete before its request starts"
                }
                current.copy(phase = DeletionJournalPhase.AuthDeletedCleanupPending)
            }
        }
    }

    /** Owners whose Auth deletion was started but not durably completed. */
    internal fun authDeletionStartedOwnerIds(): Set<String> = synchronized(stateLock) {
        readJournal().filterValues { it.phase == DeletionJournalPhase.AuthDeletionPending }.keys
    }

    /** Owners whose deletion was active before the process last stopped. */
    internal fun inProgressOwnerIds(): Set<String> = synchronized(stateLock) {
        readJournal().filterValues { it.phase == DeletionJournalPhase.InProgress }.keys
    }

    /**
     * Promotes Auth-deletion intent to local cleanup after Auth absence is
     * confirmed. It never clears the recovery copy marker.
     */
    internal fun promoteAuthDeletionCleanup(ownerId: String) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        synchronized(stateLock) {
            transitionJournal(ownerId) { current ->
                check(current.phase == DeletionJournalPhase.AuthDeletionPending) {
                    "Auth cleanup promotion requires a pending Auth deletion"
                }
                current.copy(phase = DeletionJournalPhase.AuthDeletedCleanupPending)
            }
        }
    }

    /** Removes the journal after an existing account has been re-queued. */
    internal fun resolveAuthDeletionAccountStillExists(ownerId: String) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        synchronized(stateLock) {
            sameProcessInterruptedOwners.remove(ownerId)
            removeJournalEntry(ownerId)
        }
    }

    /** Clears an interrupted pre-Auth deletion after its local rows are queued. */
    internal fun resolveInterruptedDeletion(ownerId: String) {
        resolveAuthDeletionAccountStillExists(ownerId)
    }

    /** Waits for a sync that acquired the write gate before the barrier. */
    internal suspend fun awaitDeletionWriters() {
        withContext(NonCancellable) {
            cloudWriteMutex.lock()
            synchronized(stateLock) { deletionLockHeld = true }
        }
    }

    /**
     * Finishes the in-process barrier and persists the outcome. An interrupted
     * deletion keeps its journal; only a same-process pre-cloud interruption is
     * eligible for immediate retry.
     */
    internal suspend fun endDeletionBlock(
        ownerId: String,
        outcome: AccountDeletionOutcome,
    ) {
        var persistenceFailure: Exception? = null
        val unlock = synchronized(stateLock) {
            if (inProcessDeletionOwner != ownerId) return@synchronized false
            try {
                when (outcome) {
                    AccountDeletionOutcome.Completed,
                    AccountDeletionOutcome.RetryableFailure,
                    -> removeJournalEntry(ownerId)

                    AccountDeletionOutcome.CleanupPending -> {
                        transitionJournal(ownerId) { current ->
                            current.copy(phase = DeletionJournalPhase.AuthDeletedCleanupPending)
                        }
                    }

                    AccountDeletionOutcome.AuthDeletionPending -> {
                        transitionJournal(ownerId) { current ->
                            current.copy(phase = DeletionJournalPhase.AuthDeletionPending)
                        }
                    }

                    AccountDeletionOutcome.LocalRecoveryPending -> {
                        transitionJournal(ownerId) { current ->
                            current.copy(phase = DeletionJournalPhase.CloudDeleted)
                        }
                    }

                    AccountDeletionOutcome.Interrupted -> {
                        val current = readJournal()[ownerId]
                        if (current == null || current.isSafeSameProcessRetry) {
                            sameProcessInterruptedOwners += ownerId
                        } else {
                            sameProcessInterruptedOwners.remove(ownerId)
                        }
                    }
                }
            } catch (error: Exception) {
                // A failed commit is safer as a global in-process block than
                // allowing a local callback to resurrect a deleted account.
                legacyDeletionBlocked = true
                deletionBlocked = true
                persistenceFailure = error
            }
            inProcessDeletionOwner = null
            if (!legacyDeletionBlocked) deletionBlocked = false
            val wasHeld = deletionLockHeld
            deletionLockHeld = false
            wasHeld
        }
        if (unlock) cloudWriteMutex.unlock()
        persistenceFailure?.let { throw it }
    }

    /** Clears a durable cleanup marker after owner-cache cleanup succeeds. */
    internal fun completeDeletionCleanup(ownerId: String) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        synchronized(stateLock) {
            sameProcessInterruptedOwners.remove(ownerId)
            removeJournalEntry(ownerId)
        }
    }

    internal fun pendingCleanupOwnerIds(): Set<String> = synchronized(stateLock) {
        readJournal().filterValues {
            it.phase == DeletionJournalPhase.AuthDeletedCleanupPending
        }.keys
    }

    /** Owners whose cloud data is gone but Auth completion is not resolved. */
    internal fun cloudDeletionPendingOwnerIds(): Set<String> = synchronized(stateLock) {
        readJournal().filterValues {
            it.phase == DeletionJournalPhase.CloudDeleted ||
                it.phase == DeletionJournalPhase.AuthDeletionPending
        }.keys
    }

    internal fun isDeletionBlocked(ownerId: String?): Boolean = synchronized(stateLock) {
        isDeletionBlockedLocked(ownerId)
    }

    internal fun cloudWriteGate(): CloudWriteGate =
        object : CloudWriteGate {
            override suspend fun <T> withWrite(ownerId: String, block: suspend () -> T): T =
                withCloudWrite(ownerId, block)
        }

    internal suspend fun <T> withCloudWrite(
        ownerId: String,
        block: suspend () -> T,
    ): T {
        val generation = synchronized(stateLock) {
            if (isDeletionBlockedLocked(ownerId)) {
                throw AccountDeletionInProgressException(ownerId)
            }
            cloudWriteGeneration
        }
        return cloudWriteMutex.withLock {
            synchronized(stateLock) {
                if (isDeletionBlockedLocked(ownerId) || cloudWriteGeneration != generation) {
                    throw AccountDeletionInProgressException(ownerId)
                }
            }
            block()
        }
    }

    private fun isDeletionBlockedLocked(ownerId: String?): Boolean {
        if (legacyDeletionBlocked) return true
        inProcessDeletionOwner?.let { deletingOwner ->
            // A deletion in this process only blocks the account whose cloud
            // paths are being removed. A different account may still sync.
            if (ownerId == null || ownerId == deletingOwner) return true
        }
        val journalOwners = readJournal().keys
        return ownerId?.let(journalOwners::contains) ?: journalOwners.isNotEmpty()
    }

    private fun readJournal(): Map<String, DeletionJournalEntry> =
        requireNotNull(stateStore) { "Deletion journal is not configured" }.readJournal()

    private fun writeJournalEntry(ownerId: String, entry: DeletionJournalEntry) {
        val entries = readJournal().toMutableMap()
        entries[ownerId] = entry
        requireValidJournal(entries)
        requireNotNull(stateStore).writeJournal(entries)
    }

    private fun transitionJournal(
        ownerId: String,
        transform: (DeletionJournalEntry) -> DeletionJournalEntry,
    ) {
        val current = readJournal()[ownerId]
            ?: error("No deletion journal entry for owner $ownerId")
        val next = transform(current)
        requireValidTransition(current, next)
        writeJournalEntry(ownerId, next)
    }

    private fun removeJournalEntry(ownerId: String) {
        val entries = readJournal()
        if (ownerId !in entries) return
        requireNotNull(stateStore).writeJournal(entries - ownerId)
    }

    private fun requireValidJournal(entries: Map<String, DeletionJournalEntry>) {
        entries.values.forEach { entry ->
            check(entry.version == DeletionJournalEntry.CURRENT_VERSION)
        }
    }

    private fun requireValidTransition(
        current: DeletionJournalEntry,
        next: DeletionJournalEntry,
    ) {
        if (current == next) return
        val allowed = when (current.phase) {
            DeletionJournalPhase.InProgress ->
                next.phase == DeletionJournalPhase.InProgress ||
                    next.phase == DeletionJournalPhase.CloudDeleted
            DeletionJournalPhase.CloudDeleted ->
                next.phase == DeletionJournalPhase.CloudDeleted ||
                    next.phase == DeletionJournalPhase.AuthDeletionPending
            DeletionJournalPhase.AuthDeletionPending ->
                next.phase == DeletionJournalPhase.AuthDeletionPending ||
                    next.phase == DeletionJournalPhase.AuthDeletedCleanupPending
            DeletionJournalPhase.AuthDeletedCleanupPending ->
                next.phase == DeletionJournalPhase.AuthDeletedCleanupPending
        }
        check(allowed) {
            "Invalid deletion journal transition: ${current.phase} -> ${next.phase}"
        }
    }

    private val DeletionJournalEntry.isSafeSameProcessRetry: Boolean
        get() = phase == DeletionJournalPhase.InProgress &&
            recoveryCopy == RecoveryCopyState.None
}
