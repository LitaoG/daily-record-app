package io.github.litaog.dailyrecord.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object HandBrewSyncScheduler {
    // Kept stable so upgrades do not leave an older WorkManager request orphaned.
    private const val UNIQUE_WORK_NAME = "hand-brew-cloud-sync"
    internal val workPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE

    fun schedule(context: Context) {
        val request = OneTimeWorkRequestBuilder<HandBrewSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            workPolicy,
            request,
        )
    }

    suspend fun cancelAndAwait(context: Context) {
        withContext(Dispatchers.IO) {
            WorkManager.getInstance(context.applicationContext)
                .cancelUniqueWork(UNIQUE_WORK_NAME)
                .result
                .get()
        }
    }
}
