package io.github.litaog.dailyrecord.core.sync

import io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity
import io.github.litaog.dailyrecord.core.database.SYNC_PENDING
import io.github.litaog.dailyrecord.core.database.SYNCED
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyCountSyncEngineTest {
    private val owner = "owner"
    private val date = LocalDate.of(2026, 7, 17)
    private val t0 = Instant.parse("2026-07-17T00:00:00Z")

    @Test
    fun cleanSyncDoesNotPerformASecondFullFetch() = runSync {
        val store = FakeStore()
        val remote = FakeRemote()
        val engine = DailyCountSyncEngine(store, remote)
        store.rows[date] = entity(
            brewCount = 1,
            updatedAt = t0,
            remoteRevision = 5,
        ).copy(syncState = SYNCED)
        remote.server[date] = remoteOf(brewCount = 1, clientUpdatedAt = t0, revision = 5)

        engine.syncOnce(owner)

        assertEquals(1, remote.fetchCalls)
    }

    @Test
    fun inFlightEditSurvivesItsOwnEarlierCommit() = runSync {
        val store = FakeStore()
        val remote = FakeRemote()
        val engine = DailyCountSyncEngine(store, remote)
        store.rows[date] = entity(brewCount = 1, updatedAt = t0, remoteRevision = 5)
        remote.server[date] = remoteOf(brewCount = 1, clientUpdatedAt = t0, revision = 5)

        // The user edits the day while the commit request is in flight.
        remote.onCommit = {
            store.rows[date] = entity(brewCount = 3, updatedAt = t0.plusSeconds(10), remoteRevision = 5)
        }

        val first = engine.syncOnce(owner)
        val afterFirst = requireNotNull(store.rows[date])
        // The newer edit stays pending and is re-based on the confirmed revision.
        assertEquals(3, afterFirst.brewCount)
        assertEquals(SYNC_PENDING, afterFirst.syncState)
        assertEquals(6, afterFirst.remoteRevision)
        assertEquals(1, first.pending)

        remote.onCommit = null
        val second = engine.syncOnce(owner)
        val afterSecond = requireNotNull(store.rows[date])
        assertEquals(3, afterSecond.brewCount)
        assertEquals(SYNCED, afterSecond.syncState)
        assertEquals(7, afterSecond.remoteRevision)
        assertEquals(0, second.pending)
        assertEquals(1, second.uploaded)
        // The server must end up with the newer local edit, not the stale v1.
        assertEquals(3, requireNotNull(remote.server[date]).brewCount)
        assertEquals(7, requireNotNull(remote.server[date]).revision)
    }

    @Test
    fun foreignServerVersionWinsWhenLocalRowIsUnchanged() = runSync {
        val store = FakeStore()
        val remote = FakeRemote()
        val engine = DailyCountSyncEngine(store, remote)
        store.rows[date] = entity(brewCount = 1, updatedAt = t0, remoteRevision = 5)
        // Another device already advanced the server to rev 6 with count 5.
        remote.server[date] = remoteOf(brewCount = 5, clientUpdatedAt = t0.plusSeconds(5), revision = 6)

        val result = engine.syncOnce(owner)

        val row = requireNotNull(store.rows[date])
        assertEquals(SYNCED, row.syncState)
        assertEquals(5, row.brewCount)
        assertEquals(6, row.remoteRevision)
        assertEquals(1, result.downloaded)
        assertEquals(0, result.pending)
    }

    @Test
    fun foreignConflictWithInFlightEditStaysServerPriorityAndIsNotRebased() = runSync {
        val store = FakeStore()
        val remote = FakeRemote()
        val engine = DailyCountSyncEngine(store, remote)
        store.rows[date] = entity(brewCount = 1, updatedAt = t0, remoteRevision = 5)
        remote.server[date] = remoteOf(brewCount = 5, clientUpdatedAt = t0.plusSeconds(5), revision = 6)

        // The user edits locally while the conflicting commit is in flight.
        remote.onCommit = {
            store.rows[date] = entity(brewCount = 3, updatedAt = t0.plusSeconds(10), remoteRevision = 5)
        }

        val first = engine.syncOnce(owner)
        val afterFirst = requireNotNull(store.rows[date])
        // A foreign revision must never be adopted as the baseline for a local
        // overwrite: the row stays pending without a re-base (matches=false).
        assertEquals(3, afterFirst.brewCount)
        assertEquals(SYNC_PENDING, afterFirst.syncState)
        assertEquals(5, afterFirst.remoteRevision)

        remote.onCommit = null
        val second = engine.syncOnce(owner)
        val afterSecond = requireNotNull(store.rows[date])
        // The server version is authoritative per ADR-010.
        assertEquals(SYNCED, afterSecond.syncState)
        assertEquals(5, afterSecond.brewCount)
        assertEquals(6, afterSecond.remoteRevision)
        assertEquals(5, requireNotNull(remote.server[date]).brewCount)
    }

    private fun entity(
        brewCount: Int,
        updatedAt: Instant,
        remoteRevision: Long,
    ) = HandBrewRecordEntity(
        id = "record-$date",
        localDate = date,
        ownerId = owner,
        brewCount = brewCount,
        createdAt = t0,
        updatedAt = updatedAt,
        isDeleted = false,
        syncState = SYNC_PENDING,
        remoteRevision = remoteRevision,
    )

    private fun remoteOf(
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
}

private fun runSync(block: suspend () -> Unit) = kotlinx.coroutines.runBlocking { block() }

private class FakeStore : DailyCountSyncStore<HandBrewRecordEntity, RemoteHandBrewRecord> {
    val rows = mutableMapOf<LocalDate, HandBrewRecordEntity>()

    override fun observePendingCount(ownerId: String): Flow<Int> = MutableStateFlow(rows.size)

    override suspend fun pending(ownerId: String): List<HandBrewRecordEntity> =
        rows.values.filter { it.syncState == SYNC_PENDING }.sortedBy { it.updatedAt }

    override suspend fun pendingCount(ownerId: String): Int =
        rows.values.count { it.syncState == SYNC_PENDING }

    override suspend fun adoptLocalRecords(ownerId: String): Int = 0

    override suspend fun applyRemote(ownerId: String, records: List<RemoteHandBrewRecord>): Int {
        var changed = 0
        records.forEach { remote ->
            val local = rows[remote.localDate]
            // Mirrors the production guard: pending local edits and revisions
            // at or above the remote one must never be overwritten by a fetch.
            if (local?.syncState == SYNC_PENDING ||
                (local != null && local.remoteRevision >= remote.revision)
            ) {
                return@forEach
            }
            rows[remote.localDate] = remote.toEntity()
            changed += 1
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

private class FakeRemote : DailyCountRemoteDataSource<HandBrewRecordEntity, RemoteHandBrewRecord> {
    val server = mutableMapOf<LocalDate, RemoteHandBrewRecord>()
    var fetchCalls = 0

    /** Simulates a user edit landing while the commit request is in flight. */
    var onCommit: (() -> Unit)? = null

    override fun observe(ownerId: String): Flow<RemoteSnapshot> = MutableStateFlow(fetchSnapshot())

    override suspend fun fetch(ownerId: String): RemoteSnapshot {
        fetchCalls += 1
        return fetchSnapshot()
    }

    override fun recordsFrom(snapshot: RemoteSnapshot): List<RemoteHandBrewRecord> =
        snapshot.records.filterIsInstance<RemoteHandBrewRecord>()

    override suspend fun commit(
        ownerId: String,
        local: HandBrewRecordEntity,
    ): RemoteHandBrewRecord {
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

    override suspend fun deleteAll(ownerId: String) {
        server.clear()
    }

    private fun fetchSnapshot() = RemoteSnapshot(
        records = server.values.toList(),
        fromCache = false,
    )
}

private fun RemoteHandBrewRecord.toEntity(): HandBrewRecordEntity = HandBrewRecordEntity(
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
