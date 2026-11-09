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

internal abstract class SexSyncCoordinatorTestBase {
    protected val databases = mutableListOf<DailyRecordDatabase>()
    protected val ownerId = "test-user"
    protected val date = LocalDate.of(2026, 7, 16)
    protected val firstInstant = Instant.parse("2026-07-16T08:00:00Z")

    @After
    fun tearDown() {
        databases.forEach(DailyRecordDatabase::close)
    }

    protected fun remote(
        failFirstObservation: Boolean = false,
        failFetchAttempts: Int = 0,
        rejectedRecordCount: Int = 0,
    ) = FakeDailyCountRemoteDataSource<SexRecordEntity, RemoteSexRecord, RemoteSexDetail>(
        collectionName = SEX_RECORDS_COLLECTION,
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
        entityOwnerId = { it.ownerId },
        remoteCount = { it.sexCount },
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

    protected fun database(): DailyRecordDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, DailyRecordDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            .also(databases::add)
    }

    protected fun repository(database: DailyRecordDatabase, now: Instant): SexRecordRepository =
        RoomSexRecordRepository(
            database = database,
            ownerId = ownerId,
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )

    protected fun coordinator(
        database: DailyRecordDatabase,
        remote: FakeDailyCountRemoteDataSource<SexRecordEntity, RemoteSexRecord, RemoteSexDetail>,
    ) = moduleSyncCoordinator(RoomSexSyncStore(database), remote)

    protected fun record(count: Int, updatedAt: Instant) = SexRecord(
        id = "record-${date}",
        localDate = date,
        sexCount = count,
        createdAt = firstInstant,
        updatedAt = updatedAt,
    )

    protected fun SexRecordEntity.asRemote(revision: Long) = RemoteSexRecord(
        id = id,
        localDate = localDate,
        sexCount = sexCount,
        createdAt = createdAt,
        clientUpdatedAt = updatedAt,
        deleted = isDeleted,
        revision = revision,
    )
}
