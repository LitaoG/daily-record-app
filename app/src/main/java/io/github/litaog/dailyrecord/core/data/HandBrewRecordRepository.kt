package io.github.litaog.dailyrecord.core.data

import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface HandBrewRecordRepository {
    fun observeRecord(localDate: LocalDate): Flow<HandBrewRecord?>

    fun observeRecords(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<HandBrewRecord>>

    suspend fun saveRecord(record: HandBrewRecord): HandBrewRecord

    suspend fun clearRecord(localDate: LocalDate): Boolean
}
