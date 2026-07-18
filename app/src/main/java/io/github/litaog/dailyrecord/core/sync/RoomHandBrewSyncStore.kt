package io.github.litaog.dailyrecord.core.sync

import androidx.room.withTransaction
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.core.database.SYNCED
import io.github.litaog.dailyrecord.core.database.SYNC_PENDING
import kotlinx.coroutines.flow.Flow

internal class RoomHandBrewSyncStore(
    private val database: DailyRecordDatabase,
) {
    private val dao = database.handBrewRecordDao()

    fun observePendingCount(ownerId: String): Flow<Int> = dao.observePendingCount(ownerId)

    suspend fun pending(ownerId: String): List<HandBrewRecordEntity> = dao.getPending(ownerId)

    suspend fun pendingCount(ownerId: String): Int = dao.getPending(ownerId).size

    suspend fun prepareAccount(ownerId: String, remoteRecords: List<RemoteHandBrewRecord>): Int =
        database.withTransaction {
            val localRecords = dao.getAllForSync(LOCAL_OWNER_ID)
            var changed = applyRemoteRecords(ownerId, remoteRecords)

            localRecords.forEach { local ->
                val accountRecord = dao.getByDate(ownerId, local.localDate)
                if (accountRecord == null || local.updatedAt.isAfter(accountRecord.updatedAt)) {
                    dao.deleteByOwnerDate(ownerId, local.localDate)
                    dao.upsert(
                        local.copy(
                            id = accountRecord?.id ?: local.id,
                            ownerId = ownerId,
                            syncState = SYNC_PENDING,
                            remoteRevision = accountRecord?.remoteRevision ?: 0,
                        ),
                    )
                    changed += 1
                }
            }
            if (localRecords.isNotEmpty()) dao.deleteOwnerCache(LOCAL_OWNER_ID)
            changed
        }

    suspend fun applyRemote(ownerId: String, records: List<RemoteHandBrewRecord>): Int =
        database.withTransaction { applyRemoteRecords(ownerId, records) }

    suspend fun applyCommitIfUnchanged(
        ownerId: String,
        local: HandBrewRecordEntity,
        committed: RemoteHandBrewRecord,
    ): Boolean = database.withTransaction {
        val current = dao.getByDate(ownerId, local.localDate)
        if (current == null || !current.isSamePendingVersion(local)) {
            return@withTransaction false
        }
        dao.deleteByOwnerDate(ownerId, local.localDate)
        dao.upsert(committed.asEntity(ownerId))
        true
    }

    suspend fun deleteOwnerCache(ownerId: String): Int = dao.deleteOwnerCache(ownerId)

    private suspend fun applyRemoteRecords(
        ownerId: String,
        records: List<RemoteHandBrewRecord>,
    ): Int {
        var changed = 0
        records.forEach { remote ->
            val local = dao.getByDate(ownerId, remote.localDate)
            if (local?.syncState == SYNC_PENDING || (local != null && local.remoteRevision >= remote.revision)) {
                return@forEach
            }
            if (local != null && local.id != remote.id) {
                dao.deleteByOwnerDate(ownerId, remote.localDate)
            }
            dao.upsert(remote.asEntity(ownerId))
            changed += 1
        }
        return changed
    }
}

private fun RemoteHandBrewRecord.asEntity(ownerId: String): HandBrewRecordEntity =
    HandBrewRecordEntity(
        id = id,
        localDate = localDate,
        ownerId = ownerId,
        brewCount = brewCount,
        createdAt = createdAt,
        updatedAt = clientUpdatedAt,
        isDeleted = deleted,
        syncState = SYNCED,
        remoteRevision = revision,
    )

private fun HandBrewRecordEntity.isSamePendingVersion(other: HandBrewRecordEntity): Boolean =
    syncState == SYNC_PENDING &&
        id == other.id &&
        localDate == other.localDate &&
        brewCount == other.brewCount &&
        updatedAt == other.updatedAt &&
        isDeleted == other.isDeleted
