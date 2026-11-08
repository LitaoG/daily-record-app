package io.github.litaog.dailyrecord.core.statistics

import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsMonthModelsTest {
    private val today = LocalDate.of(2026, 7, 17)

    private fun record(date: LocalDate, count: Int) = DailyCountEntry(
        localDate = date,
        count = count,
    )

    @Test
    fun summaryAverageUsesOnlyRecordedDaysAndHandlesEmptyData() {
        assertEquals(1.7, StatisticsSummary(128, 74).average, 0.05)
        assertEquals(0.0, StatisticsSummary(0, 0).average, 0.0)
    }

    @Test
    fun historicalMonthUsesAnchorAndDailyDetailsReconcile() {
        val model = buildDailyCountStatistics(
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
        assertEquals(5L, model.summary.totalCount)
        assertEquals(2, model.summary.recordedDays)
        assertEquals(5L, model.details.sumOf { it.count ?: 0L })
        assertEquals(2, model.details.sumOf { it.days ?: 0 })
        assertEquals("1日", model.details.first().label)
        assertEquals("31日", model.details.last().label)
        val month = requireNotNull(model.month)
        assertEquals(31, month.days.size)
        assertEquals(3, month.savedDays)
        assertEquals(1, month.explicitZeroDays)
        assertEquals(3L, month.maximum?.count)
        assertEquals(2L, month.minimumPositive?.count)
    }


    @Test
    fun changingMonthRebuildsDailyDetailsWithoutKeepingPreviousMonth() {
        val records = listOf(
            record(LocalDate.of(2026, 5, 4), 2),
            record(LocalDate.of(2026, 6, 8), 7),
        )

        val may = buildDailyCountStatistics(
            StatisticsPeriod.Month,
            LocalDate.of(2026, 5, 17),
            today,
            records,
        )
        val june = buildDailyCountStatistics(
            StatisticsPeriod.Month,
            LocalDate.of(2026, 6, 17),
            today,
            records,
        )

        assertEquals(2L, may.summary.totalCount)
        assertEquals(2L, may.details.sumOf { it.count ?: 0L })
        assertEquals(7L, june.summary.totalCount)
        assertEquals(7L, june.details.sumOf { it.count ?: 0L })
        assertFalse(june.details.any { it.count == 2L })
    }


    @Test
    fun totalsUseLongAndDoNotOverflowAcrossHighCountDays() {
        val model = buildDailyCountStatistics(
            period = StatisticsPeriod.Month,
            anchorDate = LocalDate.of(2026, 5, 17),
            today = today,
            records = listOf(
                record(LocalDate.of(2026, 5, 4), Int.MAX_VALUE),
                record(LocalDate.of(2026, 5, 5), Int.MAX_VALUE),
            ),
        )

        assertEquals(4_294_967_294L, model.summary.totalCount)
        assertEquals(4_294_967_294L, model.details.sumOf { it.count ?: 0L })
    }


    @Test
    fun monthStatisticsSeparatesSavedZeroUnfilledAndFutureDays() {
        val model = buildDailyCountStatistics(
            period = StatisticsPeriod.Month,
            anchorDate = LocalDate.of(2026, 8, 15),
            today = LocalDate.of(2026, 8, 15),
            records = listOf(
                record(LocalDate.of(2026, 8, 1), 0),
                record(LocalDate.of(2026, 8, 3), 2),
                record(LocalDate.of(2026, 8, 10), 1),
            ),
        )

        val month = requireNotNull(model.month)
        assertEquals(31, month.days.size)
        assertTrue(month.days[0].recorded)
        assertEquals(0L, month.days[0].count)
        assertFalse(month.days[14].future)
        assertTrue(month.days[15].future)
        assertNull(month.days[15].count)
        assertFalse(month.days[15].recorded)
        assertEquals(3, month.savedDays)
        assertEquals(1, month.explicitZeroDays)
        assertEquals(1, month.oneCountDays)
        assertEquals(1, month.twoCountDays)
        assertEquals(0, month.threePlusCountDays)
        assertEquals(12, month.unfilledElapsedDays)
        assertEquals(16, month.futureDays)
        assertEquals(2L, month.maximum?.count)
        assertEquals(1L, month.minimumPositive?.count)
    }


    @Test
    fun leapFebruaryContainsEveryRealDayWithoutWeekBuckets() {
        val model = buildDailyCountStatistics(
            period = StatisticsPeriod.Month,
            anchorDate = LocalDate.of(2028, 2, 10),
            today = LocalDate.of(2028, 2, 29),
            records = emptyList(),
        )

        val month = requireNotNull(model.month)
        assertEquals(29, month.days.size)
        assertEquals(LocalDate.of(2028, 2, 1), month.days.first().date)
        assertEquals(LocalDate.of(2028, 2, 29), month.days.last().date)
        assertTrue(month.days.all { !it.future })
        assertFalse(month.days.last().recorded)
        assertEquals(29, month.unfilledElapsedDays)
        assertEquals(0, month.futureDays)
    }


    @Test
    fun monthCompositionAndExtremesExcludeZeroFromMinimum() {
        val model = buildDailyCountStatistics(
            period = StatisticsPeriod.Month,
            anchorDate = LocalDate.of(2026, 7, 15),
            today = LocalDate.of(2026, 8, 2),
            records = listOf(
                record(LocalDate.of(2026, 7, 2), 0),
                record(LocalDate.of(2026, 7, 3), 2),
                record(LocalDate.of(2026, 7, 7), 1),
                record(LocalDate.of(2026, 7, 8), 0),
                record(LocalDate.of(2026, 7, 12), 2),
                record(LocalDate.of(2026, 7, 18), 1),
                record(LocalDate.of(2026, 7, 23), 3),
                record(LocalDate.of(2026, 7, 27), 3),
            ),
        )

        val month = requireNotNull(model.month)
        assertEquals(12L, model.summary.totalCount)
        assertEquals(6, model.summary.recordedDays)
        assertEquals(8, month.savedDays)
        assertEquals(2, month.explicitZeroDays)
        assertEquals(2, month.oneCountDays)
        assertEquals(2, month.twoCountDays)
        assertEquals(2, month.threePlusCountDays)
        assertEquals(23, month.unfilledElapsedDays)
        assertEquals(0, month.futureDays)
        assertEquals(3L, month.maximum?.count)
        assertEquals(1L, month.minimumPositive?.count)
    }


    @Test
    fun emptyMonthHasNoDayExtremes() {
        val today = LocalDate.of(2026, 8, 2)
        val model = buildDailyCountStatistics(
            period = StatisticsPeriod.Month,
            anchorDate = LocalDate.of(2026, 7, 15),
            today = today,
            records = emptyList(),
        )

        val month = requireNotNull(model.month)
        assertNull(month.maximum)
        assertNull(month.minimumPositive)
    }


}
