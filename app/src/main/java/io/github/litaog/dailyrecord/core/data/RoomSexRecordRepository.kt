package io.github.litaog.dailyrecord.core.data

import androidx.room.withTransaction
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.core.database.asEntity
import io.github.litaog.dailyrecord.core.database.asExternalModel
import io.github.litaog.dailyrecord.core.model.SexRecord
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

    override fun observeRecord(localDate: LocalDate): Flow<SexRecord?> =
        recordDao.observeByDate(ownerId, localDate).map { it?.asExternalModel() }

    override fun observeRecords(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<SexRecord>> {
        requireValidRecordRange(startDate, endExclusive)
        return recordDao.observeForRange(ownerId, startDate, endExclusive).map { records ->
            records.map { it.asExternalModel() }
        }
    }

    override suspend fun saveRecord(record: SexRecord): SexRecord {
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
            saved
        }
        notifyLocalChangeSafely(onLocalChange)
        return saved
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
            recordDao.markDeleted(ownerId, existing.id, existing.updatedAt, updatedAt) == 1
        }
        if (cleared) notifyLocalChangeSafely(onLocalChange)
        return cleared
    }
}
