package io.github.litaog.dailyrecord.core.data

import io.github.litaog.dailyrecord.core.model.SexRecord
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface SexRecordRepository {
    fun observeRecord(localDate: LocalDate): Flow<SexRecord?>

    fun observeRecords(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<SexRecord>>

    suspend fun saveRecord(record: SexRecord): SexRecord

    suspend fun clearRecord(localDate: LocalDate): Boolean
}
