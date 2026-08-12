package io.github.litaog.dailyrecord.ui

import io.github.litaog.dailyrecord.core.data.SexRecordRepository
import io.github.litaog.dailyrecord.core.model.SexRecord
import io.github.litaog.dailyrecord.core.model.SexRecordDetail
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal class FakeSexRecordRepository(
    initialRecords: List<SexRecord> = emptyList(),
    initialDetails: List<SexRecordDetail> = emptyList(),
) : SexRecordRepository {
    private val records = MutableStateFlow(initialRecords)
    private val details = MutableStateFlow(initialDetails)

    override fun observeRecord(localDate: LocalDate): Flow<SexRecord?> =
        records.map { values -> values.firstOrNull { it.localDate == localDate } }

    override suspend fun getRecord(localDate: LocalDate): SexRecord? =
        records.value.firstOrNull { it.localDate == localDate }

    override fun observeRecords(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<SexRecord>> = records.map { values ->
        values.filter { it.localDate >= startDate && it.localDate < endExclusive }
        }

    override fun observeDetails(localDate: LocalDate): Flow<List<SexRecordDetail>> =
        details.map { values -> values.filter { it.localDate == localDate } }

    override suspend fun saveRecord(record: SexRecord): SexRecord {
        records.value = records.value.filterNot { it.localDate == record.localDate } + record
        return record
    }

    override suspend fun saveRecord(
        record: SexRecord,
        details: List<SexRecordDetail>,
    ): SexRecord {
        records.value = records.value.filterNot { it.localDate == record.localDate } + record
        this.details.value = this.details.value.filterNot { it.localDate == record.localDate } + details
        return record
    }

    override suspend fun clearRecord(localDate: LocalDate): Boolean {
        val before = records.value.size
        records.value = records.value.filterNot { it.localDate == localDate }
        details.value = details.value.filterNot { it.localDate == localDate }
        return records.value.size != before
    }
}
