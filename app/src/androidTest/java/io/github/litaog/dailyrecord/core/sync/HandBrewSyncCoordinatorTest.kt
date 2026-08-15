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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
    fun localAdoptionKeepsNewerLocalEditWhenDeviceClockIsBehind() = runBlocking {
        val remote = FakeRemoteDataSource()
        val database = database()
        val accountRepository = repository(database, firstInstant.plusSeconds(60))
        val accountCoordinator = coordinator(database, remote)
        accountRepository.saveRecord(record(1, firstInstant.plusSeconds(60)))
        accountCoordinator.syncOnce(ownerId)

        // The device clock is now far behind the account cache, so comparing
        // updatedAt would wrongly discard the local edit.
        val localRepository = RoomHandBrewRecordRepository(
            database = database,
            ownerId = io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID,
            clock = Clock.fixed(firstInstant, ZoneOffset.UTC),
        )
        localRepository.saveRecord(record(5, firstInstant).copy(id = "local-$date"))

        val store = RoomHandBrewSyncStore(database)
        assertEquals(1, store.adoptLocalRecords(ownerId))

        val adopted = store.pending(ownerId).single()
        assertEquals(5, adopted.brewCount)
        assertEquals(io.github.litaog.dailyrecord.core.database.SYNC_PENDING, adopted.syncState)
        assertEquals(1L, adopted.remoteRevision)

        // The server revision still matches the adopted baseline, so the local
        // edit uploads instead of being silently dropped.
        val result = accountCoordinator.syncOnce(ownerId)
        assertEquals(1, result.uploaded)
        assertEquals(5, remote.fetch(ownerId).records.filterIsInstance<RemoteHandBrewRecord>().single().brewCount)
    }

    @Test
    fun localAdoptionRespectsNewerServerRevisionInsteadOfDeviceClock() = runBlocking {
        val remote = FakeRemoteDataSource()
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
        val localRepository = RoomHandBrewRecordRepository(
            database = firstDatabase,
            ownerId = io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID,
            clock = Clock.fixed(firstInstant, ZoneOffset.UTC),
        )
        localRepository.saveRecord(record(5, firstInstant).copy(id = "local-$date"))
        val store = RoomHandBrewSyncStore(firstDatabase)
        assertEquals(1, store.adoptLocalRecords(ownerId))
        assertEquals(1L, store.pending(ownerId).single().remoteRevision)

        firstCoordinator.syncOnce(ownerId)

        // The server revision moved past the adopted baseline, so the server
        // value wins; the device clock plays no part in the outcome.
        assertEquals(9, firstRepository.observeRecord(date).first()?.brewCount)
        assertEquals(9, remote.fetch(ownerId).records.filterIsInstance<RemoteHandBrewRecord>().single().brewCount)
    }

    @Test
    fun authFailureIsRetryableForTokenRefreshAndPermissionDeniedIsNot() {
        val unauthenticated = com.google.firebase.firestore.FirebaseFirestoreException(
            "token expired",
            com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAUTHENTICATED,
        )
        val denied = com.google.firebase.firestore.FirebaseFirestoreException(
            "permission denied",
            com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED,
        )
        val auth = com.google.firebase.auth.FirebaseAuthException(
            "ERROR_USER_TOKEN_EXPIRED",
            "token refresh pending",
        )

        assertTrue(unauthenticated.isRetryableRemoteObservation())
        assertTrue(
            IllegalStateException("listener", unauthenticated)
                .isRetryableRemoteObservation(),
        )
        assertTrue(auth.isRetryableRemoteObservation())
        assertFalse(denied.isRetryableRemoteObservation())
    }

    @Test
    fun realtimeSubscriptionResubscribesAfterUnauthenticatedRecovers() = runBlocking {
        val recovered = MutableStateFlow(RemoteSnapshot(fromCache = false))
        val operations = object : AccountSyncOperations {
            val subscriptions = MutableStateFlow(0)

            override fun observeRemote(ownerId: String): Flow<RemoteSnapshot> = flow {
                val attempt = subscriptions.value + 1
                subscriptions.value = attempt
                if (attempt == 1) {
                    throw com.google.firebase.firestore.FirebaseFirestoreException(
                        "token expired",
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAUTHENTICATED,
                    )
                }
                emitAll(recovered)
            }

            override fun observePendingCount(ownerId: String): Flow<Int> = MutableStateFlow(0)

            override suspend fun pendingCount(ownerId: String): Int = 0

            override suspend fun applySnapshot(ownerId: String, snapshot: RemoteSnapshot): Int = 0

            override suspend fun syncOnce(ownerId: String): SyncResult =
                SyncResult(uploaded = 0, downloaded = 0, pending = 0)
        }
        val manager = AccountSyncManager(
            ownerId = ownerId,
            coordinator = operations,
            productionConfigured = true,
            networkAvailable = MutableStateFlow(true),
            remoteRetryDelayMillis = { 0L },
        )

        val jobs = manager.start(this)
        try {
            kotlinx.coroutines.withTimeout(10_000) {
                operations.subscriptions.first { it >= 2 }
            }
            assertEquals(2, operations.subscriptions.value)
            assertTrue(manager.status.value !is SyncStatus.Failed)
        } finally {
            jobs.forEach { it.cancel() }
        }
    }

    @Test
    fun pendingEditRecreatesCloudRecordAfterRemoteDocumentDisappears() = runBlocking {
        val remote = FakeRemoteDataSource()
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
        assertEquals(4, remote.fetch(ownerId).records.filterIsInstance<RemoteHandBrewRecord>().single().brewCount)
    }

    @Test
    fun remoteRevisionRejectsStaleBaseEvenWhenItsDeviceClockIsFarAhead() = runBlocking {
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

        firstRepository.saveRecord(record(9, firstInstant.plusSeconds(3_600)))
        secondRepository.saveRecord(record(2, firstInstant.plusSeconds(10)))
        secondCoordinator.syncOnce(ownerId)
        firstCoordinator.syncOnce(ownerId)
        secondCoordinator.syncOnce(ownerId)

        assertEquals(2, firstRepository.observeRecord(date).first()?.brewCount)
        assertEquals(2, secondRepository.observeRecord(date).first()?.brewCount)
        assertEquals(2, remote.fetch(ownerId).records.filterIsInstance<RemoteHandBrewRecord>().single().brewCount)
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
        assertEquals(3, remote.fetch(ownerId).records.filterIsInstance<RemoteHandBrewRecord>().single().brewCount)
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
    fun cancellingManualSyncIsNotConvertedIntoAFailureState() = runBlocking {
        val remote = FakeRemoteDataSource()
        val database = database()
        val manager = AccountSyncManager(
            ownerId = ownerId,
            coordinator = coordinator(database, remote),
            productionConfigured = true,
        )

        manager.syncNow()
        assertEquals(SyncStatus.UpToDate, manager.status.value)
        remote.fetchGate = kotlinx.coroutines.CompletableDeferred()

        val job = launch { manager.syncNow() }
        kotlinx.coroutines.withTimeout(5_000) {
            while (remote.fetchCalls == 0) kotlinx.coroutines.yield()
        }
        job.cancelAndJoin()

        assertTrue(job.isCancelled)
        assertEquals(SyncStatus.UpToDate, manager.status.value)
    }

    @Test
    fun manualSyncTimesOutAndReportsNetworkFailure() = runBlocking {
        val remote = FakeRemoteDataSource().apply {
            fetchGate = kotlinx.coroutines.CompletableDeferred()
        }
        val database = database()
        val manager = AccountSyncManager(
            ownerId = ownerId,
            coordinator = coordinator(database, remote),
            productionConfigured = true,
            syncAttemptTimeoutMillis = 50L,
        )

        manager.syncNow()

        val failure = manager.status.value as SyncStatus.Failed
        assertEquals(SyncFailureKind.Network, failure.kind)
        // Even a sub-second test timeout is presented as at least one second so
        // user-facing copy never reports an impossible "0 秒" wait.
        assertTrue(failure.message.contains("等待云服务超过 1 秒"))
        assertEquals(1, remote.fetchCalls)
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
        assertEquals(6, remote.fetch(ownerId).records.filterIsInstance<RemoteHandBrewRecord>().single().brewCount)
        assertTrue(
            RoomHandBrewSyncStore(database)
                .pending(io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID)
                .isEmpty(),
        )
    }

    @Test
    fun firstLoginAdoptsLocalRecordsBeforeAnyRemoteRequest() = runBlocking {
        val remote = FakeRemoteDataSource(failFetchAttempts = Int.MAX_VALUE)
        val database = database()
        val localRepository = RoomHandBrewRecordRepository(
            database = database,
            ownerId = io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID,
            clock = Clock.fixed(firstInstant, ZoneOffset.UTC),
        )
        val accountRepository = repository(database, firstInstant)
        val coordinator = coordinator(database, remote)
        localRepository.saveRecord(record(4, firstInstant).copy(id = "local-$date"))

        assertEquals(1, coordinator.prepareLocalAccount(ownerId))
        assertEquals(4, accountRepository.observeRecord(date).first()?.brewCount)
        assertTrue(
            RoomHandBrewSyncStore(database)
                .pending(io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID)
                .isEmpty(),
        )
        assertTrue(runCatching { coordinator.syncOnce(ownerId) }.isFailure)
        assertEquals(4, accountRepository.observeRecord(date).first()?.brewCount)
    }

    @Test
    fun freshSnapshotFlushesPendingAfterFirebaseRecoversWithoutNetworkStateChange() = runBlocking {
        val remote = FakeRemoteDataSource(
            failFirstObservation = true,
            failFetchAttempts = 1,
        )
        val database = database()
        repository(database, firstInstant).saveRecord(record(7, firstInstant))
        val manager = AccountSyncManager(
            ownerId = ownerId,
            coordinator = coordinator(database, remote),
            productionConfigured = true,
            networkAvailable = MutableStateFlow(true),
            remoteRetryDelayMillis = { 0L },
        )

        val jobs = manager.start(this)
        kotlinx.coroutines.withTimeout(5_000) {
            manager.status.first { it == SyncStatus.UpToDate }
        }
        assertEquals(7, remote.fetch(ownerId).records.filterIsInstance<RemoteHandBrewRecord>().single().brewCount)
        assertTrue(remote.observationAttempts.value >= 2)
        jobs.forEach { it.cancel() }
    }

    @Test
    fun malformedCloudRecordReportsDataFailureWithoutRetryLoop() = runBlocking {
        val remote = FakeRemoteDataSource(rejectedRecordCount = 1)
        val database = database()
        val manager = AccountSyncManager(
            ownerId = ownerId,
            coordinator = coordinator(database, remote),
            productionConfigured = true,
        )

        manager.syncNow()

        val failure = manager.status.value as SyncStatus.Failed
        assertEquals(SyncFailureKind.Data, failure.kind)
        assertTrue(failure.message.contains("其余记录已同步"))
        // A rejected snapshot is a terminal data failure and has no pending
        // local edit to confirm, so the optimized sync path performs one read.
        assertEquals(1, remote.fetchCalls)
    }

    @Test
    fun accountDeletionCanKeepVisibleRecordsLocallyWithoutKeepingTombstones() = runBlocking {
        val database = database()
        val accountRepository = repository(database, firstInstant)
        val secondDate = date.plusDays(1)
        accountRepository.saveRecord(record(3, firstInstant))
        accountRepository.saveRecord(
            record(4, firstInstant).copy(
                id = "record-$secondDate",
                localDate = secondDate,
            ),
        )
        assertTrue(accountRepository.clearRecord(secondDate))
        val store = RoomHandBrewSyncStore(database)

        store.stageLocalRecoveryCopy(ownerId)
        store.deleteOwnerCache(ownerId)
        store.promoteLocalRecoveryCopy(ownerId)

        val localRepository = RoomHandBrewRecordRepository(
            database = database,
            ownerId = io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID,
        )
        assertEquals(3, localRepository.observeRecord(date).first()?.brewCount)
        assertNull(localRepository.observeRecord(secondDate).first())
        assertEquals(0, database.handBrewRecordDao().countForOwner(ownerId))
    }

    @Test
    fun perOccurrenceDetailsRoundTripAcrossDevices() = runBlocking {
        val remote = FakeRemoteDataSource()
        val firstDatabase = database()
        val firstRepository = repository(firstDatabase, firstInstant)
        val firstCoordinator = coordinator(firstDatabase, remote)
        val detail = HandBrewRecordDetail(
            id = "detail-$date-1",
            localDate = date,
            occurrenceIndex = 1,
            startTime = LocalTime.of(8, 30),
            endTime = LocalTime.of(9, 5),
            feeling = "清醒",
        )
        firstRepository.saveRecord(record(2, firstInstant), listOf(detail))
        remote.detailsProvider = { localDate ->
            firstRepository.observeDetails(localDate).first().map { value ->
                RemoteHandBrewDetail(
                    id = value.id,
                    occurrenceIndex = value.occurrenceIndex,
                    startTime = value.startTime,
                    endTime = value.endTime,
                    feeling = value.feeling,
                )
            }
        }

        assertEquals(1, firstCoordinator.syncOnce(ownerId).uploaded)

        val secondDatabase = database()
        val secondRepository = repository(secondDatabase, firstInstant.plusSeconds(30))
        val secondResult = coordinator(secondDatabase, remote).syncOnce(ownerId)
        assertEquals(1, secondResult.downloaded)

        val restored = secondRepository.observeDetails(date).first()
        assertEquals(1, restored.size)
        assertEquals(detail.id, restored.single().id)
        assertEquals(detail.occurrenceIndex, restored.single().occurrenceIndex)
        assertEquals(detail.startTime, restored.single().startTime)
        assertEquals(detail.endTime, restored.single().endTime)
        assertEquals(detail.feeling, restored.single().feeling)
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
        moduleSyncCoordinator(RoomHandBrewSyncStore(database), remote)

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
    private val failFetchAttempts: Int = 0,
    private val rejectedRecordCount: Int = 0,
) : HandBrewRemoteDataSource {
    private val mutex = Mutex()
    private val values = MutableStateFlow<Map<LocalDate, RemoteHandBrewRecord>>(emptyMap())
    val observationAttempts = MutableStateFlow(0)
    var fetchCalls: Int = 0
        private set
    var fetchGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
    var detailsProvider: suspend (LocalDate) -> List<RemoteHandBrewDetail> = { emptyList() }

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

    override suspend fun commit(ownerId: String, local: HandBrewRecordEntity): RemoteHandBrewRecord =
        mutex.withLock {
            val current = values.value[local.localDate]
            if (
                current != null &&
                (local.remoteRevision != current.revision || local.id != current.id)
            ) {
                return@withLock current
            }
            // Mirrors the production data source (issue #104): a missing cloud
            // document is recreated from the local pending edit instead of
            // permanently failing the PENDING row on its stale revision
            // baseline. Normal clears still write a tombstone and participate
            // in the revision check above.
            val committed = RemoteHandBrewRecord(
                id = current?.id
                    ?: if (local.remoteRevision > 0) UUID.randomUUID().toString() else local.id,
                localDate = local.localDate,
                brewCount = local.brewCount,
                createdAt = current?.createdAt ?: local.createdAt,
                clientUpdatedAt = maxOf(local.updatedAt, current?.createdAt ?: local.createdAt),
                deleted = local.isDeleted,
                revision = (current?.revision ?: 0) + 1,
                details = detailsProvider(local.localDate),
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
