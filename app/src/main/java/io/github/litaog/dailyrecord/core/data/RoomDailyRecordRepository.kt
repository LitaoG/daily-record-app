package io.github.litaog.dailyrecord.core.data

import androidx.room.withTransaction
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.asEntity
import io.github.litaog.dailyrecord.core.database.asExternalModel
import io.github.litaog.dailyrecord.core.model.DailyRecord
import io.github.litaog.dailyrecord.core.model.RecordStatus
import io.github.litaog.dailyrecord.core.model.RecordValuePolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

internal class RoomDailyRecordRepository(
    private val database: DailyRecordDatabase,
) : DailyRecordRepository {
    private val activityDao = database.activityDao()
    private val dailyRecordDao = database.dailyRecordDao()

    override fun observeRecord(
        ownerId: String,
        activityId: String,
        localDate: LocalDate,
    ): Flow<DailyRecord?> = dailyRecordDao.observeByKey(ownerId, activityId, localDate).map {
        it?.asExternalModel()
    }

    override fun observeRecords(
        ownerId: String,
        startDate: LocalDate,
        endExclusive: LocalDate,
        activityId: String?,
    ): Flow<List<DailyRecord>> {
        require(startDate < endExclusive) { "Date range must be non-empty and use [start, endExclusive)." }
        return dailyRecordDao.observeForRange(ownerId, activityId, startDate, endExclusive).map { records ->
            records.map { it.asExternalModel() }
        }
    }

    override suspend fun saveRecord(record: DailyRecord): DailyRecord = database.withTransaction {
        val activity = checkNotNull(activityDao.getById(record.ownerId, record.activityId)) {
            "Cannot save a record for a missing or deleted activity."
        }
        check(!activity.isArchived) { "Cannot add new records to an archived activity." }

        val normalized = RecordValuePolicy.normalize(
            measurementType = activity.measurementType,
            requestedStatus = record.status,
            quantity = record.quantity,
        )
        val existing = dailyRecordDao.getAnyByKey(record.ownerId, record.activityId, record.localDate)
        val saved = record.copy(
            id = existing?.id ?: record.id,
            status = normalized.status,
            quantity = normalized.quantity,
            createdAt = existing?.createdAt ?: record.createdAt,
            deletedAt = null,
            revision = existing?.revision?.plus(1) ?: record.revision,
        )
        dailyRecordDao.upsert(saved.asEntity())
        saved
    }

    override suspend fun clearRecord(
        ownerId: String,
        activityId: String,
        localDate: LocalDate,
        changedAt: Instant,
    ): Boolean = dailyRecordDao.softDeleteByKey(
        ownerId = ownerId,
        activityId = activityId,
        localDate = localDate,
        deletedAt = changedAt,
        unsetStatus = RecordStatus.UNSET,
    ) == 1
}
