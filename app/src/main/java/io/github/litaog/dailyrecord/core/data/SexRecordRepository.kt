package io.github.litaog.dailyrecord.core.data

import io.github.litaog.dailyrecord.core.model.SexRecord
import io.github.litaog.dailyrecord.core.model.SexRecordDetail
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface SexRecordRepository : DailyCountRecordRepository<SexRecord> {
    /** Optional per-occurrence details stay in the sex module boundary. */
    fun observeDetails(localDate: LocalDate): Flow<List<SexRecordDetail>> = flowOf(emptyList())

    /**
     * Saves the count together with its per-occurrence details. Count-only edits keep using
     * `saveRecord(record)`; this default ignores [details] and the Room repository overrides it.
     */
    suspend fun saveRecord(
        record: SexRecord,
        details: List<SexRecordDetail>,
    ): SexRecord = saveRecord(record)
}
