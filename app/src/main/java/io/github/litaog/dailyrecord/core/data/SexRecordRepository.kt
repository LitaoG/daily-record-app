package io.github.litaog.dailyrecord.core.data

import io.github.litaog.dailyrecord.core.model.SexRecord
import io.github.litaog.dailyrecord.core.model.SexRecordDetail
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface SexRecordRepository : DailyCountRecordRepository<SexRecord> {
    /** Optional per-occurrence details stay in the sex module boundary. */
    fun observeDetails(localDate: LocalDate): Flow<List<SexRecordDetail>> = flowOf(emptyList())

    /** The overload is additive; legacy callers can continue saving the count only. */
    suspend fun saveRecord(
        record: SexRecord,
        details: List<SexRecordDetail>,
    ): SexRecord = saveRecord(record)
}
