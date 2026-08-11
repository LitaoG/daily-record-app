package io.github.litaog.dailyrecord.core.sync

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import io.github.litaog.dailyrecord.core.cloud.awaitResult
import io.github.litaog.dailyrecord.core.database.SexRecordEntity
import io.github.litaog.dailyrecord.core.database.SexRecordDetailEntity
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val FIELD_SEX_COUNT = "sexCount"

internal class FirebaseSexRemoteDataSource(
    private val firestore: FirebaseFirestore,
    private val detailsProvider: suspend (ownerId: String, localDate: LocalDate) ->
        List<SexRecordDetailEntity> = { _, _ -> emptyList() },
) : SexRemoteDataSource {
    override fun observe(ownerId: String): Flow<RemoteSnapshot> = callbackFlow {
        val registration = records(ownerId).addSnapshotListener { snapshot, error ->
            when {
                error != null -> close(error)
                snapshot != null -> trySend(snapshot.toRemoteSnapshot())
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun fetch(ownerId: String): RemoteSnapshot {
        val snapshot = records(ownerId).get(Source.SERVER).awaitResult()
        return snapshot.toRemoteSnapshot()
    }

    override suspend fun commit(ownerId: String, local: SexRecordEntity): RemoteSexRecord {
        require(local.ownerId == ownerId) { "Cannot upload a record owned by another account" }
        val details = detailsProvider(ownerId, local.localDate)
        val reference = records(ownerId).document(local.localDate.toString())
        return firestore.runTransaction { transaction ->
            val current = transaction.get(reference)
            val currentRemote = if (current.exists()) {
                requireNotNull(current.toRemoteSexRecord()) { "Cloud record is malformed" }
            } else {
                null
            }
            if (
                currentRemote != null &&
                (local.remoteRevision != currentRemote.revision || local.id != currentRemote.id)
            ) {
                return@runTransaction currentRemote
            }
            // Recreate a missing document from a retained local pending edit.
            // Ordinary record clearing writes a tombstone, so the optimistic
            // revision check above still protects normal concurrent edits.
            val revision = (currentRemote?.revision ?: 0L) + 1L
            // Preserve the caller's id for the first creation (revision 0),
            // but mint a new identity when a previously confirmed document
            // is physically recreated. The latter marks a new cloud
            // generation so peers can accept its restarted revision.
            val stableId = currentRemote?.id
                ?: if (local.remoteRevision > 0) UUID.randomUUID().toString() else local.id
            val stableCreatedAt = current.getLong(FIELD_CREATED_AT) ?: local.createdAt.toEpochMilli()
            val committedUpdatedAt = maxOf(
                local.updatedAt,
                Instant.ofEpochMilli(stableCreatedAt),
            )
            transaction.set(
                reference,
                mapOf(
                    FIELD_ID to stableId,
                    FIELD_LOCAL_DATE to local.localDate.toString(),
                    FIELD_SEX_COUNT to local.sexCount.toLong(),
                    FIELD_CREATED_AT to stableCreatedAt,
                    FIELD_CLIENT_UPDATED_AT to committedUpdatedAt.toEpochMilli(),
                    FIELD_DELETED to local.isDeleted,
                    FIELD_REVISION to revision,
                    FIELD_SCHEMA_VERSION to 1L,
                    FIELD_DETAILS to details.map {
                        detailToMap(it.id, it.occurrenceIndex, it.startTime, it.endTime, it.feeling)
                    },
                    FIELD_SERVER_UPDATED_AT to FieldValue.serverTimestamp(),
                ),
            )
            RemoteSexRecord(
                id = stableId,
                localDate = local.localDate,
                sexCount = local.sexCount,
                createdAt = Instant.ofEpochMilli(stableCreatedAt),
                clientUpdatedAt = committedUpdatedAt,
                deleted = local.isDeleted,
                revision = revision,
                details = details.map {
                    RemoteSexDetail(it.id, it.occurrenceIndex, it.startTime, it.endTime, it.feeling)
                },
            )
        }.awaitResult()
    }

    override suspend fun deleteAll(ownerId: String) {
        while (true) {
            val snapshot = records(ownerId)
                .limit(DELETE_BATCH_SIZE)
                .get(Source.SERVER)
                .awaitResult()
            if (snapshot.isEmpty) return
            val batch = firestore.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().awaitResult()
        }
    }

    private fun records(ownerId: String) = firestore
        .collection(USERS_COLLECTION)
        .document(ownerId)
        .collection("sexRecords")

    private fun DocumentSnapshot.toRemoteSexRecord() = parseRemoteSexRecord(
        documentId = id,
        values = requireNotNull(data) { "Cloud record has no data" },
    )

    private fun QuerySnapshot.toRemoteSnapshot(): RemoteSnapshot {
        val parsed = parseRemoteSexRecords(
            documents.map { document -> document.id to document.data },
        )
        return RemoteSnapshot(
            sexRecords = parsed.records,
            fromCache = metadata.isFromCache,
            rejectedRecordCount = parsed.rejectedRecordCount,
        )
    }
}

internal data class ParsedRemoteSexRecords(
    val records: List<RemoteSexRecord>,
    val rejectedRecordCount: Int,
)

internal fun parseRemoteSexRecords(
    documents: List<Pair<String, Map<String, Any?>?>>,
): ParsedRemoteSexRecords {
    var rejected = 0
    val records = documents.mapNotNull { (documentId, values) ->
        try {
            parseRemoteSexRecord(
                documentId = documentId,
                values = requireNotNull(values) { "Cloud record has no data" },
            )
        } catch (_: RuntimeException) {
            rejected += 1
            null
        }
    }
    return ParsedRemoteSexRecords(records, rejected)
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

