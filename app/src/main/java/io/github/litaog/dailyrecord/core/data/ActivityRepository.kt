package io.github.litaog.dailyrecord.core.data

import io.github.litaog.dailyrecord.core.model.Activity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface ActivityRepository {
    fun observeActivities(ownerId: String, includeArchived: Boolean = false): Flow<List<Activity>>

    suspend fun getActivity(ownerId: String, activityId: String): Activity?

    suspend fun saveActivity(activity: Activity)

    suspend fun archiveActivity(ownerId: String, activityId: String, updatedAt: Instant)
}
