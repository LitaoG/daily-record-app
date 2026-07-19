package io.github.litaog.dailyrecord.core.sync

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import io.github.litaog.dailyrecord.core.cloud.awaitResult
import io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val FIELD_ID = "id"
private const val FIELD_LOCAL_DATE = "localDate"
private const val FIELD_BREW_COUNT = "brewCount"
private const val FIELD_CREATED_AT = "createdAtMillis"
private const val FIELD_CLIENT_UPDATED_AT = "clientUpdatedAtMillis"
private const val FIELD_DELETED = "deleted"
private const val FIELD_REVISION = "revision"
private const val FIELD_SCHEMA_VERSION = "schemaVersion"
private const val FIELD_SERVER_UPDATED_AT = "serverUpdatedAt"
internal const val MAX_SUPPORTED_EPOCH_MILLIS = 253_402_300_799_999L

internal class FirebaseHandBrewRemoteDataSource(
    private val firestore: FirebaseFirestore,
) : HandBrewRemoteDataSource {
    override fun observe(ownerId: String): Flow<RemoteSnapshot> = callbackFlow {
        val registration = records(ownerId).addSnapshotListener { snapshot, error ->
            when {
                error != null -> close(error)
                snapshot != null -> runCatching {
                    RemoteSnapshot(
                        records = snapshot.documents.map { it.toRemoteRecord() },
                        fromCache = snapshot.metadata.isFromCache,
                    )
                }.onSuccess(::trySend).onFailure(::close)
            }
        }
        awaitClose { registration.remove() }
    }

    override suspend fun fetch(ownerId: String): RemoteSnapshot {
        val snapshot = records(ownerId).get(Source.SERVER).awaitResult()
        return RemoteSnapshot(
            records = snapshot.documents.map { it.toRemoteRecord() },
            fromCache = snapshot.metadata.isFromCache,
        )
    }

    override suspend fun commit(
        ownerId: String,
        local: HandBrewRecordEntity,
    ): RemoteHandBrewRecord {
        require(local.ownerId == ownerId) { "Cannot upload a record owned by another account" }
        val reference = records(ownerId).document(local.localDate.toString())
        return firestore.runTransaction { transaction ->
            val current = transaction.get(reference)
            val currentRemote = if (current.exists()) {
                requireNotNull(current.toRemoteRecord()) { "Cloud record is malformed" }
            } else {
                null
            }
            if (currentRemote != null && !local.updatedAt.isAfter(currentRemote.clientUpdatedAt)) {
                return@runTransaction currentRemote
            }
            val revision = (current.getLong(FIELD_REVISION) ?: 0L) + 1L
            val stableId = current.getString(FIELD_ID) ?: local.id
            val stableCreatedAt = current.getLong(FIELD_CREATED_AT) ?: local.createdAt.toEpochMilli()
            transaction.set(
                reference,
                mapOf(
                    FIELD_ID to stableId,
                    FIELD_LOCAL_DATE to local.localDate.toString(),
                    FIELD_BREW_COUNT to local.brewCount.toLong(),
                    FIELD_CREATED_AT to stableCreatedAt,
                    FIELD_CLIENT_UPDATED_AT to local.updatedAt.toEpochMilli(),
                    FIELD_DELETED to local.isDeleted,
                    FIELD_REVISION to revision,
                    FIELD_SCHEMA_VERSION to 1L,
                    FIELD_SERVER_UPDATED_AT to FieldValue.serverTimestamp(),
                ),
            )
            RemoteHandBrewRecord(
                id = stableId,
                localDate = local.localDate,
                brewCount = local.brewCount,
                createdAt = Instant.ofEpochMilli(stableCreatedAt),
                clientUpdatedAt = local.updatedAt,
                deleted = local.isDeleted,
                revision = revision,
            )
        }.awaitResult()
    }

    private fun records(ownerId: String) = firestore
        .collection("users")
        .document(ownerId)
        .collection("handBrewRecords")

    private fun DocumentSnapshot.toRemoteRecord() = parseRemoteHandBrewRecord(
        documentId = id,
        values = requireNotNull(data) { "Cloud record has no data" },
    )
}

internal fun parseRemoteHandBrewRecord(
    documentId: String,
    values: Map<String, Any?>,
): RemoteHandBrewRecord {
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
    return RemoteHandBrewRecord(
        id = requireNotNull(values[FIELD_ID] as? String),
        localDate = LocalDate.parse(dateText),
        brewCount = count.toInt(),
        createdAt = Instant.ofEpochMilli(createdAtMillis),
        clientUpdatedAt = Instant.ofEpochMilli(updatedAtMillis),
        deleted = requireNotNull(values[FIELD_DELETED] as? Boolean),
        revision = revision,
    )
}
