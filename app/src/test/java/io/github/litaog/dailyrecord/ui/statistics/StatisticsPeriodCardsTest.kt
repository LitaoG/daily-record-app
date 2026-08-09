package io.github.litaog.dailyrecord.ui.statistics

import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsPeriodCardsTest {
    @Test
    fun `ring state keeps zero, unfilled and future distinct`() {
        assertEquals(
            WeekRingState.ExplicitZero,
            weekRingState(StatisticsDetail("周三 22日", count = 0L, days = 0, recorded = true)),
        )
        assertEquals(
            WeekRingState.Unrecorded,
            weekRingState(StatisticsDetail("周一 20日", count = null, days = null, recorded = false)),
        )
        assertEquals(
            WeekRingState.Future,
            weekRingState(
                StatisticsDetail(
                    "周四 23日",
                    count = null,
                    days = null,
                    future = true,
                    recorded = false,
                ),
            ),
        )
    }

    @Test
    fun `positive ring counts use fixed one two three and four plus bands`() {
        assertEquals(
            WeekRingCountBand.One,
            weekRingCountBand(StatisticsDetail("周一 20日", count = 1L, days = 1)),
        )
        assertEquals(
            WeekRingCountBand.Two,
            weekRingCountBand(StatisticsDetail("周二 21日", count = 2L, days = 1)),
        )
        assertEquals(
            WeekRingCountBand.Three,
            weekRingCountBand(StatisticsDetail("周三 22日", count = 3L, days = 1)),
        )
        assertEquals(
            WeekRingCountBand.FourPlus,
            weekRingCountBand(StatisticsDetail("周四 23日", count = 4L, days = 1)),
        )
        assertEquals(
            WeekRingCountBand.FourPlus,
            weekRingCountBand(StatisticsDetail("周五 24日", count = 12L, days = 1)),
        )
    }

    @Test
    fun `non-positive states do not receive a count color band`() {
        val zero = StatisticsDetail("周一 20日", count = 0L, days = 0, recorded = true)
        val future = StatisticsDetail("周四 23日", count = null, days = null, future = true, recorded = false)

        assertEquals(null, weekRingCountBand(zero))
        assertEquals(null, weekRingCountBand(future))
    }

    @Test
    fun `reference labels occupy gaps and arcs precede their labels`() {
        repeat(7) { index ->
            assertEquals((index - 1) * 360f / 7f, weekRingLabelAngleDegrees(index), 0.001f)
            assertEquals(
                weekRingLabelAngleDegrees(index) - 360f / 14f - 90f,
                weekRingCanvasMidpointDegrees(index),
                0.001f,
            )
        }
    }

    @Test
    fun `count bands keep four visible colors independent`() {
        val colors = WeekRingCountBand.entries.map { weekRingColorForBand(it, HandBrewColorTokens) }

        assertEquals(4, colors.distinct().size)
        assertNotEquals(colors[0], colors[1])
        assertNotEquals(colors[1], colors[2])
        assertNotEquals(colors[2], colors[3])
    }

    @Test
    fun `label radial distance compensates for top and side text boxes`() {
        val top = weekRingLabelRadialDistance(
            angleDegrees = 0f,
            ringOuterRadius = 100f,
            labelHalfWidth = 34f,
            labelHalfHeight = 22f,
            gap = 8f,
        )
        val side = weekRingLabelRadialDistance(
            angleDegrees = 90f,
            ringOuterRadius = 100f,
            labelHalfWidth = 34f,
            labelHalfHeight = 22f,
            gap = 8f,
        )

        assertEquals(130f, top, 0.001f)
        assertEquals(142f, side, 0.001f)
        assertTrue(side > top)
    }
}
