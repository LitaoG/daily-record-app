package io.github.litaog.dailyrecord.core.account

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.litaog.dailyrecord.core.data.RoomHandBrewRecordRepository
import io.github.litaog.dailyrecord.core.data.RoomSexRecordRepository
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.SexRecord
import io.github.litaog.dailyrecord.core.sync.RoomHandBrewSyncStore
import io.github.litaog.dailyrecord.core.sync.RoomSexSyncStore
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CombinedAccountDeletionLocalStoreTest {
    private lateinit var database: DailyRecordDatabase
    private val ownerId = "owner"
    private val date = LocalDate.of(2026, 7, 16)
    private val now = Instant.parse("2026-07-16T00:00:00Z")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DailyRecordDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun keepCopiesBothVisibleModulesAndDeleteRemovesBothOwnerCaches() = runBlocking {
        val handBrew = RoomHandBrewRecordRepository(database, ownerId)
        val sex = RoomSexRecordRepository(database, ownerId)
        handBrew.saveRecord(HandBrewRecord("brew", date, 2, now, now))
        sex.saveRecord(SexRecord("sex", date, 1, now, now))
        val store = CombinedAccountDeletionLocalStore(
            listOf(
                RoomHandBrewSyncStore(database),
                RoomSexSyncStore(database),
            ),
        )

        store.stageLocalRecoveryCopy(ownerId)
        store.deleteOwnerCache(ownerId)
        store.promoteLocalRecoveryCopy(ownerId)

        assertEquals(
            2,
            RoomHandBrewRecordRepository(database, LOCAL_OWNER_ID)
                .observeRecord(date)
                .first()
                ?.brewCount,
        )
        assertEquals(
            1,
            RoomSexRecordRepository(database, LOCAL_OWNER_ID)
                .observeRecord(date)
                .first()
                ?.sexCount,
        )
        assertNull(handBrew.observeRecord(date).first())
        assertNull(sex.observeRecord(date).first())
    }

    @Test
    fun interruptedDeletionLeftoverCopyDoesNotBlockARetryedKeepDeletion() = runBlocking {
        val handBrew = RoomHandBrewRecordRepository(database, ownerId)
        val sex = RoomSexRecordRepository(database, ownerId)
        handBrew.saveRecord(HandBrewRecord("brew", date, 2, now, now))
        sex.saveRecord(SexRecord("sex", date, 1, now, now))
        val store = CombinedAccountDeletionLocalStore(
            listOf(
                RoomHandBrewSyncStore(database),
                RoomSexSyncStore(database),
            ),
        )

        // First interrupted attempt: a recovery copy is staged, then the
        // process dies before the account is deleted. The user retries.
        store.stageLocalRecoveryCopy(ownerId)
        store.stageLocalRecoveryCopy(ownerId)
        store.deleteOwnerCache(ownerId)
        store.promoteLocalRecoveryCopy(ownerId)

        assertEquals(
            2,
            RoomHandBrewRecordRepository(database, LOCAL_OWNER_ID)
                .observeRecord(date)
                .first()
                ?.brewCount,
        )
        assertEquals(
            1,
            RoomSexRecordRepository(database, LOCAL_OWNER_ID)
                .observeRecord(date)
                .first()
                ?.sexCount,
        )
    }
}
