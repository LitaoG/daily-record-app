package io.github.litaog.dailyrecord.core.sync

import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.HandBrewRecordDetailEntity
import io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.core.database.SYNC_PENDING
import io.github.litaog.dailyrecord.core.database.SYNCED
import java.time.LocalDate

internal class RoomHandBrewSyncStore(
    database: DailyRecordDatabase,
) : RoomDailyCountSyncStoreBase<
        HandBrewRecordEntity,
        RemoteHandBrewRecord,
        HandBrewRecordDetailEntity,
        RemoteHandBrewDetail,
    >(
        database = database,
        dao = database.handBrewRecordDao(),
        detailDao = database.handBrewRecordDetailDao(),
    ) {
    override fun RemoteHandBrewRecord.localDateOf(): LocalDate = localDate

    override fun RemoteHandBrewRecord.deletedOf(): Boolean = deleted

    override fun RemoteHandBrewRecord.detailsOf(): List<RemoteHandBrewDetail> = details

    override fun RemoteHandBrewRecord.asRecordEntity(ownerId: String): HandBrewRecordEntity =
        asEntity(ownerId)

    override fun RemoteHandBrewDetail.asDetailEntity(
        ownerId: String,
        localDate: LocalDate,
    ): HandBrewRecordDetailEntity = asEntity(ownerId, localDate)

    override fun HandBrewRecordEntity.isSamePendingVersionOf(other: HandBrewRecordEntity): Boolean =
        isSamePendingVersion(other)

    override fun HandBrewRecordEntity.withSyncIdentity(
        id: String,
        ownerId: String,
        syncState: String,
        remoteRevision: Long,
    ): HandBrewRecordEntity = copy(
        id = id,
        ownerId = ownerId,
        syncState = syncState,
        remoteRevision = remoteRevision,
    )

    override fun HandBrewRecordDetailEntity.withOwner(ownerId: String): HandBrewRecordDetailEntity =
        copy(ownerId = ownerId)

    override fun HandBrewRecordDetailEntity.withRecoveryCopyIdentity(
        ownerId: String,
    ): HandBrewRecordDetailEntity = copy(id = localCopyId(id), ownerId = ownerId)

    override fun HandBrewRecordDetailEntity.withPromotedLocalIdentity(): HandBrewRecordDetailEntity =
        copy(id = localCopySourceId(id), ownerId = LOCAL_OWNER_ID)

    override fun recordEntityIdOf(entity: HandBrewRecordEntity): String = entity.id

    override fun recordLocalDateOf(entity: HandBrewRecordEntity): LocalDate = entity.localDate

    override fun recordSyncStateOf(entity: HandBrewRecordEntity): String = entity.syncState

    override fun recordRemoteRevisionOf(entity: HandBrewRecordEntity): Long = entity.remoteRevision

    override fun recordIsDeletedOf(entity: HandBrewRecordEntity): Boolean = entity.isDeleted

    override fun remoteRecordIdOf(remote: RemoteHandBrewRecord): String = remote.id

    override fun remoteRecordRevisionOf(remote: RemoteHandBrewRecord): Long = remote.revision
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
    localDate: LocalDate,
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
