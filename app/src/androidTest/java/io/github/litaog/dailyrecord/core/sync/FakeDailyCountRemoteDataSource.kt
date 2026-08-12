package io.github.litaog.dailyrecord.core.sync

import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Shared in-memory remote for sync-coordinator tests. Mirrors the production
 * Firestore revision protocol (issue #104): a missing cloud document is
 * recreated from the local pending edit; ordinary clears keep tombstones.
 */
internal class FakeDailyCountRemoteDataSource<E : Any, R : RemoteDailyCountRecord, RD>(
    private val failFirstObservation: Boolean = false,
    private val failFetchAttempts: Int = 0,
    private val rejectedRecordCount: Int = 0,
    private val entityId: (E) -> String,
    private val entityLocalDate: (E) -> LocalDate,
    private val entityCount: (E) -> Int,
    private val entityCreatedAt: (E) -> Instant,
    private val entityUpdatedAt: (E) -> Instant,
    private val entityDeleted: (E) -> Boolean,
    private val entityRemoteRevision: (E) -> Long,
    private val buildRemote: (
        id: String,
        localDate: LocalDate,
        count: Int,
        createdAt: Instant,
        clientUpdatedAt: Instant,
        deleted: Boolean,
        revision: Long,
    ) -> R,
) : DailyCountRemoteDataSource<E, R> {
    private val mutex = Mutex()
    private val values = MutableStateFlow<Map<LocalDate, R>>(emptyMap())
    val observationAttempts = MutableStateFlow(0)
    var fetchCalls: Int = 0
        private set
    var fetchGate: CompletableDeferred<Unit>? = null
    var detailsProvider: suspend (LocalDate) -> List<RD> = { emptyList() }

    override fun recordsFrom(snapshot: RemoteSnapshot): List<R> =
        snapshot.records.mapNotNull { it as? R }

    override fun matches(remote: R, local: E): Boolean = true

    override fun observe(ownerId: String): Flow<RemoteSnapshot> = flow {
        val attempt = observationAttempts.value + 1
        observationAttempts.value = attempt
        if (failFirstObservation && attempt == 1) {
            throw IOException("temporary listener failure")
        }
        emitAll(
            values.map {
                RemoteSnapshot(
                    records = it.values.toList(),
                    fromCache = false,
                    rejectedRecordCount = rejectedRecordCount,
                )
            },
        )
    }

    override suspend fun fetch(ownerId: String): RemoteSnapshot {
        fetchCalls += 1
        if (fetchCalls <= failFetchAttempts) throw IOException("temporary fetch failure")
        fetchGate?.await()
        return RemoteSnapshot(
            records = values.value.values.toList(),
            fromCache = false,
            rejectedRecordCount = rejectedRecordCount,
        )
    }

    override suspend fun commit(ownerId: String, local: E): R =
        mutex.withLock {
            val current = values.value[entityLocalDate(local)]
            if (
                current != null &&
                (entityRemoteRevision(local) != current.revision || entityId(local) != current.id)
            ) {
                return@withLock current
            }
            // Mirrors the production data source (issue #104): a missing cloud
            // document is recreated from the local pending edit instead of
            // permanently failing the PENDING row on its stale revision
            // baseline. Normal clears still write a tombstone and participate
            // in the revision check above.
            val committed = buildRemote(current?.id
                    ?: if (entityRemoteRevision(local) > 0) UUID.randomUUID().toString() else entityId(local),
                entityLocalDate(local),
                entityCount(local),
                current?.createdAt ?: entityCreatedAt(local),
                maxOf(
                    entityUpdatedAt(local),
                    current?.createdAt ?: entityCreatedAt(local),
                ),
                entityDeleted(local),
                (current?.revision ?: 0) + 1,
            )
            values.value = values.value + (entityLocalDate(local) to committed)
            committed
        }

    override suspend fun deleteAll(ownerId: String) {
        values.value = emptyMap()
    }

    /** Simulates the cloud document disappearing (e.g. account data cleanup). */
    fun removeRemote(localDate: LocalDate) {
        values.value = values.value - localDate
    }
}
