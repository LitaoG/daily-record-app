package io.github.litaog.dailyrecord.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.SexRecord
import io.github.litaog.dailyrecord.core.model.SexRecordDetail
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
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

    @Test
    fun fixedYearFixtureReportsExactCountsAndDays() = runBlocking {
        val monthlyCounts = listOf(13, 11, 12, 14, 15, 16, 13, 14, 11, 9)
        val monthlyDays = listOf(9, 7, 8, 8, 10, 9, 8, 9, 4, 2)

        monthlyCounts.zip(monthlyDays).forEachIndexed { monthIndex, (count, days) ->
            val baseCount = count / days
            val remainder = count % days
            repeat(days) { dayIndex ->
                sexRepository.saveRecord(
                    sexRecord(
                        id = "2026-${monthIndex + 1}-${dayIndex + 1}",
                        date = LocalDate.of(2026, monthIndex + 1, dayIndex + 1),
                        count = baseCount + if (dayIndex < remainder) 1 else 0,
                    ),
                )
            }
        }

        val start = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2027, 1, 1)
        val records = sexRepository.observeRecords(start, end).first()
        val totalCount = records.sumOf { it.sexCount.toLong() }
        val sexDays = records.count { it.sexCount > 0 }

        assertEquals(74, records.size)
        assertEquals(128L, totalCount)
        assertEquals(74, sexDays)
        assertEquals(1.7, totalCount.toDouble() / sexDays, 0.05)
        assertFalse(records.any { it.localDate.monthValue > 10 })
    }

    @Test
    fun explicitNoBrewDoesNotIncreaseStatistics() = runBlocking {
        val start = LocalDate.of(2026, 7, 1)
        sexRepository.saveRecord(sexRecord("no-sex", start, 0))
        sexRepository.saveRecord(sexRecord("sexed", start.plusDays(1), 2))

        val records = sexRepository.observeRecords(start, start.plusMonths(1)).first()
        assertEquals(2L, records.sumOf { it.sexCount.toLong() })
        assertEquals(1, records.count { it.sexCount > 0 })
    }

    @Test
    fun deviceClockRollbackCannotMakeEditsOlderThanStoredRecord() = runBlocking {
        val date = LocalDate.of(2026, 7, 16)
        val first = sexRepository.saveRecord(
            sexRecord("clock-record", date, 1).copy(updatedAt = now.plusSeconds(20)),
        )
        val rollbackRepository = RoomSexRecordRepository(
            database = database,
            clock = Clock.fixed(now.minusSeconds(60), ZoneOffset.UTC),
        )

        val saved = rollbackRepository.saveRecord(
            SexRecord(
                id = "replacement-id",
                localDate = date,
                sexCount = 2,
                createdAt = now.minusSeconds(10),
                updatedAt = now.minusSeconds(5),
            ),
        )
        assertTrue(saved.updatedAt.isAfter(first.updatedAt))

        assertTrue(rollbackRepository.clearRecord(date))
        val tombstone = database.sexRecordDao()
            .getByDate(io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID, date)
        assertTrue(requireNotNull(tombstone).updatedAt.isAfter(saved.updatedAt))
    }

    @Test
    fun matchingTimestampClearStillRemovesTheRecord() = runBlocking {
        val date = LocalDate.of(2026, 7, 16)
        sexRepository.saveRecord(sexRecord("fresh-clear", date, 2))
        val dao = database.sexRecordDao()
        val existing = requireNotNull(
            dao.getByDate(io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID, date),
        )

        val affected = dao.markDeleted(
            ownerId = io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID,
            id = existing.id,
            expectedUpdatedAt = existing.updatedAt,
            updatedAt = existing.updatedAt.plusSeconds(1),
        )

        assertEquals(1, affected)
        assertTrue(requireNotNull(dao.getByDate(
            io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID,
            date,
        )).isDeleted)
    }

    @Test
    fun detailsSaveAtomicallyAndCountOnlyEditsPruneRemovedOccurrences() = runBlocking {
        val date = LocalDate.of(2026, 7, 16)
        sexRepository.saveRecord(
            sexRecord("with-details", date, 2),
            listOf(
                sexDetail(date, 1, LocalTime.of(9, 20), LocalTime.of(9, 35), "平静"),
                sexDetail(date, 2, LocalTime.of(21, 40), LocalTime.of(21, 52), "更专注"),
            ),
        )

        assertEquals(2, sexRepository.observeDetails(date).first().size)
        assertEquals("更专注", sexRepository.observeDetails(date).first()[1].feeling)

        sexRepository.saveRecord(sexRecord("replacement", date, 1).copy(updatedAt = now.plusSeconds(1)))
        assertEquals(listOf(1), sexRepository.observeDetails(date).first().map { it.occurrenceIndex })

        assertTrue(sexRepository.clearRecord(date))
        assertEquals(emptyList<SexRecordDetail>(), sexRepository.observeDetails(date).first())
    }

    @Test
    fun editingADetailKeepsItsCreationTimestamp() = runBlocking {
        val date = LocalDate.of(2026, 7, 16)
        val firstCreated = now
        sexRepository.saveRecord(
            sexRecord("created-at", date, 1),
            listOf(sexDetail(date, 1, LocalTime.of(9, 20), LocalTime.of(9, 35), "平静")),
        )
        val stored = sexRepository.observeDetails(date).first().single()
        assertEquals(firstCreated, stored.createdAt)

        // The UI always passes a fresh timestamp for every detail; the
        // repository must keep the original createdAt and only advance
        // updatedAt when the same occurrence is edited again.
        val editTimestamp = now.plusSeconds(60)
        sexRepository.saveRecord(
            sexRecord("created-at", date, 1).copy(updatedAt = editTimestamp),
            listOf(
                SexRecordDetail(
                    id = "ignored-new-id",
                    localDate = date,
                    occurrenceIndex = 1,
                    startTime = LocalTime.of(10, 0),
                    endTime = null,
                    feeling = "更平静",
                    createdAt = editTimestamp,
                    updatedAt = editTimestamp,
                ),
            ),
        )

        val edited = sexRepository.observeDetails(date).first().single()
        assertEquals(firstCreated, edited.createdAt)
        assertEquals(editTimestamp, edited.updatedAt)
        assertEquals("更平静", edited.feeling)
    }

    private fun sexDetail(
        date: LocalDate,
        occurrenceIndex: Int,
        startTime: LocalTime,
        endTime: LocalTime,
        feeling: String,
    ) = SexRecordDetail(
        id = "$date-$occurrenceIndex",
        localDate = date,
        occurrenceIndex = occurrenceIndex,
        startTime = startTime,
        endTime = endTime,
        feeling = feeling,
        createdAt = now,
        updatedAt = now,
    )

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
