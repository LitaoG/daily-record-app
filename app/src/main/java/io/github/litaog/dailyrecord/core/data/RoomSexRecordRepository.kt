package io.github.litaog.dailyrecord.core.data

import androidx.room.withTransaction
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.core.database.asEntity
import io.github.litaog.dailyrecord.core.database.asExternalModel
import io.github.litaog.dailyrecord.core.model.SexRecord
import io.github.litaog.dailyrecord.core.model.SexRecordDetail
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomSexRecordRepository(
    private val database: DailyRecordDatabase,
    private val ownerId: String = LOCAL_OWNER_ID,
    private val clock: Clock = Clock.systemUTC(),
    private val onLocalChange: () -> Unit = {},
) : SexRecordRepository {
    private val recordDao = database.sexRecordDao()
    private val detailDao = database.sexRecordDetailDao()

    override fun observeRecord(localDate: LocalDate): Flow<SexRecord?> =
        recordDao.observeByDate(ownerId, localDate).map { it?.asExternalModel() }

    override fun observeDetails(localDate: LocalDate): Flow<List<SexRecordDetail>> =
        detailDao.observeByDate(ownerId, localDate).map { details ->
            details.map { it.asExternalModel() }
        }

    override fun observeRecords(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<SexRecord>> {
        requireValidRecordRange(startDate, endExclusive)
        return recordDao.observeForRange(ownerId, startDate, endExclusive).map { records ->
            records.map { it.asExternalModel() }
        }
    }

    override suspend fun saveRecord(record: SexRecord): SexRecord =
        saveRecordInternal(record, details = null)

    override suspend fun saveRecord(
        record: SexRecord,
        details: List<SexRecordDetail>,
    ): SexRecord = saveRecordInternal(record, details)

    private suspend fun saveRecordInternal(
        record: SexRecord,
        details: List<SexRecordDetail>?,
    ): SexRecord {
        val saved = database.withTransaction {
            val existing = recordDao.getByDate(ownerId, record.localDate)
            val createdAt = existing?.createdAt ?: record.createdAt
            val updatedAt = maxOf(
                record.updatedAt,
                createdAt,
                existing?.updatedAt?.nextRecordTimestamp() ?: record.updatedAt,
            )
            val saved = record.copy(
                id = existing?.id ?: record.id,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
            recordDao.upsert(
                saved.asEntity(
                    ownerId = ownerId,
                    remoteRevision = existing?.remoteRevision ?: 0,
                ),
            )
            if (details == null) {
                pruneDetailsToCount(saved.localDate, saved.sexCount)
            } else {
                replaceDetails(saved, details)
            }
            saved
        }
        notifyLocalChangeSafely(onLocalChange)
        return saved
    }

    private suspend fun replaceDetails(
        saved: SexRecord,
        details: List<SexRecordDetail>,
    ) {
        require(details.size <= saved.sexCount) {
            "Sex details cannot exceed the saved count."
        }
        require(details.all { it.localDate == saved.localDate }) {
            "Sex detail date must match the aggregate record date."
        }
        require(details.map { it.occurrenceIndex }.distinct().size == details.size) {
            "Sex detail occurrence indices must be unique."
        }
        require(details.all { it.occurrenceIndex in 1..saved.sexCount }) {
            "Sex detail occurrence index must be within the saved count."
        }
        val existingByOccurrence = detailDao.getByDate(ownerId, saved.localDate)
            .associateBy { it.occurrenceIndex }
        val normalized = details.map { detail ->
            val existing = existingByOccurrence[detail.occurrenceIndex]
            val createdAt = maxOf(detail.createdAt, existing?.createdAt ?: saved.createdAt)
            val updatedAt = maxOf(
                detail.updatedAt,
                createdAt,
                existing?.updatedAt?.nextRecordTimestamp() ?: detail.updatedAt,
            )
            detail.copy(
                id = existing?.id ?: detail.id,
                createdAt = createdAt,
                updatedAt = updatedAt,
            ).asEntity(ownerId = ownerId)
        }
        detailDao.deleteByOwnerDate(ownerId, saved.localDate)
        detailDao.upsertAll(normalized)
    }

    private suspend fun pruneDetailsToCount(localDate: LocalDate, count: Int) {
        val existing = detailDao.getByDate(ownerId, localDate)
        val kept = existing.filter { it.occurrenceIndex <= count }
        if (kept.size != existing.size) {
            detailDao.deleteByOwnerDate(ownerId, localDate)
            detailDao.upsertAll(kept)
        }
    }

    override suspend fun clearRecord(localDate: LocalDate): Boolean {
        val cleared = database.withTransaction {
            val existing = recordDao.getByDate(ownerId, localDate)
                ?.takeUnless { it.isDeleted }
                ?: return@withTransaction false
            val updatedAt = maxOf(
                Instant.now(clock),
                existing.createdAt,
                existing.updatedAt.nextRecordTimestamp(),
            )
            val marked = recordDao.markDeleted(
                ownerId = ownerId,
                id = existing.id,
                expectedUpdatedAt = existing.updatedAt,
                updatedAt = updatedAt,
            ) == 1
            if (marked) detailDao.deleteByOwnerDate(ownerId, localDate)
            marked
        }
        if (cleared) notifyLocalChangeSafely(onLocalChange)
        return cleared
    }
}
