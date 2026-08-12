package io.github.litaog.dailyrecord.core.sync

import com.google.firebase.firestore.FirebaseFirestore
import io.github.litaog.dailyrecord.core.database.HandBrewRecordDetailEntity
import io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity
import java.time.Instant
import java.time.LocalDate

internal class FirebaseHandBrewRemoteDataSource(
    firestore: FirebaseFirestore,
    detailsProvider: suspend (ownerId: String, localDate: LocalDate) ->
        List<HandBrewRecordDetailEntity> = { _, _ -> emptyList() },
) : FirebaseDailyCountRemoteDataSource<
        HandBrewRecordEntity,
        HandBrewRecordDetailEntity,
        RemoteHandBrewRecord,
        RemoteHandBrewDetail,
    >(
        firestore = firestore,
        detailsProvider = detailsProvider,
        collectionName = "handBrewRecords",
        countFieldName = FIELD_BREW_COUNT,
        parseRecords = ::parseRemoteHandBrewRecords,
        toRemoteDetails = { details ->
            details.map {
                RemoteHandBrewDetail(it.id, it.occurrenceIndex, it.startTime, it.endTime, it.feeling)
            }
        },
        entityId = { it.id },
        entityLocalDate = { it.localDate },
        entityCount = { it.brewCount },
        entityCreatedAt = { it.createdAt },
        entityUpdatedAt = { it.updatedAt },
        entityIsDeleted = { it.isDeleted },
        entityOwnerId = { it.ownerId },
        entityRemoteRevision = { it.remoteRevision },
        detailToMap = { detailToMap(it.id, it.occurrenceIndex, it.startTime, it.endTime, it.feeling) },
        buildCommittedRecord = { local, stableId, stableCreatedAt, committedUpdatedAt, revision, details ->
            RemoteHandBrewRecord(
                id = stableId,
                localDate = local.localDate,
                brewCount = local.brewCount,
                createdAt = stableCreatedAt,
                clientUpdatedAt = committedUpdatedAt,
                deleted = local.isDeleted,
                revision = revision,
                details = details,
            )
        },
    ),
    HandBrewRemoteDataSource

private const val FIELD_BREW_COUNT = "brewCount"

internal fun parseRemoteHandBrewRecords(
    documents: List<Pair<String, Map<String, Any?>?>>,
): ParsedRemoteRecords<RemoteHandBrewRecord> = parseRemoteRecords(documents) { documentId, values ->
    parseRemoteHandBrewRecord(documentId, values)
}

internal fun parseRemoteHandBrewRecord(
    documentId: String,
    values: Map<String, Any?>,
): RemoteHandBrewRecord = try {
    val dateText = requireNotNull(values[FIELD_LOCAL_DATE] as? String)
    require(documentId == dateText) { "Document id and localDate must match" }
    val count = requireNotNull(values[FIELD_BREW_COUNT] as? Long)
    require(count in 0..Int.MAX_VALUE.toLong()) { "brewCount is out of range" }
    val revision = requireNotNull(values[FIELD_REVISION] as? Long)
    require(revision >= 1) { "revision must be positive" }
    val createdAtMillis = requireNotNull(values[FIELD_CREATED_AT] as? Long)
    require(createdAtMillis in 0..MAX_SUPPORTED_EPOCH_MILLIS) {
        "createdAtMillis is out of range"
    }
    val updatedAtMillis = requireNotNull(values[FIELD_CLIENT_UPDATED_AT] as? Long)
    require(updatedAtMillis in createdAtMillis..MAX_SUPPORTED_EPOCH_MILLIS) {
        "clientUpdatedAtMillis is out of range"
    }
    val details = (values[FIELD_DETAILS] as? List<*>).orEmpty().map {
        parseRemoteDetail(it, "hand-brew").let { parsed ->
            RemoteHandBrewDetail(parsed.id, parsed.occurrenceIndex, parsed.startTime, parsed.endTime, parsed.feeling)
        }
    }
    require(details.size <= count) { "hand-brew details exceed brewCount" }
    requireUniqueRemoteDetailIdentity(
        ids = details.map { it.id },
        occurrenceIndexes = details.map { it.occurrenceIndex },
        label = "hand-brew",
    )
    require(details.all { it.occurrenceIndex <= count.toInt() }) {
        "hand-brew detail occurrenceIndex exceeds brewCount"
    }
    RemoteHandBrewRecord(
        id = requireNotNull(values[FIELD_ID] as? String),
        localDate = LocalDate.parse(dateText),
        brewCount = count.toInt(),
        createdAt = Instant.ofEpochMilli(createdAtMillis),
        clientUpdatedAt = Instant.ofEpochMilli(updatedAtMillis),
        deleted = requireNotNull(values[FIELD_DELETED] as? Boolean),
        revision = revision,
        details = details,
    )
} catch (error: MalformedRemoteRecordException) {
    throw error
} catch (error: RuntimeException) {
    throw MalformedRemoteRecordException(error)
}
