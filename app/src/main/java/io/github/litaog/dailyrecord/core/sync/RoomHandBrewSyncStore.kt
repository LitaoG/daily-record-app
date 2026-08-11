package io.github.litaog.dailyrecord.core.sync

import androidx.room.withTransaction
import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalStore
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity
import io.github.litaog.dailyrecord.core.database.HandBrewRecordDetailEntity
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.core.database.SYNCED
import io.github.litaog.dailyrecord.core.database.SYNC_PENDING
import kotlinx.coroutines.flow.Flow

internal class RoomHandBrewSyncStore(
    private val database: DailyRecordDatabase,
) : AccountDeletionLocalStore,
    DailyCountSyncStore<HandBrewRecordEntity, RemoteHandBrewRecord> {
    private val dao = database.handBrewRecordDao()
    private val detailDao = database.handBrewRecordDetailDao()

    override fun observePendingCount(ownerId: String): Flow<Int> = dao.observePendingCount(ownerId)

    override suspend fun pending(ownerId: String): List<HandBrewRecordEntity> = dao.getPending(ownerId)

    override suspend fun pendingCount(ownerId: String): Int = dao.getPending(ownerId).size

    override suspend fun adoptLocalRecords(ownerId: String): Int =
        database.withTransaction {
            val localRecords = dao.getAllForSync(LOCAL_OWNER_ID)
            var changed = 0

            localRecords.forEach { local ->
                val accountRecord = dao.getByDate(ownerId, local.localDate)
                // Device clocks must never decide cross-device outcomes (ADR-010).
                // Local edits always enter the account space as new pending edits;
                // the account's last confirmed remote revision is kept as the
                // optimistic-concurrency baseline so the shared revision protocol
                // (not the wall clock) decides the result.
                dao.deleteByOwnerDate(ownerId, local.localDate)
                detailDao.deleteByOwnerDate(ownerId, local.localDate)
                dao.upsert(
                    local.copy(
                        id = accountRecord?.id ?: local.id,
                        ownerId = ownerId,
                        syncState = SYNC_PENDING,
                        remoteRevision = accountRecord?.remoteRevision ?: 0,
                    ),
                )
                detailDao.upsertAll(
                    detailDao.getByDate(LOCAL_OWNER_ID, local.localDate).map {
                        it.copy(ownerId = ownerId)
                    },
                )
                changed += 1
            }
            if (localRecords.isNotEmpty()) {
                dao.deleteOwnerCache(LOCAL_OWNER_ID)
                detailDao.deleteOwnerCache(LOCAL_OWNER_ID)
            }
            changed
        }

    override suspend fun applyRemote(ownerId: String, records: List<RemoteHandBrewRecord>): Int =
        database.withTransaction { applyRemoteRecords(ownerId, records) }

    override suspend fun alignUnbasedPendingRevisions(
        ownerId: String,
        records: List<RemoteHandBrewRecord>,
    ): Int = database.withTransaction {
        records.sumOf { remote ->
            dao.setRemoteRevisionForUnbasedPending(
                ownerId = ownerId,
                localDate = remote.localDate,
                remoteId = remote.id,
                remoteRevision = remote.revision,
            )
        }
    }

    override suspend fun applyCommitIfUnchanged(
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
        detailDao.deleteByOwnerDate(ownerId, local.localDate)
        detailDao.upsertAll(committed.details.map { it.asEntity(ownerId, committed.localDate) })
        true
    }

    override suspend fun stageLocalRecoveryCopy(ownerId: String) {
        database.withTransaction {
            require(dao.countForOwner(LOCAL_OWNER_ID) == 0) {
                "Local recovery space must be empty while an account is signed in"
            }
            dao.getAllForSync(ownerId)
                .filterNot { it.isDeleted }
                .forEach { accountRecord ->
                    dao.upsert(
                        accountRecord.copy(
                            ownerId = LOCAL_OWNER_ID,
                            syncState = SYNC_PENDING,
                            remoteRevision = 0,
                        ),
                    )
                    detailDao.upsertAll(
                        detailDao.getByDate(ownerId, accountRecord.localDate).map {
                            it.copy(ownerId = LOCAL_OWNER_ID)
                        },
                    )
                }
        }
    }

    override suspend fun discardLocalRecoveryCopy() {
        dao.deleteOwnerCache(LOCAL_OWNER_ID)
        detailDao.deleteOwnerCache(LOCAL_OWNER_ID)
    }

    override suspend fun deleteOwnerCache(ownerId: String) {
        dao.deleteOwnerCache(ownerId)
        detailDao.deleteOwnerCache(ownerId)
    }

    private suspend fun applyRemoteRecords(
        ownerId: String,
        records: List<RemoteHandBrewRecord>,
    ): Int {
        var changed = 0
        records.forEach { remote ->
            val local = dao.getByDate(ownerId, remote.localDate)
            if (local?.syncState == SYNC_PENDING) {
                return@forEach
            }
            if (local != null && local.id == remote.id && local.remoteRevision >= remote.revision) {
                return@forEach
            }
            if (local != null && local.id != remote.id) {
                dao.deleteByOwnerDate(ownerId, remote.localDate)
            }
            dao.upsert(remote.asEntity(ownerId))
            detailDao.deleteByOwnerDate(ownerId, remote.localDate)
            if (!remote.deleted) {
                detailDao.upsertAll(remote.details.map { it.asEntity(ownerId, remote.localDate) })
            }
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

private fun RemoteHandBrewDetail.asEntity(
    ownerId: String,
    localDate: java.time.LocalDate,
): HandBrewRecordDetailEntity = HandBrewRecordDetailEntity(
    id = id,
    localDate = localDate,
    ownerId = ownerId,
    occurrenceIndex = occurrenceIndex,
    startTime = startTime,
    endTime = endTime,
    feeling = feeling,
    createdAt = java.time.Instant.EPOCH,
    updatedAt = java.time.Instant.EPOCH,
)

private fun HandBrewRecordEntity.isSamePendingVersion(other: HandBrewRecordEntity): Boolean =
    syncState == SYNC_PENDING &&
        id == other.id &&
        localDate == other.localDate &&
        brewCount == other.brewCount &&
        updatedAt == other.updatedAt &&
        isDeleted == other.isDeleted
