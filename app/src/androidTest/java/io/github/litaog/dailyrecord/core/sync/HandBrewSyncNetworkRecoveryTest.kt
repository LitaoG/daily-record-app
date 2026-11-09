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

internal class HandBrewSyncNetworkRecoveryTest : HandBrewSyncCoordinatorTestBase() {

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
        val recovered = MutableStateFlow(
            RemoteSnapshot(sourceCollection = HAND_BREW_RECORDS_COLLECTION, fromCache = false),
        )
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

}
