package io.github.litaog.dailyrecord.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.litaog.dailyrecord.core.data.RoomHandBrewRecordRepository
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HandBrewSyncCoordinatorTest {
    private val databases = mutableListOf<DailyRecordDatabase>()
    private val ownerId = "test-user"
    private val date = LocalDate.of(2026, 7, 16)
    private val firstInstant = Instant.parse("2026-07-16T08:00:00Z")

    @After
    fun tearDown() {
        databases.forEach(DailyRecordDatabase::close)
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

    @Test
    fun twoDevicesRestoreEditsAndDeletesForSameAccount() = runBlocking {
        val remote = FakeRemoteDataSource()
        val firstDatabase = database()
        val secondDatabase = database()
        val firstRepository = repository(firstDatabase, firstInstant)
        val secondRepository = repository(secondDatabase, firstInstant.plusSeconds(5))
        val firstCoordinator = coordinator(firstDatabase, remote)
        val secondCoordinator = coordinator(secondDatabase, remote)

        firstRepository.saveRecord(record(3, firstInstant))
        assertEquals(1, firstCoordinator.syncOnce(ownerId).uploaded)
        secondCoordinator.syncOnce(ownerId)
        assertEquals(3, secondRepository.observeRecord(date).first()?.brewCount)

        assertTrue(secondRepository.clearRecord(date))
        secondCoordinator.syncOnce(ownerId)
        firstCoordinator.syncOnce(ownerId)
        assertNull(firstRepository.observeRecord(date).first())
    }

    @Test
    fun newerClientEditWinsOfflineConflictAndConverges() = runBlocking {
        val remote = FakeRemoteDataSource()
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

        assertEquals(4, firstRepository.observeRecord(date).first()?.brewCount)
        assertEquals(4, secondRepository.observeRecord(date).first()?.brewCount)
        assertEquals(4, remote.fetch(ownerId).records.single().brewCount)
    }

    @Test
    fun offlineStatusPreventsNetworkAttemptAndReconnectFlushesPendingRecord() = runBlocking {
        val remote = FakeRemoteDataSource()
        val database = database()
        val repository = repository(database, firstInstant)
        val network = MutableStateFlow(false)
        val manager = AccountSyncManager(
            ownerId = ownerId,
            coordinator = coordinator(database, remote),
            productionConfigured = true,
            networkAvailable = network,
        )
        repository.saveRecord(record(3, firstInstant))

        manager.syncNow()
        assertEquals(SyncStatus.Offline, manager.status.value)
        assertEquals(0, remote.fetchCalls)

        val jobs = manager.start(this)
        network.value = true
        kotlinx.coroutines.withTimeout(5_000) {
            manager.status.first { it == SyncStatus.UpToDate }
        }
        assertTrue(remote.fetchCalls > 0)
        assertEquals(3, remote.fetch(ownerId).records.single().brewCount)
        jobs.forEach { it.cancel() }
    }

    @Test
    fun realtimeListenerRetriesAfterTransientFailure() = runBlocking {
        val remote = FakeRemoteDataSource(failFirstObservation = true)
        val database = database()
        val manager = AccountSyncManager(
            ownerId = ownerId,
            coordinator = coordinator(database, remote),
            productionConfigured = true,
            remoteRetryDelayMillis = { 0L },
        )

        val jobs = manager.start(this)
        kotlinx.coroutines.withTimeout(5_000) {
            remote.observationAttempts.first { it >= 2 }
        }

        assertTrue(remote.observationAttempts.value >= 2)
        jobs.forEach { it.cancel() }
    }

    @Test
    fun localRecordsCreatedAfterEarlierLoginMergeIntoNextSignedInAccount() = runBlocking {
        val remote = FakeRemoteDataSource()
        val database = database()
        val accountRepository = repository(database, firstInstant)
        val accountCoordinator = coordinator(database, remote)
        accountRepository.saveRecord(record(1, firstInstant))
        accountCoordinator.syncOnce(ownerId)

        val localRepository = RoomHandBrewRecordRepository(
            database = database,
            ownerId = io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID,
            clock = Clock.fixed(firstInstant.plusSeconds(30), ZoneOffset.UTC),
        )
        localRepository.saveRecord(
            record(6, firstInstant.plusSeconds(30)).copy(id = "local-${date}"),
        )

        val result = accountCoordinator.syncOnce(ownerId)
        assertEquals(1, result.uploaded)
        assertEquals(6, accountRepository.observeRecord(date).first()?.brewCount)
        assertEquals(6, remote.fetch(ownerId).records.single().brewCount)
        assertTrue(
            RoomHandBrewSyncStore(database)
                .pending(io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID)
                .isEmpty(),
        )
    }

    private fun database(): DailyRecordDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, DailyRecordDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            .also(databases::add)
    }

    private fun repository(database: DailyRecordDatabase, now: Instant) = RoomHandBrewRecordRepository(
        database = database,
        ownerId = ownerId,
        clock = Clock.fixed(now, ZoneOffset.UTC),
    )

    private fun coordinator(database: DailyRecordDatabase, remote: FakeRemoteDataSource) =
        HandBrewSyncCoordinator(RoomHandBrewSyncStore(database), remote)

    private fun record(count: Int, updatedAt: Instant) = HandBrewRecord(
        id = "record-${date}",
        localDate = date,
        brewCount = count,
        createdAt = firstInstant,
        updatedAt = updatedAt,
    )

    private fun HandBrewRecordEntity.asRemote(revision: Long) = RemoteHandBrewRecord(
        id = id,
        localDate = localDate,
        brewCount = brewCount,
        createdAt = createdAt,
        clientUpdatedAt = updatedAt,
        deleted = isDeleted,
        revision = revision,
    )
}

private class FakeRemoteDataSource(
    private val failFirstObservation: Boolean = false,
) : HandBrewRemoteDataSource {
    private val mutex = Mutex()
    private val values = MutableStateFlow<Map<LocalDate, RemoteHandBrewRecord>>(emptyMap())
    val observationAttempts = MutableStateFlow(0)
    var fetchCalls: Int = 0
        private set

    override fun observe(ownerId: String): Flow<RemoteSnapshot> = flow {
        val attempt = observationAttempts.value + 1
        observationAttempts.value = attempt
        if (failFirstObservation && attempt == 1) {
            throw IOException("temporary listener failure")
        }
        emitAll(values.map { RemoteSnapshot(it.values.toList(), fromCache = false) })
    }

    override suspend fun fetch(ownerId: String): RemoteSnapshot {
        fetchCalls += 1
        return RemoteSnapshot(values.value.values.toList(), fromCache = false)
    }

    override suspend fun commit(ownerId: String, local: HandBrewRecordEntity): RemoteHandBrewRecord =
        mutex.withLock {
            val current = values.value[local.localDate]
            if (current != null && !local.updatedAt.isAfter(current.clientUpdatedAt)) {
                return@withLock current
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
            values.value = values.value + (local.localDate to committed)
            committed
        }
}
