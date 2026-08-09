package io.github.litaog.dailyrecord.core.database

import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.HandBrewRecordDetail
import io.github.litaog.dailyrecord.core.model.SexRecord
import io.github.litaog.dailyrecord.core.model.SexRecordDetail
import java.time.Instant

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

internal fun SexRecordEntity.asExternalModel(): SexRecord = SexRecord(
    id = id,
    localDate = localDate,
    sexCount = sexCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun SexRecord.asEntity(
    ownerId: String = LOCAL_OWNER_ID,
    syncState: String = SYNC_PENDING,
    remoteRevision: Long = 0,
): SexRecordEntity = SexRecordEntity(
    id = id,
    localDate = localDate,
    ownerId = ownerId,
    sexCount = sexCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = false,
    syncState = syncState,
    remoteRevision = remoteRevision,
)

internal fun HandBrewRecordDetailEntity.asExternalModel(): HandBrewRecordDetail = HandBrewRecordDetail(
    id = id,
    localDate = localDate,
    occurrenceIndex = occurrenceIndex,
    startTime = startTime,
    endTime = endTime,
    feeling = feeling,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun HandBrewRecordDetail.asEntity(
    ownerId: String = LOCAL_OWNER_ID,
    createdAt: Instant = this.createdAt,
    updatedAt: Instant = this.updatedAt,
): HandBrewRecordDetailEntity = HandBrewRecordDetailEntity(
    id = id,
    localDate = localDate,
    ownerId = ownerId,
    occurrenceIndex = occurrenceIndex,
    startTime = startTime,
    endTime = endTime,
    feeling = feeling,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun SexRecordDetailEntity.asExternalModel(): SexRecordDetail = SexRecordDetail(
    id = id,
    localDate = localDate,
    occurrenceIndex = occurrenceIndex,
    startTime = startTime,
    endTime = endTime,
    feeling = feeling,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun SexRecordDetail.asEntity(
    ownerId: String = LOCAL_OWNER_ID,
    createdAt: Instant = this.createdAt,
    updatedAt: Instant = this.updatedAt,
): SexRecordDetailEntity = SexRecordDetailEntity(
    id = id,
    localDate = localDate,
    ownerId = ownerId,
    occurrenceIndex = occurrenceIndex,
    startTime = startTime,
    endTime = endTime,
    feeling = feeling,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
