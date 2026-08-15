package io.github.litaog.dailyrecord.core.di

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import io.github.litaog.dailyrecord.core.sync.DeletionBarrier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * WorkManager scheduling for the daily-count cloud sync. The deletion state
 * machine lives in [DeletionBarrier]; this scheduler only enqueues work and
 * consults the barrier before doing so.
 */
internal object DailyRecordSyncScheduler {
    private const val UNIQUE_WORK_NAME = "daily-record-cloud-sync"
    // The legacy name is cancelled on every schedule so a hand-brew-only
    // request enqueued before the scheduler became module-agnostic can
    // never run concurrently (cancelling an absent work is a no-op).
    private const val LEGACY_UNIQUE_WORK_NAME = "hand-brew-cloud-sync"
    internal const val SYNC_DEBOUNCE_MILLIS = 750L
    internal val workName = UNIQUE_WORK_NAME
    internal val workPolicy = ExistingWorkPolicy.REPLACE

    /** Schedules only when the account is not blocked for deletion (durable or in-process). */
    fun schedule(context: Context, ownerId: String? = null) {
        if (DeletionBarrier.isDeletionBlocked(ownerId)) return
        val workManager = WorkManager.getInstance(context.applicationContext)
        workManager.cancelUniqueWork(LEGACY_UNIQUE_WORK_NAME)
        val request = OneTimeWorkRequestBuilder<DailyRecordSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setInitialDelay(SYNC_DEBOUNCE_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            workPolicy,
            request,
        )
    }

    suspend fun cancelAndAwait(context: Context) {
        withContext(Dispatchers.IO) {
            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME).result.get()
            workManager.cancelUniqueWork(LEGACY_UNIQUE_WORK_NAME).result.get()
        }
    }
}
