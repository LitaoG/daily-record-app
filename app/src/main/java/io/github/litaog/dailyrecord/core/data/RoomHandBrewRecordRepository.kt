package io.github.litaog.dailyrecord.core.data

import androidx.room.withTransaction
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.asEntity
import io.github.litaog.dailyrecord.core.database.asExternalModel
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.HandBrewSummary
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomHandBrewRecordRepository(
    private val database: DailyRecordDatabase,
) : HandBrewRecordRepository {
    private val recordDao = database.handBrewRecordDao()

    override fun observeRecord(localDate: LocalDate): Flow<HandBrewRecord?> =
        recordDao.observeByDate(localDate).map { it?.asExternalModel() }

    override fun observeRecords(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<HandBrewRecord>> {
        requireValidRange(startDate, endExclusive)
        return recordDao.observeForRange(startDate, endExclusive).map { records ->
            records.map { it.asExternalModel() }
        }
    }

    override fun observeSummary(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<HandBrewSummary> {
        requireValidRange(startDate, endExclusive)
        return recordDao.observeSummary(startDate, endExclusive)
    }

    override suspend fun saveRecord(record: HandBrewRecord): HandBrewRecord =
        database.withTransaction {
            val existing = recordDao.getByDate(record.localDate)
            val saved = record.copy(
                id = existing?.id ?: record.id,
                createdAt = existing?.createdAt ?: record.createdAt,
            )
            recordDao.upsert(saved.asEntity())
            saved
        }

    override suspend fun clearRecord(localDate: LocalDate): Boolean =
        recordDao.deleteByDate(localDate) == 1

    private fun requireValidRange(startDate: LocalDate, endExclusive: LocalDate) {
        require(startDate < endExclusive) {
            "Date range must be non-empty and use [start, endExclusive)."
        }
    }
}
