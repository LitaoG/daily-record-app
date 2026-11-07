package io.github.litaog.dailyrecord.ui.statistics

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartScrubTest {
    @Test
    fun monthIndexInvertsTheSharedDayPosition() {
        listOf(28, 29, 30, 31).forEach { dayCount ->
            listOf(300f, 712f, 1080f).forEach { width ->
                (1..dayCount).forEach { day ->
                    val x = monthDailyChartXPosition(day, dayCount, width, inset = 12f)
                    assertEquals(
                        "day $day of $dayCount at width $width",
                        day - 1,
                        monthDailyChartIndexAt(x, dayCount, width, inset = 12f),
                    )
                }
            }
        }
    }

    @Test
    fun monthIndexSnapsToTheNearestDayAndClampsOutsideThePlot() {
        // 31 days on 312px with a 6px inset: one day every 10px.
        assertEquals(0, monthDailyChartIndexAt(-40f, 31, 312f, 6f))
        assertEquals(0, monthDailyChartIndexAt(10.9f, 31, 312f, 6f))
        assertEquals(1, monthDailyChartIndexAt(11.1f, 31, 312f, 6f))
        assertEquals(30, monthDailyChartIndexAt(400f, 31, 312f, 6f))
    }

    @Test
    fun monthIndexHandlesDegenerateCharts() {
        assertEquals(0, monthDailyChartIndexAt(50f, 1, 300f, 6f))
        assertEquals(0, monthDailyChartIndexAt(50f, 0, 300f, 6f))
        assertEquals(0, monthDailyChartIndexAt(50f, 31, 10f, 6f))
    }

    @Test
    fun yearIndexUsesEqualMonthSlots() {
        val width = 600f
        (0 until 12).forEach { month ->
            assertEquals(month, yearLineChartIndexAt(width * (month + .5f) / 12f, width))
        }
        assertEquals(0, yearLineChartIndexAt(49.5f, width))
        assertEquals(1, yearLineChartIndexAt(50.5f, width))
    }

    @Test
    fun yearIndexClampsOutsideThePlotAndHandlesAnEmptyWidth() {
        assertEquals(0, yearLineChartIndexAt(-5f, 600f))
        assertEquals(11, yearLineChartIndexAt(605f, 600f))
        assertEquals(0, yearLineChartIndexAt(10f, 0f))
    }

    @Test
    fun bubbleCentersOnTheProbeAndStaysInsideThePlot() {
        assertEquals(150, chartBubbleX(anchorX = 200f, bubbleWidth = 100, containerWidth = 600))
        assertEquals(0, chartBubbleX(anchorX = 10f, bubbleWidth = 100, containerWidth = 600))
        assertEquals(500, chartBubbleX(anchorX = 590f, bubbleWidth = 100, containerWidth = 600))
        // A bubble wider than the plot starts at the left edge.
        assertEquals(0, chartBubbleX(anchorX = 50f, bubbleWidth = 700, containerWidth = 600))
    }

    @Test
    fun scrubStateKeepsTheLastPointWhileFadingOut() {
        val state = ChartScrubState()
        state.update(4)
        state.update(5)
        state.update(null)

        assertEquals(null, state.activeIndex)
        assertEquals(5, state.shownIndex)
    }
}
