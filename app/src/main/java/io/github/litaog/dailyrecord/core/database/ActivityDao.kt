package io.github.litaog.dailyrecord.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
internal interface ActivityDao {
    @Query(
        """
        SELECT * FROM activities
        WHERE owner_id = :ownerId
          AND deleted_at IS NULL
          AND (:includeArchived = 1 OR is_archived = 0)
        ORDER BY sort_order ASC, created_at ASC, id ASC
        """,
    )
    fun observeByOwner(ownerId: String, includeArchived: Boolean): Flow<List<ActivityEntity>>

    @Query(
        """
        SELECT * FROM activities
        WHERE owner_id = :ownerId AND id = :activityId AND deleted_at IS NULL
        LIMIT 1
        """,
    )
    suspend fun getById(ownerId: String, activityId: String): ActivityEntity?

    @Upsert
    suspend fun upsert(activity: ActivityEntity)

    @Query(
        """
        UPDATE activities
        SET is_archived = 1,
            updated_at = :updatedAt,
            revision = revision + 1
        WHERE owner_id = :ownerId AND id = :activityId AND deleted_at IS NULL
        """,
    )
    suspend fun archive(ownerId: String, activityId: String, updatedAt: Instant): Int
}
