package io.github.litaog.dailyrecord.core.sync

import io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity
import io.github.litaog.dailyrecord.core.database.SYNCED
import io.github.litaog.dailyrecord.core.database.SYNC_PENDING
import java.io.IOException
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyCountSyncCommitConflictTest {
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

}
