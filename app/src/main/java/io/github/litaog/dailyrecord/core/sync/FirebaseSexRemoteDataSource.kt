package io.github.litaog.dailyrecord.core.sync

import com.google.firebase.firestore.FirebaseFirestore
import io.github.litaog.dailyrecord.core.database.SexRecordDetailEntity
import io.github.litaog.dailyrecord.core.database.SexRecordEntity
import java.time.Instant
import java.time.LocalDate

internal class FirebaseSexRemoteDataSource(
    firestore: FirebaseFirestore,
    detailsProvider: suspend (ownerId: String, localDate: LocalDate) ->
        List<SexRecordDetailEntity> = { _, _ -> emptyList() },
) : FirebaseDailyCountRemoteDataSource<
        SexRecordEntity,
        SexRecordDetailEntity,
        RemoteSexRecord,
        RemoteSexDetail,
    >(
        firestore = firestore,
        detailsProvider = detailsProvider,
        collectionName = "sexRecords",
        countFieldName = FIELD_SEX_COUNT,
        parseRecord = ::parseRemoteSexRecord,
        parseRecords = ::parseRemoteSexRecords,
        toRemoteDetails = { details ->
            details.map {
                RemoteSexDetail(it.id, it.occurrenceIndex, it.startTime, it.endTime, it.feeling)
            }
        },
        entityId = { it.id },
        entityLocalDate = { it.localDate },
        entityCount = { it.sexCount },
        entityCreatedAt = { it.createdAt },
        entityUpdatedAt = { it.updatedAt },
        entityIsDeleted = { it.isDeleted },
        entityOwnerId = { it.ownerId },
        entityRemoteRevision = { it.remoteRevision },
        detailToMap = { detailToMap(it.id, it.occurrenceIndex, it.startTime, it.endTime, it.feeling) },
        buildCommittedRecord = { local, stableId, stableCreatedAt, committedUpdatedAt, revision, details ->
            RemoteSexRecord(
                id = stableId,
                localDate = local.localDate,
                sexCount = local.sexCount,
                createdAt = stableCreatedAt,
                clientUpdatedAt = committedUpdatedAt,
                deleted = local.isDeleted,
                revision = revision,
                details = details,
            )
        },
    ),
    SexRemoteDataSource

private const val FIELD_SEX_COUNT = "sexCount"

internal fun parseRemoteSexRecords(
    documents: List<Pair<String, Map<String, Any?>?>>,
): ParsedRemoteRecords<RemoteSexRecord> = parseRemoteRecords(documents) { documentId, values ->
    parseRemoteSexRecord(documentId, values)
}

internal fun parseRemoteSexRecord(
    documentId: String,
    values: Map<String, Any?>,
): RemoteSexRecord = try {
    val dateText = requireNotNull(values[FIELD_LOCAL_DATE] as? String)
    require(documentId == dateText) { "Document id and localDate must match" }
    val count = requireNotNull(values[FIELD_SEX_COUNT] as? Long)
    require(count in 0..Int.MAX_VALUE.toLong()) { "sexCount is out of range" }
    val revision = requireNotNull(values[FIELD_REVISION] as? Long)
    require(revision >= 1) { "revision must be positive" }
    val createdAtMillis = requireNotNull(values[FIELD_CREATED_AT] as? Long)
    require(createdAtMillis in 0..MAX_SUPPORTED_EPOCH_MILLIS)
    val updatedAtMillis = requireNotNull(values[FIELD_CLIENT_UPDATED_AT] as? Long)
    require(updatedAtMillis in createdAtMillis..MAX_SUPPORTED_EPOCH_MILLIS)
    val details = (values[FIELD_DETAILS] as? List<*>).orEmpty().map {
        parseRemoteDetail(it, "sex").let { parsed ->
            RemoteSexDetail(parsed.id, parsed.occurrenceIndex, parsed.startTime, parsed.endTime, parsed.feeling)
        }
    }
    require(details.size <= count) { "sex details exceed sexCount" }
    requireUniqueRemoteDetailIdentity(
        ids = details.map { it.id },
        occurrenceIndexes = details.map { it.occurrenceIndex },
        label = "sex",
    )
    require(details.all { it.occurrenceIndex <= count.toInt() }) {
        "sex detail occurrenceIndex exceeds sexCount"
    }
    RemoteSexRecord(
        id = requireNotNull(values[FIELD_ID] as? String),
        localDate = LocalDate.parse(dateText),
        sexCount = count.toInt(),
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
