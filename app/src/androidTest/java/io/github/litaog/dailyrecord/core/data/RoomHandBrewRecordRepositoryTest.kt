package io.github.litaog.dailyrecord.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import java.time.Instant
import java.time.LocalDate
import java.time.Clock
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomHandBrewRecordRepositoryTest {
    private lateinit var database: DailyRecordDatabase
    private lateinit var repository: HandBrewRecordRepository
    private val now = Instant.parse("2026-07-16T00:00:00Z")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DailyRecordDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoomHandBrewRecordRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sameDateUpdatesTheOnlyAggregateAndKeepsIdentity() = runBlocking {
        val date = LocalDate.of(2026, 7, 16)
        val first = repository.saveRecord(record("first-id", date, 1))
        val second = repository.saveRecord(record("second-id", date, 3).copy(updatedAt = now.plusSeconds(1)))

        val records = repository.observeRecords(date, date.plusDays(1)).first()
        assertEquals(1, records.size)
        assertEquals(first.id, second.id)
        assertEquals(3, records.single().brewCount)
    }

    @Test
    fun zeroIsExplicitNoBrewAndClearReturnsDateToUnset() = runBlocking {
        val date = LocalDate.of(2026, 7, 16)
        repository.saveRecord(record("record-1", date, 0))

        val explicitNoBrew = repository.observeRecord(date).first()
        assertEquals(0, explicitNoBrew?.brewCount)
        assertFalse(requireNotNull(explicitNoBrew).wasBrewed)

        assertTrue(repository.clearRecord(date))
        assertEquals(null, repository.observeRecord(date).first())
        assertFalse(repository.clearRecord(date))
    }

    @Test
    fun fixedYearFixtureReports128BrewsAcross74Days() = runBlocking {
        val monthlyCounts = listOf(13, 11, 12, 14, 15, 16, 13, 14, 11, 9)
        val monthlyDays = listOf(9, 7, 8, 8, 10, 9, 8, 9, 4, 2)

        monthlyCounts.zip(monthlyDays).forEachIndexed { monthIndex, (count, days) ->
            val baseCount = count / days
            val remainder = count % days
            repeat(days) { dayIndex ->
                repository.saveRecord(
                    record(
                        id = "2026-${monthIndex + 1}-${dayIndex + 1}",
                        date = LocalDate.of(2026, monthIndex + 1, dayIndex + 1),
                        count = baseCount + if (dayIndex < remainder) 1 else 0,
                    ),
                )
            }
        }

        val start = LocalDate.of(2026, 1, 1)
        val end = LocalDate.of(2027, 1, 1)
        val records = repository.observeRecords(start, end).first()
        val summary = repository.observeSummary(start, end).first()

        assertEquals(74, records.size)
        assertEquals(128L, summary.totalCount)
        assertEquals(74, summary.brewDays)
        assertEquals(1.7, summary.averagePerBrewDay, 0.05)
        assertFalse(records.any { it.localDate.monthValue > 10 })
    }

    @Test
    fun explicitNoBrewDoesNotIncreaseStatistics() = runBlocking {
        val start = LocalDate.of(2026, 7, 1)
        repository.saveRecord(record("no-brew", start, 0))
        repository.saveRecord(record("brewed", start.plusDays(1), 2))

        val summary = repository.observeSummary(start, start.plusMonths(1)).first()
        assertEquals(2L, summary.totalCount)
        assertEquals(1, summary.brewDays)
    }

    @Test
    fun emptyOrReversedRangeIsRejected() {
        val date = LocalDate.of(2026, 7, 16)
        assertThrows(IllegalArgumentException::class.java) {
            repository.observeRecords(date, date)
        }
        assertThrows(IllegalArgumentException::class.java) {
            repository.observeSummary(date, date.minusDays(1))
        }
    }

    @Test
    fun deviceClockRollbackCannotMakeEditsOlderThanStoredRecord() = runBlocking {
        val date = LocalDate.of(2026, 7, 16)
        val first = repository.saveRecord(
            record("clock-record", date, 1).copy(updatedAt = now.plusSeconds(20)),
        )
        val rollbackRepository = RoomHandBrewRecordRepository(
            database = database,
            clock = Clock.fixed(now.minusSeconds(60), ZoneOffset.UTC),
        )

        val saved = rollbackRepository.saveRecord(
            HandBrewRecord(
                id = "replacement-id",
                localDate = date,
                brewCount = 2,
                createdAt = now.minusSeconds(10),
                updatedAt = now.minusSeconds(5),
            ),
        )
        assertTrue(saved.updatedAt.isAfter(first.updatedAt))

        assertTrue(rollbackRepository.clearRecord(date))
        val tombstone = database.handBrewRecordDao()
            .getByDate(io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID, date)
        assertTrue(requireNotNull(tombstone).updatedAt.isAfter(saved.updatedAt))
    }

    private fun record(id: String, date: LocalDate, count: Int) = HandBrewRecord(
        id = id,
        localDate = date,
        brewCount = count,
        createdAt = now,
        updatedAt = now,
    )
}
