package io.github.litaog.dailyrecord.ui

import io.github.litaog.dailyrecord.core.data.HandBrewRecordRepository
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.HandBrewSummary
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeHandBrewRecordRepository(
    initialRecords: List<HandBrewRecord> = emptyList(),
    private val recordFlowOverride: Flow<HandBrewRecord?>? = null,
) : HandBrewRecordRepository {
    private val records = MutableStateFlow(initialRecords)

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
    ): Flow<List<HandBrewRecord>> = records.map { values ->
        values.filter { it.localDate >= startDate && it.localDate < endExclusive }
    }

    override fun observeSummary(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<HandBrewSummary> = observeRecords(startDate, endExclusive).map { values ->
        val totalCount = values.sumOf { it.brewCount.toLong() }
        val brewDays = values.count { it.brewCount > 0 }
        HandBrewSummary(totalCount = totalCount, brewDays = brewDays)
    }

    override suspend fun saveRecord(record: HandBrewRecord): HandBrewRecord {
        saveCalls += 1
        if (failSave) error("simulated save failure")
        saveGate?.await()
        records.value = records.value.filterNot { it.localDate == record.localDate } + record
        return record
    }

    override suspend fun clearRecord(localDate: LocalDate): Boolean {
        clearCalls += 1
        if (failClear) error("simulated clear failure")
        val before = records.value.size
        records.value = records.value.filterNot { it.localDate == localDate }
        return records.value.size != before
    }
}
