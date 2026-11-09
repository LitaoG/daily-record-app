package io.github.litaog.dailyrecord.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.litaog.dailyrecord.core.data.RoomSexRecordRepository
import io.github.litaog.dailyrecord.core.data.SexRecordRepository
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.core.database.SexRecordEntity
import io.github.litaog.dailyrecord.core.model.SexRecord
import io.github.litaog.dailyrecord.core.model.SexRecordDetail
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Full mirror of the hand-brew sync-coordinator scenarios for the sex module
 * (#165): every scenario from [HandBrewSyncCoordinatorTest] runs against the
 * sex store, collection mapping and record type, plus the combined-module
 * isolation case.
 */
@RunWith(AndroidJUnit4::class)

internal class SexSyncAdoptionConflictTest : SexSyncCoordinatorTestBase() {

    @Test
    fun localAdoptionKeepsNewerLocalEditWhenDeviceClockIsBehind() = runBlocking {
        val remote = remote()
        val database = database()
        val accountRepository = repository(database, firstInstant.plusSeconds(60))
        val accountCoordinator = coordinator(database, remote)
        accountRepository.saveRecord(record(1, firstInstant.plusSeconds(60)))
        accountCoordinator.syncOnce(ownerId)

        // The device clock is now far behind the account cache, so comparing
        // updatedAt would wrongly discard the local edit.
        val localRepository = RoomSexRecordRepository(
            database = database,
            ownerId = LOCAL_OWNER_ID,
            clock = Clock.fixed(firstInstant, ZoneOffset.UTC),
        )
        localRepository.saveRecord(record(5, firstInstant).copy(id = "local-$date"))

        val store = RoomSexSyncStore(database)
        assertEquals(1, store.adoptLocalRecords(ownerId))

        val adopted = store.pending(ownerId).single()
        assertEquals(5, adopted.sexCount)
        assertEquals(io.github.litaog.dailyrecord.core.database.SYNC_PENDING, adopted.syncState)
        assertEquals(1L, adopted.remoteRevision)

        // The server revision still matches the adopted baseline, so the local
        // edit uploads instead of being silently dropped.
        val result = accountCoordinator.syncOnce(ownerId)
        assertEquals(1, result.uploaded)
        assertEquals(5, remote.fetch(ownerId).records.filterIsInstance<RemoteSexRecord>().single().sexCount)
    }

    @Test
    fun localAdoptionRespectsNewerServerRevisionInsteadOfDeviceClock() = runBlocking {
        val remote = remote()
        val firstDatabase = database()
        val secondDatabase = database()
        val firstRepository = repository(firstDatabase, firstInstant)
        val firstCoordinator = coordinator(firstDatabase, remote)
        firstRepository.saveRecord(record(1, firstInstant))
        firstCoordinator.syncOnce(ownerId)

        // A second device pushes a newer server revision after this device's
        // account cache was written.
        val secondRepository = repository(secondDatabase, firstInstant.plusSeconds(30))
        val secondCoordinator = coordinator(secondDatabase, remote)
        secondRepository.saveRecord(record(9, firstInstant.plusSeconds(30)))
        secondCoordinator.syncOnce(ownerId)

        // This device then edits in local mode with a clock behind the account cache.
        val localRepository = RoomSexRecordRepository(
            database = firstDatabase,
            ownerId = LOCAL_OWNER_ID,
            clock = Clock.fixed(firstInstant, ZoneOffset.UTC),
        )
        localRepository.saveRecord(record(5, firstInstant).copy(id = "local-$date"))
        val store = RoomSexSyncStore(firstDatabase)
        assertEquals(1, store.adoptLocalRecords(ownerId))
        assertEquals(1L, store.pending(ownerId).single().remoteRevision)

        firstCoordinator.syncOnce(ownerId)

        // The server revision moved past the adopted baseline, so the server
        // value wins; the device clock plays no part in the outcome.
        assertEquals(9, firstRepository.observeRecord(date).first()?.sexCount)
        assertEquals(9, remote.fetch(ownerId).records.filterIsInstance<RemoteSexRecord>().single().sexCount)
    }

    @Test
    fun twoDevicesRestoreEditsAndDeletesForSameAccount() = runBlocking {
        val remote = remote()
        val firstDatabase = database()
        val secondDatabase = database()
        val firstRepository = repository(firstDatabase, firstInstant)
        val secondRepository = repository(secondDatabase, firstInstant.plusSeconds(5))
        val firstCoordinator = coordinator(firstDatabase, remote)
        val secondCoordinator = coordinator(secondDatabase, remote)

        firstRepository.saveRecord(record(3, firstInstant))
        assertEquals(1, firstCoordinator.syncOnce(ownerId).uploaded)
        secondCoordinator.syncOnce(ownerId)
        assertEquals(3, secondRepository.observeRecord(date).first()?.sexCount)

        assertTrue(secondRepository.clearRecord(date))
        secondCoordinator.syncOnce(ownerId)
        firstCoordinator.syncOnce(ownerId)
        assertNull(firstRepository.observeRecord(date).first())
    }

    @Test
    fun newerClientEditWinsOfflineConflictAndConverges() = runBlocking {
        val remote = remote()
        val firstDatabase = database()
        val secondDatabase = database()
        val firstRepository = repository(firstDatabase, firstInstant)
        val secondRepository = repository(secondDatabase, firstInstant)
        val firstCoordinator = coordinator(firstDatabase, remote)
        val secondCoordinator = coordinator(secondDatabase, remote)

        firstRepository.saveRecord(record(1, firstInstant))
        firstCoordinator.syncOnce(ownerId)
        secondCoordinator.syncOnce(ownerId)

        firstRepository.saveRecord(record(2, firstInstant.plusSeconds(10)))
        secondRepository.saveRecord(record(4, firstInstant.plusSeconds(20)))
        secondCoordinator.syncOnce(ownerId)
        firstCoordinator.syncOnce(ownerId)
        secondCoordinator.syncOnce(ownerId)

        assertEquals(4, firstRepository.observeRecord(date).first()?.sexCount)
        assertEquals(4, secondRepository.observeRecord(date).first()?.sexCount)
        assertEquals(4, remote.fetch(ownerId).records.filterIsInstance<RemoteSexRecord>().single().sexCount)
    }

    @Test
    fun remoteRevisionRejectsStaleBaseEvenWhenItsDeviceClockIsFarAhead() = runBlocking {
        val remote = remote()
        val firstDatabase = database()
        val secondDatabase = database()
        val firstRepository = repository(firstDatabase, firstInstant)
        val secondRepository = repository(secondDatabase, firstInstant)
        val firstCoordinator = coordinator(firstDatabase, remote)
        val secondCoordinator = coordinator(secondDatabase, remote)

        firstRepository.saveRecord(record(1, firstInstant))
        firstCoordinator.syncOnce(ownerId)
        secondCoordinator.syncOnce(ownerId)

        firstRepository.saveRecord(record(9, firstInstant.plusSeconds(3_600)))
        secondRepository.saveRecord(record(2, firstInstant.plusSeconds(10)))
        secondCoordinator.syncOnce(ownerId)
        firstCoordinator.syncOnce(ownerId)
        secondCoordinator.syncOnce(ownerId)

        assertEquals(2, firstRepository.observeRecord(date).first()?.sexCount)
        assertEquals(2, secondRepository.observeRecord(date).first()?.sexCount)
        assertEquals(2, remote.fetch(ownerId).records.filterIsInstance<RemoteSexRecord>().single().sexCount)
    }

    @Test
    fun localRecordsCreatedAfterEarlierLoginMergeIntoNextSignedInAccount() = runBlocking {
        val remote = remote()
        val database = database()
        val accountRepository = repository(database, firstInstant)
        val accountCoordinator = coordinator(database, remote)
        accountRepository.saveRecord(record(1, firstInstant))
        accountCoordinator.syncOnce(ownerId)

        val localRepository = RoomSexRecordRepository(
            database = database,
            ownerId = LOCAL_OWNER_ID,
            clock = Clock.fixed(firstInstant.plusSeconds(30), ZoneOffset.UTC),
        )
        localRepository.saveRecord(
            record(6, firstInstant.plusSeconds(30)).copy(id = "local-${date}"),
        )

        val result = accountCoordinator.syncOnce(ownerId)
        assertEquals(1, result.uploaded)
        assertEquals(6, accountRepository.observeRecord(date).first()?.sexCount)
        assertEquals(6, remote.fetch(ownerId).records.filterIsInstance<RemoteSexRecord>().single().sexCount)
        assertTrue(
            RoomSexSyncStore(database)
                .pending(LOCAL_OWNER_ID)
                .isEmpty(),
        )
    }

    @Test
    fun firstLoginAdoptsLocalRecordsBeforeAnyRemoteRequest() = runBlocking {
        val remote = remote(failFetchAttempts = Int.MAX_VALUE)
        val database = database()
        val localRepository = RoomSexRecordRepository(
            database = database,
            ownerId = LOCAL_OWNER_ID,
            clock = Clock.fixed(firstInstant, ZoneOffset.UTC),
        )
        val accountRepository = repository(database, firstInstant)
        val coordinator = coordinator(database, remote)
        localRepository.saveRecord(record(4, firstInstant).copy(id = "local-$date"))

        assertEquals(1, coordinator.prepareLocalAccount(ownerId))
        assertEquals(4, accountRepository.observeRecord(date).first()?.sexCount)
        assertTrue(
            RoomSexSyncStore(database)
                .pending(LOCAL_OWNER_ID)
                .isEmpty(),
        )
        assertTrue(runCatching { coordinator.syncOnce(ownerId) }.isFailure)
        assertEquals(4, accountRepository.observeRecord(date).first()?.sexCount)
    }

}
