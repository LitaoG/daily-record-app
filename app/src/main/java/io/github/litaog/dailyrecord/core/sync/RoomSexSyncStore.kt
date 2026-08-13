package io.github.litaog.dailyrecord.core.sync

import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.core.database.SYNC_PENDING
import io.github.litaog.dailyrecord.core.database.SYNCED
import io.github.litaog.dailyrecord.core.database.SexRecordDetailEntity
import io.github.litaog.dailyrecord.core.database.SexRecordEntity
import java.time.LocalDate

internal class RoomSexSyncStore(
    database: DailyRecordDatabase,
) : RoomDailyCountSyncStoreBase<
        SexRecordEntity,
        RemoteSexRecord,
        SexRecordDetailEntity,
        RemoteSexDetail,
    >(
        database = database,
        dao = database.sexRecordDao(),
        detailDao = database.sexRecordDetailDao(),
    ) {
    override fun RemoteSexRecord.localDateOf(): LocalDate = localDate

    override fun RemoteSexRecord.deletedOf(): Boolean = deleted

    override fun RemoteSexRecord.detailsOf(): List<RemoteSexDetail> = details

    override fun RemoteSexRecord.asRecordEntity(ownerId: String): SexRecordEntity =
        asEntity(ownerId)

    override fun RemoteSexDetail.asDetailEntity(
        ownerId: String,
        localDate: LocalDate,
    ): SexRecordDetailEntity = asEntity(ownerId, localDate)

    override fun SexRecordEntity.isSamePendingVersionOf(other: SexRecordEntity): Boolean =
        isSamePendingVersion(other)

    override fun SexRecordEntity.withSyncIdentity(
        id: String,
        ownerId: String,
        syncState: String,
        remoteRevision: Long,
    ): SexRecordEntity = copy(
        id = id,
        ownerId = ownerId,
        syncState = syncState,
        remoteRevision = remoteRevision,
    )

    override fun SexRecordDetailEntity.withOwner(ownerId: String): SexRecordDetailEntity =
        copy(ownerId = ownerId)

    override fun SexRecordDetailEntity.withRecoveryCopyIdentity(
        ownerId: String,
    ): SexRecordDetailEntity = copy(id = localCopyId(id), ownerId = ownerId)

    override fun SexRecordDetailEntity.withPromotedLocalIdentity(): SexRecordDetailEntity =
        copy(id = localCopySourceId(id), ownerId = LOCAL_OWNER_ID)

    override fun recordEntityIdOf(entity: SexRecordEntity): String = entity.id

    override fun recordLocalDateOf(entity: SexRecordEntity): LocalDate = entity.localDate

    override fun recordSyncStateOf(entity: SexRecordEntity): String = entity.syncState

    override fun recordRemoteRevisionOf(entity: SexRecordEntity): Long = entity.remoteRevision

    override fun recordIsDeletedOf(entity: SexRecordEntity): Boolean = entity.isDeleted

    override fun remoteRecordIdOf(remote: RemoteSexRecord): String = remote.id

    override fun remoteRecordRevisionOf(remote: RemoteSexRecord): Long = remote.revision
}

private fun RemoteSexRecord.asEntity(ownerId: String): SexRecordEntity =
    SexRecordEntity(
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
    localDate: LocalDate,
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

private fun SexRecordEntity.isSamePendingVersion(other: SexRecordEntity): Boolean =
    syncState == SYNC_PENDING &&
        id == other.id &&
        localDate == other.localDate &&
        sexCount == other.sexCount &&
        updatedAt == other.updatedAt &&
        isDeleted == other.isDeleted
