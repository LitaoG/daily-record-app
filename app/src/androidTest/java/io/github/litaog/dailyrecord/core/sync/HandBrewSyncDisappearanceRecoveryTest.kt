package io.github.litaog.dailyrecord.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.litaog.dailyrecord.core.data.RoomHandBrewRecordRepository
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.HandBrewRecordDetail
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)

internal class HandBrewSyncDisappearanceRecoveryTest : HandBrewSyncCoordinatorTestBase() {

    @Test
    fun confirmedRowStopsDisplayingAfterPhysicalCloudDisappearance() = runBlocking {
        val remote = FakeRemoteDataSource()
        val database = database()
        val repository = repository(database, firstInstant)
        val coordinator = coordinator(database, remote)

        repository.saveRecord(record(3, firstInstant))
        coordinator.syncOnce(ownerId)
        assertEquals(3, repository.observeRecord(date).first()?.brewCount)

        // Trusted account-data cleanup or trigger cleanup physically removes
        // the document while this device has no pending edit. A fresh complete
        // server snapshot is authoritative about absence (ADR-020): the
        // confirmed local mirror must stop being displayed.
        remote.removeRemote(date)
        coordinator.applySnapshot(ownerId, remote.fetch(ownerId))

        assertNull(repository.observeRecord(date).first())
    }

    @Test
    fun pendingEditRecreatesCloudRecordAfterRemoteDocumentDisappears() = runBlocking {        val remote = FakeRemoteDataSource()
        val database = database()
        val repository = repository(database, firstInstant)
        val coordinator = coordinator(database, remote)

        // Device A syncs a record so the local row carries a remote revision.
        repository.saveRecord(record(1, firstInstant))
        assertEquals(1, coordinator.syncOnce(ownerId).uploaded)
        assertEquals(1, repository.observeRecord(date).first()?.brewCount)

        // The cloud document disappears (account data cleanup on another
        // device), while this device keeps editing the same date offline.
        remote.removeRemote(date)
        repository.saveRecord(record(2, firstInstant.plusSeconds(30)))
        val pending = RoomHandBrewSyncStore(database).pending(ownerId).single()
        assertEquals(1L, pending.remoteRevision)
        assertEquals(2, pending.brewCount)

        // The next sync must recreate the document from the pending edit
        // instead of failing permanently on the stale revision baseline.
        val result = coordinator.syncOnce(ownerId)

        assertEquals(1, result.uploaded)
        assertEquals(0, result.pending)
        val cloud = remote.fetch(ownerId).records.filterIsInstance<RemoteHandBrewRecord>().single()
        assertEquals(2, cloud.brewCount)
        // The recreated document restarts its revision count at 1 (ADR-017).
        assertEquals(1L, cloud.revision)
        // The local row is confirmed against the recreated document.
        assertEquals(2, repository.observeRecord(date).first()?.brewCount)
        assertEquals(0, RoomHandBrewSyncStore(database).pendingCount(ownerId))
    }

    @Test
    fun accountSyncManagerRecoversPendingEditAfterRemoteDocumentDisappears() = runBlocking {
        val remote = FakeRemoteDataSource()
        val database = database()
        val repository = repository(database, firstInstant)
        val manager = AccountSyncManager(
            ownerId = ownerId,
            coordinator = coordinator(database, remote),
            productionConfigured = true,
            networkAvailable = MutableStateFlow(true),
        )

        repository.saveRecord(record(1, firstInstant))
        manager.syncNow()
        remote.removeRemote(date)
        repository.saveRecord(record(5, firstInstant.plusSeconds(30)))

        manager.syncNow()

        assertEquals(SyncStatus.UpToDate, manager.status.value)
        assertEquals(5, repository.observeRecord(date).first()?.brewCount)
        assertEquals(0, RoomHandBrewSyncStore(database).pendingCount(ownerId))
    }

    @Test
    fun physicalDisappearanceKeepsAnExplicitClearAsATombstone() = runBlocking {
        val remote = FakeRemoteDataSource()
        val database = database()
        val repository = repository(database, firstInstant)
        val coordinator = coordinator(database, remote)

        repository.saveRecord(record(3, firstInstant))
        coordinator.syncOnce(ownerId)
        assertTrue(repository.clearRecord(date))
        remote.removeRemote(date)

        coordinator.syncOnce(ownerId)

        val cloud = remote.fetch(ownerId).records.filterIsInstance<RemoteHandBrewRecord>().single()
        assertTrue(cloud.deleted)
        assertNull(repository.observeRecord(date).first())
        assertEquals(0, RoomHandBrewSyncStore(database).pendingCount(ownerId))
    }

