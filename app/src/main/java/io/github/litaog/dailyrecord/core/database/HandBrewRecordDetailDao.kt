package io.github.litaog.dailyrecord.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
internal interface HandBrewRecordDetailDao : DailyCountRecordDetailDao<HandBrewRecordDetailEntity> {
    @Query(
        "SELECT * FROM hand_brew_record_details " +
            "WHERE owner_id = :ownerId AND local_date = :localDate " +
            "ORDER BY occurrence_index ASC",
    )
    override fun observeByDate(ownerId: String, localDate: LocalDate): Flow<List<HandBrewRecordDetailEntity>>

    @Query(
        "SELECT * FROM hand_brew_record_details " +
            "WHERE owner_id = :ownerId AND local_date = :localDate " +
            "ORDER BY occurrence_index ASC",
    )
    override suspend fun getByDate(ownerId: String, localDate: LocalDate): List<HandBrewRecordDetailEntity>

    @Query(
        "SELECT * FROM hand_brew_record_details " +
            "WHERE owner_id = :ownerId ORDER BY local_date ASC, occurrence_index ASC",
    )
    override suspend fun getAllForSync(ownerId: String): List<HandBrewRecordDetailEntity>

    @Upsert
    override suspend fun upsertAll(details: List<HandBrewRecordDetailEntity>)

    @Query(
        "DELETE FROM hand_brew_record_details " +
            "WHERE owner_id = :ownerId AND local_date = :localDate",
    )
    override suspend fun deleteByOwnerDate(ownerId: String, localDate: LocalDate): Int

    @Query(
        "DELETE FROM hand_brew_record_details " +
            "WHERE owner_id = :ownerId AND local_date = :localDate AND id IN (:ids)",
    )
    override suspend fun deleteByOwnerDateAndIds(
        ownerId: String,
        localDate: LocalDate,
        ids: List<String>,
    ): Int

    @Query("SELECT COUNT(*) FROM hand_brew_record_details WHERE owner_id = :ownerId")
    override suspend fun countForOwner(ownerId: String): Int

    @Query("DELETE FROM hand_brew_record_details WHERE owner_id = :ownerId")
    override suspend fun deleteOwnerCache(ownerId: String): Int
}
