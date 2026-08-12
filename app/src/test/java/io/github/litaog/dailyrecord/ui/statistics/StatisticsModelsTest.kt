package io.github.litaog.dailyrecord.ui.statistics

import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.ui.statistics.StatisticsPeriod
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
    fun summaryAverageUsesOnlyRecordedDaysAndHandlesEmptyData() {
        assertEquals(1.7, StatisticsSummary(128, 74).average, 0.05)
        assertEquals(0.0, StatisticsSummary(0, 0).average, 0.0)
    }

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

        assertEquals(5L, model.summary.totalCount)
        assertEquals(4, model.summary.recordedDays)
        assertEquals(0L, model.details[1].count)
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

        assertEquals(0L, model.details[0].count)
        assertFalse(model.details[0].recorded)
        assertFalse(model.details[0].future)
        assertEquals(0L, model.details[1].count)
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

        assertEquals(128L, model.summary.totalCount)
        assertEquals(7, model.summary.recordedDays)
        assertEquals(128L, model.details.filterNot { it.future }.sumOf { it.count ?: 0L })
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

        assertEquals(326L, model.summary.totalCount)
        assertEquals(listOf("2026年", "2025年", "2024年"), model.details.map { it.label })
        assertEquals(326L, model.details.sumOf { it.count ?: 0L })
    }

    @Test
    fun earliestWeekNeverShowsPre1970Dates() {
        val earliest = LocalDate.of(1970, 1, 1)
        val model = buildStatistics(
            period = StatisticsPeriod.Week,
            anchorDate = earliest,
            today = LocalDate.of(1970, 1, 4),
            records = listOf(
                record(earliest, 2),
                record(LocalDate.of(1970, 1, 2), 1),
            ),
        )

        // 1970-01-01 is a Thursday; the Monday of that week (1969-12-29) must
        // not appear. The visible week starts at the supported boundary.
        assertEquals("1970年 1月1日–1月4日", model.title)
        assertEquals(4, model.details.size)
        assertEquals(3L, model.summary.totalCount)
        assertFalse(model.details.any { it.future })
        // The visible list is clipped, but each item retains its Monday-based
        // source index so the ring chart keeps Thursday on the fourth segment.
        assertEquals(listOf(3, 4, 5, 6), model.details.map { it.calendarIndex })
        assertTrue(model.details[0].recorded)
        assertTrue(model.details[1].recorded)
    }

    @Test
    fun earliestWeekStillMarksFutureDaysInsideSupportedRange() {
        val earliest = LocalDate.of(1970, 1, 1)
        val model = buildStatistics(
            period = StatisticsPeriod.Week,
            anchorDate = earliest,
            today = earliest,
            records = listOf(record(earliest, 2)),
        )

        assertEquals(4, model.details.size)
        assertEquals(1, model.details.count { !it.future })
        assertEquals(3, model.details.count { it.future })
    }

    @Test
    fun crossYearWeekEndingInJanuaryKeepsMondayStartAndSummary() {
        val model = buildStatistics(
            period = StatisticsPeriod.Week,
            anchorDate = LocalDate.of(2026, 1, 2),
            today = LocalDate.of(2026, 1, 4),
            records = listOf(
                record(LocalDate.of(2025, 12, 31), 1),
                record(LocalDate.of(2026, 1, 1), 2),
                record(LocalDate.of(2026, 1, 2), 3),
            ),
        )

        assertEquals("2025年12月29日–2026年1月4日", model.title)
        assertEquals(7, model.details.size)
        assertEquals(6L, model.summary.totalCount)
        assertEquals(0, model.details.count { it.future })
        assertEquals(1L, model.details[2].count)
        assertEquals(2L, model.details[3].count)
        assertEquals(3L, model.details[4].count)
    }

    @Test
    fun earliestWeekTitleUsesClippedStartNotMondayOfPre1970Week() {
        val earliest = LocalDate.of(1970, 1, 1)
        val model = buildStatistics(
            period = StatisticsPeriod.Week,
            anchorDate = earliest,
            today = earliest,
            records = emptyList(),
        )

        assertEquals("1970年 1月1日–1月4日", model.title)
        assertEquals(0L, model.summary.totalCount)
    }

    @Test
    fun historicalMonthUsesAnchorAndDailyDetailsReconcile() {
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

        assertEquals(2L, may.summary.totalCount)
        assertEquals(2L, may.details.sumOf { it.count ?: 0L })
        assertEquals(7L, june.summary.totalCount)
        assertEquals(7L, june.details.sumOf { it.count ?: 0L })
        assertFalse(june.details.any { it.count == 2L })
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
        assertEquals(3L, model.summary.totalCount)
        assertEquals(2, model.summary.recordedDays)
        assertEquals("周三 6日", model.details[2].label)
        assertEquals(3L, model.details.sumOf { it.count ?: 0L })
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
        assertEquals(2L, model.details.sumOf { it.count ?: 0L })
    }

    @Test
    fun totalsUseLongAndDoNotOverflowAcrossHighCountDays() {
        val model = buildStatistics(
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
        val model = buildStatistics(
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
        val model = buildStatistics(
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
        val model = buildStatistics(
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
    fun yearAnalysisExcludesIncompleteMonthFromExtremaButKeepsItsTotals() {
        val today = LocalDate.of(2026, 7, 17)
        val model = buildStatistics(
            period = StatisticsPeriod.Year,
            anchorDate = today,
            today = today,
            records = listOf(
                record(LocalDate.of(2026, 1, 3), 10),
                record(LocalDate.of(2026, 2, 3), 0),
                record(LocalDate.of(2026, 3, 3), 10),
                record(LocalDate.of(2026, 4, 3), 5),
                record(LocalDate.of(2026, 5, 3), 5),
                record(LocalDate.of(2026, 7, 3), 99),
            ),
        )

        val year = requireNotNull(model.year)
        assertEquals(129L, model.summary.totalCount)
        assertEquals(129L, year.quarters.sumOf { it.totalCount })
        assertEquals(129L, year.months.sumOf { it.count ?: 0L })
        assertEquals(listOf(1, 3), year.maximumMonths.map { it.month.monthValue })
        assertEquals(listOf(2), year.minimumMonths.map { it.month.monthValue })
        assertTrue(year.months[6].inProgress)
        assertFalse(year.months[6].complete)
        assertEquals(5, year.months.count { it.future })
        assertEquals(129.0 / 7.0, year.monthlyAverage, 0.001)
    }

    @Test
    fun quarterSlicesAssignMonthsToTheCorrectCalendarQuarter() {
        val today = LocalDate.of(2026, 12, 31)
        val model = buildStatistics(
            period = StatisticsPeriod.Year,
            anchorDate = LocalDate.of(2026, 12, 31),
            today = today,
            records = listOf(
                // Q1: 1-3月; Q2: 4-6月; Q3: 7-9月; Q4: 10-12月.
                record(LocalDate.of(2026, 1, 3), 1),
                record(LocalDate.of(2026, 2, 3), 2),
                record(LocalDate.of(2026, 3, 3), 3),
                record(LocalDate.of(2026, 4, 3), 4),
                record(LocalDate.of(2026, 5, 3), 5),
                record(LocalDate.of(2026, 6, 3), 6),
                record(LocalDate.of(2026, 7, 3), 7),
                record(LocalDate.of(2026, 8, 3), 8),
                record(LocalDate.of(2026, 9, 3), 9),
                record(LocalDate.of(2026, 10, 3), 10),
                record(LocalDate.of(2026, 11, 3), 11),
                record(LocalDate.of(2026, 12, 3), 12),
            ),
        )

        val year = requireNotNull(model.year)
        // Distinct per-quarter totals make any Q1/Q2 mis-slicing detectable
        // instead of being hidden by the sum-only identity.
        assertEquals(listOf(1, 2, 3, 4), year.quarters.map { it.quarter })
        assertEquals(listOf(6L, 15L, 24L, 33L), year.quarters.map { it.totalCount })
        assertEquals(78L, year.quarters.sumOf { it.totalCount })
        assertEquals(78L, year.months.sumOf { it.count ?: 0L })
    }

    @Test
    fun completedDecemberParticipatesInExtremaAtTheYearBoundary() {
        val today = LocalDate.of(2027, 1, 1)
        val model = buildStatistics(
            period = StatisticsPeriod.Year,
            anchorDate = LocalDate.of(2026, 12, 31),
            today = today,
            records = listOf(
                record(LocalDate.of(2026, 1, 4), 2),
                record(LocalDate.of(2026, 12, 31), 12),
            ),
        )

        val year = requireNotNull(model.year)
        assertTrue(year.months[11].complete)
        assertEquals(listOf(12), year.maximumMonths.map { it.month.monthValue })
        assertEquals(listOf(1), year.minimumMonths.map { it.month.monthValue })
    }

    @Test
    fun currentDecemberIsKeptOutOfExtremaUntilTheYearHasEnded() {
        val today = LocalDate.of(2026, 12, 31)
        val model = buildStatistics(
            period = StatisticsPeriod.Year,
            anchorDate = today,
            today = today,
            records = listOf(
                record(LocalDate.of(2026, 1, 4), 2),
                record(LocalDate.of(2026, 11, 30), 8),
                record(LocalDate.of(2026, 12, 31), 99),
            ),
        )

        val year = requireNotNull(model.year)
        assertTrue(year.months[11].inProgress)
        assertFalse(year.months[11].complete)
        assertEquals(listOf(11), year.maximumMonths.map { it.month.monthValue })
        assertEquals(listOf(1), year.minimumMonths.map { it.month.monthValue })
    }

    @Test
    fun decemberRemainsInTiedExtremaListsInsteadOfBeingDroppedAtTheEnd() {
        val today = LocalDate.of(2027, 1, 1)
        val model = buildStatistics(
            period = StatisticsPeriod.Year,
            anchorDate = LocalDate.of(2026, 12, 20),
            today = today,
            records = listOf(
                record(LocalDate.of(2026, 1, 4), 5),
                record(LocalDate.of(2026, 6, 4), 1),
                record(LocalDate.of(2026, 12, 31), 5),
            ),
        )

        val year = requireNotNull(model.year)
        assertEquals(listOf(1, 12), year.maximumMonths.map { it.month.monthValue })
        assertEquals(listOf(6), year.minimumMonths.map { it.month.monthValue })
    }

    private fun record(date: LocalDate, count: Int) = HandBrewRecord(
        id = date.toString(),
        localDate = date,
        brewCount = count,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}
