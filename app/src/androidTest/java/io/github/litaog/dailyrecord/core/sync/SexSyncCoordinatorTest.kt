package io.github.litaog.dailyrecord.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.litaog.dailyrecord.core.data.RoomSexRecordRepository
import io.github.litaog.dailyrecord.core.data.RoomHandBrewRecordRepository
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity
import io.github.litaog.dailyrecord.core.database.SexRecordEntity
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.SexRecord
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SexSyncCoordinatorTest {
    private val databases = mutableListOf<DailyRecordDatabase>()
    private val ownerId = "sex-sync-owner"
    private val date = LocalDate.of(2026, 7, 16)
    private val firstInstant = Instant.parse("2026-07-16T08:00:00Z")

    @After
    fun tearDown() {
        databases.forEach(DailyRecordDatabase::close)
    }

    @Test
    fun twoDevicesRestoreEditsAndTombstones() = runBlocking {
        val remote = FakeSexRemoteDataSource()
        val firstDatabase = database()
        val secondDatabase = database()
        val firstRepository = repository(firstDatabase, firstInstant)
        val secondRepository = repository(secondDatabase, firstInstant.plusSeconds(5))
        val firstCoordinator = coordinator(firstDatabase, remote)
        val secondCoordinator = coordinator(secondDatabase, remote)

        firstRepository.saveRecord(record(2, firstInstant))
        assertEquals(1, firstCoordinator.syncOnce(ownerId).uploaded)
        secondCoordinator.syncOnce(ownerId)
        assertEquals(2, secondRepository.observeRecord(date).first()?.sexCount)

        assertTrue(secondRepository.clearRecord(date))
        secondCoordinator.syncOnce(ownerId)
        firstCoordinator.syncOnce(ownerId)
        assertNull(firstRepository.observeRecord(date).first())
    }

    @Test
    fun firstLoginAdoptsLocalSexRecordBeforeRemoteAccess() = runBlocking {
        val database = database()
        val localRepository = RoomSexRecordRepository(
            database = database,
            clock = Clock.fixed(firstInstant, ZoneOffset.UTC),
        )
        val accountRepository = repository(database, firstInstant)
        val coordinator = coordinator(database, FakeSexRemoteDataSource())
        localRepository.saveRecord(record(3, firstInstant))

        assertEquals(1, coordinator.prepareLocalAccount(ownerId))
        assertEquals(3, accountRepository.observeRecord(date).first()?.sexCount)
        assertEquals(0, database.sexRecordDao().countForOwner(
            io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID,
        ))
    }

    @Test
    fun localAdoptionKeepsNewerSexEditWhenDeviceClockIsBehind() = runBlocking {
        val remote = FakeSexRemoteDataSource()
        val database = database()
        val accountRepository = repository(database, firstInstant.plusSeconds(60))
        val accountCoordinator = coordinator(database, remote)
        accountRepository.saveRecord(record(1, firstInstant.plusSeconds(60)))
        accountCoordinator.syncOnce(ownerId)

        val localRepository = RoomSexRecordRepository(
            database = database,
            ownerId = io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID,
            clock = Clock.fixed(firstInstant, ZoneOffset.UTC),
        )
        localRepository.saveRecord(record(5, firstInstant).copy(id = "local-sex-$date"))

        val store = RoomSexSyncStore(database)
        assertEquals(1, store.adoptLocalRecords(ownerId))

        val adopted = store.pending(ownerId).single()
        assertEquals(5, adopted.sexCount)
        assertEquals(io.github.litaog.dailyrecord.core.database.SYNC_PENDING, adopted.syncState)
        assertEquals(1L, adopted.remoteRevision)

        val result = accountCoordinator.syncOnce(ownerId)
        assertEquals(1, result.uploaded)
        assertEquals(5, remote.fetch(ownerId).sexRecords.single().sexCount)
    }

    @Test
    fun pendingSexEditRecreatesCloudRecordAfterRemoteDocumentDisappears() = runBlocking {
        val remote = FakeSexRemoteDataSource()
        val database = database()
        val sexRepository = repository(database, firstInstant)
        val sexCoordinator = coordinator(database, remote)

        // Device A syncs a sex record so the local row carries a remote revision.
        sexRepository.saveRecord(record(1, firstInstant))
        assertEquals(1, sexCoordinator.syncOnce(ownerId).uploaded)
        assertEquals(1, sexRepository.observeRecord(date).first()?.sexCount)

        // The cloud document disappears, while this device keeps editing
        // the same date offline.
        remote.removeRemote(date)
        sexRepository.saveRecord(record(2, firstInstant.plusSeconds(30)))
        val pending = RoomSexSyncStore(database).pending(ownerId).single()
        assertEquals(1L, pending.remoteRevision)
        assertEquals(2, pending.sexCount)

        // The next sync must recreate the document instead of failing
        // permanently on the stale revision baseline.
        val result = sexCoordinator.syncOnce(ownerId)

        assertEquals(1, result.uploaded)
        assertEquals(0, result.pending)
        val cloud = remote.fetch(ownerId).sexRecords.single()
        assertEquals(2, cloud.sexCount)
        // The recreated document restarts its revision count at 1 (ADR-017).
        assertEquals(1L, cloud.revision)
        assertEquals(2, sexRepository.observeRecord(date).first()?.sexCount)
        assertEquals(0, RoomSexSyncStore(database).pendingCount(ownerId))
    }

    @Test
    fun syncedPeerAcceptsRecreatedCloudGenerationWithRestartedRevision() = runBlocking {
        val remote = FakeSexRemoteDataSource()
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
        secondRepository.saveRecord(record(6, firstInstant.plusSeconds(60)))
        secondCoordinator.syncOnce(ownerId)

        val recreated = remote.fetch(ownerId).sexRecords.single()
        assertNotEquals(originalId, recreated.id)
        assertEquals(1L, recreated.revision)

        // Device A has no pending edit, but must accept the new generation
        // even though its numeric revision is equal to the old revision.
        firstCoordinator.syncOnce(ownerId)
        val restored = firstRepository.observeRecord(date).first()!!
        assertEquals(6, restored.sexCount)
        assertEquals(recreated.id, restored.id)
    }

    @Test
    fun combinedCoordinatorUploadsAndCountsBothModulesWithoutMixingThem() = runBlocking {
        val database = database()
        val handRemote = FakeHandBrewRemoteForCombined()
        val sexRemote = FakeSexRemoteDataSource()
        val handRepository = RoomHandBrewRecordRepository(database, ownerId)
        val sexRepository = repository(database, firstInstant)
        handRepository.saveRecord(
            HandBrewRecord("brew-$date", date, 4, firstInstant, firstInstant),
        )
        sexRepository.saveRecord(record(2, firstInstant))
        val coordinator = CombinedSyncCoordinator(
            HandBrewSyncCoordinator(RoomHandBrewSyncStore(database), handRemote),
            SexSyncCoordinator(RoomSexSyncStore(database), sexRemote),
        )

        val result = coordinator.syncOnce(ownerId)

        assertEquals(2, result.uploaded)
        assertEquals(0, result.pending)
        assertEquals(4, handRemote.fetch(ownerId).records.single().brewCount)
        assertEquals(2, sexRemote.fetch(ownerId).sexRecords.single().sexCount)

        assertTrue(sexRepository.clearRecord(date))
        coordinator.syncOnce(ownerId)
        assertEquals(4, handRepository.observeRecord(date).first()?.brewCount)
        assertNull(sexRepository.observeRecord(date).first())
    }

    private fun database(): DailyRecordDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, DailyRecordDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            .also(databases::add)
    }

    private fun repository(database: DailyRecordDatabase, now: Instant) =
        RoomSexRecordRepository(
            database = database,
            ownerId = ownerId,
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

    private fun coordinator(database: DailyRecordDatabase, remote: FakeSexRemoteDataSource) =
        SexSyncCoordinator(RoomSexSyncStore(database), remote)

    private fun record(count: Int, updatedAt: Instant) = SexRecord(
        id = "sex-$date",
        localDate = date,
        sexCount = count,
        createdAt = firstInstant,
        updatedAt = updatedAt,
    )
}

