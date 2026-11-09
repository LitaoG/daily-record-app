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

internal class SexSyncDataIntegrityTest : SexSyncCoordinatorTestBase() {

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
        val store = RoomSexSyncStore(database)

        store.stageLocalRecoveryCopy(ownerId)
        store.deleteOwnerCache(ownerId)
        store.promoteLocalRecoveryCopy(ownerId)

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
    fun fakeRemoteScopesRecordsByOwnerAndMatchesAllSyncFields() = runBlocking {
        val remote = remote()
        val ownerRecord = SexRecordEntity(
            id = "record-owner-a",
            localDate = date,
            ownerId = ownerId,
            sexCount = 2,
            createdAt = firstInstant,
            updatedAt = firstInstant,
            isDeleted = false,
            syncState = io.github.litaog.dailyrecord.core.database.SYNC_PENDING,
            remoteRevision = 0,
        )
        val otherOwnerRecord = ownerRecord.copy(
            id = "record-owner-b",
            ownerId = "other-user",
        )

        val committed = remote.commit(ownerId, ownerRecord)
        assertEquals(1, remote.fetch(ownerId).records.size)
        assertTrue(remote.fetch("other-user").records.isEmpty())
        assertTrue(remote.matches(committed, ownerRecord))

        assertFalse(remote.matches(committed, ownerRecord.copy(localDate = date.plusDays(1))))
        assertFalse(remote.matches(committed, ownerRecord.copy(sexCount = 3)))
        assertFalse(remote.matches(committed, ownerRecord.copy(updatedAt = firstInstant.plusSeconds(1))))
        assertFalse(remote.matches(committed, ownerRecord.copy(isDeleted = true)))

        remote.commit("other-user", otherOwnerRecord)
        assertEquals(1, remote.fetch(ownerId).records.size)
        assertEquals(1, remote.fetch("other-user").records.size)

        var rejected = false
        try {
            remote.commit(ownerId, otherOwnerRecord)
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun combinedCoordinatorUploadsAndCountsBothModulesWithoutMixingThem() = runBlocking {
        val database = database()
        val handRemote = FakeDailyCountRemoteDataSource<
            io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity,
            RemoteHandBrewRecord,
            RemoteHandBrewDetail,
        >(
            collectionName = HAND_BREW_RECORDS_COLLECTION,
            entityId = { it.id },
            entityLocalDate = { it.localDate },
            entityCount = { it.brewCount },
            entityCreatedAt = { it.createdAt },
            entityUpdatedAt = { it.updatedAt },
            entityDeleted = { it.isDeleted },
            entityRemoteRevision = { it.remoteRevision },
            entityOwnerId = { it.ownerId },
            remoteCount = { it.brewCount },
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

}
