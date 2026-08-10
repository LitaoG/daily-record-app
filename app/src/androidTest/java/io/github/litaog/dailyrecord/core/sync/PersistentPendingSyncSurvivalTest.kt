package io.github.litaog.dailyrecord.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.litaog.dailyrecord.core.data.RoomHandBrewRecordRepository
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.core.database.SYNC_PENDING
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the P0 acceptance that pending local records survive a process
 * restart: a persistent database (not in-memory) keeps PENDING rows across
 * close/reopen, so WorkManager retries after process death without data loss.
 */
@RunWith(AndroidJUnit4::class)
class PersistentPendingSyncSurvivalTest {
    private val databaseName = "pending-survival-test.db"

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun pendingLocalRecordsSurviveDatabaseCloseAndReopen() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val date = LocalDate.of(2026, 7, 16)
        val now = Instant.parse("2026-07-16T00:00:00Z")

        val firstOpen = Room.databaseBuilder(
            context,
            DailyRecordDatabase::class.java,
            databaseName,
        ).addMigrations(*DailyRecordDatabase.MIGRATIONS).build()
        try {
            RoomHandBrewRecordRepository(firstOpen, LOCAL_OWNER_ID).saveRecord(
                HandBrewRecord("pending-1", date, 3, now, now),
            )
            assertEquals(1, RoomHandBrewSyncStore(firstOpen).pendingCount(LOCAL_OWNER_ID))
        } finally {
            firstOpen.close()
        }

        // Simulate process death: reopen the same persistent database file.
        val secondOpen = Room.databaseBuilder(
            context,
            DailyRecordDatabase::class.java,
            databaseName,
        ).addMigrations(*DailyRecordDatabase.MIGRATIONS).build()
        try {
            val store = RoomHandBrewSyncStore(secondOpen)
            assertEquals(1, store.pendingCount(LOCAL_OWNER_ID))
            val pending = store.pending(LOCAL_OWNER_ID).single()
            assertEquals(SYNC_PENDING, pending.syncState)
            assertEquals(3, pending.brewCount)
            assertEquals(
                3,
                RoomHandBrewRecordRepository(secondOpen, LOCAL_OWNER_ID)
                    .observeRecord(date).first()?.brewCount,
            )
        } finally {
            secondOpen.close()
        }
    }
}
