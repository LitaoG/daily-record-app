package io.github.litaog.dailyrecord.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.litaog.dailyrecord.core.cloud.FirebaseServices
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase

class HandBrewSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val services = FirebaseServices.create(applicationContext)
        if (!services.productionConfigured) return Result.failure()
        val ownerId = services.currentUserId() ?: return Result.success()
        val database = DailyRecordDatabase.create(applicationContext)
        return try {
            val result = HandBrewSyncCoordinator(
                store = RoomHandBrewSyncStore(database),
                remote = services.remoteDataSource,
            ).syncOnce(ownerId)
            if (result.pending == 0) Result.success() else Result.retry()
        } catch (_: Exception) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        } finally {
            database.close()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
    }
}
