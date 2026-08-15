package io.github.litaog.dailyrecord.core.sync

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import io.github.litaog.dailyrecord.core.common.awaitResult
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Shared Firestore implementation for the isolated daily-count modules.
 *
 * Each module keeps its own collection, count field name, parse rules and
 * remote record type (AGENTS: module boundaries stay independent); this base
 * only removes the duplicated observe/fetch/commit/deleteAll orchestration.
 */
internal open class FirebaseDailyCountRemoteDataSource<E : Any, D : Any, R : RemoteDailyCountRecord, RD>(
    private val firestore: FirebaseFirestore,
    private val detailsProvider: suspend (ownerId: String, localDate: LocalDate) -> List<D>,
    private val collectionName: String,
    private val countFieldName: String,
    private val parseRecord: (documentId: String, values: Map<String, Any?>) -> R,
    private val parseRecords: (List<Pair<String, Map<String, Any?>?>>) -> ParsedRemoteRecords<R>,
    private val toRemoteDetails: (List<D>) -> List<RD>,
    private val entityId: (E) -> String,
    private val entityLocalDate: (E) -> LocalDate,
    private val entityCount: (E) -> Int,
    private val entityCreatedAt: (E) -> Instant,
    private val entityUpdatedAt: (E) -> Instant,
    private val entityIsDeleted: (E) -> Boolean,
    private val entityOwnerId: (E) -> String,
    private val entityRemoteRevision: (E) -> Long,
    private val detailToMap: (D) -> Map<String, Any?>,
    private val buildCommittedRecord: (
        local: E,
        stableId: String,
        stableCreatedAt: Instant,
        committedUpdatedAt: Instant,
        revision: Long,
        details: List<RD>,
    ) -> R,
) {
    fun observe(ownerId: String): Flow<RemoteSnapshot> = callbackFlow {
        val registration = records(ownerId).addSnapshotListener { snapshot, error ->
            when {
                error != null -> close(error)
                snapshot != null -> trySend(snapshot.toRemoteSnapshot())
            }
        }
        awaitClose { registration.remove() }
    }

    suspend fun fetch(ownerId: String): RemoteSnapshot {
        val snapshot = records(ownerId).get(Source.SERVER).awaitResult()
        return snapshot.toRemoteSnapshot()
    }

    suspend fun commit(ownerId: String, local: E): R {
        require(entityOwnerId(local) == ownerId) { "Cannot upload a record owned by another account" }
        val details = detailsProvider(ownerId, entityLocalDate(local))
        val reference = records(ownerId).document(entityLocalDate(local).toString())
        return firestore.runTransaction { transaction ->
            val current = transaction.get(reference)
            val currentRemote = if (current.exists()) {
                requireNotNull(current.toRemoteRecord()) { "Cloud record is malformed" }
            } else {
                null
            }
            if (
                currentRemote != null &&
                (entityRemoteRevision(local) != currentRemote.revision || entityId(local) != currentRemote.id)
            ) {
                return@runTransaction currentRemote
            }
            // A missing document can occur after account data was removed and
            // the local pending edit was retained for recovery. Treat it as a
            // new document rather than permanently failing the PENDING row on
            // its stale revision baseline. Normal clears use a tombstone and
            // still participate in the optimistic revision/id check above.
            val revision = (currentRemote?.revision ?: 0L) + 1L
            // Preserve the caller's id for the first creation (revision 0),
            // but mint a new identity when a previously confirmed document
            // is physically recreated. The latter marks a new cloud
            // generation so peers can accept its restarted revision.
            val stableId = currentRemote?.id
                ?: if (entityRemoteRevision(local) > 0) UUID.randomUUID().toString() else entityId(local)
            val stableCreatedAt = current.getLong(FIELD_CREATED_AT) ?: entityCreatedAt(local).toEpochMilli()
            val committedUpdatedAt = maxOf(
                entityUpdatedAt(local),
                Instant.ofEpochMilli(stableCreatedAt),
            )
            transaction.set(
                reference,
                mapOf(
                    FIELD_ID to stableId,
                    FIELD_LOCAL_DATE to entityLocalDate(local).toString(),
                    countFieldName to entityCount(local).toLong(),
                    FIELD_CREATED_AT to stableCreatedAt,
                    FIELD_CLIENT_UPDATED_AT to committedUpdatedAt.toEpochMilli(),
                    FIELD_DELETED to entityIsDeleted(local),
                    FIELD_REVISION to revision,
                    FIELD_SCHEMA_VERSION to 1L,
                    FIELD_DETAILS to details.map { detailToMap(it) },
                    FIELD_SERVER_UPDATED_AT to FieldValue.serverTimestamp(),
                ),
            )
            buildCommittedRecord(
                local,
                stableId,
                Instant.ofEpochMilli(stableCreatedAt),
                committedUpdatedAt,
                revision,
                toRemoteDetails(details),
            )
        }.awaitResult()
    }

    suspend fun deleteAll(ownerId: String) {
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
        .collection(collectionName)

    private fun DocumentSnapshot.toRemoteRecord(): R = parseRemoteRecordOrThrow(
        documentId = id,
        values = data,
        parse = parseRecord,
    )

    private fun QuerySnapshot.toRemoteSnapshot(): RemoteSnapshot {
        val parsed = parseRecords(
            documents.map { document ->
                document.id to document.data
            },
        )
        return RemoteSnapshot(
            records = parsed.records,
            fromCache = metadata.isFromCache,
            rejectedRecordCount = parsed.rejectedRecordCount,
        )
    }
}

/**
 * Parse a single document without the rejection-swallowing behavior used for
 * list snapshots. Transactional compare-and-set reads need the typed
 * [MalformedRemoteRecordException] so the sync engine can quarantine the bad
 * document instead of aborting the whole module sync with a generic error.
 */
internal fun <R> parseRemoteRecordOrThrow(
    documentId: String,
    values: Map<String, Any?>?,
    parse: (documentId: String, values: Map<String, Any?>) -> R,
): R = try {
    parse(documentId, requireNotNull(values) { "Cloud record has no data" })
} catch (error: MalformedRemoteRecordException) {
    throw error
} catch (error: RuntimeException) {
    throw MalformedRemoteRecordException(error)
}

/** Shared parse-result shape for all daily-count modules. */
internal data class ParsedRemoteRecords<R>(
    val records: List<R>,
    val rejectedRecordCount: Int,
)

/** Shared rejection-swallowing batch parser for all daily-count modules. */
internal fun <R : RemoteDailyCountRecord> parseRemoteRecords(
    documents: List<Pair<String, Map<String, Any?>?>>,
    parse: (documentId: String, values: Map<String, Any?>) -> R,
): ParsedRemoteRecords<R> {
    var rejected = 0
    val records = documents.mapNotNull { (documentId, values) ->
        try {
            parse(
                documentId,
                requireNotNull(values) { "Cloud record has no data" },
            )
        } catch (_: RuntimeException) {
            rejected += 1
            null
        }
    }
    return ParsedRemoteRecords(
        records = records,
        rejectedRecordCount = rejected,
    )
}
