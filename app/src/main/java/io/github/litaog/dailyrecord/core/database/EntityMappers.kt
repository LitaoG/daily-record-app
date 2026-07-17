package io.github.litaog.dailyrecord.core.database

import io.github.litaog.dailyrecord.core.model.HandBrewRecord

internal fun HandBrewRecordEntity.asExternalModel(): HandBrewRecord = HandBrewRecord(
    id = id,
    localDate = localDate,
    brewCount = brewCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

internal fun HandBrewRecord.asEntity(): HandBrewRecordEntity = HandBrewRecordEntity(
    id = id,
    localDate = localDate,
    brewCount = brewCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
