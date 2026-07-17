package io.github.litaog.dailyrecord.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.github.litaog.dailyrecord.core.model.HandBrewSummary
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
internal interface HandBrewRecordDao {
    @Query("SELECT * FROM hand_brew_records WHERE local_date = :localDate LIMIT 1")
    fun observeByDate(localDate: LocalDate): Flow<HandBrewRecordEntity?>

    @Query("SELECT * FROM hand_brew_records WHERE local_date = :localDate LIMIT 1")
    suspend fun getByDate(localDate: LocalDate): HandBrewRecordEntity?

    @Query(
        """
        SELECT * FROM hand_brew_records
        WHERE local_date >= :startDate AND local_date < :endExclusive
        ORDER BY local_date ASC
        """,
    )
    fun observeForRange(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<HandBrewRecordEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(brew_count), 0) AS totalCount,
               COALESCE(SUM(CASE WHEN brew_count > 0 THEN 1 ELSE 0 END), 0) AS brewDays
        FROM hand_brew_records
        WHERE local_date >= :startDate AND local_date < :endExclusive
        """,
    )
    fun observeSummary(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<HandBrewSummary>

    @Upsert
    suspend fun upsert(record: HandBrewRecordEntity)

    @Query("DELETE FROM hand_brew_records WHERE local_date = :localDate")
    suspend fun deleteByDate(localDate: LocalDate): Int
}
