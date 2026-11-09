package io.github.litaog.dailyrecord.core.statistics

import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsWeekModelsTest {
    private val today = LocalDate.of(2026, 7, 17)

    private fun record(date: LocalDate, count: Int) = DailyCountEntry(
        localDate = date,
        count = count,
    )

    @Test
    fun weekDistinguishesExplicitZeroAndFutureDays() {
        val model = buildDailyCountStatistics(
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
        val model = buildDailyCountStatistics(
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
    fun earliestWeekNeverShowsPre1970Dates() {
        val earliest = LocalDate.of(1970, 1, 1)
        val model = buildDailyCountStatistics(
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
        val model = buildDailyCountStatistics(
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
        val model = buildDailyCountStatistics(
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
        val model = buildDailyCountStatistics(
            period = StatisticsPeriod.Week,
            anchorDate = earliest,
            today = earliest,
            records = emptyList(),
        )

        assertEquals("1970年 1月1日–1月4日", model.title)
        assertEquals(0L, model.summary.totalCount)
    }


    @Test
    fun historicalWeekUsesWeekContainingAnchorWithoutCurrentMonthLeakage() {
        val model = buildDailyCountStatistics(
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
    fun crossYearWeekWithAnchorAtYearEndKeepsJanuaryDaysInTheSameWeek() {
        val today = LocalDate.of(2027, 1, 5)
        val model = buildDailyCountStatistics(
            period = StatisticsPeriod.Week,
            anchorDate = LocalDate.of(2026, 12, 31),
            today = today,
            records = listOf(
                record(LocalDate.of(2026, 12, 29), 3),
                record(LocalDate.of(2027, 1, 2), 2),
            ),
        )

        assertEquals(5L, model.summary.totalCount)
        assertEquals(2, model.summary.recordedDays)
        assertEquals(
            listOf(28, 29, 30, 31),
            model.details.take(4).mapNotNull { it.date?.dayOfMonth },
        )
        assertEquals(
            listOf(1, 2, 3),
            model.details.drop(4).mapNotNull { it.date?.dayOfMonth },
        )
    }

}
