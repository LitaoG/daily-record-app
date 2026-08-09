package io.github.litaog.dailyrecord.core.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
}
