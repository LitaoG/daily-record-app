package io.github.litaog.dailyrecord.ui.statistics

import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.ui.components.StatisticsPeriod
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatisticsModelsTest {
    private val today = LocalDate.of(2026, 7, 17)

    @Test
    fun weekDistinguishesExplicitZeroAndFutureDays() {
        val model = buildStatistics(
            period = StatisticsPeriod.Week,
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
        assertNull(model.details[5].count)
        assertNull(model.details[6].days)
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

        val model = buildStatistics(StatisticsPeriod.Year, today, records)

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

    private fun record(date: LocalDate, count: Int) = HandBrewRecord(
        id = date.toString(),
        localDate = date,
        brewCount = count,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}
