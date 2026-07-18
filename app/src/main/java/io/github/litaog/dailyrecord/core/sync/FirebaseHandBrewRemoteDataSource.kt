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

    private fun DocumentSnapshot.toRemoteRecord(): RemoteHandBrewRecord {
        val dateText = requireNotNull(getString(FIELD_LOCAL_DATE))
        require(id == dateText) { "Document id and localDate must match" }
        val count = requireNotNull(getLong(FIELD_BREW_COUNT))
        require(count in 0..Int.MAX_VALUE.toLong()) { "brewCount is out of range" }
        val revision = requireNotNull(getLong(FIELD_REVISION))
        require(revision >= 1) { "revision must be positive" }
        val createdAt = Instant.ofEpochMilli(requireNotNull(getLong(FIELD_CREATED_AT)))
        val updatedAt = Instant.ofEpochMilli(requireNotNull(getLong(FIELD_CLIENT_UPDATED_AT)))
        require(!updatedAt.isBefore(createdAt)) { "clientUpdatedAt must not precede createdAt" }
        return RemoteHandBrewRecord(
            id = requireNotNull(getString(FIELD_ID)),
            localDate = LocalDate.parse(dateText),
            brewCount = count.toInt(),
            createdAt = createdAt,
            clientUpdatedAt = updatedAt,
            deleted = requireNotNull(getBoolean(FIELD_DELETED)),
            revision = revision,
        )
    }

    private companion object {
        const val FIELD_ID = "id"
        const val FIELD_LOCAL_DATE = "localDate"
        const val FIELD_BREW_COUNT = "brewCount"
        const val FIELD_CREATED_AT = "createdAtMillis"
        const val FIELD_CLIENT_UPDATED_AT = "clientUpdatedAtMillis"
        const val FIELD_DELETED = "deleted"
        const val FIELD_REVISION = "revision"
        const val FIELD_SCHEMA_VERSION = "schemaVersion"
        const val FIELD_SERVER_UPDATED_AT = "serverUpdatedAt"
    }
}
