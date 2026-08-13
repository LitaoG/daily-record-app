package io.github.litaog.dailyrecord.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SexRecordDao : DailyCountRecordDao<SexRecordEntity> {
    @Query(
        "SELECT * FROM sex_records " +
            "WHERE owner_id = :ownerId AND local_date = :localDate AND is_deleted = 0 LIMIT 1",
    )
    override fun observeByDate(ownerId: String, localDate: LocalDate): Flow<SexRecordEntity?>

    @Query(
        "SELECT * FROM sex_records " +
            "WHERE owner_id = :ownerId AND local_date = :localDate LIMIT 1",
    )
    override suspend fun getByDate(ownerId: String, localDate: LocalDate): SexRecordEntity?

    @Query(
        """
        SELECT * FROM sex_records
        WHERE owner_id = :ownerId
          AND is_deleted = 0
          AND local_date >= :startDate AND local_date < :endExclusive
        ORDER BY local_date ASC
        """,
    )
    override fun observeForRange(
        ownerId: String,
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<SexRecordEntity>>

    @Upsert
    override suspend fun upsert(record: SexRecordEntity)

    @Query(
        """
        UPDATE sex_records
        SET is_deleted = 1,
            updated_at = :updatedAt,
            sync_state = '$SYNC_PENDING'
        WHERE id = :id
          AND owner_id = :ownerId
          AND is_deleted = 0
          AND updated_at = :expectedUpdatedAt
        """,
    )
    override suspend fun markDeleted(
        ownerId: String,
        id: String,
        expectedUpdatedAt: Instant,
        updatedAt: Instant,
    ): Int

    @Query(
        "SELECT * FROM sex_records " +
            "WHERE owner_id = :ownerId AND sync_state = '$SYNC_PENDING' ORDER BY updated_at ASC",
    )
    override suspend fun getPending(ownerId: String): List<SexRecordEntity>

    @Query("SELECT * FROM sex_records WHERE owner_id = :ownerId ORDER BY local_date ASC")
    override suspend fun getAllForSync(ownerId: String): List<SexRecordEntity>

    @Query(
        """
        UPDATE sex_records
        SET id = :remoteId,
            remote_revision = :remoteRevision
        WHERE owner_id = :ownerId
          AND local_date = :localDate
          AND sync_state = '$SYNC_PENDING'
          AND remote_revision = 0
        """,
    )
    override suspend fun setRemoteRevisionForUnbasedPending(
        ownerId: String,
        localDate: LocalDate,
        remoteId: String,
        remoteRevision: Long,
    ): Int

    @Query(
        """
        UPDATE sex_records
        SET remote_revision = :remoteRevision
        WHERE owner_id = :ownerId
          AND local_date = :localDate
          AND sync_state = '$SYNC_PENDING'
        """,
    )
    override suspend fun setRemoteRevisionForPending(
        ownerId: String,
        localDate: LocalDate,
        remoteRevision: Long,
    ): Int

    @Query("SELECT COUNT(*) FROM sex_records WHERE owner_id = :ownerId AND sync_state = '$SYNC_PENDING'")
    override fun observePendingCount(ownerId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM sex_records WHERE owner_id = :ownerId AND sync_state = '$SYNC_PENDING'")
    override suspend fun countPending(ownerId: String): Int

    @Query("SELECT COUNT(*) FROM sex_records WHERE owner_id = :ownerId")
    override suspend fun countForOwner(ownerId: String): Int

    @Query(
        """
        UPDATE sex_records
        SET sync_state = '$SYNC_PENDING'
        WHERE owner_id = :ownerId
        """,
    )
    override suspend fun markOwnerPendingForResync(ownerId: String): Int
    @Query("DELETE FROM sex_records WHERE owner_id = :ownerId")
    override suspend fun deleteOwnerCache(ownerId: String): Int

    @Query("DELETE FROM sex_records WHERE owner_id = :ownerId AND local_date = :localDate")
    override suspend fun deleteByOwnerDate(ownerId: String, localDate: LocalDate): Int
}
