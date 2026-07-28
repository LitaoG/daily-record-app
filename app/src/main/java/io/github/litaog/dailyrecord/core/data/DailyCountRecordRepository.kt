package io.github.litaog.dailyrecord.core.data

import io.github.litaog.dailyrecord.core.model.DailyCountRecord
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Storage contract shared by isolated daily-count modules.
 *
 * Each module still owns its entity, DAO and cloud collection; this interface only removes
 * duplicated orchestration from the UI layer.
 */
interface DailyCountRecordRepository<T : DailyCountRecord> {
    fun observeRecord(localDate: LocalDate): Flow<T?>

    fun observeRecords(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<T>>

    suspend fun saveRecord(record: T): T

    suspend fun clearRecord(localDate: LocalDate): Boolean
}
