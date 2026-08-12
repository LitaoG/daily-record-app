package io.github.litaog.dailyrecord.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SexRecordDetailDao : DailyCountRecordDetailDao<SexRecordDetailEntity> {
    @Query(
        "SELECT * FROM sex_record_details " +
            "WHERE owner_id = :ownerId AND local_date = :localDate " +
            "ORDER BY occurrence_index ASC",
    )
    override fun observeByDate(ownerId: String, localDate: LocalDate): Flow<List<SexRecordDetailEntity>>

    @Query(
        "SELECT * FROM sex_record_details " +
            "WHERE owner_id = :ownerId AND local_date = :localDate " +
            "ORDER BY occurrence_index ASC",
    )
    override suspend fun getByDate(ownerId: String, localDate: LocalDate): List<SexRecordDetailEntity>

    @Upsert
    override suspend fun upsertAll(details: List<SexRecordDetailEntity>)

    @Query(
        "DELETE FROM sex_record_details " +
            "WHERE owner_id = :ownerId AND local_date = :localDate",
    )
    override suspend fun deleteByOwnerDate(ownerId: String, localDate: LocalDate): Int

    @Query("SELECT COUNT(*) FROM sex_record_details WHERE owner_id = :ownerId")
    override suspend fun countForOwner(ownerId: String): Int

    @Query("DELETE FROM sex_record_details WHERE owner_id = :ownerId")
    override suspend fun deleteOwnerCache(ownerId: String): Int
}
