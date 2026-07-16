package io.github.litaog.dailyrecord.core.data

import androidx.room.withTransaction
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.asEntity
import io.github.litaog.dailyrecord.core.database.asExternalModel
import io.github.litaog.dailyrecord.core.model.Activity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

internal class RoomActivityRepository(
    private val database: DailyRecordDatabase,
) : ActivityRepository {
    private val activityDao = database.activityDao()
    private val dailyRecordDao = database.dailyRecordDao()

    override fun observeActivities(ownerId: String, includeArchived: Boolean): Flow<List<Activity>> =
        activityDao.observeByOwner(ownerId, includeArchived).map { activities ->
            activities.map { it.asExternalModel() }
        }

    override suspend fun getActivity(ownerId: String, activityId: String): Activity? =
        activityDao.getById(ownerId, activityId)?.asExternalModel()

    override suspend fun saveActivity(activity: Activity) {
        database.withTransaction {
            val existing = activityDao.getById(activity.ownerId, activity.id)
            if (
                existing != null &&
                existing.measurementType != activity.measurementType &&
                dailyRecordDao.countForActivity(activity.ownerId, activity.id) > 0
            ) {
                error("Cannot change measurementType after records exist; archive and create a new activity.")
            }
            activityDao.upsert(activity.asEntity())
        }
    }

    override suspend fun archiveActivity(ownerId: String, activityId: String, updatedAt: Instant) {
        val changed = activityDao.archive(ownerId, activityId, updatedAt)
        check(changed == 1) { "Activity does not exist or is already deleted." }
    }
}
