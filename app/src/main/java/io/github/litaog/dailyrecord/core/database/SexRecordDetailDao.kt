package io.github.litaog.dailyrecord.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
internal interface SexRecordDetailDao {
    @Query(
        "SELECT * FROM sex_record_details " +
            "WHERE owner_id = :ownerId AND local_date = :localDate " +
            "ORDER BY occurrence_index ASC",
    )
    fun observeByDate(ownerId: String, localDate: LocalDate): Flow<List<SexRecordDetailEntity>>

    @Query(
        "SELECT * FROM sex_record_details " +
            "WHERE owner_id = :ownerId AND local_date = :localDate " +
            "ORDER BY occurrence_index ASC",
    )
    suspend fun getByDate(ownerId: String, localDate: LocalDate): List<SexRecordDetailEntity>

    @Upsert
    suspend fun upsertAll(details: List<SexRecordDetailEntity>)

    @Query(
        "DELETE FROM sex_record_details " +
            "WHERE owner_id = :ownerId AND local_date = :localDate",
    )
    suspend fun deleteByOwnerDate(ownerId: String, localDate: LocalDate): Int

    @Query("SELECT * FROM sex_record_details WHERE owner_id = :ownerId ORDER BY local_date, occurrence_index")
    suspend fun getAllForSync(ownerId: String): List<SexRecordDetailEntity>

    @Query(
        "UPDATE sex_record_details " +
            "SET owner_id = :newOwnerId " +
            "WHERE owner_id = :oldOwnerId",
    )
    suspend fun moveOwner(oldOwnerId: String, newOwnerId: String): Int

    @Query("SELECT COUNT(*) FROM sex_record_details WHERE owner_id = :ownerId")
    suspend fun countForOwner(ownerId: String): Int

    @Query("DELETE FROM sex_record_details WHERE owner_id = :ownerId")
    suspend fun deleteOwnerCache(ownerId: String): Int
}
