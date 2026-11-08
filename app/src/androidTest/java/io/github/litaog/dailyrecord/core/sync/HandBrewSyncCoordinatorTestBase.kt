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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)

internal abstract class HandBrewSyncCoordinatorTestBase {
    protected val databases = mutableListOf<DailyRecordDatabase>()
    protected val ownerId = "test-user"
    protected val date = LocalDate.of(2026, 7, 16)
    protected val firstInstant = Instant.parse("2026-07-16T08:00:00Z")

    @After
    fun tearDown() {
        databases.forEach(DailyRecordDatabase::close)
    }

    protected fun database(): DailyRecordDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, DailyRecordDatabase::class.java)
            .allowMainThreadQueries()
            .build()
            .also(databases::add)
    }

    protected fun repository(database: DailyRecordDatabase, now: Instant) = RoomHandBrewRecordRepository(
        database = database,
        ownerId = ownerId,
        clock = Clock.fixed(now, ZoneOffset.UTC),
    )

    protected fun coordinator(database: DailyRecordDatabase, remote: FakeRemoteDataSource) =
        moduleSyncCoordinator(RoomHandBrewSyncStore(database), remote)

    protected fun record(count: Int, updatedAt: Instant) = HandBrewRecord(
        id = "record-${date}",
        localDate = date,
        brewCount = count,
        createdAt = firstInstant,
        updatedAt = updatedAt,
    )

    protected fun HandBrewRecordEntity.asRemote(revision: Long) = RemoteHandBrewRecord(
        id = id,
        localDate = localDate,
        brewCount = brewCount,
        createdAt = createdAt,
        clientUpdatedAt = updatedAt,
        deleted = isDeleted,
        revision = revision,
    )
}

internal class FakeRemoteDataSource(
    private val failFirstObservation: Boolean = false,
    private val failFetchAttempts: Int = 0,
    private val rejectedRecordCount: Int = 0,
) : HandBrewRemoteDataSource {
    override val collectionName: String = HAND_BREW_RECORDS_COLLECTION
    private val mutex = Mutex()
    private val values = MutableStateFlow<Map<LocalDate, RemoteHandBrewRecord>>(emptyMap())
    val observationAttempts = MutableStateFlow(0)
    var fetchCalls: Int = 0
        private set
    var fetchGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
    var detailsProvider: suspend (LocalDate) -> List<RemoteHandBrewDetail> = { emptyList() }

    override fun observe(ownerId: String): Flow<RemoteSnapshot> = flow {
        val attempt = observationAttempts.value + 1
        observationAttempts.value = attempt
        if (failFirstObservation && attempt == 1) {
            throw IOException("temporary listener failure")
        }
        emitAll(
            values.map {
                RemoteSnapshot(
                    sourceCollection = collectionName,
                    records = it.values.toList(),
                    fromCache = false,
                    rejectedRecordCount = rejectedRecordCount,
                )
            },
        )
    }

    override suspend fun fetch(ownerId: String): RemoteSnapshot {
        fetchCalls += 1
        if (fetchCalls <= failFetchAttempts) throw IOException("temporary fetch failure")
        fetchGate?.await()
        return RemoteSnapshot(
            sourceCollection = collectionName,
            records = values.value.values.toList(),
            fromCache = false,
            rejectedRecordCount = rejectedRecordCount,
        )
    }

    override suspend fun commit(ownerId: String, local: HandBrewRecordEntity): RemoteHandBrewRecord =
        mutex.withLock {
            val current = values.value[local.localDate]
            if (
                current != null &&
                (local.remoteRevision != current.revision || local.id != current.id)
            ) {
                return@withLock current
            }
            // Mirrors the production data source (issue #104): a missing cloud
            // document is recreated from the local pending edit instead of
            // permanently failing the PENDING row on its stale revision
            // baseline. Normal clears still write a tombstone and participate
            // in the revision check above.
            val committed = RemoteHandBrewRecord(
                id = current?.id
                    ?: if (local.remoteRevision > 0) UUID.randomUUID().toString() else local.id,
                localDate = local.localDate,
                brewCount = local.brewCount,
                createdAt = current?.createdAt ?: local.createdAt,
                clientUpdatedAt = maxOf(local.updatedAt, current?.createdAt ?: local.createdAt),
                deleted = local.isDeleted,
                revision = (current?.revision ?: 0) + 1,
                details = detailsProvider(local.localDate),
            )
            values.value = values.value + (local.localDate to committed)
            committed
        }

    suspend fun deleteAll(ownerId: String) {
        values.value = emptyMap()
    }

    /** Simulates the cloud document disappearing (e.g. account data cleanup). */
    fun removeRemote(localDate: LocalDate) {
        values.value = values.value - localDate
    }
}
