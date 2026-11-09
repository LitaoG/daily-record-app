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

class DailyCountSyncSnapshotCollectionTest {
    private val owner = "owner"

    private val date = LocalDate.of(2026, 7, 17)

    private val t0 = Instant.parse("2026-07-17T00:00:00Z")

    @Test
    fun predicateOnlyCollectsConfirmedCloudBackedRows() {
        assertTrue(shouldGcLocallyAbsentRow(SYNCED, 1))
        assertFalse(shouldGcLocallyAbsentRow(SYNC_PENDING, 5))
        assertFalse(shouldGcLocallyAbsentRow(SYNCED, 0))
    }

    @Test
    fun absentCloudRowsAreCollectedOnlyFromCompleteServerSnapshots() = runSync {
        val store = FakeStore()
        val remote = FakeRemote()
        val engine = DailyCountSyncEngine(store, remote)
        store.rows[date] = entity(brewCount = 2, updatedAt = t0, remoteRevision = 3)
            .copy(syncState = SYNCED)
        val pendingDate = date.plusDays(1)
        store.rows[pendingDate] = entity(localDate = pendingDate, brewCount = 1, updatedAt = t0, remoteRevision = 0)
        val neverUploadedDate = date.plusDays(2)
        store.rows[neverUploadedDate] = entity(
            localDate = neverUploadedDate,
            brewCount = 1,
            updatedAt = t0,
            remoteRevision = 0,
        ).copy(syncState = SYNCED)

        engine.applySnapshot(
            owner,
            RemoteSnapshot(
                sourceCollection = HAND_BREW_RECORDS_COLLECTION,
                records = emptyList(),
                fromCache = false,
                rejectedRecordCount = 0,
            ),
        )

        assertFalse(store.rows.containsKey(date))
        assertTrue(store.rows.containsKey(pendingDate))
        assertTrue(store.rows.containsKey(neverUploadedDate))
    }

    @Test
    fun cacheAndRejectedSnapshotsNeverCollectLocalMirrors() = runSync {
        val store = FakeStore()
        val remote = FakeRemote()
        val engine = DailyCountSyncEngine(store, remote)
        store.rows[date] = entity(brewCount = 2, updatedAt = t0, remoteRevision = 3)
            .copy(syncState = SYNCED)

        engine.applySnapshot(
            owner,
            RemoteSnapshot(
                sourceCollection = HAND_BREW_RECORDS_COLLECTION,
                records = emptyList(),
                fromCache = true,
                rejectedRecordCount = 0,
            ),
        )
        assertTrue(store.rows.containsKey(date))

        engine.applySnapshot(
            owner,
            RemoteSnapshot(
                sourceCollection = HAND_BREW_RECORDS_COLLECTION,
                records = emptyList(),
                fromCache = false,
                rejectedRecordCount = 1,
            ),
        )
        assertTrue(store.rows.containsKey(date))
    }

    @Test
    fun completeSnapshotOfTheSiblingCollectionNeverCollectsThisModuleRows() = runSync {
        val store = FakeStore()
        val remote = FakeRemote()
        val engine = DailyCountSyncEngine(store, remote)
        store.rows[date] = entity(brewCount = 2, updatedAt = t0, remoteRevision = 3)
            .copy(syncState = SYNCED)

        val changed = engine.applySnapshot(
            owner,
            RemoteSnapshot(
                sourceCollection = SEX_RECORDS_COLLECTION,
                records = emptyList(),
                fromCache = false,
                rejectedRecordCount = 0,
            ),
        )

        assertEquals(0, changed)
        assertTrue(store.rows.containsKey(date))
    }

    @Test
    fun dataPoisonDateIsQuarantinedWhileOtherDatesStillUpload() = runSync {
        val store = FakeStore()
        val remote = FakeRemote()
        val engine = DailyCountSyncEngine(store, remote)
        val goodDate = date.plusDays(1)
        store.rows[date] = entity(brewCount = 1, updatedAt = t0, remoteRevision = 0)
        store.rows[goodDate] = entity(
            localDate = goodDate,
            brewCount = 2,
            updatedAt = t0.plusSeconds(1),
            remoteRevision = 0,
        )
        // The server refuses the first date with a data error (for example a
        // detail the callable rejects); the rest of the queue must proceed.
        remote.poisonDates = mapOf(
            date to ClassifiedSyncException(SyncFailureKind.Data, IllegalArgumentException("bad details")),
        )

        val result = engine.syncOnce(owner)

        assertEquals(1, result.uploaded)
        assertEquals(1, result.quarantinedLocalRecords)
        assertEquals(0, result.rejectedRemoteRecords)
        assertEquals(SYNCED, requireNotNull(store.rows[goodDate]).syncState)
        // The poisoned row stays pending for an explicit user retry instead of
        // wedging the module or being dropped silently.
        assertEquals(SYNC_PENDING, requireNotNull(store.rows[date]).syncState)
        assertEquals(2, remote.fetchCalls)
    }

    @Test
    fun confirmedSnapshotRefreshesInitialRejectedCountButKeepsPoison() = runSync {
        val store = FakeStore()
        val remote = FakeRemote()
        val engine = DailyCountSyncEngine(store, remote)
        store.rows[date] = entity(brewCount = 1, updatedAt = t0, remoteRevision = 0)
        remote.poisonDates = mapOf(
            date to ClassifiedSyncException(SyncFailureKind.Data, IllegalArgumentException("bad details")),
        )
        // A trigger cleans the two initially malformed documents between the
        // two reads; the locally quarantined date is counted separately.
        remote.rejectedPerFetch = listOf(2, 0)

        val result = engine.syncOnce(owner)

        assertEquals(0, result.rejectedRemoteRecords)
        assertEquals(1, result.quarantinedLocalRecords)
        assertEquals(0, result.uploaded)
        assertEquals(SYNC_PENDING, requireNotNull(store.rows[date]).syncState)
    }

    @Test
    fun nonDataCommitFailureStillAbortsTheModuleAttempt() = runSync {
        val store = FakeStore()
        val remote = FakeRemote()
        val engine = DailyCountSyncEngine(store, remote)
        store.rows[date] = entity(brewCount = 1, updatedAt = t0, remoteRevision = 0)
        remote.poisonDates = mapOf(
            date to ClassifiedSyncException(SyncFailureKind.Network, IOException("offline")),
        )

        val result = runCatching { engine.syncOnce(owner) }

        assertTrue(result.exceptionOrNull() is ClassifiedSyncException)
        assertEquals(SYNC_PENDING, requireNotNull(store.rows[date]).syncState)
    }

}
