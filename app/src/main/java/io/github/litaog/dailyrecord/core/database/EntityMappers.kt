package io.github.litaog.dailyrecord.core.database

import io.github.litaog.dailyrecord.core.model.Activity
import io.github.litaog.dailyrecord.core.model.DailyRecord

internal fun ActivityEntity.asExternalModel(): Activity = Activity(
    id = id,
    ownerId = ownerId,
    name = name,
    iconKey = iconKey,
    colorArgb = colorArgb,
    measurementType = measurementType,
    unit = unit,
    sortOrder = sortOrder,
    isArchived = isArchived,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    revision = revision,
)

internal fun Activity.asEntity(): ActivityEntity = ActivityEntity(
    id = id,
    ownerId = ownerId,
    name = name,
    iconKey = iconKey,
    colorArgb = colorArgb,
    measurementType = measurementType,
    unit = unit,
    sortOrder = sortOrder,
    isArchived = isArchived,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    revision = revision,
)

internal fun DailyRecordEntity.asExternalModel(): DailyRecord = DailyRecord(
    id = id,
    ownerId = ownerId,
    activityId = activityId,
    localDate = localDate,
    status = status,
    quantity = quantity,
    note = note,
    timezoneId = timezoneId,
    occurredAt = occurredAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    revision = revision,
)

internal fun DailyRecord.asEntity(): DailyRecordEntity = DailyRecordEntity(
    id = id,
    ownerId = ownerId,
    activityId = activityId,
    localDate = localDate,
    status = status,
    quantity = quantity,
    note = note,
    timezoneId = timezoneId,
    occurredAt = occurredAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    revision = revision,
)
