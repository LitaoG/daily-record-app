package io.github.litaog.dailyrecord.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.litaog.dailyrecord.core.model.RecordStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
internal interface DailyRecordDao {
    @Query(
        """
        SELECT * FROM daily_records
        WHERE owner_id = :ownerId
          AND activity_id = :activityId
          AND local_date = :localDate
          AND deleted_at IS NULL
        LIMIT 1
        """,
    )
    fun observeByKey(
        ownerId: String,
        activityId: String,
        localDate: LocalDate,
    ): Flow<DailyRecordEntity?>

    @Query(
        """
        SELECT * FROM daily_records
        WHERE owner_id = :ownerId
          AND activity_id = :activityId
          AND local_date = :localDate
          AND deleted_at IS NULL
        LIMIT 1
        """,
    )
    suspend fun getByKey(
        ownerId: String,
        activityId: String,
        localDate: LocalDate,
    ): DailyRecordEntity?

    @Query(
        """
        SELECT * FROM daily_records
        WHERE owner_id = :ownerId
          AND activity_id = :activityId
          AND local_date = :localDate
        LIMIT 1
        """,
    )
    suspend fun getAnyByKey(
        ownerId: String,
        activityId: String,
        localDate: LocalDate,
    ): DailyRecordEntity?

    @Query(
        """
        SELECT * FROM daily_records
        WHERE owner_id = :ownerId
          AND local_date >= :startDate
          AND local_date < :endExclusive
          AND deleted_at IS NULL
          AND (:activityId IS NULL OR activity_id = :activityId)
        ORDER BY local_date ASC, activity_id ASC
        """,
    )
    fun observeForRange(
        ownerId: String,
        activityId: String?,
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<DailyRecordEntity>>

    @Query(
        """
        SELECT * FROM daily_records
        WHERE owner_id = :ownerId
          AND local_date >= :startDate
          AND local_date < :endExclusive
          AND deleted_at IS NULL
          AND (:activityId IS NULL OR activity_id = :activityId)
        ORDER BY local_date ASC, activity_id ASC
        """,
    )
    suspend fun getForRange(
        ownerId: String,
        activityId: String?,
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): List<DailyRecordEntity>

    @Query(
        """
        SELECT COUNT(*) FROM daily_records
        WHERE owner_id = :ownerId
          AND activity_id = :activityId
        """,
    )
    suspend fun countForActivity(ownerId: String, activityId: String): Int

    @Upsert
    suspend fun upsert(record: DailyRecordEntity)

    @Query(
        """
        UPDATE daily_records
        SET status = :unsetStatus,
            quantity = NULL,
            note = NULL,
            occurred_at = NULL,
            updated_at = :deletedAt,
            deleted_at = :deletedAt,
            revision = revision + 1
        WHERE owner_id = :ownerId
          AND activity_id = :activityId
          AND local_date = :localDate
          AND deleted_at IS NULL
        """,
    )
    suspend fun softDeleteByKey(
        ownerId: String,
        activityId: String,
        localDate: LocalDate,
        deletedAt: Instant,
        unsetStatus: RecordStatus,
    ): Int
}
