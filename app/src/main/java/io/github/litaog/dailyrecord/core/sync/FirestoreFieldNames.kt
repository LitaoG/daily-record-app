package io.github.litaog.dailyrecord.core.sync

/**
 * Firestore field names and shared constants for the daily-count modules.
 * Module-specific count fields (brewCount/sexCount) stay in each module's
 * data source; everything else is shared so the wire format can never drift
 * between modules.
 */
internal const val FIELD_ID = "id"
internal const val FIELD_LOCAL_DATE = "localDate"
internal const val FIELD_CREATED_AT = "createdAtMillis"
internal const val FIELD_CLIENT_UPDATED_AT = "clientUpdatedAtMillis"
internal const val FIELD_DELETED = "deleted"
internal const val FIELD_REVISION = "revision"
internal const val FIELD_SCHEMA_VERSION = "schemaVersion"
internal const val FIELD_SERVER_UPDATED_AT = "serverUpdatedAt"
internal const val FIELD_DETAILS = "details"

internal const val DETAIL_ID = "id"
internal const val DETAIL_OCCURRENCE_INDEX = "occurrenceIndex"
internal const val DETAIL_START_TIME = "startTime"
internal const val DETAIL_END_TIME = "endTime"
internal const val DETAIL_FEELING = "feeling"

internal const val DELETE_BATCH_SIZE = 400L
internal const val MAX_SUPPORTED_EPOCH_MILLIS = 253_402_300_799_999L
internal const val USERS_COLLECTION = "users"
