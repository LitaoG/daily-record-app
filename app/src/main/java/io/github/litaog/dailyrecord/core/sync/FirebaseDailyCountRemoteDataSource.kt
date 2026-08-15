package io.github.litaog.dailyrecord.core.sync

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import com.google.firebase.functions.FirebaseFunctions
import io.github.litaog.dailyrecord.core.common.awaitResult
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Shared Firestore implementation for the isolated daily-count modules.
 *
 * Each module keeps its own collection, count field name, parse rules and
 * remote record type (AGENTS: module boundaries stay independent); this base
 * only removes the duplicated observe/fetch/commit orchestration. Record
 * writes go through the trusted Functions endpoint so the server validates
 * every detail item before a document is committed.
 */
internal open class FirebaseDailyCountRemoteDataSource<E : Any, D : Any, R : RemoteDailyCountRecord>(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val detailsProvider: suspend (ownerId: String, localDate: LocalDate) -> List<D>,
    private val collectionName: String,
    private val parseRecord: (documentId: String, values: Map<String, Any?>) -> R,
    private val parseRecords: (List<Pair<String, Map<String, Any?>?>>) -> ParsedRemoteRecords<R>,
    private val entityId: (E) -> String,
    private val entityLocalDate: (E) -> LocalDate,
    private val entityCount: (E) -> Int,
    private val entityCreatedAt: (E) -> Instant,
    private val entityUpdatedAt: (E) -> Instant,
    private val entityIsDeleted: (E) -> Boolean,
    private val entityOwnerId: (E) -> String,
    private val entityRemoteRevision: (E) -> Long,
    private val detailToMap: (D) -> Map<String, Any?>,
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
        val result = functions.getHttpsCallable(WRITE_FUNCTION_NAME)
            .call(
                mapOf(
                    "collection" to collectionName,
                    "localDate" to entityLocalDate(local).toString(),
                    "id" to entityId(local),
                    "count" to entityCount(local).toLong(),
                    "createdAtMillis" to entityCreatedAt(local).toEpochMilli(),
                    "clientUpdatedAtMillis" to entityUpdatedAt(local).toEpochMilli(),
                    "deleted" to entityIsDeleted(local),
                    "remoteRevision" to entityRemoteRevision(local),
                    "details" to details.map(detailToMap),
                ),
            )
            .awaitResult()
            .data
        val resultMap = result as? Map<*, *> ?: error("Cloud write response is malformed")
        val recordMap = resultMap["record"] as? Map<*, *>
            ?: error("Cloud write response has no record")
        val values = recordMap.entries.associate { (key, value) ->
            require(key is String) { "Cloud write response has a non-string field" }
            key to normalizeCallableValue(value)
        }
        return parseRecord(entityLocalDate(local).toString(), values)
    }

    private fun records(ownerId: String) = firestore
        .collection(USERS_COLLECTION)
        .document(ownerId)
        .collection(collectionName)

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

    private fun normalizeCallableValue(value: Any?): Any? = when (value) {
        is Map<*, *> -> value.entries.associate { (key, nested) ->
            require(key is String) { "Cloud write response has a non-string nested field" }
            key to normalizeCallableValue(nested)
        }
        is List<*> -> value.map(::normalizeCallableValue)
        is Number -> value.toLong()
        else -> value
    }

    private companion object {
        const val WRITE_FUNCTION_NAME = "writeDailyCountRecord"
    }
}

/**
 * Parse a single document without the rejection-swallowing behavior used for
 * list snapshots. Callable compare-and-set responses need the typed
 * [MalformedRemoteRecordException] so the sync engine can quarantine a bad
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