    @Test
    fun syncedPeerAcceptsRecreatedCloudGenerationWithRestartedRevision() = runBlocking {
        val remote = FakeRemoteDataSource()
        val firstDatabase = database()
        val secondDatabase = database()
        val firstRepository = repository(firstDatabase, firstInstant)
        val secondRepository = repository(secondDatabase, firstInstant.plusSeconds(30))
        val firstCoordinator = coordinator(firstDatabase, remote)
        val secondCoordinator = coordinator(secondDatabase, remote)

        firstRepository.saveRecord(record(1, firstInstant))
        firstCoordinator.syncOnce(ownerId)
        secondCoordinator.syncOnce(ownerId)
        val originalId = firstRepository.observeRecord(date).first()!!.id

        remote.removeRemote(date)
        secondRepository.saveRecord(record(7, firstInstant.plusSeconds(60)))
        secondCoordinator.syncOnce(ownerId)

        val recreated = remote.fetch(ownerId).records.filterIsInstance<RemoteHandBrewRecord>().single()
        assertNotEquals(originalId, recreated.id)
        assertEquals(1L, recreated.revision)

        // Device A has no pending edit, but must accept the new generation
        // even though its numeric revision is equal to the old revision.
        firstCoordinator.syncOnce(ownerId)
        val restored = firstRepository.observeRecord(date).first()!!
        assertEquals(7, restored.brewCount)
        assertEquals(recreated.id, restored.id)
    }

    @Test
    fun staleBaselineStillLosesToConcurrentRemoteEditDuringRecreate() = runBlocking {
        val remote = FakeRemoteDataSource()
        val firstDatabase = database()
        val secondDatabase = database()
        val firstRepository = repository(firstDatabase, firstInstant)
        val secondRepository = repository(secondDatabase, firstInstant.plusSeconds(30))
        val firstCoordinator = coordinator(firstDatabase, remote)
        val secondCoordinator = coordinator(secondDatabase, remote)

        firstRepository.saveRecord(record(1, firstInstant))
        firstCoordinator.syncOnce(ownerId)
        secondCoordinator.syncOnce(ownerId)

        // Cloud document disappears; the second device recreates it (rev 1),
        // then edits again so the server revision moves past the first
        // device's stale baseline (rev 1).
        remote.removeRemote(date)
        secondRepository.saveRecord(record(9, firstInstant.plusSeconds(60)))
        secondCoordinator.syncOnce(ownerId)
        secondRepository.saveRecord(record(10, firstInstant.plusSeconds(61)))
        secondCoordinator.syncOnce(ownerId)
        assertEquals(2L, remote.fetch(ownerId).records.filterIsInstance<RemoteHandBrewRecord>().single().revision)

        firstRepository.saveRecord(record(2, firstInstant.plusSeconds(90)))
        firstCoordinator.syncOnce(ownerId)

        // The recreated server document has moved past the stale baseline, so
        // the server value wins; no device clock is involved.
        val cloud = remote.fetch(ownerId).records.filterIsInstance<RemoteHandBrewRecord>().single()
        assertEquals(10, cloud.brewCount)
        assertEquals(10, firstRepository.observeRecord(date).first()?.brewCount)
    }

    @Test
    fun clearCreatesPendingTombstoneAndStaleAckCannotHideNewEdit() = runBlocking {
        val database = database()
        val repository = repository(database, firstInstant)
        val store = RoomHandBrewSyncStore(database)
        repository.saveRecord(record(1, firstInstant))
        val firstPending = store.pending(ownerId).single()

        repository(database, firstInstant.plusSeconds(1)).saveRecord(record(2, firstInstant.plusSeconds(1)))
        assertFalse(
            store.applyCommitIfUnchanged(
                ownerId,
                firstPending,
                firstPending.asRemote(revision = 1),
            ),
        )
        assertEquals(2, store.pending(ownerId).single().brewCount)

        assertTrue(repository(database, firstInstant.plusSeconds(2)).clearRecord(date))
        assertNull(repository.observeRecord(date).first())
        val tombstone = store.pending(ownerId).single()
        assertTrue(tombstone.isDeleted)
        assertEquals(2, tombstone.brewCount)
        assertFalse(repository.clearRecord(date))
    }

}
