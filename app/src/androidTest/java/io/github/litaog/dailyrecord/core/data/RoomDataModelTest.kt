package io.github.litaog.dailyrecord.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.model.Activity
import io.github.litaog.dailyrecord.core.model.DailyRecord
import io.github.litaog.dailyrecord.core.model.MeasurementType
import io.github.litaog.dailyrecord.core.model.RecordStatus
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDataModelTest {
    private lateinit var database: DailyRecordDatabase
    private lateinit var activityRepository: ActivityRepository
    private lateinit var recordRepository: DailyRecordRepository

    private val ownerId = "local-owner"
    private val now = Instant.parse("2026-07-16T00:00:00Z")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DailyRecordDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        activityRepository = RoomActivityRepository(database)
        recordRepository = RoomDailyRecordRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun seventhActivityIsDataNotASchemaChange() = runBlocking {
        repeat(7) { index ->
            activityRepository.saveActivity(
                activity(
                    id = "activity-$index",
                    name = "活动 ${index + 1}",
                    sortOrder = index,
                    measurementType = MeasurementType.entries[index % MeasurementType.entries.size],
                ),
            )
        }

        val activities = activityRepository.observeActivities(ownerId).first()
        assertEquals(7, activities.size)
        assertEquals((0..6).toList(), activities.map(Activity::sortOrder))
        assertEquals(1, database.openHelper.readableDatabase.version)
    }

    @Test
    fun archiveKeepsHistoricalRecordsAndRemovesActivityFromDefaultList() = runBlocking {
        val handBrew = activity("hand-brew", "手冲", 0, MeasurementType.COUNT)
        activityRepository.saveActivity(handBrew)
        recordRepository.saveRecord(record("record-1", handBrew.id, LocalDate.of(2026, 7, 16), 3))

        activityRepository.archiveActivity(ownerId, handBrew.id, now.plusSeconds(1))

        assertTrue(activityRepository.observeActivities(ownerId).first().isEmpty())
        assertTrue(activityRepository.observeActivities(ownerId, includeArchived = true).first().single().isArchived)
        assertEquals(
            1,
            recordRepository.observeRecords(
                ownerId = ownerId,
                activityId = handBrew.id,
                startDate = LocalDate.of(2026, 1, 1),
                endExclusive = LocalDate.of(2027, 1, 1),
            ).first().size,
        )
    }

    @Test
    fun sameActivityAndDateUpdatesExistingAggregate() = runBlocking {
        val handBrew = activity("hand-brew", "手冲", 0, MeasurementType.COUNT)
        val date = LocalDate.of(2026, 7, 16)
        activityRepository.saveActivity(handBrew)

        val first = recordRepository.saveRecord(record("first-id", handBrew.id, date, 1))
        val second = recordRepository.saveRecord(record("second-id", handBrew.id, date, 3))
        val records = recordRepository.observeRecords(ownerId, date, date.plusDays(1)).first()

        assertEquals(1, records.size)
        assertEquals(first.id, second.id)
        assertNotEquals("second-id", second.id)
        assertEquals(3L, records.single().quantity)
        assertEquals(1L, records.single().revision)
    }

    @Test
    fun zeroMeansMissedAndClearMeansUnset() = runBlocking {
        val handBrew = activity("hand-brew", "手冲", 0, MeasurementType.COUNT)
        val date = LocalDate.of(2026, 7, 16)
        activityRepository.saveActivity(handBrew)

        val zero = recordRepository.saveRecord(
            record("record-1", handBrew.id, date, 0).copy(status = RecordStatus.DONE),
        )
        assertEquals(RecordStatus.MISSED, zero.status)
        assertEquals(0L, zero.quantity)

        assertTrue(recordRepository.clearRecord(ownerId, handBrew.id, date, now.plusSeconds(2)))
        assertTrue(recordRepository.observeRecords(ownerId, date, date.plusDays(1)).first().isEmpty())

        val restored = recordRepository.saveRecord(record("new-id", handBrew.id, date, 2))
        assertEquals(zero.id, restored.id)
        assertEquals(RecordStatus.DONE, restored.status)
        assertEquals(2L, restored.quantity)
        assertEquals(2L, restored.revision)
    }

    @Test
    fun measurementTypeCannotChangeAfterAnyRecordExists() = runBlocking {
        val handBrew = activity("hand-brew", "手冲", 0, MeasurementType.COUNT)
        activityRepository.saveActivity(handBrew)
        recordRepository.saveRecord(record("record-1", handBrew.id, LocalDate.of(2026, 7, 16), 1))

        var rejected = false
        try {
            activityRepository.saveActivity(handBrew.copy(measurementType = MeasurementType.BOOLEAN))
        } catch (_: IllegalStateException) {
            rejected = true
        }

        assertTrue(rejected)
        assertEquals(MeasurementType.COUNT, activityRepository.getActivity(ownerId, handBrew.id)?.measurementType)
    }

    @Test
    fun fixedYearFixturePersists128CountAcross74Days() = runBlocking {
        val handBrew = activity("hand-brew", "手冲", 0, MeasurementType.COUNT)
        activityRepository.saveActivity(handBrew)
        val monthlyCounts = listOf(13, 11, 12, 14, 15, 16, 13, 14, 11, 9)
        val monthlyDays = listOf(9, 7, 8, 8, 10, 9, 8, 9, 4, 2)

        monthlyCounts.zip(monthlyDays).forEachIndexed { monthIndex, (count, days) ->
            val baseQuantity = count / days
            val remainder = count % days
            repeat(days) { dayIndex ->
                val quantity = baseQuantity + if (dayIndex < remainder) 1 else 0
                recordRepository.saveRecord(
                    record(
                        id = "2026-${monthIndex + 1}-${dayIndex + 1}",
                        activityId = handBrew.id,
                        date = LocalDate.of(2026, monthIndex + 1, dayIndex + 1),
                        quantity = quantity.toLong(),
                    ),
                )
            }
        }

        val records = recordRepository.observeRecords(
            ownerId = ownerId,
            activityId = handBrew.id,
            startDate = LocalDate.of(2026, 1, 1),
            endExclusive = LocalDate.of(2027, 1, 1),
        ).first()

        assertEquals(74, records.size)
        assertEquals(128L, records.sumOf { it.quantity ?: 0 })
        assertEquals(74, records.map(DailyRecord::localDate).distinct().size)
        assertEquals(1.7, 128.0 / 74.0, 0.05)
        assertFalse(records.any { it.localDate.monthValue > 10 })
    }

    private fun activity(
        id: String,
        name: String,
        sortOrder: Int,
        measurementType: MeasurementType,
    ) = Activity(
        id = id,
        ownerId = ownerId,
        name = name,
        iconKey = if (id == "hand-brew") "flight" else "activity",
        colorArgb = 0xFFCC5A35.toInt() + sortOrder,
        measurementType = measurementType,
        unit = when (measurementType) {
            MeasurementType.COUNT -> "次"
            MeasurementType.BOOLEAN -> null
            MeasurementType.DURATION -> "分钟"
        },
        sortOrder = sortOrder,
        isArchived = false,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        revision = 0,
    )

    private fun record(
        id: String,
        activityId: String,
        date: LocalDate,
        quantity: Long,
    ) = DailyRecord(
        id = id,
        ownerId = ownerId,
        activityId = activityId,
        localDate = date,
        status = if (quantity == 0L) RecordStatus.MISSED else RecordStatus.DONE,
        quantity = quantity,
        note = null,
        timezoneId = "Asia/Shanghai",
        occurredAt = now,
        createdAt = now,
        updatedAt = now,
        deletedAt = null,
        revision = 0,
    )
}
