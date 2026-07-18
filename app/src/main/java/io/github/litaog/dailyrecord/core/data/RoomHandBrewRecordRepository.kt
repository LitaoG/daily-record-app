package io.github.litaog.dailyrecord.core.data

import androidx.room.withTransaction
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.asEntity
import io.github.litaog.dailyrecord.core.database.asExternalModel
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.HandBrewSummary
import java.time.LocalDate
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomHandBrewRecordRepository(
    private val database: DailyRecordDatabase,
    private val ownerId: String = io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID,
    private val clock: Clock = Clock.systemUTC(),
    private val onLocalChange: () -> Unit = {},
) : HandBrewRecordRepository {
    private val recordDao = database.handBrewRecordDao()

    override fun observeRecord(localDate: LocalDate): Flow<HandBrewRecord?> =
        recordDao.observeByDate(ownerId, localDate).map { it?.asExternalModel() }

    override fun observeRecords(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<HandBrewRecord>> {
        requireValidRange(startDate, endExclusive)
        return recordDao.observeForRange(ownerId, startDate, endExclusive).map { records ->
            records.map { it.asExternalModel() }
        }
    }

    override fun observeSummary(
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<HandBrewSummary> {
        requireValidRange(startDate, endExclusive)
        return recordDao.observeSummary(ownerId, startDate, endExclusive)
    }

    override suspend fun saveRecord(record: HandBrewRecord): HandBrewRecord {
        val saved = database.withTransaction {
            val existing = recordDao.getByDate(ownerId, record.localDate)
            val createdAt = existing?.createdAt ?: record.createdAt
            val updatedAt = maxOf(
                record.updatedAt,
                createdAt,
                existing?.updatedAt?.nextInstant() ?: record.updatedAt,
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
        onLocalChange()
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
                existing.updatedAt.nextInstant(),
            )
            recordDao.markDeleted(ownerId, existing.id, updatedAt) == 1
        }
        if (cleared) onLocalChange()
        return cleared
    }

    private fun requireValidRange(startDate: LocalDate, endExclusive: LocalDate) {
        require(startDate < endExclusive) {
            "Date range must be non-empty and use [start, endExclusive)."
        }
    }
}

private fun Instant.nextInstant(): Instant = runCatching { plusMillis(1) }.getOrDefault(this)
