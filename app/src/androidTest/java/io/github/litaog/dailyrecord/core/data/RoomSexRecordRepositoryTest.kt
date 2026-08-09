package io.github.litaog.dailyrecord.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.SexRecord
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomSexRecordRepositoryTest {
    private lateinit var database: DailyRecordDatabase
    private lateinit var sexRepository: SexRecordRepository
    private lateinit var handBrewRepository: HandBrewRecordRepository
    private val now = Instant.parse("2026-07-16T00:00:00Z")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DailyRecordDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        sexRepository = RoomSexRecordRepository(database)
        handBrewRepository = RoomHandBrewRecordRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun zeroIsExplicitAndClearReturnsDateToUnset() = runBlocking {
        val date = LocalDate.of(2026, 7, 16)
        sexRepository.saveRecord(sexRecord("sex-1", date, 0))

        val explicitZero = sexRepository.observeRecord(date).first()
        assertEquals(0, explicitZero?.sexCount)
        assertFalse(requireNotNull(explicitZero).occurred)

        assertTrue(sexRepository.clearRecord(date))
        assertNull(sexRepository.observeRecord(date).first())
        assertFalse(sexRepository.clearRecord(date))
    }

    @Test
    fun sameDateModulesNeverOverwriteOrClearEachOther() = runBlocking {
        val date = LocalDate.of(2026, 7, 16)
        handBrewRepository.saveRecord(handBrewRecord(date, 3))
        sexRepository.saveRecord(sexRecord("sex-1", date, 2))

        assertEquals(3, handBrewRepository.observeRecord(date).first()?.brewCount)
        assertEquals(2, sexRepository.observeRecord(date).first()?.sexCount)

        assertTrue(sexRepository.clearRecord(date))
        assertNull(sexRepository.observeRecord(date).first())
        assertEquals(3, handBrewRepository.observeRecord(date).first()?.brewCount)
    }

    @Test
    fun ownerIsolationAppliesToSexRecords() = runBlocking {
        val date = LocalDate.of(2026, 7, 16)
        val ownerA = RoomSexRecordRepository(database, ownerId = "owner-a")
        val ownerB = RoomSexRecordRepository(database, ownerId = "owner-b")

        ownerA.saveRecord(sexRecord("owner-a-sex", date, 1))

        assertEquals(1, ownerA.observeRecord(date).first()?.sexCount)
        assertNull(ownerB.observeRecord(date).first())
    }

    @Test
    fun staleClearDoesNotDeleteANewerSavedSexEdit() = runBlocking {
        val date = LocalDate.of(2026, 7, 16)
        sexRepository.saveRecord(sexRecord("stale-clear", date, 1))
        val dao = database.sexRecordDao()
        val existing = requireNotNull(
            dao.getByDate(io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID, date),
        )

        // Simulate a concurrent save committing between the clear's read and write.
        val newerTimestamp = existing.updatedAt.plusSeconds(1)
        dao.upsert(existing.copy(sexCount = 3, updatedAt = newerTimestamp))

        val affected = dao.markDeleted(
            ownerId = io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID,
            id = existing.id,
            expectedUpdatedAt = existing.updatedAt,
            updatedAt = newerTimestamp.plusSeconds(1),
        )

        assertEquals(0, affected)
        val after = requireNotNull(
            dao.getByDate(io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID, date),
        )
        assertEquals(3, after.sexCount)
        assertFalse(after.isDeleted)
    }

    private fun sexRecord(id: String, date: LocalDate, count: Int) = SexRecord(
        id = id,
        localDate = date,
        sexCount = count,
        createdAt = now,
        updatedAt = now,
    )

    private fun handBrewRecord(date: LocalDate, count: Int) = HandBrewRecord(
        id = "hand-brew-1",
        localDate = date,
        brewCount = count,
        createdAt = now,
        updatedAt = now,
    )
}
