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

internal class HandBrewSyncDataIntegrityTest : HandBrewSyncCoordinatorTestBase() {

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

}
