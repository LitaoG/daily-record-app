package io.github.litaog.dailyrecord.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal enum class AccountDeletionOutcome {
    Completed,
    CleanupPending,
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

internal object DailyRecordSyncScheduler {
    private const val UNIQUE_WORK_NAME = "daily-record-cloud-sync"
    // The legacy name is cancelled on every schedule so a hand-brew-only
    // request enqueued before the scheduler became module-agnostic can
    // never run concurrently (cancelling an absent work is a no-op).
    private const val LEGACY_UNIQUE_WORK_NAME = "hand-brew-cloud-sync"
    internal val workName = UNIQUE_WORK_NAME
    internal val workPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE

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
    internal fun beginDeletionBlock(context: Context, ownerId: String) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        synchronized(stateLock) {
            check(inProcessDeletionOwner == null) {
                "An account deletion is already active in this process."
            }
            inProcessDeletionOwner = ownerId
            deletionBlocked = true
            cloudWriteGeneration += 1
            try {
                updateOwners(context, KEY_IN_PROGRESS) { it + ownerId }
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
        context: Context,
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
                            context = context,
                            inProgress = { it - ownerId },
                            cleanupPending = { it - ownerId },
                        )
                    }
                    AccountDeletionOutcome.CleanupPending -> {
                        updateDeletionMarkers(
                            context = context,
                            inProgress = { it - ownerId },
                            cleanupPending = { it + ownerId },
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
    internal fun completeDeletionCleanup(context: Context, ownerId: String) {
        synchronized(stateLock) {
            updateDeletionMarkers(
                context = context,
                inProgress = { it - ownerId },
                cleanupPending = { it - ownerId },
            )
        }
    }

    internal fun pendingCleanupOwnerIds(context: Context): Set<String> = synchronized(stateLock) {
        readOwners(context, KEY_CLEANUP_PENDING)
    }

    internal fun isDeletionBlocked(context: Context, ownerId: String): Boolean =
        synchronized(stateLock) {
            isDeletionBlockedLocked(context, ownerId)
        }

    /** Schedules only when the account is not blocked for deletion (durable or in-process). */
    fun schedule(context: Context, ownerId: String? = null) {
        synchronized(stateLock) {
            if (isDeletionBlockedLocked(context, ownerId)) return
            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.cancelUniqueWork(LEGACY_UNIQUE_WORK_NAME)
            val request = OneTimeWorkRequestBuilder<DailyRecordSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            workManager.enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                workPolicy,
                request,
            )
        }
    }

    internal fun cloudWriteGate(context: Context): CloudWriteGate =
        object : CloudWriteGate {
            override suspend fun <T> withWrite(ownerId: String, block: suspend () -> T): T =
                withCloudWrite(context, ownerId, block)
        }

    internal suspend fun <T> withCloudWrite(
        context: Context,
        ownerId: String,
        block: suspend () -> T,
    ): T {
        val generation = synchronized(stateLock) {
            if (isDeletionBlockedLocked(context, ownerId)) {
                throw AccountDeletionInProgressException(ownerId)
            }
            cloudWriteGeneration
        }
        return cloudWriteMutex.withLock {
            synchronized(stateLock) {
                if (isDeletionBlockedLocked(context, ownerId) ||
                    cloudWriteGeneration != generation
                ) {
                    throw AccountDeletionInProgressException(ownerId)
                }
            }
            block()
        }
    }

    suspend fun cancelAndAwait(context: Context) {
        withContext(Dispatchers.IO) {
            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME).result.get()
            workManager.cancelUniqueWork(LEGACY_UNIQUE_WORK_NAME).result.get()
        }
    }

    private fun isDeletionBlockedLocked(context: Context, ownerId: String?): Boolean {
        if (legacyDeletionBlocked) return true
        inProcessDeletionOwner?.let { deletingOwner ->
            // A deletion in this process only blocks the account whose cloud
            // paths are being removed. A different signed-in account may still
            // sync while the deletion is waiting on the first account's writes.
            if (ownerId == null || ownerId == deletingOwner) return true
        }
        val inProgress = readOwners(context, KEY_IN_PROGRESS)
        val cleanupPending = readOwners(context, KEY_CLEANUP_PENDING)
        return if (ownerId == null) {
            inProgress.isNotEmpty() || cleanupPending.isNotEmpty()
        } else {
            ownerId in inProgress || ownerId in cleanupPending
        }
    }

    private fun readOwners(context: Context, key: String): Set<String> = context
        .applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        .getStringSet(key, emptySet())
        .orEmpty()

    private fun updateOwners(
        context: Context,
        key: String,
        transform: (Set<String>) -> Set<String>,
    ) {
        val preferences = context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        val updated = transform(preferences.getStringSet(key, emptySet()).orEmpty())
        check(preferences.edit().putStringSet(key, updated).commit()) {
            "Unable to persist account deletion state"
        }
    }

    private fun updateDeletionMarkers(
        context: Context,
        inProgress: (Set<String>) -> Set<String>,
        cleanupPending: (Set<String>) -> Set<String>,
    ) {
        val preferences = context.applicationContext.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )
        val currentInProgress = preferences.getStringSet(KEY_IN_PROGRESS, emptySet()).orEmpty()
        val currentCleanupPending = preferences
            .getStringSet(KEY_CLEANUP_PENDING, emptySet())
            .orEmpty()
        check(
            preferences.edit()
                .putStringSet(KEY_IN_PROGRESS, inProgress(currentInProgress))
                .putStringSet(KEY_CLEANUP_PENDING, cleanupPending(currentCleanupPending))
                .commit(),
        ) {
            "Unable to persist account deletion state"
        }
    }

    private const val PREFERENCES_NAME = "daily_record_deletion_state"
    private const val KEY_IN_PROGRESS = "in_progress_owner_ids"
    private const val KEY_CLEANUP_PENDING = "cleanup_pending_owner_ids"
}
