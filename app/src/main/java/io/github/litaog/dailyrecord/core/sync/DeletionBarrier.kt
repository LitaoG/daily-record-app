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

/** Durable owner-set journal for the deletion barrier. */
internal interface DeletionStateStore {
    fun readOwners(key: String): Set<String>

    fun writeOwners(key: String, owners: Set<String>)

    /**
     * Replaces every deletion marker set as one durable state transition.
     * Implementations must make the complete journal visible atomically so a
     * process crash cannot leave an account unblocked between deletion phases.
     */
    fun writeMarkers(
        inProgress: Set<String>,
        cleanupPending: Set<String>,
        cloudDeletionPending: Set<String>,
        authDeletionStarted: Set<String>,
        authDeletionComplete: Set<String>,
        localRecoveryCopyPending: Set<String>,
        localRecoveryCopyReady: Set<String>,
    )
}

internal class SharedPreferencesDeletionStateStore(context: Context) : DeletionStateStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun readOwners(key: String): Set<String> =
        preferences.getStringSet(key, emptySet()).orEmpty()

    override fun writeOwners(key: String, owners: Set<String>) {
        check(preferences.edit().putStringSet(key, owners).commit()) {
            "Unable to persist account deletion state"
        }
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
        check(
            preferences.edit()
                .putStringSet(KEY_IN_PROGRESS, inProgress)
                .putStringSet(KEY_CLEANUP_PENDING, cleanupPending)
                .putStringSet(KEY_CLOUD_DELETION_PENDING, cloudDeletionPending)
                .putStringSet(KEY_AUTH_DELETION_STARTED, authDeletionStarted)
                .putStringSet(KEY_AUTH_DELETION_COMPLETE, authDeletionComplete)
                .putStringSet(KEY_LOCAL_RECOVERY_COPY_PENDING, localRecoveryCopyPending)
                .putStringSet(KEY_LOCAL_RECOVERY_COPY_READY, localRecoveryCopyReady)
                .commit(),
        ) {
            "Unable to persist account deletion state"
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "daily_record_deletion_state"
        const val KEY_IN_PROGRESS = "in_progress_owner_ids"
        const val KEY_CLEANUP_PENDING = "cleanup_pending_owner_ids"
        const val KEY_CLOUD_DELETION_PENDING = "cloud_deletion_pending_owner_ids"
        const val KEY_AUTH_DELETION_STARTED = "auth_deletion_started_owner_ids"
        const val KEY_AUTH_DELETION_COMPLETE = "auth_deletion_complete_owner_ids"
        const val KEY_LOCAL_RECOVERY_COPY_PENDING = "local_recovery_copy_pending_owner_ids"
        const val KEY_LOCAL_RECOVERY_COPY_READY = "local_recovery_copy_ready_owner_ids"
    }
}

/**
 * Process-wide deletion barrier: serializes cloud writes against account
 * deletion and persists the barrier durably per owner.
 *
 * This is the single owner of the deletion state machine. [DailyRecordSyncScheduler]
 * only schedules WorkManager work and consults this barrier before enqueuing.
 */
internal object DeletionBarrier {
    /**
     * Serializes a complete sync attempt with account deletion. The persistent
     * owner marker blocks new writers before this mutex is acquired; the mutex
     * then lets an already-running write finish before deletion starts.
     */
    private val cloudWriteMutex = Mutex()
    private val stateLock = Any()
    private var inProcessDeletionOwner: String? = null
    private var deletionLockHeld = false
    private var legacyDeletionBlocked = false
    /** Invalidates writers that observed an open gate but were still queued. */
    private var cloudWriteGeneration = 0L

    @Volatile
    private var deletionBlocked = false

    @Volatile
    private var stateStore: DeletionStateStore? = null

    /** Binds the durable journal; called once at process start. */
    internal fun configure(context: Context) {
        stateStore = SharedPreferencesDeletionStateStore(context)
    }

