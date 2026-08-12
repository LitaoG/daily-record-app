package io.github.litaog.dailyrecord.core.sync

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import io.github.litaog.dailyrecord.core.di.FIREBASE_EMULATOR_APP_NAME
import io.github.litaog.dailyrecord.core.di.FirebaseServices
import io.github.litaog.dailyrecord.core.common.awaitResult
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity
import io.github.litaog.dailyrecord.core.database.SYNC_PENDING
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyRecordSyncWorkerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val persistentOwner = "persistent-deletion-owner-test"

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @After
    fun tearDown() {
        DailyRecordSyncScheduler.endDeletionBlock()
        DailyRecordSyncScheduler.completeDeletionCleanup(context, persistentOwner)
    }

    @Test
    fun workerExitsBeforeRemoteWorkWhileDeletionBlocked() = runBlocking {
        DailyRecordSyncScheduler.beginDeletionBlock()
        val worker = TestListenableWorkerBuilder<DailyRecordSyncWorker>(context).build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(DailyRecordSyncScheduler.isDeletionBlocked)
    }

    @Test
    fun scheduleDoesNotEnqueueWhileDeletionBlocked() {
        val workManager = WorkManager.getInstance(context)
        DailyRecordSyncScheduler.beginDeletionBlock()

        DailyRecordSyncScheduler.schedule(context)

        val workInfos = workManager.getWorkInfosForUniqueWork(
            DailyRecordSyncScheduler.workName,
        ).get()
        assertTrue(workInfos.isEmpty())
    }

    @Test
    fun scheduleEnqueuesAfterDeletionBlockEnds() {
        val workManager = WorkManager.getInstance(context)
        try {
            DailyRecordSyncScheduler.beginDeletionBlock()
            DailyRecordSyncScheduler.schedule(context)
            assertTrue(
                workManager.getWorkInfosForUniqueWork(
                    DailyRecordSyncScheduler.workName,
                ).get().isEmpty(),
            )
        } finally {
            DailyRecordSyncScheduler.endDeletionBlock()
        }

        DailyRecordSyncScheduler.schedule(context)

        val workInfos = workManager.getWorkInfosForUniqueWork(
            DailyRecordSyncScheduler.workName,
        ).get()
        assertTrue(workInfos.isNotEmpty())
        assertFalse(DailyRecordSyncScheduler.isDeletionBlocked)
    }

    @Test
    fun persistentDeletionMarkerBlocksOnlyTheAccountBeingDeleted() = runBlocking {
        DailyRecordSyncScheduler.beginDeletionBlock(context, persistentOwner)

        assertTrue(DailyRecordSyncScheduler.isDeletionBlocked(context, persistentOwner))
        assertFalse(
            DailyRecordSyncScheduler.isDeletionBlocked(
                context,
                ownerId = "different-account",
            ),
        )

        DailyRecordSyncScheduler.endDeletionBlock(
            context,
            persistentOwner,
            AccountDeletionOutcome.CleanupPending,
        )
        assertTrue(DailyRecordSyncScheduler.isDeletionBlocked(context, persistentOwner))

        DailyRecordSyncScheduler.completeDeletionCleanup(context, persistentOwner)
        assertFalse(DailyRecordSyncScheduler.isDeletionBlocked(context, persistentOwner))
    }

    @Test
    fun interruptedDeletionKeepsDurableMarkerButReleasesProcessLock() = runBlocking {
        DailyRecordSyncScheduler.beginDeletionBlock(context, persistentOwner)

        DailyRecordSyncScheduler.endDeletionBlock(
            context,
            persistentOwner,
            AccountDeletionOutcome.Interrupted,
        )

        assertTrue(DailyRecordSyncScheduler.isDeletionBlocked(context, persistentOwner))
        assertFalse(DailyRecordSyncScheduler.isLegacyDeletionBlocked)

        // A retry in the same process must be possible; the durable marker is
        // replaced rather than treated as a second concurrent deletion.
        DailyRecordSyncScheduler.beginDeletionBlock(context, persistentOwner)
        DailyRecordSyncScheduler.endDeletionBlock(
            context,
            persistentOwner,
            AccountDeletionOutcome.Completed,
        )
        assertFalse(DailyRecordSyncScheduler.isDeletionBlocked(context, persistentOwner))
    }

    @Test
    fun workerRecoversPendingEditAfterCloudDocumentDisappears() = runBlocking {
        assumeAuthEmulatorReachable()
        val services = FirebaseServices.create(context, emulatorHost = "10.0.2.2")
        services.authRepository.signOut()
        var ownerId: String? = null
        var database: DailyRecordDatabase? = null
        try {
            val suffix = UUID.randomUUID().toString().take(10)
            val account = services.authRepository.register(
                email = "worker-recreate-$suffix@example.com",
                password = "test-password-2026",
            )
            ownerId = account.uid
            val date = LocalDate.of(2026, 7, 20)
            val local = HandBrewRecordEntity(
                id = "worker-recreate-$suffix",
                localDate = date,
                ownerId = account.uid,
                brewCount = 2,
                createdAt = Instant.parse("2026-07-20T08:00:00Z"),
                updatedAt = Instant.parse("2026-07-20T08:00:01Z"),
                isDeleted = false,
                syncState = SYNC_PENDING,
                remoteRevision = 0,
            )
            val initial = services.remoteDataSource.commit(account.uid, local)
            FirebaseFirestore.getInstance(FirebaseApp.getInstance(FIREBASE_EMULATOR_APP_NAME))
                .collection("users").document(account.uid)
                .collection("handBrewRecords").document(date.toString())
                .delete().awaitResult()

            database = DailyRecordDatabase.create(context)
            database!!.handBrewRecordDao().upsert(
                local.copy(
                    id = initial.id,
                    brewCount = 5,
                    updatedAt = Instant.parse("2026-07-20T08:00:02Z"),
                    remoteRevision = initial.revision,
                ),
            )
            database!!.close()
            database = null

            val worker = TestListenableWorkerBuilder<DailyRecordSyncWorker>(context)
                .setWorkerFactory(workerFactoryForEmulator())
                .build()
            assertEquals(ListenableWorker.Result.success(), worker.doWork())

            database = DailyRecordDatabase.create(context)
            val restored = database!!.handBrewRecordDao().getByDate(account.uid, date)
            assertEquals(5, restored?.brewCount)
            assertEquals("SYNCED", restored?.syncState)
            assertEquals(0, database!!.handBrewRecordDao().getPending(account.uid).size)
        } finally {
            database?.close()
            ownerId?.let { owner ->
                val cleanup = DailyRecordDatabase.create(context)
                cleanup.handBrewRecordDao().deleteOwnerCache(owner)
                cleanup.close()
            }
            services.authRepository.signOut()
        }
    }

    private fun workerFactoryForEmulator(): WorkerFactory = object : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
        ): ListenableWorker? {
            if (workerClassName != DailyRecordSyncWorker::class.java.name) return null
            return DailyRecordSyncWorker(
                appContext,
                workerParameters,
                DailyRecordSyncServicesProvider { context, database ->
                    // The emulator deliberately uses a demo project id. Marking
                    // this test-only provider as configured exercises the same
                    // worker branch without ever touching production Firebase.
                    FirebaseServices.create(
                        context,
                        emulatorHost = "10.0.2.2",
                        database = database,
                    ).copy(productionConfigured = true)
                },
            )
        }
    }

    private fun assumeAuthEmulatorReachable() {
        val connection = URL(
            "http://10.0.2.2:9099/emulator/v1/projects/demo-daily-record-app/config",
        ).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 1_000
            connection.readTimeout = 1_000
            connection.connect()
            assumeTrue(connection.responseCode in 200..499)
        } catch (_: Exception) {
            assumeTrue("Firebase Auth emulator is not running", false)
        } finally {
            connection.disconnect()
        }
    }
}
