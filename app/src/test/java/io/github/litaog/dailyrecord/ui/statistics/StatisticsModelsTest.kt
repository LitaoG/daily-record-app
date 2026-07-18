package io.github.litaog.dailyrecord.ui.statistics

import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.ui.components.StatisticsPeriod
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsModelsTest {
    private val today = LocalDate.of(2026, 7, 17)

    @Test
    fun weekDistinguishesExplicitZeroAndFutureDays() {
        val model = buildStatistics(
            period = StatisticsPeriod.Week,
            anchorDate = today,
            today = today,
            records = listOf(
                record(LocalDate.of(2026, 7, 13), 1),
                record(LocalDate.of(2026, 7, 14), 0),
                record(LocalDate.of(2026, 7, 15), 2),
                record(LocalDate.of(2026, 7, 16), 1),
                record(LocalDate.of(2026, 7, 17), 1),
            ),
        )

        assertEquals(5, model.summary.totalCount)
        assertEquals(4, model.summary.brewDays)
        assertEquals(0, model.details[1].count)
        assertEquals(0, model.details[1].days)
        assertTrue(model.details[1].recorded)
        assertNull(model.details[5].count)
        assertNull(model.details[6].days)
        assertTrue(model.details[5].future)
        assertFalse(model.details[5].recorded)
    }

    @Test
    fun pastUnfilledDayIsNotReportedAsExplicitZero() {
        val model = buildStatistics(
            period = StatisticsPeriod.Week,
            anchorDate = today,
            today = today,
            records = listOf(record(LocalDate.of(2026, 7, 14), 0)),
        )

        assertEquals(0, model.details[0].count)
        assertFalse(model.details[0].recorded)
        assertFalse(model.details[0].future)
        assertEquals(0, model.details[1].count)
        assertTrue(model.details[1].recorded)
    }

    @Test
    fun yearDetailsReconcileWithSummaryAndFutureMonthsStayEmpty() {
        val records = listOf(
            record(LocalDate.of(2026, 1, 3), 12),
            record(LocalDate.of(2026, 2, 4), 15),
            record(LocalDate.of(2026, 3, 5), 18),
            record(LocalDate.of(2026, 4, 6), 20),
            record(LocalDate.of(2026, 5, 7), 21),
            record(LocalDate.of(2026, 6, 8), 24),
            record(LocalDate.of(2026, 7, 9), 18),
        )

        val model = buildStatistics(StatisticsPeriod.Year, today, today, records)

        assertEquals(128, model.summary.totalCount)
        assertEquals(7, model.summary.brewDays)
        assertEquals(128, model.details.filterNot { it.future }.sumOf { it.count ?: 0 })
        assertEquals(5, model.details.count { it.future })
    }

    @Test
    fun allHistoryGroupsByYearAndIgnoresFutureRecords() {
        val model = buildStatistics(
            StatisticsPeriod.All,
            today,
            today,
            listOf(
                record(LocalDate.of(2024, 4, 3), 56),
                record(LocalDate.of(2025, 2, 1), 142),
                record(LocalDate.of(2026, 6, 1), 128),
                record(LocalDate.of(2026, 8, 1), 999),
            ),
        )

        assertEquals(326, model.summary.totalCount)
        assertEquals(listOf("2026年", "2025年", "2024年"), model.details.map { it.label })
        assertEquals(326, model.details.sumOf { it.count ?: 0 })
    }

    @Test
    fun historicalMonthUsesAnchorAndWeeklyDetailsReconcile() {
        val model = buildStatistics(
            period = StatisticsPeriod.Month,
            anchorDate = LocalDate.of(2026, 5, 31),
            today = today,
            records = listOf(
                record(LocalDate.of(2026, 5, 1), 2),
                record(LocalDate.of(2026, 5, 10), 0),
                record(LocalDate.of(2026, 5, 31), 3),
                record(LocalDate.of(2026, 7, 1), 99),
            ),
        )

        assertEquals("2026年 5月", model.title)
        assertEquals("已结束", model.status)
        assertEquals(5, model.summary.totalCount)
        assertEquals(2, model.summary.brewDays)
        assertEquals(5, model.details.sumOf { it.count ?: 0 })
        assertEquals(2, model.details.sumOf { it.days ?: 0 })
        assertEquals("第1周 1–3日", model.details.first().label)
        assertEquals("第5周 25–31日", model.details.last().label)
    }

    @Test
    fun changingMonthRebuildsWeeklyDetailsWithoutKeepingPreviousMonth() {
        val records = listOf(
            record(LocalDate.of(2026, 5, 4), 2),
            record(LocalDate.of(2026, 6, 8), 7),
        )

        val may = buildStatistics(
            StatisticsPeriod.Month,
            LocalDate.of(2026, 5, 17),
            today,
            records,
        )
        val june = buildStatistics(
            StatisticsPeriod.Month,
            LocalDate.of(2026, 6, 17),
            today,
            records,
        )

        assertEquals(2, may.summary.totalCount)
        assertEquals(2, may.details.sumOf { it.count ?: 0 })
        assertEquals(7, june.summary.totalCount)
        assertEquals(7, june.details.sumOf { it.count ?: 0 })
        assertFalse(june.details.any { it.count == 2 })
    }

    @Test
    fun historicalWeekUsesWeekContainingAnchorWithoutCurrentMonthLeakage() {
        val model = buildStatistics(
            period = StatisticsPeriod.Week,
            anchorDate = LocalDate.of(2026, 5, 6),
            today = today,
            records = listOf(
                record(LocalDate.of(2026, 5, 4), 1),
                record(LocalDate.of(2026, 5, 6), 2),
                record(LocalDate.of(2026, 5, 11), 50),
                record(LocalDate.of(2026, 7, 17), 50),
            ),
        )

        assertEquals("2026年 5月4日–5月10日", model.title)
        assertEquals("已结束", model.status)
        assertEquals(3, model.summary.totalCount)
        assertEquals(2, model.summary.brewDays)
        assertEquals("周三 6日", model.details[2].label)
        assertEquals(3, model.details.sumOf { it.count ?: 0 })
    }

    @Test
    fun pastYearContainsNoFuturePlaceholders() {
        val model = buildStatistics(
            period = StatisticsPeriod.Year,
            anchorDate = LocalDate.of(2025, 7, 17),
            today = today,
            records = listOf(record(LocalDate.of(2025, 12, 31), 2)),
        )

        assertEquals("2025年", model.title)
        assertEquals("已结束", model.status)
        assertEquals(0, model.details.count { it.future })
        assertEquals(2, model.details.sumOf { it.count ?: 0 })
    }

    private fun record(date: LocalDate, count: Int) = HandBrewRecord(
        id = date.toString(),
        localDate = date,
        brewCount = count,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}
