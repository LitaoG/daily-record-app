package io.github.litaog.dailyrecord.core.data

import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.HandBrewRecordDetail
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface HandBrewRecordRepository : DailyCountRecordRepository<HandBrewRecord> {
    /** Optional per-occurrence details stay in the hand-brew module boundary. */
    fun observeDetails(localDate: LocalDate): Flow<List<HandBrewRecordDetail>> = flowOf(emptyList())

    /** The overload is additive; legacy callers can continue saving the count only. */
    suspend fun saveRecord(
        record: HandBrewRecord,
        details: List<HandBrewRecordDetail>,
    ): HandBrewRecord = saveRecord(record)
}
