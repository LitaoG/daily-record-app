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
import org.junit.After
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
class SexSyncCoordinatorTest {
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
        val remote = remote()
        val database = database()
        val repository = repository(database, firstInstant)
        val coordinator = coordinator(database, remote)

        // Device A syncs a record so the local row carries a remote revision.
        repository.saveRecord(record(1, firstInstant))
        assertEquals(1, coordinator.syncOnce(ownerId).uploaded)
        assertEquals(1, repository.observeRecord(date).first()?.sexCount)

        // The cloud document disappears (account data cleanup on another
        // device), while this device keeps editing the same date offline.
        remote.removeRemote(date)
        repository.saveRecord(record(2, firstInstant.plusSeconds(30)))
        val pending = RoomSexSyncStore(database).pending(ownerId).single()
        assertEquals(1L, pending.remoteRevision)
        assertEquals(2, pending.sexCount)

        // The next sync must recreate the document from the pending edit
        // instead of failing permanently on the stale revision baseline.
        val result = coordinator.syncOnce(ownerId)

        assertEquals(1, result.uploaded)
        assertEquals(0, result.pending)
        val cloud = remote.fetch(ownerId).records.filterIsInstance<RemoteSexRecord>().single()
        assertEquals(2, cloud.sexCount)
        // The recreated document restarts its revision count at 1 (ADR-017).
        assertEquals(1L, cloud.revision)
        // The local row is confirmed against the recreated document.
        assertEquals(2, repository.observeRecord(date).first()?.sexCount)
        assertEquals(0, RoomSexSyncStore(database).pendingCount(ownerId))
    }

    @Test
    fun accountSyncManagerRecoversPendingEditAfterRemoteDocumentDisappears() = runBlocking {
        val remote = remote()
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
        assertEquals(5, repository.observeRecord(date).first()?.sexCount)
        assertEquals(0, RoomSexSyncStore(database).pendingCount(ownerId))
    }

    @Test
    fun physicalDisappearanceKeepsAnExplicitClearAsATombstone() = runBlocking {
        val remote = remote()
        val database = database()
        val repository = repository(database, firstInstant)
        val coordinator = coordinator(database, remote)

        repository.saveRecord(record(3, firstInstant))
        coordinator.syncOnce(ownerId)
        assertTrue(repository.clearRecord(date))
        remote.removeRemote(date)

        coordinator.syncOnce(ownerId)

        val cloud = remote.fetch(ownerId).records.filterIsInstance<RemoteSexRecord>().single()
        assertTrue(cloud.deleted)
        assertNull(repository.observeRecord(date).first())
        assertEquals(0, RoomSexSyncStore(database).pendingCount(ownerId))
    }

    @Test
    fun syncedPeerAcceptsRecreatedCloudGenerationWithRestartedRevision() = runBlocking {
        val remote = remote()
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

        val recreated = remote.fetch(ownerId).records.filterIsInstance<RemoteSexRecord>().single()
        assertNotEquals(originalId, recreated.id)
        assertEquals(1L, recreated.revision)

        // Device A has no pending edit, but must accept the new generation
        // even though its numeric revision is equal to the old revision.
        firstCoordinator.syncOnce(ownerId)
        val restored = firstRepository.observeRecord(date).first()!!
        assertEquals(7, restored.sexCount)
        assertEquals(recreated.id, restored.id)
    }

    @Test
    fun staleBaselineStillLosesToConcurrentRemoteEditDuringRecreate() = runBlocking {
        val remote = remote()
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
        assertEquals(2L, remote.fetch(ownerId).records.filterIsInstance<RemoteSexRecord>().single().revision)

        firstRepository.saveRecord(record(2, firstInstant.plusSeconds(90)))
        firstCoordinator.syncOnce(ownerId)

        // The recreated server document has moved past the stale baseline, so
        // the server value wins; no device clock is involved.
        val cloud = remote.fetch(ownerId).records.filterIsInstance<RemoteSexRecord>().single()
        assertEquals(10, cloud.sexCount)
        assertEquals(10, firstRepository.observeRecord(date).first()?.sexCount)
    }

    @Test
    fun clearCreatesPendingTombstoneAndStaleAckCannotHideNewEdit() = runBlocking {
        val database = database()
        val repository = repository(database, firstInstant)
        val store = RoomSexSyncStore(database)
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
        assertEquals(2, store.pending(ownerId).single().sexCount)

        assertTrue(repository(database, firstInstant.plusSeconds(2)).clearRecord(date))
        assertNull(repository.observeRecord(date).first())
        val tombstone = store.pending(ownerId).single()
        assertTrue(tombstone.isDeleted)
        assertEquals(2, tombstone.sexCount)
        assertFalse(repository.clearRecord(date))
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
    fun offlineStatusPreventsNetworkAttemptAndReconnectFlushesPendingRecord() = runBlocking {
        val remote = remote()
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
        assertEquals(3, remote.fetch(ownerId).records.filterIsInstance<RemoteSexRecord>().single().sexCount)
        jobs.forEach { it.cancel() }
    }

    @Test
    fun realtimeListenerRetriesAfterTransientFailure() = runBlocking {
        val remote = remote(failFirstObservation = true)
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
        val remote = remote()
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
            while (remote.fetchCalls == 0) yield()
        }
        job.cancelAndJoin()

        assertTrue(job.isCancelled)
        assertEquals(SyncStatus.UpToDate, manager.status.value)
    }

    @Test
    fun manualSyncTimesOutAndReportsNetworkFailure() = runBlocking {
        val remote = remote().apply {
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

    @Test
    fun freshSnapshotFlushesPendingAfterFirebaseRecoversWithoutNetworkStateChange() = runBlocking {
        val remote = remote(
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
        assertEquals(7, remote.fetch(ownerId).records.filterIsInstance<RemoteSexRecord>().single().sexCount)
        assertTrue(remote.observationAttempts.value >= 2)
        jobs.forEach { it.cancel() }
    }

    @Test
    fun malformedCloudRecordReportsDataFailureWithoutRetryLoop() = runBlocking {
        val remote = remote(rejectedRecordCount = 1)
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
        assertEquals(2, remote.fetchCalls)
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
        val store = RoomSexSyncStore(database)

        store.stageLocalRecoveryCopy(ownerId)
        store.deleteOwnerCache(ownerId)

        val localRepository = RoomSexRecordRepository(
            database = database,
            ownerId = LOCAL_OWNER_ID,
        )
        assertEquals(3, localRepository.observeRecord(date).first()?.sexCount)
        assertNull(localRepository.observeRecord(secondDate).first())
        assertEquals(0, database.sexRecordDao().countForOwner(ownerId))
    }

    @Test
    fun perOccurrenceDetailsRoundTripAcrossDevices() = runBlocking {
        val remote = remote()
        val firstDatabase = database()
        val firstRepository = repository(firstDatabase, firstInstant)
        val firstCoordinator = coordinator(firstDatabase, remote)
        val detail = SexRecordDetail(
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
                RemoteSexDetail(
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

    @Test
    fun combinedCoordinatorUploadsAndCountsBothModulesWithoutMixingThem() = runBlocking {
        val database = database()
        val handRemote = FakeDailyCountRemoteDataSource<
            io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity,
            RemoteHandBrewRecord,
            RemoteHandBrewDetail,
        >(
            entityId = { it.id },
            entityLocalDate = { it.localDate },
            entityCount = { it.brewCount },
            entityCreatedAt = { it.createdAt },
            entityUpdatedAt = { it.updatedAt },
            entityDeleted = { it.isDeleted },
            entityRemoteRevision = { it.remoteRevision },
            buildRemote = { id, localDate, count, createdAt, clientUpdatedAt, deleted, revision, details ->
                RemoteHandBrewRecord(
                    id = id,
                    localDate = localDate,
                    brewCount = count,
                    createdAt = createdAt,
                    clientUpdatedAt = clientUpdatedAt,
                    deleted = deleted,
                    revision = revision,
                    details = details,
                )
            },
        )
        val sexRemote = remote()
        val handRepository = io.github.litaog.dailyrecord.core.data.RoomHandBrewRecordRepository(database, ownerId)
        val sexRepository = repository(database, firstInstant)
        handRepository.saveRecord(
            io.github.litaog.dailyrecord.core.model.HandBrewRecord("brew-$date", date, 4, firstInstant, firstInstant),
        )
        sexRepository.saveRecord(record(2, firstInstant))
        val coordinator = CombinedSyncCoordinator(
            listOf(
                moduleSyncCoordinator(io.github.litaog.dailyrecord.core.sync.RoomHandBrewSyncStore(database), handRemote),
                moduleSyncCoordinator(RoomSexSyncStore(database), sexRemote),
            ),
        )

        val result = coordinator.syncOnce(ownerId)

        assertEquals(2, result.uploaded)
        assertEquals(0, result.pending)
        assertEquals(4, handRemote.fetch(ownerId).records.filterIsInstance<RemoteHandBrewRecord>().single().brewCount)
        assertEquals(2, sexRemote.fetch(ownerId).records.filterIsInstance<RemoteSexRecord>().single().sexCount)

        assertTrue(sexRepository.clearRecord(date))
        coordinator.syncOnce(ownerId)
        assertEquals(4, handRepository.observeRecord(date).first()?.brewCount)
        assertNull(sexRepository.observeRecord(date).first())
    }

    private fun remote(
        failFirstObservation: Boolean = false,
        failFetchAttempts: Int = 0,
        rejectedRecordCount: Int = 0,
    ) = FakeDailyCountRemoteDataSource<SexRecordEntity, RemoteSexRecord, RemoteSexDetail>(
        failFirstObservation = failFirstObservation,
        failFetchAttempts = failFetchAttempts,
        rejectedRecordCount = rejectedRecordCount,
        entityId = { it.id },
        entityLocalDate = { it.localDate },
        entityCount = { it.sexCount },
        entityCreatedAt = { it.createdAt },
        entityUpdatedAt = { it.updatedAt },
        entityDeleted = { it.isDeleted },
        entityRemoteRevision = { it.remoteRevision },
        buildRemote = { id, localDate, count, createdAt, clientUpdatedAt, deleted, revision, details ->
            RemoteSexRecord(
                id = id,
                localDate = localDate,
                sexCount = count,
                createdAt = createdAt,
                clientUpdatedAt = clientUpdatedAt,
                deleted = deleted,
                revision = revision,
                details = details,
            )
        },
    )

    private fun database(): DailyRecordDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, DailyRecordDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            .also(databases::add)
    }

    private fun repository(database: DailyRecordDatabase, now: Instant): SexRecordRepository =
        RoomSexRecordRepository(
            database = database,
            ownerId = ownerId,
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

    private fun coordinator(
        database: DailyRecordDatabase,
        remote: FakeDailyCountRemoteDataSource<SexRecordEntity, RemoteSexRecord, RemoteSexDetail>,
    ) = moduleSyncCoordinator(RoomSexSyncStore(database), remote)

    private fun record(count: Int, updatedAt: Instant) = SexRecord(
        id = "record-${date}",
        localDate = date,
        sexCount = count,
        createdAt = firstInstant,
        updatedAt = updatedAt,
    )

    private fun SexRecordEntity.asRemote(revision: Long) = RemoteSexRecord(
        id = id,
        localDate = localDate,
        sexCount = sexCount,
        createdAt = createdAt,
        clientUpdatedAt = updatedAt,
        deleted = isDeleted,
        revision = revision,
    )
}