    /** Test hook: swaps the durable journal for an in-memory one. */
    internal fun installStateStoreForTest(store: DeletionStateStore) {
        stateStore = store
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
            deletionBlocked = false
        }
    }

    /**
     * Persist the deletion barrier before the caller cancels workers or
     * touches Firestore. This survives process death and is keyed by account,
     * so a different account is not accidentally blocked by a stale marker.
     */
    internal fun beginDeletionBlock(ownerId: String) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        synchronized(stateLock) {
            check(inProcessDeletionOwner == null) {
                "An account deletion is already active in this process."
            }
            val cleanupPending = readOwners(KEY_CLEANUP_PENDING)
            val inProgress = readOwners(KEY_IN_PROGRESS)
            val cloudDeletionPending = readOwners(KEY_CLOUD_DELETION_PENDING)
            val authDeletionStarted = readOwners(KEY_AUTH_DELETION_STARTED)
            val authDeletionComplete = readOwners(KEY_AUTH_DELETION_COMPLETE)
            val localRecoveryCopyPending = readOwners(KEY_LOCAL_RECOVERY_COPY_PENDING)
            val localRecoveryCopyReady = readOwners(KEY_LOCAL_RECOVERY_COPY_READY)
            check(
                localRecoveryCopyPending.isEmpty() && localRecoveryCopyReady.isEmpty(),
            ) {
                "A local recovery copy is awaiting durable recovery."
            }
            check(
                ownerId !in inProgress &&
                    ownerId !in cleanupPending &&
                    ownerId !in cloudDeletionPending &&
                    ownerId !in authDeletionStarted &&
                    ownerId !in authDeletionComplete,
            ) {
                "Account deletion for $ownerId is awaiting durable recovery."
            }
            inProcessDeletionOwner = ownerId
            deletionBlocked = true
            cloudWriteGeneration += 1
            try {
                updateDeletionMarkers(
                    inProgress = { it + ownerId },
                    cleanupPending = { it },
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

    /**
     * Records the post-cloud phase before local owner-cache cleanup begins.
     * This is deliberately separate from [KEY_IN_PROGRESS]: an interrupted
     * deletion before Auth succeeds must not cause startup to delete local
     * data for an account that can still be recovered.
     */
    internal fun markCloudDeletionComplete(ownerId: String) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        synchronized(stateLock) {
            updateDeletionMarkers(
                inProgress = { it },
                cleanupPending = { it },
                cloudDeletionPending = { it + ownerId },
            )
        }
    }

    /** Durable intent written immediately before invoking Firebase Auth delete. */
    internal fun markAuthDeletionStarted(ownerId: String) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        synchronized(stateLock) {
            updateDeletionMarkers(
                inProgress = { it },
                cleanupPending = { it },
                cloudDeletionPending = { it },
                authDeletionStarted = { it + ownerId },
                authDeletionComplete = { it - ownerId },
            )
        }
    }

    /** Durable intent that the optional local recovery copy is being prepared. */
    internal fun markLocalRecoveryCopyPending(ownerId: String) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        synchronized(stateLock) {
            updateDeletionMarkers(
                inProgress = { it },
                cleanupPending = { it },
                localRecoveryCopyPending = { it + ownerId },
                localRecoveryCopyReady = { it - ownerId },
            )
        }
    }

    internal fun localRecoveryCopyPendingOwnerIds(): Set<String> = synchronized(stateLock) {
        readOwners(KEY_LOCAL_RECOVERY_COPY_PENDING)
    }

    internal fun markLocalRecoveryCopyReady(ownerId: String) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        synchronized(stateLock) {
            updateDeletionMarkers(
                inProgress = { it },
                cleanupPending = { it },
                localRecoveryCopyPending = { it - ownerId },
                localRecoveryCopyReady = { it + ownerId },
            )
        }
    }

    internal fun localRecoveryCopyReadyOwnerIds(): Set<String> = synchronized(stateLock) {
        readOwners(KEY_LOCAL_RECOVERY_COPY_READY)
    }

    internal fun clearLocalRecoveryCopyPending(ownerId: String) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        synchronized(stateLock) {
            updateDeletionMarkers(
                inProgress = { it },
                cleanupPending = { it },
                localRecoveryCopyPending = { it - ownerId },
                localRecoveryCopyReady = { it - ownerId },
            )
        }
    }

    /** Records a definitive Auth success before local cache cleanup begins. */
    internal fun markAuthDeletionComplete(ownerId: String) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        synchronized(stateLock) {
            updateDeletionMarkers(
                inProgress = { it },
                cleanupPending = { it },
                cloudDeletionPending = { it - ownerId },
                authDeletionStarted = { it - ownerId },
                authDeletionComplete = { it + ownerId },
                localRecoveryCopyPending = { it - ownerId },
                localRecoveryCopyReady = { it },
            )
        }
    }

    /** Owners whose Auth deletion was started but not durably completed. */
    internal fun authDeletionStartedOwnerIds(): Set<String> = synchronized(stateLock) {
        readOwners(KEY_AUTH_DELETION_STARTED)
    }

    /** Owners whose deletion was active when the process last stopped. */
    internal fun inProgressOwnerIds(): Set<String> = synchronized(stateLock) {
        readOwners(KEY_IN_PROGRESS)
    }

    /**
     * Promotes an Auth-deletion intent to local cleanup only after the caller
     * has confirmed that Auth no longer reports that owner on this device.
     */
    internal fun promoteAuthDeletionCleanup(ownerId: String) {
        synchronized(stateLock) {
            updateDeletionMarkers(
                inProgress = { it - ownerId },
                cleanupPending = { it + ownerId },
                cloudDeletionPending = { it - ownerId },
                authDeletionStarted = { it - ownerId },
                authDeletionComplete = { it + ownerId },
                localRecoveryCopyPending = { it },
                localRecoveryCopyReady = { it },
            )
        }
    }

    /**
     * Explicitly resolves a pending Auth request as still present. The account
     * can resume normal synchronization after its local rows are re-queued.
     */
    internal fun resolveAuthDeletionAccountStillExists(ownerId: String) {
        synchronized(stateLock) {
            updateDeletionMarkers(
                inProgress = { it - ownerId },
                cleanupPending = { it - ownerId },
                cloudDeletionPending = { it - ownerId },
                authDeletionStarted = { it - ownerId },
                authDeletionComplete = { it - ownerId },
                localRecoveryCopyPending = { it - ownerId },
                localRecoveryCopyReady = { it - ownerId },
            )
        }
    }

    /** Clears an interrupted pre-Auth deletion after its local rows are queued. */
    internal fun resolveInterruptedDeletion(ownerId: String) {
        resolveAuthDeletionAccountStillExists(ownerId)
    }

    /** Wait until any sync that acquired the write gate before the barrier has ended. */
    internal suspend fun awaitDeletionWriters() {
        // Once lock acquisition starts, cancellation must not strand the mutex.
        // Cloud writes already inside the gate have their own bounded timeout,
        // so a non-cancellable hand-off is safe and finite.
        withContext(NonCancellable) {
            cloudWriteMutex.lock()
            synchronized(stateLock) { deletionLockHeld = true }
        }
    }

    /**
     * Finish the in-process barrier and persist the outcome. Interrupted
     * deletion intentionally keeps the in-progress marker so a future worker
     * cannot resurrect cloud records; the user can retry deletion after login.
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
                    -> {
                        updateDeletionMarkers(
                            inProgress = { it - ownerId },
                            cleanupPending = { it - ownerId },
                            cloudDeletionPending = { it - ownerId },
                            authDeletionStarted = { it - ownerId },
                            authDeletionComplete = { it - ownerId },
                            localRecoveryCopyPending = { it - ownerId },
                            localRecoveryCopyReady = { it - ownerId },
                        )
                    }
                    AccountDeletionOutcome.CleanupPending -> {
                        updateDeletionMarkers(
                            inProgress = { it - ownerId },
                            cleanupPending = { it + ownerId },
                            cloudDeletionPending = { it + ownerId },
                            authDeletionStarted = { it - ownerId },
                            authDeletionComplete = { it + ownerId },
                            localRecoveryCopyPending = { it - ownerId },
                            localRecoveryCopyReady = { it },
                        )
                    }
                    AccountDeletionOutcome.AuthDeletionPending -> {
                        // Keep the auth intent as the durable owner block, but
                        // release the process-local in-progress lock. We must
                        // not clear the staged copy or schedule a resync until
                        // Firebase confirms whether the account still exists.
                        updateDeletionMarkers(
                            inProgress = { it - ownerId },
                            cleanupPending = { it },
                            cloudDeletionPending = { it },
                            authDeletionStarted = { it + ownerId },
                            authDeletionComplete = { it - ownerId },
                            localRecoveryCopyPending = { it },
                            localRecoveryCopyReady = { it },
                        )
                    }
                    AccountDeletionOutcome.LocalRecoveryPending -> {
                        // The cloud data is gone, but Auth was not invoked and
                        // the global __local__ recovery copy could not be
                        // discarded. Keep the cloud phase and recovery marker
                        // until startup can finish the discard and re-queue the
                        // still-existing account.
                        updateDeletionMarkers(
                            inProgress = { it - ownerId },
                            cleanupPending = { it },
                            cloudDeletionPending = { it + ownerId },
                            authDeletionStarted = { it - ownerId },
                            authDeletionComplete = { it - ownerId },
                            localRecoveryCopyPending = { it },
                            localRecoveryCopyReady = { it },
                        )
                    }
                    // Keep the durable in-progress marker, but release the
                    // process-local lock so a user can retry after cancellation.
                    AccountDeletionOutcome.Interrupted -> Unit
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

    /** Clears a durable cleanup marker after the owner cache was removed. */
    internal fun completeDeletionCleanup(ownerId: String) {
        synchronized(stateLock) {
            updateDeletionMarkers(
                inProgress = { it - ownerId },
                cleanupPending = { it - ownerId },
                cloudDeletionPending = { it - ownerId },
                authDeletionStarted = { it - ownerId },
                authDeletionComplete = { it - ownerId },
                localRecoveryCopyPending = { it - ownerId },
                localRecoveryCopyReady = { it - ownerId },
            )
        }
    }

    internal fun pendingCleanupOwnerIds(): Set<String> = synchronized(stateLock) {
        readOwners(KEY_CLEANUP_PENDING) + readOwners(KEY_AUTH_DELETION_COMPLETE)
    }

    /** Owners whose cloud data is gone but Auth completion is not resolved. */
    internal fun cloudDeletionPendingOwnerIds(): Set<String> = synchronized(stateLock) {
        readOwners(KEY_CLOUD_DELETION_PENDING) -
            readOwners(KEY_CLEANUP_PENDING) -
            readOwners(KEY_AUTH_DELETION_COMPLETE)
    }

    internal fun isDeletionBlocked(ownerId: String?): Boolean =
        synchronized(stateLock) {
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
                if (isDeletionBlockedLocked(ownerId) ||
                    cloudWriteGeneration != generation
                ) {
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
            // paths are being removed. A different signed-in account may still
            // sync while the deletion is waiting on the first account's writes.
            if (ownerId == null || ownerId == deletingOwner) return true
        }
        val inProgress = readOwners(KEY_IN_PROGRESS)
        val cleanupPending = readOwners(KEY_CLEANUP_PENDING)
        val cloudDeletionPending = readOwners(KEY_CLOUD_DELETION_PENDING)
        val authDeletionStarted = readOwners(KEY_AUTH_DELETION_STARTED)
        val authDeletionComplete = readOwners(KEY_AUTH_DELETION_COMPLETE)
        val localRecoveryCopyPending = readOwners(KEY_LOCAL_RECOVERY_COPY_PENDING)
        val localRecoveryCopyReady = readOwners(KEY_LOCAL_RECOVERY_COPY_READY)
        // The recovery copy uses one global LOCAL_OWNER_ID namespace. It must
        // therefore block every account until it is either discarded or
        // promoted to durable cleanup; otherwise another account's first sync
        // could adopt the unresolved copy.
        if (localRecoveryCopyPending.isNotEmpty() || localRecoveryCopyReady.isNotEmpty()) {
            return true
        }
        return if (ownerId == null) {
            inProgress.isNotEmpty() || cleanupPending.isNotEmpty() || cloudDeletionPending.isNotEmpty() || authDeletionStarted.isNotEmpty() || authDeletionComplete.isNotEmpty()
        } else {
            ownerId in inProgress || ownerId in cleanupPending || ownerId in cloudDeletionPending || ownerId in authDeletionStarted || ownerId in authDeletionComplete
        }
    }

    private fun readOwners(key: String): Set<String> =
        requireNotNull(stateStore).readOwners(key)

    private fun updateDeletionMarkers(
        inProgress: (Set<String>) -> Set<String>,
        cleanupPending: (Set<String>) -> Set<String>,
        cloudDeletionPending: (Set<String>) -> Set<String> = { it },
        authDeletionStarted: (Set<String>) -> Set<String> = { it },
        authDeletionComplete: (Set<String>) -> Set<String> = { it },
        localRecoveryCopyPending: (Set<String>) -> Set<String> = { it },
        localRecoveryCopyReady: (Set<String>) -> Set<String> = { it },
    ) {
        val store = requireNotNull(stateStore)
        val currentInProgress = store.readOwners(KEY_IN_PROGRESS)
        val currentCleanupPending = store.readOwners(KEY_CLEANUP_PENDING)
        val currentCloudDeletionPending = store.readOwners(KEY_CLOUD_DELETION_PENDING)
        val currentAuthDeletionStarted = store.readOwners(KEY_AUTH_DELETION_STARTED)
        val currentAuthDeletionComplete = store.readOwners(KEY_AUTH_DELETION_COMPLETE)
        val currentLocalRecoveryCopyPending = store.readOwners(KEY_LOCAL_RECOVERY_COPY_PENDING)
        val currentLocalRecoveryCopyReady = store.readOwners(KEY_LOCAL_RECOVERY_COPY_READY)
        store.writeMarkers(
            inProgress(currentInProgress),
            cleanupPending(currentCleanupPending),
            cloudDeletionPending(currentCloudDeletionPending),
            authDeletionStarted(currentAuthDeletionStarted),
            authDeletionComplete(currentAuthDeletionComplete),
            localRecoveryCopyPending(currentLocalRecoveryCopyPending),
            localRecoveryCopyReady(currentLocalRecoveryCopyReady),
        )
    }

    private const val KEY_IN_PROGRESS = "in_progress_owner_ids"
    private const val KEY_CLEANUP_PENDING = "cleanup_pending_owner_ids"
    private const val KEY_CLOUD_DELETION_PENDING = "cloud_deletion_pending_owner_ids"
    private const val KEY_AUTH_DELETION_STARTED = "auth_deletion_started_owner_ids"
    private const val KEY_AUTH_DELETION_COMPLETE = "auth_deletion_complete_owner_ids"
    private const val KEY_LOCAL_RECOVERY_COPY_PENDING = "local_recovery_copy_pending_owner_ids"
    private const val KEY_LOCAL_RECOVERY_COPY_READY = "local_recovery_copy_ready_owner_ids"
}
