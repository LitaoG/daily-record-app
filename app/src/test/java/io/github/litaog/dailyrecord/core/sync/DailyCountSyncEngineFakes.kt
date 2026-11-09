package io.github.litaog.dailyrecord.core.sync

import io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity
import io.github.litaog.dailyrecord.core.database.SYNC_PENDING
import io.github.litaog.dailyrecord.core.database.SYNCED
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

private val owner = "owner"

private val date = LocalDate.of(2026, 7, 17)

private val t0 = Instant.parse("2026-07-17T00:00:00Z")

internal fun entity(
        brewCount: Int,
        updatedAt: Instant,
        remoteRevision: Long,
        localDate: LocalDate = date,
    ) = HandBrewRecordEntity(
        id = "record-$localDate",
        localDate = localDate,
        ownerId = owner,
        brewCount = brewCount,
        createdAt = t0,
        updatedAt = updatedAt,
        isDeleted = false,
        syncState = SYNC_PENDING,
        remoteRevision = remoteRevision,
    )

internal fun remoteOf(
        brewCount: Int,
        clientUpdatedAt: Instant,
        revision: Long,
    ) = RemoteHandBrewRecord(
        id = "record-$date",
        localDate = date,
        brewCount = brewCount,
        createdAt = t0,
        clientUpdatedAt = clientUpdatedAt,
        deleted = false,
        revision = revision,
    )

internal fun runSync(block: suspend () -> Unit) = kotlinx.coroutines.runBlocking { block() }

internal class FakeStore : DailyCountSyncStore<HandBrewRecordEntity, RemoteHandBrewRecord> {
    val rows = mutableMapOf<LocalDate, HandBrewRecordEntity>()

    override fun observePendingCount(ownerId: String): Flow<Int> = MutableStateFlow(rows.size)

    override suspend fun pending(ownerId: String): List<HandBrewRecordEntity> =
        rows.values.filter { it.syncState == SYNC_PENDING }.sortedBy { it.updatedAt }

    override suspend fun pendingCount(ownerId: String): Int =
        rows.values.count { it.syncState == SYNC_PENDING }

    override suspend fun adoptLocalRecords(ownerId: String): Int = 0

    override suspend fun applyRemote(
        ownerId: String,
        records: List<RemoteHandBrewRecord>,
        completeServerSnapshot: Boolean,
    ): Int {
        var changed = 0
        records.forEach { remote ->
            val local = rows[remote.localDate]
            // Simplified production guard: the real RoomDailyCountSyncStoreBase
            // also compares the document id and deletes the local row when the
            // remote generation changed, so a recreated cloud document with a
            // restarted revision is adopted here even though this fake would
            // keep the stale row. That branch is covered by the two
            // syncedPeerAcceptsRecreatedCloudGenerationWithRestartedRevision
            // coordinator tests; do not read this fake as the full rule.
            if (local?.syncState == SYNC_PENDING ||
                (local != null && local.remoteRevision >= remote.revision)
            ) {
                return@forEach
            }
            rows[remote.localDate] = remote.toEntity()
            changed += 1
        }
        // Mirror the production ADR-020 collection rule.
        if (completeServerSnapshot) {
            val remoteDates = records.mapTo(mutableSetOf()) { it.localDate }
            rows.entries
                .filter { (localDate, row) ->
                    localDate !in remoteDates &&
                        shouldGcLocallyAbsentRow(row.syncState, row.remoteRevision)
                }
                .map { it.key }
                .toList()
                .forEach { rows.remove(it) }
        }
        return changed
    }

    override suspend fun alignUnbasedPendingRevisions(
        ownerId: String,
        records: List<RemoteHandBrewRecord>,
    ): Int = 0

    override suspend fun applyCommitIfUnchanged(
        ownerId: String,
        local: HandBrewRecordEntity,
        committed: RemoteHandBrewRecord,
    ): Boolean {
        val current = rows[local.localDate] ?: return false
        if (current != local) return false
        rows[local.localDate] = committed.toEntity()
        return true
    }

    override suspend fun rebasePending(
        ownerId: String,
        local: HandBrewRecordEntity,
        committed: RemoteHandBrewRecord,
    ): Int {
        val current = rows[local.localDate] ?: return 0
        if (current.syncState != SYNC_PENDING) return 0
        rows[local.localDate] = current.copy(remoteRevision = committed.revision)
        return 1
    }
}

internal class FakeRemote : DailyCountRemoteDataSource<HandBrewRecordEntity, RemoteHandBrewRecord> {
    val server = mutableMapOf<LocalDate, RemoteHandBrewRecord>()
    var fetchCalls = 0
    override val collectionName: String = HAND_BREW_RECORDS_COLLECTION

    /** Simulates a user edit landing while the commit request is in flight. */
    var onCommit: (() -> Unit)? = null

    /** Rejected counts served per fetch call, in order; defaults to zero. */
    var rejectedPerFetch: List<Int> = emptyList()

    override fun observe(ownerId: String): Flow<RemoteSnapshot> = MutableStateFlow(fetchSnapshot())

    override suspend fun fetch(ownerId: String): RemoteSnapshot {
        fetchCalls += 1
        return fetchSnapshot(rejectedPerFetch.getOrElse(fetchCalls - 1) { 0 })
    }

    override fun recordsFrom(snapshot: RemoteSnapshot): List<RemoteHandBrewRecord> =
        snapshot.records.filterIsInstance<RemoteHandBrewRecord>()

    /** Dates whose commit must fail with the mapped error instead of uploading. */
    var poisonDates: Map<LocalDate, Throwable> = emptyMap()

    override suspend fun commit(
        ownerId: String,
        local: HandBrewRecordEntity,
    ): RemoteHandBrewRecord {
        poisonDates[local.localDate]?.let { throw it }
        onCommit?.invoke()
        val current = server[local.localDate]
        if (current != null && local.remoteRevision != current.revision) {
            // Optimistic concurrency conflict: return the server version.
            return current
        }
        val committed = RemoteHandBrewRecord(
            id = current?.id ?: local.id,
            localDate = local.localDate,
            brewCount = local.brewCount,
            createdAt = current?.createdAt ?: local.createdAt,
            clientUpdatedAt = local.updatedAt,
            deleted = local.isDeleted,
            revision = (current?.revision ?: 0) + 1,
        )
        server[local.localDate] = committed
        return committed
    }

    override fun matches(remote: RemoteHandBrewRecord, local: HandBrewRecordEntity): Boolean =
        remote.localDate == local.localDate &&
            remote.brewCount == local.brewCount &&
            remote.clientUpdatedAt == local.updatedAt &&
            remote.deleted == local.isDeleted

    suspend fun deleteAll(ownerId: String) {
        server.clear()
    }

    private fun fetchSnapshot(rejected: Int = 0) = RemoteSnapshot(
        sourceCollection = collectionName,
        records = server.values.toList(),
        fromCache = false,
        rejectedRecordCount = rejected,
    )
}

internal fun RemoteHandBrewRecord.toEntity(): HandBrewRecordEntity = HandBrewRecordEntity(
    id = id,
    localDate = localDate,
    ownerId = "owner",
    brewCount = brewCount,
    createdAt = createdAt,
    updatedAt = clientUpdatedAt,
    isDeleted = deleted,
    syncState = SYNCED,
    remoteRevision = revision,
)
