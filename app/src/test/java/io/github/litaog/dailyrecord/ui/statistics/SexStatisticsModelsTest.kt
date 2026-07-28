package io.github.litaog.dailyrecord.ui.statistics

import io.github.litaog.dailyrecord.ui.DailyCountEntry
import io.github.litaog.dailyrecord.ui.components.StatisticsPeriod
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SexStatisticsModelsTest {
    private val today = LocalDate.of(2026, 7, 17)
    private val sexEntries = listOf(
        DailyCountEntry(LocalDate.of(2025, 12, 31), 2),
        DailyCountEntry(LocalDate.of(2026, 7, 13), 1),
        DailyCountEntry(LocalDate.of(2026, 7, 14), 0),
        DailyCountEntry(LocalDate.of(2026, 7, 16), 2),
        DailyCountEntry(LocalDate.of(2026, 8, 1), 99),
    )

    @Test
    fun sexCountsRemainIndependentAcrossEveryStatisticsPeriod() {
        val week = buildDailyCountStatistics(
            StatisticsPeriod.Week,
            LocalDate.of(2026, 7, 16),
            today,
            sexEntries,
        )
        val month = buildDailyCountStatistics(
            StatisticsPeriod.Month,
            LocalDate.of(2026, 7, 1),
            today,
            sexEntries,
        )
        val year = buildDailyCountStatistics(
            StatisticsPeriod.Year,
            LocalDate.of(2026, 1, 1),
            today,
            sexEntries,
        )
        val all = buildDailyCountStatistics(
            StatisticsPeriod.All,
            today,
            today,
            sexEntries,
        )

        assertEquals(3L, week.summary.totalCount)
        assertEquals(2, week.summary.recordedDays)
        assertEquals(3L, month.summary.totalCount)
        assertEquals(2, month.summary.recordedDays)
        assertEquals(3L, year.summary.totalCount)
        assertEquals(2, year.summary.recordedDays)
        assertEquals(5L, all.summary.totalCount)
        assertEquals(3, all.summary.recordedDays)
    }

    @Test
    fun explicitZeroHasNoDayOrBarValueWhileFutureStaysEmpty() {
        val model = buildDailyCountStatistics(
            StatisticsPeriod.Week,
            LocalDate.of(2026, 7, 14),
            today,
            sexEntries,
        )
        val explicitZero = model.details[1]
        val future = model.details[5]

        assertEquals(0L, explicitZero.count)
        assertEquals(0, explicitZero.days)
        assertTrue(explicitZero.recorded)
        assertFalse(explicitZero.future)
        assertNull(future.count)
        assertNull(future.days)
        assertFalse(future.recorded)
        assertTrue(future.future)
    }
}
