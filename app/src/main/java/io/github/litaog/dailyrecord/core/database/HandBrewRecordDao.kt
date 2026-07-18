package io.github.litaog.dailyrecord.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.litaog.dailyrecord.core.model.HandBrewSummary
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
internal interface HandBrewRecordDao {
    @Query(
        "SELECT * FROM hand_brew_records " +
            "WHERE owner_id = :ownerId AND local_date = :localDate AND is_deleted = 0 LIMIT 1",
    )
    fun observeByDate(ownerId: String, localDate: LocalDate): Flow<HandBrewRecordEntity?>

    @Query(
        "SELECT * FROM hand_brew_records " +
            "WHERE owner_id = :ownerId AND local_date = :localDate LIMIT 1",
    )
    suspend fun getByDate(ownerId: String, localDate: LocalDate): HandBrewRecordEntity?

    @Query(
        """
        SELECT * FROM hand_brew_records
        WHERE owner_id = :ownerId
          AND is_deleted = 0
          AND local_date >= :startDate AND local_date < :endExclusive
        ORDER BY local_date ASC
        """,
    )
    fun observeForRange(
        ownerId: String,
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<HandBrewRecordEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(brew_count), 0) AS totalCount,
               COALESCE(SUM(CASE WHEN brew_count > 0 THEN 1 ELSE 0 END), 0) AS brewDays
        FROM hand_brew_records
        WHERE owner_id = :ownerId
          AND is_deleted = 0
          AND local_date >= :startDate AND local_date < :endExclusive
        """,
    )
    fun observeSummary(
        ownerId: String,
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<HandBrewSummary>

    @Upsert
    suspend fun upsert(record: HandBrewRecordEntity)

    @Query(
        """
        UPDATE hand_brew_records
        SET is_deleted = 1,
            updated_at = :updatedAt,
            sync_state = 'PENDING'
        WHERE id = :id AND owner_id = :ownerId AND is_deleted = 0
        """,
    )
    suspend fun markDeleted(ownerId: String, id: String, updatedAt: Instant): Int

    @Query(
        "SELECT * FROM hand_brew_records " +
            "WHERE owner_id = :ownerId AND sync_state = 'PENDING' ORDER BY updated_at ASC",
    )
    suspend fun getPending(ownerId: String): List<HandBrewRecordEntity>

    @Query("SELECT * FROM hand_brew_records WHERE owner_id = :ownerId ORDER BY local_date ASC")
    suspend fun getAllForSync(ownerId: String): List<HandBrewRecordEntity>

    @Query(
        """
        UPDATE hand_brew_records
        SET sync_state = 'SYNCED', remote_revision = :remoteRevision
        WHERE id = :id
          AND owner_id = :ownerId
          AND updated_at = :expectedUpdatedAt
          AND sync_state = 'PENDING'
        """,
    )
    suspend fun markSyncedIfUnchanged(
        ownerId: String,
        id: String,
        expectedUpdatedAt: Instant,
        remoteRevision: Long,
    ): Int

    @Query("SELECT COUNT(*) FROM hand_brew_records WHERE owner_id = :ownerId AND sync_state = 'PENDING'")
    fun observePendingCount(ownerId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM hand_brew_records WHERE owner_id = :ownerId")
    suspend fun countForOwner(ownerId: String): Int

    @Query(
        """
        UPDATE hand_brew_records
        SET owner_id = :newOwnerId, sync_state = 'PENDING', remote_revision = 0
        WHERE owner_id = :oldOwnerId
        """,
    )
    suspend fun moveOwner(oldOwnerId: String, newOwnerId: String): Int

    @Query("DELETE FROM hand_brew_records WHERE owner_id = :ownerId")
    suspend fun deleteOwnerCache(ownerId: String): Int

    @Query("DELETE FROM hand_brew_records WHERE owner_id = :ownerId AND local_date = :localDate")
    suspend fun deleteByOwnerDate(ownerId: String, localDate: LocalDate): Int
}
