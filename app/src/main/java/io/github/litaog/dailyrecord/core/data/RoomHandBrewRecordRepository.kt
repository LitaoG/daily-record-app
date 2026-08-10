package io.github.litaog.dailyrecord.core.data

import androidx.room.withTransaction
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.core.database.asEntity
import io.github.litaog.dailyrecord.core.database.asExternalModel
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.HandBrewRecordDetail
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomHandBrewRecordRepository(
    private val database: DailyRecordDatabase,
    private val ownerId: String = LOCAL_OWNER_ID,
    private val clock: Clock = Clock.systemUTC(),
    private val onLocalChange: () -> Unit = {},
) : HandBrewRecordRepository {
    private val recordDao = database.handBrewRecordDao()
    private val detailDao = database.handBrewRecordDetailDao()

    override fun observeRecord(localDate: LocalDate): Flow<HandBrewRecord?> =
        recordDao.observeByDate(ownerId, localDate).map { it?.asExternalModel() }

    override fun observeDetails(localDate: LocalDate): Flow<List<HandBrewRecordDetail>> =
        detailDao.observeByDate(ownerId, localDate).map { details ->
            details.map { it.asExternalModel() }
        }

    override fun observeRecords(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<HandBrewRecord>> {
        requireValidRecordRange(startDate, endExclusive)
        return recordDao.observeForRange(ownerId, startDate, endExclusive).map { records ->
            records.map { it.asExternalModel() }
        }
    }

    override suspend fun saveRecord(record: HandBrewRecord): HandBrewRecord =
        saveRecordInternal(record, details = null)

    override suspend fun saveRecord(
        record: HandBrewRecord,
        details: List<HandBrewRecordDetail>,
    ): HandBrewRecord = saveRecordInternal(record, details)

    private suspend fun saveRecordInternal(
        record: HandBrewRecord,
        details: List<HandBrewRecordDetail>?,
    ): HandBrewRecord {
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
                pruneDetailsToCount(saved.localDate, saved.brewCount)
            } else {
                replaceDetails(saved, details)
            }
            saved
        }
        notifyLocalChangeSafely(onLocalChange)
        return saved
    }

    private suspend fun replaceDetails(
        saved: HandBrewRecord,
        details: List<HandBrewRecordDetail>,
    ) {
        require(details.size <= saved.brewCount) {
            "Hand-brew details cannot exceed the saved count."
        }
        require(details.all { it.localDate == saved.localDate }) {
            "Hand-brew detail date must match the aggregate record date."
        }
        require(details.map { it.occurrenceIndex }.distinct().size == details.size) {
            "Hand-brew detail occurrence indices must be unique."
        }
        require(details.all { it.occurrenceIndex in 1..saved.brewCount }) {
            "Hand-brew detail occurrence index must be within the saved count."
        }
        val existingByOccurrence = detailDao.getByDate(ownerId, saved.localDate)
            .associateBy { it.occurrenceIndex }
        val normalized = details.map { detail ->
            val existing = existingByOccurrence[detail.occurrenceIndex]
            // createdAt is the first-creation timestamp and must never move
            // forward on later edits: keep the stored value for existing
            // details and only take the passed-in value for new ones.
            val createdAt = existing?.createdAt ?: detail.createdAt
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
