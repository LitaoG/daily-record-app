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

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @After
    fun tearDown() {
        DailyRecordSyncScheduler.endDeletionBlock()
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
}