private class FakeSexRemoteDataSource : SexRemoteDataSource {
    private val mutex = Mutex()
    private val values = MutableStateFlow<Map<LocalDate, RemoteSexRecord>>(emptyMap())

    override fun observe(ownerId: String): Flow<RemoteSnapshot> = values.map {
        RemoteSnapshot(sexRecords = it.values.toList(), fromCache = false)
    }

    override suspend fun fetch(ownerId: String) = RemoteSnapshot(
        sexRecords = values.value.values.toList(),
        fromCache = false,
    )

    override suspend fun commit(ownerId: String, local: SexRecordEntity): RemoteSexRecord =
        mutex.withLock {
            val current = values.value[local.localDate]
            if (
                current != null &&
                (local.remoteRevision != current.revision || local.id != current.id)
            ) {
                return@withLock current
            }
            // Mirrors the production data source (issue #104): recreate a
            // missing cloud document from the local pending edit instead of
            // permanently failing the PENDING row on its stale baseline.
            val committed = RemoteSexRecord(
                id = current?.id
                    ?: if (local.remoteRevision > 0) UUID.randomUUID().toString() else local.id,
                localDate = local.localDate,
                sexCount = local.sexCount,
                createdAt = current?.createdAt ?: local.createdAt,
                clientUpdatedAt = maxOf(local.updatedAt, current?.createdAt ?: local.createdAt),
                deleted = local.isDeleted,
                revision = (current?.revision ?: 0) + 1,
            )
            values.value = values.value + (local.localDate to committed)
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

private class FakeHandBrewRemoteForCombined : HandBrewRemoteDataSource {
    private val mutex = Mutex()
    private val values = MutableStateFlow<Map<LocalDate, RemoteHandBrewRecord>>(emptyMap())

    override fun observe(ownerId: String): Flow<RemoteSnapshot> = values.map {
        RemoteSnapshot(records = it.values.toList(), fromCache = false)
    }

    override suspend fun fetch(ownerId: String) = RemoteSnapshot(
        records = values.value.values.toList(),
        fromCache = false,
    )

    override suspend fun commit(
        ownerId: String,
        local: HandBrewRecordEntity,
    ): RemoteHandBrewRecord = mutex.withLock {
        val current = values.value[local.localDate]
        if (
            current != null &&
            (local.remoteRevision != current.revision || local.id != current.id)
        ) {
            return@withLock current
        }
        // Mirrors the production data source (issue #104): recreate a
        // missing cloud document from the local pending edit.
        val committed = RemoteHandBrewRecord(
            id = current?.id
                ?: if (local.remoteRevision > 0) UUID.randomUUID().toString() else local.id,
            localDate = local.localDate,
            brewCount = local.brewCount,
            createdAt = current?.createdAt ?: local.createdAt,
            clientUpdatedAt = maxOf(local.updatedAt, current?.createdAt ?: local.createdAt),
            deleted = local.isDeleted,
            revision = (current?.revision ?: 0) + 1,
        )
        values.value = values.value + (local.localDate to committed)
        committed
    }

    override suspend fun deleteAll(ownerId: String) {
        values.value = emptyMap()
    }
}
