package io.github.litaog.dailyrecord.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.litaog.dailyrecord.core.cloud.BACKGROUND_CLOUD_TIMEOUT_MILLIS
import io.github.litaog.dailyrecord.core.cloud.FirebaseServices
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

internal fun interface DailyRecordSyncServicesProvider {
    fun create(context: Context, database: DailyRecordDatabase): FirebaseServices
}

private object DefaultDailyRecordSyncServicesProvider : DailyRecordSyncServicesProvider {
    override fun create(context: Context, database: DailyRecordDatabase): FirebaseServices =
        FirebaseServices.create(context, database = database)
}

class DailyRecordSyncWorker internal constructor(
    appContext: Context,
    workerParams: WorkerParameters,
    private val servicesProvider: DailyRecordSyncServicesProvider,
) : CoroutineWorker(appContext, workerParams) {
    constructor(appContext: Context, workerParams: WorkerParameters) : this(
        appContext = appContext,
        workerParams = workerParams,
        servicesProvider = DefaultDailyRecordSyncServicesProvider,
    )

    override suspend fun doWork(): Result {
        // Kept for the lightweight unit-test hook and for callers that do not
        // yet have an authenticated owner. Production account barriers are
        // checked again below, after resolving the Firebase owner id.
        if (DailyRecordSyncScheduler.isLegacyDeletionBlocked) return Result.success()
        val database = DailyRecordDatabase.create(applicationContext)
        return try {
            val services = servicesProvider.create(applicationContext, database)
            val ownerId = services.currentUserId()
            when {
                !services.productionConfigured -> Result.failure()
                ownerId == null -> Result.success()
                // Account deletion owns the user's cloud paths. Check the durable
                // account marker after resolving the owner so a stale marker for a
                // different account cannot silently affect this session.
                DailyRecordSyncScheduler.isLegacyDeletionBlocked ||
                    DailyRecordSyncScheduler.isDeletionBlocked(applicationContext, ownerId) -> {
                    Result.success()
                }
                else -> {
                    val coordinator = CombinedSyncCoordinator(
                        handBrew = HandBrewSyncCoordinator(
                            store = RoomHandBrewSyncStore(database),
                            remote = services.remoteDataSource,
                        ),
                        sex = SexSyncCoordinator(
                            store = RoomSexSyncStore(database),
                            remote = services.sexRemoteDataSource,
                        ),
                    )
                    val result = withTimeout(BACKGROUND_CLOUD_TIMEOUT_MILLIS) {
                        DailyRecordSyncScheduler.withCloudWrite(
                            context = applicationContext,
                            ownerId = ownerId,
                        ) {
                            coordinator.syncOnce(ownerId)
                        }
                    }
                    if (result.workerShouldRetry()) Result.retry() else Result.success()
                }
            }
        } catch (_: AccountDeletionInProgressException) {
            Result.success()
        } catch (_: TimeoutCancellationException) {
            // Background reconciliation is bounded independently from the
            // five-second interactive action, then WorkManager retries it.
            Result.retry()
        } catch (error: CancellationException) {
            throw error
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

/**
 * A sync attempt that still has pending rows should be retried only when the
 * rows failed for transient reasons. Rows whose cloud documents were rejected
 * as malformed can never sync: retrying them would burn WorkManager backoff
 * and Firestore quota until the ceiling without making progress.
 */
internal fun SyncResult.workerShouldRetry(): Boolean =
    pending > 0 && rejectedRemoteRecords == 0
