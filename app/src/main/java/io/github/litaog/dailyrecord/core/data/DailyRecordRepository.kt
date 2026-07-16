package io.github.litaog.dailyrecord.core.data

import io.github.litaog.dailyrecord.core.model.DailyRecord
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

interface DailyRecordRepository {
    fun observeRecord(ownerId: String, activityId: String, localDate: LocalDate): Flow<DailyRecord?>

    fun observeRecords(
        ownerId: String,
        startDate: LocalDate,
        endExclusive: LocalDate,
        activityId: String? = null,
    ): Flow<List<DailyRecord>>

    suspend fun saveRecord(record: DailyRecord): DailyRecord

    suspend fun clearRecord(
        ownerId: String,
        activityId: String,
        localDate: LocalDate,
        changedAt: Instant,
    ): Boolean
}
