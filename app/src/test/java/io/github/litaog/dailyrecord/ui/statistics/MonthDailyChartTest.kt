package io.github.litaog.dailyrecord.ui.statistics

import io.github.litaog.dailyrecord.ui.DailyCountEntry
import io.github.litaog.dailyrecord.ui.components.StatisticsPeriod
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class MonthDailyChartTest {
    @Test
    fun xAxisAnchorsHighlightReadableDatesWithoutDuplicatingShortMonths() {
        assertEquals(listOf(1, 5, 10, 15, 20, 25, 31), monthDailyChartTickDays(31))
        assertEquals(listOf(1, 5, 10, 15, 20, 25, 28), monthDailyChartTickDays(28))
        assertEquals(listOf(1, 5, 10), monthDailyChartTickDays(10))
        assertEquals(emptyList<Int>(), monthDailyChartTickDays(0))
    }

    @Test
    fun xAxisLabelsShareThePlotEdgeAnchors() {
        val first = monthDailyChartXPosition(day = 1, dayCount = 31, width = 300f, inset = 5f)
        val middle = monthDailyChartXPosition(day = 10, dayCount = 31, width = 300f, inset = 5f)
        val last = monthDailyChartXPosition(day = 31, dayCount = 31, width = 300f, inset = 5f)

        assertEquals(5f, first, 0.001f)
        assertEquals(92f, middle, 0.001f)
        assertEquals(295f, last, 0.001f)
        assertEquals(5f, monthDailyChartXPosition(day = 1, dayCount = 1, width = 300f, inset = 5f), 0.001f)
    }

    @Test
    fun scaleUsesReadableTicksAboveLargestDailyCount() {
        val month = monthStatistics(
            DailyCountEntry(LocalDate.of(2026, 7, 3), 2),
            DailyCountEntry(LocalDate.of(2026, 7, 23), 9),
        )

        assertEquals(MonthDailyChartScale(maximum = 10L, ticks = listOf(0L, 5L, 10L)), monthDailyChartScale(month))
    }

    @Test
    fun emptyAndExplicitZeroMonthsKeepARealZeroBaseline() {
        val empty = monthStatistics()
        val explicitZero = monthStatistics(DailyCountEntry(LocalDate.of(2026, 7, 8), 0))

        val expected = MonthDailyChartScale(maximum = 1L, ticks = listOf(0L, 1L))
        assertEquals(expected, monthDailyChartScale(empty))
        assertEquals(expected, monthDailyChartScale(explicitZero))
    }

    private fun monthStatistics(vararg records: DailyCountEntry): MonthStatistics = requireNotNull(
        buildDailyCountStatistics(
            period = StatisticsPeriod.Month,
            anchorDate = LocalDate.of(2026, 7, 15),
            today = LocalDate.of(2026, 8, 2),
            records = records.toList(),
        ).month,
    )
}
