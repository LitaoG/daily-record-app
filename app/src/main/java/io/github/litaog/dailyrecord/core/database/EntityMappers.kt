package io.github.litaog.dailyrecord.core.database

import io.github.litaog.dailyrecord.core.model.HandBrewRecord

internal fun HandBrewRecordEntity.asExternalModel(): HandBrewRecord = HandBrewRecord(
    id = id,
    localDate = localDate,
    brewCount = brewCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun HandBrewRecord.asEntity(
    ownerId: String = LOCAL_OWNER_ID,
    syncState: String = SYNC_PENDING,
    remoteRevision: Long = 0,
): HandBrewRecordEntity = HandBrewRecordEntity(
    id = id,
    localDate = localDate,
    ownerId = ownerId,
    brewCount = brewCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = false,
    syncState = syncState,
    remoteRevision = remoteRevision,
)
