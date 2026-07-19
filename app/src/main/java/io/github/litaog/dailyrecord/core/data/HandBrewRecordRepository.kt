package io.github.litaog.dailyrecord.core.data

import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.HandBrewSummary
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface HandBrewRecordRepository {
    fun observeRecord(localDate: LocalDate): Flow<HandBrewRecord?>

    fun observeRecords(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<HandBrewRecord>>

    fun observeSummary(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<HandBrewSummary>

    suspend fun saveRecord(record: HandBrewRecord): HandBrewRecord

    suspend fun clearRecord(localDate: LocalDate): Boolean
}
