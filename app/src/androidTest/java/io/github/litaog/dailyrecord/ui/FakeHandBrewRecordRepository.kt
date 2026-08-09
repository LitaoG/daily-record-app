package io.github.litaog.dailyrecord.ui

import io.github.litaog.dailyrecord.core.data.HandBrewRecordRepository
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.HandBrewRecordDetail
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeHandBrewRecordRepository(
    initialRecords: List<HandBrewRecord> = emptyList(),
    private val recordFlowOverride: Flow<HandBrewRecord?>? = null,
    private val recordsFlowOverride: Flow<List<HandBrewRecord>>? = null,
    initialDetails: List<HandBrewRecordDetail> = emptyList(),
) : HandBrewRecordRepository {
    private val records = MutableStateFlow(initialRecords)
    private val details = MutableStateFlow(initialDetails)

    var saveCalls: Int = 0
        private set
    var clearCalls: Int = 0
        private set
    var failSave: Boolean = false
    var failClear: Boolean = false
    var saveGate: CompletableDeferred<Unit>? = null

    override fun observeRecord(localDate: LocalDate): Flow<HandBrewRecord?> =
        recordFlowOverride ?: records.map { values -> values.firstOrNull { it.localDate == localDate } }

    override fun observeRecords(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<HandBrewRecord>> = recordsFlowOverride ?: records.map { values ->
            values.filter { it.localDate >= startDate && it.localDate < endExclusive }
        }

    override fun observeDetails(localDate: LocalDate): Flow<List<HandBrewRecordDetail>> =
        details.map { values -> values.filter { it.localDate == localDate } }

    override suspend fun saveRecord(record: HandBrewRecord): HandBrewRecord {
        return saveRecordInternal(record, null)
    }

    override suspend fun saveRecord(
        record: HandBrewRecord,
        details: List<HandBrewRecordDetail>,
    ): HandBrewRecord = saveRecordInternal(record, details)

    private suspend fun saveRecordInternal(
        record: HandBrewRecord,
        newDetails: List<HandBrewRecordDetail>?,
    ): HandBrewRecord {
        saveCalls += 1
        if (failSave) error("simulated save failure")
        saveGate?.await()
        records.value = records.value.filterNot { it.localDate == record.localDate } + record
        if (newDetails != null) {
            details.value = details.value.filterNot { it.localDate == record.localDate } + newDetails
        }
        return record
    }

    override suspend fun clearRecord(localDate: LocalDate): Boolean {
        clearCalls += 1
        if (failClear) error("simulated clear failure")
        val before = records.value.size
        records.value = records.value.filterNot { it.localDate == localDate }
        details.value = details.value.filterNot { it.localDate == localDate }
        return records.value.size != before
    }
}
