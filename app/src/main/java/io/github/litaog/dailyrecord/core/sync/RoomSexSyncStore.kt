package io.github.litaog.dailyrecord.core.sync

import androidx.room.withTransaction
import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalStore
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.core.database.SYNCED
import io.github.litaog.dailyrecord.core.database.SYNC_PENDING
import io.github.litaog.dailyrecord.core.database.SexRecordEntity
import io.github.litaog.dailyrecord.core.database.SexRecordDetailEntity
import kotlinx.coroutines.flow.Flow

internal class RoomSexSyncStore(
    private val database: DailyRecordDatabase,
) : AccountDeletionLocalStore,
    DailyCountSyncStore<SexRecordEntity, RemoteSexRecord> {
    private val dao = database.sexRecordDao()
    private val detailDao = database.sexRecordDetailDao()

    override fun observePendingCount(ownerId: String): Flow<Int> = dao.observePendingCount(ownerId)

    override suspend fun pending(ownerId: String): List<SexRecordEntity> = dao.getPending(ownerId)

    override suspend fun pendingCount(ownerId: String): Int = dao.countPending(ownerId)

    override suspend fun adoptLocalRecords(ownerId: String): Int = database.withTransaction {
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

    override suspend fun applyRemote(ownerId: String, records: List<RemoteSexRecord>): Int =
        database.withTransaction {
            var changed = 0
            records.forEach { remote ->
                val local = dao.getByDate(ownerId, remote.localDate)
                if (
                    local?.syncState == SYNC_PENDING ||
                    (local != null && local.remoteRevision >= remote.revision)
                ) {
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
            changed
        }

    override suspend fun alignUnbasedPendingRevisions(
        ownerId: String,
        records: List<RemoteSexRecord>,
    ): Int = database.withTransaction {
        records.sumOf { remote ->
            dao.setRemoteRevisionForUnbasedPending(
                ownerId = ownerId,
                localDate = remote.localDate,
                remoteRevision = remote.revision,
            )
        }
    }

    override suspend fun applyCommitIfUnchanged(
        ownerId: String,
        local: SexRecordEntity,
        committed: RemoteSexRecord,
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

    override suspend fun rebasePending(
        ownerId: String,
        local: SexRecordEntity,
        committed: RemoteSexRecord,
    ): Int = database.withTransaction {
        val current = dao.getByDate(ownerId, local.localDate) ?: return@withTransaction 0
        if (current.syncState != SYNC_PENDING || current.id != local.id) {
            return@withTransaction 0
        }
        dao.setRemoteRevisionForPending(ownerId, local.localDate, committed.revision)
    }

    override suspend fun stageLocalRecoveryCopy(ownerId: String) {
        database.withTransaction {
            // An interrupted previous deletion may have left a staged recovery
            // copy in the local space. The account's own rows are still
            // authoritative until deletion completes, so a stale copy is
            // discarded before re-staging: retries of "keep local data" must
            // not fail on the leftover.
            if (dao.countForOwner(LOCAL_OWNER_ID) > 0) {
                dao.deleteOwnerCache(LOCAL_OWNER_ID)
                detailDao.deleteOwnerCache(LOCAL_OWNER_ID)
            }
            dao.getAllForSync(ownerId)
                .filterNot { it.isDeleted }
                .forEach { accountRecord ->
                    // The copy must not share the account row's id: @Upsert
                    // binds on the primary key, so a colliding id would
                    // silently replace the account record and lose the source
                    // for a later retry (and for deleteOwnerCache).
                    dao.upsert(
                        accountRecord.copy(
                            id = localCopyId(accountRecord.id),
                            ownerId = LOCAL_OWNER_ID,
                            syncState = SYNC_PENDING,
                            remoteRevision = 0,
                        ),
                    )
                    detailDao.upsertAll(
                        detailDao.getByDate(ownerId, accountRecord.localDate).map {
                            it.copy(id = localCopyId(it.id), ownerId = LOCAL_OWNER_ID)
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

    override suspend fun markOwnerPendingForResync(ownerId: String) {
        dao.markOwnerPendingForResync(ownerId)
    }
}

private fun RemoteSexRecord.asEntity(ownerId: String): SexRecordEntity = SexRecordEntity(
    id = id,
    localDate = localDate,
    ownerId = ownerId,
    sexCount = sexCount,
    createdAt = createdAt,
    updatedAt = clientUpdatedAt,
    isDeleted = deleted,
    syncState = SYNCED,
    remoteRevision = revision,
)

private fun RemoteSexDetail.asEntity(
    ownerId: String,
    localDate: java.time.LocalDate,
): SexRecordDetailEntity = SexRecordDetailEntity(
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

private fun localCopyId(id: String): String = "__local__-copy-$id"

private fun SexRecordEntity.isSamePendingVersion(other: SexRecordEntity): Boolean =
    syncState == SYNC_PENDING &&
        id == other.id &&
        localDate == other.localDate &&
        sexCount == other.sexCount &&
        updatedAt == other.updatedAt &&
        isDeleted == other.isDeleted
