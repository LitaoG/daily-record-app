package io.github.litaog.dailyrecord.ui.statistics

import io.github.litaog.dailyrecord.core.statistics.YearMonthStatistics
import io.github.litaog.dailyrecord.core.statistics.YearStatistics
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YearLineChartTest {
    @Test
    fun scaleUsesReadableWholeNumberTicksAboveTheHighestCount() {
        val scale = yearLineChartScale(
            yearStatistics(month(1, count = 7L)),
        )

        assertEquals(8L, scale.maximum)
        assertEquals(listOf(0L, 2L, 4L, 6L, 8L), scale.ticks)
    }

    @Test
    fun normalizesRecordedMonthsAndKeepsExplicitZeroOnTheBaseline() {
        val points = yearLineChartPoints(
            yearStatistics(
                month(1, count = 0L),
                month(2, count = 5L),
                month(3, count = 10L),
            ),
        )

        assertEquals(0f, points[0].fraction)
        assertEquals(.5f, points[1].fraction)
        assertEquals(1f, points[2].fraction)
        assertEquals(0L, points[0].count)
    }

    @Test
    fun unsetAndFutureMonthsNeverBecomeZeroValuePoints() {
        val points = yearLineChartPoints(
            yearStatistics(
                month(1, count = 4L),
                month(2, count = null),
                month(3, count = 12L, future = true),
            ),
        )

        assertNull(points[1].count)
        assertNull(points[1].fraction)
        assertNull(points[2].count)
        assertNull(points[2].fraction)
    }

    @Test
    fun countLabelsRevealOnlyAfterTheSweepReachesTheirMonth() {
        val januaryCenter = .5f / 12f

        assertEquals(0f, yearLineChartPointRevealAlpha(0, 0f), 0.0001f)
        assertEquals(0f, yearLineChartPointRevealAlpha(0, januaryCenter), 0.0001f)
        assertEquals(.5f, yearLineChartPointRevealAlpha(0, januaryCenter + .02f), 0.0001f)
        assertEquals(1f, yearLineChartPointRevealAlpha(0, januaryCenter + .04f), 0.0001f)
        assertEquals(1f, yearLineChartPointRevealAlpha(11, 1f), 0.0001f)
    }

    @Test
    fun scaleFiveStepPhaseKicksInWhenDoublingIsNotEnough() {
        val scale = yearLineChartScale(
            yearStatistics(month(1, count = 11L)),
        )

        assertEquals(15L, scale.maximum)
        assertEquals(listOf(0L, 5L, 10L, 15L), scale.ticks)
    }

    @Test
    fun scaleFourStaysOnSingleStepsAndEmptyYearStaysAtOne() {
        val scale = yearLineChartScale(
            yearStatistics(month(1, count = 4L)),
        )
        assertEquals(4L, scale.maximum)
        assertEquals(listOf(0L, 1L, 2L, 3L, 4L), scale.ticks)

        val empty = yearLineChartScale(yearStatistics())
        assertEquals(1L, empty.maximum)
    }

    private fun month(
        number: Int,
        count: Long?,
        future: Boolean = false,
    ): YearMonthStatistics = YearMonthStatistics(
        month = YearMonth.of(2026, number),
        count = count,
        recordedDays = count?.let { if (it > 0L) 1 else 0 },
        recorded = count != null,
        future = future,
        inProgress = false,
    )

    private fun yearStatistics(vararg months: YearMonthStatistics): YearStatistics = YearStatistics(
        months = months.toList(),
        quarters = emptyList(),
        monthlyAverage = 0.0,
        maximumMonths = emptyList(),
        minimumMonths = emptyList(),
    )
}
