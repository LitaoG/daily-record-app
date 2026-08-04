package io.github.litaog.dailyrecord.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object DailyRecordSyncScheduler {
    private const val UNIQUE_WORK_NAME = "daily-record-cloud-sync"
    // The old name is cancelled once so upgrades do not leave a hand-brew-only
    // request running after the scheduler became module-agnostic.
    private const val LEGACY_UNIQUE_WORK_NAME = "hand-brew-cloud-sync"
    internal val workName = UNIQUE_WORK_NAME
    internal val workPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE

    fun schedule(context: Context) {
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

    suspend fun cancelAndAwait(context: Context) {
        withContext(Dispatchers.IO) {
            val workManager = WorkManager.getInstance(context.applicationContext)
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME).result.get()
            workManager.cancelUniqueWork(LEGACY_UNIQUE_WORK_NAME).result.get()
        }
    }
}
