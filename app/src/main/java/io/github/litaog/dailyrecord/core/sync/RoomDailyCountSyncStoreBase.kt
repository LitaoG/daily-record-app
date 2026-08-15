package io.github.litaog.dailyrecord.core.sync

import androidx.room.withTransaction
import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalStore
import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalRecoveryConflictException
import io.github.litaog.dailyrecord.core.database.DailyCountRecordDao
import io.github.litaog.dailyrecord.core.database.DailyCountRecordDetailDao
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.core.database.SYNC_PENDING
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Shared Room-backed sync store behavior for the isolated daily-count modules.
 *
 * Each module keeps its own DAOs, tables, entities and remote record types
 * (AGENTS: module boundaries stay independent); this base only removes the
 * duplicated orchestration. Subclasses supply the remote<->entity mappings
 * and the entity identity rewrites.
 */
internal abstract class RoomDailyCountSyncStoreBase<L : Any, R, LD : Any, RD>(
    private val database: DailyRecordDatabase,
    private val dao: DailyCountRecordDao<L>,
    private val detailDao: DailyCountRecordDetailDao<LD>,
) : AccountDeletionLocalStore,
    DailyCountSyncStore<L, R> {
    protected abstract fun R.localDateOf(): LocalDate

    protected abstract fun R.deletedOf(): Boolean

    protected abstract fun R.detailsOf(): List<RD>

    protected abstract fun R.asRecordEntity(ownerId: String): L

    protected abstract fun RD.asDetailEntity(ownerId: String, localDate: LocalDate): LD

    protected abstract fun L.isSamePendingVersionOf(other: L): Boolean

    protected abstract fun L.withSyncIdentity(
        id: String,
        ownerId: String,
        syncState: String,
        remoteRevision: Long,
    ): L

    protected abstract fun LD.withOwner(ownerId: String): LD

    protected abstract fun LD.withPromotedLocalIdentity(): LD

    protected abstract fun LD.withRecoveryCopyIdentity(ownerId: String): LD

    override fun observePendingCount(ownerId: String): Flow<Int> = dao.observePendingCount(ownerId)

    override suspend fun pending(ownerId: String): List<L> = dao.getPending(ownerId)

    override suspend fun pendingCount(ownerId: String): Int = dao.countPending(ownerId)

    override suspend fun adoptLocalRecords(ownerId: String): Int =
        database.withTransaction {
            val localRecords = dao.getAllForSync(LOCAL_OWNER_ID)
            var changed = 0

            localRecords.forEach { local ->
                val accountRecord = dao.getByDate(ownerId, recordLocalDateOf(local))
                // Device clocks must never decide cross-device outcomes (ADR-010).
                // Local edits always enter the account space as new pending edits;
                // the account's last confirmed remote revision is kept as the
                // optimistic-concurrency baseline so the shared revision protocol
                // (not the wall clock) decides the result.
                dao.deleteByOwnerDate(ownerId, recordLocalDateOf(local))
                detailDao.deleteByOwnerDate(ownerId, recordLocalDateOf(local))
                dao.upsert(
                    local.withSyncIdentity(
                        id = accountRecord?.let { recordEntityIdOf(it) } ?: recordEntityIdOf(local),
                        ownerId = ownerId,
                        syncState = SYNC_PENDING,
                        remoteRevision = accountRecord?.let { recordRemoteRevisionOf(it) } ?: 0,
                    ),
                )
                detailDao.upsertAll(
                    detailDao.getByDate(LOCAL_OWNER_ID, recordLocalDateOf(local)).map {
                        it.withOwner(ownerId)
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

    override suspend fun applyRemote(ownerId: String, records: List<R>): Int =
        database.withTransaction { applyRemoteRecords(ownerId, records) }

    override suspend fun alignUnbasedPendingRevisions(
        ownerId: String,
        records: List<R>,
    ): Int = database.withTransaction {
        records.sumOf { remote ->
            dao.setRemoteRevisionForUnbasedPending(
                ownerId = ownerId,
                localDate = remote.localDateOf(),
                remoteId = remoteRecordIdOf(remote),
                remoteRevision = remoteRecordRevisionOf(remote),
            )
        }
    }

    override suspend fun applyCommitIfUnchanged(
        ownerId: String,
        local: L,
        committed: R,
    ): Boolean = database.withTransaction {
        val current = dao.getByDate(ownerId, recordLocalDateOf(local))
        if (current == null || !current.isSamePendingVersionOf(local)) {
            return@withTransaction false
        }
        dao.deleteByOwnerDate(ownerId, recordLocalDateOf(local))
        dao.upsert(committed.asRecordEntity(ownerId))
        detailDao.deleteByOwnerDate(ownerId, recordLocalDateOf(local))
        detailDao.upsertAll(
            committed.detailsOf().map { it.asDetailEntity(ownerId, committed.localDateOf()) },
        )
        true
    }

    override suspend fun rebasePending(
        ownerId: String,
        local: L,
        committed: R,
    ): Int = database.withTransaction {
        val current = dao.getByDate(ownerId, recordLocalDateOf(local)) ?: return@withTransaction 0
        if (recordSyncStateOf(current) != SYNC_PENDING || recordEntityIdOf(current) != recordEntityIdOf(local)) {
            return@withTransaction 0
        }
        dao.setRemoteRevisionForPending(ownerId, recordLocalDateOf(local), remoteRecordRevisionOf(committed))
    }

    override suspend fun stageLocalRecoveryCopy(ownerId: String) {
        database.withTransaction {
            val recoveryOwner = recoveryOwnerId(ownerId)
            // Re-staging replaces only this account's private recovery area.
            // Existing user-authored __local__ data is never touched here.
            dao.deleteOwnerCache(recoveryOwner)
            detailDao.deleteOwnerCache(recoveryOwner)
            dao.getAllForSync(ownerId)
                .filterNot { recordIsDeletedOf(it) }
                .forEach { accountRecord ->
                    dao.upsert(
                        accountRecord.withSyncIdentity(
                            id = localCopyId(recordEntityIdOf(accountRecord)),
                            ownerId = recoveryOwner,
                            syncState = SYNC_PENDING,
                            remoteRevision = 0,
                        ),
                    )
                    detailDao.upsertAll(
                        detailDao.getByDate(ownerId, recordLocalDateOf(accountRecord)).map {
                            it.withRecoveryCopyIdentity(recoveryOwner)
                        },
                    )
                }
        }
    }

    override suspend fun discardLocalRecoveryCopy(ownerId: String) {
        val recoveryOwner = recoveryOwnerId(ownerId)
        dao.deleteOwnerCache(recoveryOwner)
        detailDao.deleteOwnerCache(recoveryOwner)
    }

    override suspend fun promoteLocalRecoveryCopy(ownerId: String) {
        val recoveryOwner = recoveryOwnerId(ownerId)
        database.withTransaction {
            val recoveryRecords = dao.getAllForSync(recoveryOwner)
            val recoveryDetails = detailDao.getAllForSync(recoveryOwner)
            if (recoveryRecords.isEmpty() && recoveryDetails.isEmpty()) return@withTransaction
            if (dao.countForOwner(LOCAL_OWNER_ID) > 0 ||
                detailDao.countForOwner(LOCAL_OWNER_ID) > 0
            ) {
                throw AccountDeletionLocalRecoveryConflictException(
                    ownerId,
                    IllegalStateException(
                        "Local records already exist; recovery promotion requires explicit resolution",
                    ),
                )
            }
            recoveryRecords.forEach { recoveryRecord ->
                dao.upsert(
                    recoveryRecord.withSyncIdentity(
                        id = localCopySourceId(recordEntityIdOf(recoveryRecord)),
                        ownerId = LOCAL_OWNER_ID,
                        syncState = SYNC_PENDING,
                        remoteRevision = 0,
                    ),
                )
            }
            detailDao.upsertAll(
                recoveryDetails.map { it.withPromotedLocalIdentity() },
            )
            dao.deleteOwnerCache(recoveryOwner)
            detailDao.deleteOwnerCache(recoveryOwner)
        }
    }

    override suspend fun hasLocalRecoveryConflict(ownerId: String): Boolean {
        val recoveryOwner = recoveryOwnerId(ownerId)
        val hasRecovery = dao.countForOwner(recoveryOwner) > 0 ||
            detailDao.countForOwner(recoveryOwner) > 0
        return hasRecovery && (
            dao.countForOwner(LOCAL_OWNER_ID) > 0 ||
                detailDao.countForOwner(LOCAL_OWNER_ID) > 0
            )
    }

    override suspend fun deleteOwnerCache(ownerId: String) {
        dao.deleteOwnerCache(ownerId)
        detailDao.deleteOwnerCache(ownerId)
    }

    override suspend fun markOwnerPendingForResync(ownerId: String) {
        dao.markOwnerPendingForResync(ownerId)
    }

    private suspend fun applyRemoteRecords(
        ownerId: String,
        records: List<R>,
    ): Int {
        // Fetch the local snapshot once per remote snapshot. The previous
        // implementation queried Room once per remote date, turning a normal
        // account sync into an N+1 read pattern.
        val localByDate = dao.getAllForSync(ownerId)
            .associateBy(::recordLocalDateOf)
            .toMutableMap()
        var changed = 0
        records.forEach { remote ->
            val localDate = remote.localDateOf()
            val local = localByDate[localDate]
            if (local != null && recordSyncStateOf(local) == SYNC_PENDING) {
                return@forEach
            }
            if (
                local != null &&
                recordEntityIdOf(local) == remoteRecordIdOf(remote) &&
                recordRemoteRevisionOf(local) >= remoteRecordRevisionOf(remote)
            ) {
                return@forEach
            }
            if (local != null && recordEntityIdOf(local) != remoteRecordIdOf(remote)) {
                dao.deleteByOwnerDate(ownerId, localDate)
            }
            val entity = remote.asRecordEntity(ownerId)
            dao.upsert(entity)
            localByDate[localDate] = entity
            detailDao.deleteByOwnerDate(ownerId, localDate)
            if (!remote.deletedOf()) {
                detailDao.upsertAll(
                    remote.detailsOf().map { it.asDetailEntity(ownerId, localDate) },
                )
            }
            changed += 1
        }
        return changed
    }


    protected abstract fun recordEntityIdOf(entity: L): String
    protected abstract fun recordLocalDateOf(entity: L): LocalDate
    protected abstract fun recordSyncStateOf(entity: L): String
    protected abstract fun recordRemoteRevisionOf(entity: L): Long
    protected abstract fun recordIsDeletedOf(entity: L): Boolean
    protected abstract fun remoteRecordIdOf(remote: R): String
    protected abstract fun remoteRecordRevisionOf(remote: R): Long
}
