package io.github.litaog.dailyrecord.core.data

import androidx.room.withTransaction
import io.github.litaog.dailyrecord.core.database.DailyCountRecordDao
import io.github.litaog.dailyrecord.core.database.DailyCountRecordDetailDao
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.core.model.DailyCountRecord
import io.github.litaog.dailyrecord.core.model.nextRecordTimestamp
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Shared Room-backed repository behavior for the isolated daily-count modules.
 *
 * Each module keeps its own DAOs, tables and entity types (AGENTS: module
 * tables stay independent); this base only removes the duplicated
 * orchestration. Subclasses supply the entity<->model mappings and the count
 * field accessors.
 */
internal abstract class RoomDailyCountRecordRepository<T : DailyCountRecord, TD, E : Any, D : Any>(
    private val database: DailyRecordDatabase,
    private val recordDao: DailyCountRecordDao<E>,
    private val detailDao: DailyCountRecordDetailDao<D>,
    private val ownerId: String = LOCAL_OWNER_ID,
    private val clock: Clock = Clock.systemUTC(),
    private val onLocalChange: () -> Unit = {},
) {
    protected abstract val recordLabel: String

    protected abstract fun E.toModel(): T

    protected abstract fun T.toRecordEntity(ownerId: String, remoteRevision: Long): E

    protected abstract fun D.toDetailModel(): TD

    protected abstract fun TD.toDetailEntity(
        ownerId: String,
        createdAt: Instant,
        updatedAt: Instant,
    ): D

    protected abstract fun TD.idOf(): String

    protected abstract fun TD.localDateOf(): LocalDate

    protected abstract fun TD.occurrenceIndexOf(): Int

    protected abstract fun TD.createdAtOf(): Instant

    protected abstract fun TD.updatedAtOf(): Instant

    protected abstract fun TD.withIdentity(id: String, createdAt: Instant, updatedAt: Instant): TD

    protected abstract fun T.withRecordIdentity(id: String, createdAt: Instant, updatedAt: Instant): T

    protected abstract fun E.isDeletedOf(): Boolean

    protected abstract fun E.remoteRevisionOf(): Long

    fun observeRecord(localDate: LocalDate): Flow<T?> =
        recordDao.observeByDate(ownerId, localDate).map { it?.toModel() }

    suspend fun getRecord(localDate: LocalDate): T? =
        recordDao.getByDate(ownerId, localDate)?.toModel()

    open fun observeDetails(localDate: LocalDate): Flow<List<TD>> =
        detailDao.observeByDate(ownerId, localDate).map { details ->
            details.map { it.toDetailModel() }
        }

    fun observeRecords(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<T>> {
        requireValidRecordRange(startDate, endExclusive)
        return recordDao.observeForRange(ownerId, startDate, endExclusive).map { records ->
            records.map { it.toModel() }
        }
    }

    suspend fun saveRecord(record: T): T = saveRecordInternal(record, details = null)

    open suspend fun saveRecord(record: T, details: List<TD>): T = saveRecordInternal(record, details)

    private suspend fun saveRecordInternal(
        record: T,
        details: List<TD>?,
    ): T {
        val saved = database.withTransaction {
            val existing = recordDao.getByDate(ownerId, record.localDate)
            val existingModel = existing?.toModel()
            val createdAt = existingModel?.createdAt ?: record.createdAt
            val updatedAt = maxOf(
                record.updatedAt,
                createdAt,
                existingModel?.updatedAt?.nextRecordTimestamp() ?: record.updatedAt,
            )
            val saved = record.withRecordIdentity(
                id = existingModel?.id ?: record.id,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
            recordDao.upsert(
                saved.toRecordEntity(
                    ownerId = ownerId,
                    remoteRevision = existing?.remoteRevisionOf() ?: 0,
                ),
            )
            if (details == null) {
                pruneDetailsToCount(saved.localDate, saved.count)
            } else {
                replaceDetails(saved, details)
            }
            saved
        }
        notifyLocalChangeSafely(onLocalChange)
        return saved
    }

    private suspend fun replaceDetails(
        saved: T,
        details: List<TD>,
    ) {
        require(details.size <= saved.count) {
            "$recordLabel details cannot exceed the saved count."
        }
        require(details.all { it.localDateOf() == saved.localDate }) {
            "$recordLabel detail date must match the aggregate record date."
        }
        require(details.map { it.occurrenceIndexOf() }.distinct().size == details.size) {
            "$recordLabel detail occurrence indices must be unique."
        }
        require(details.all { it.occurrenceIndexOf() in 1..saved.count }) {
            "$recordLabel detail occurrence index must be within the saved count."
        }
        val existingByOccurrence = detailDao.getByDate(ownerId, saved.localDate)
            .associateBy { it.toDetailModel().occurrenceIndexOf() }
        val normalized = details.map { detail ->
            val existing = existingByOccurrence[detail.occurrenceIndexOf()]
            // createdAt is the first-creation timestamp and must never move
            // forward on later edits: keep the stored value for existing
            // details and only take the passed-in value for new ones.
            val createdAt = existing?.toDetailModel()?.createdAtOf() ?: detail.createdAtOf()
            val updatedAt = maxOf(
                detail.updatedAtOf(),
                createdAt,
                existing?.toDetailModel()?.updatedAtOf()?.nextRecordTimestamp() ?: detail.updatedAtOf(),
            )
            detail.withIdentity(
                id = existing?.toDetailModel()?.idOf() ?: detail.idOf(),
                createdAt = createdAt,
                updatedAt = updatedAt,
            ).toDetailEntity(ownerId = ownerId, createdAt = createdAt, updatedAt = updatedAt)
        }
        detailDao.deleteByOwnerDate(ownerId, saved.localDate)
        detailDao.upsertAll(normalized)
    }

    private suspend fun pruneDetailsToCount(localDate: LocalDate, count: Int) {
        val existing = detailDao.getByDate(ownerId, localDate)
        val kept = existing.filter { it.toDetailModel().occurrenceIndexOf() <= count }
        if (kept.size != existing.size) {
            detailDao.deleteByOwnerDate(ownerId, localDate)
            detailDao.upsertAll(kept)
        }
    }

    suspend fun clearRecord(localDate: LocalDate): Boolean {
        val cleared = database.withTransaction {
            val existing = recordDao.getByDate(ownerId, localDate)
                ?.takeUnless { it.isDeletedOf() }
                ?: return@withTransaction false
            val model = existing.toModel()
            val updatedAt = maxOf(
                Instant.now(clock),
                model.createdAt,
                model.updatedAt.nextRecordTimestamp(),
            )
            val marked = recordDao.markDeleted(
                ownerId = ownerId,
                id = model.id,
                expectedUpdatedAt = model.updatedAt,
                updatedAt = updatedAt,
            ) == 1
            if (marked) detailDao.deleteByOwnerDate(ownerId, localDate)
            marked
        }
        if (cleared) notifyLocalChangeSafely(onLocalChange)
        return cleared
    }
}
