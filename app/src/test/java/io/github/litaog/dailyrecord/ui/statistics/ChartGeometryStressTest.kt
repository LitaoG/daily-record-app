package io.github.litaog.dailyrecord.ui.statistics

import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.common.AppLanguage
import io.github.litaog.dailyrecord.core.common.AppLanguageState
import io.github.litaog.dailyrecord.core.common.TerminologyStyle
import io.github.litaog.dailyrecord.core.common.strings
import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.core.statistics.EARLIEST_SUPPORTED_DATE
import io.github.litaog.dailyrecord.core.statistics.StatisticsDetail
import io.github.litaog.dailyrecord.core.statistics.StatisticsPeriod
import io.github.litaog.dailyrecord.core.statistics.QuarterStatistics
import io.github.litaog.dailyrecord.core.statistics.YearStatistics
import io.github.litaog.dailyrecord.core.statistics.buildDailyCountStatistics
import io.github.litaog.dailyrecord.core.statistics.nextPeriodAnchor
import io.github.litaog.dailyrecord.core.statistics.previousPeriodAnchor
import java.time.LocalDate
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test


/**
 * Boundary stress tests for the chart geometry math: index lookups under
 * extreme coordinates and degenerate dimensions, bubble placement and scrub
 * state retention, and week-ring label distance invariants (JVM unit tests;
 * no instrumentation).
 */
class ChartGeometryStressTest {

    @Test
    fun monthDailyChartIndexAtHandlesExtremeCoordinatesAndBoundaryClamping() {
        val dayCounts = listOf(1, 2, 28, 29, 30, 31, 100)
        val insets = listOf(0f, 5f, 12f, 50f)
        val widths = listOf(100f, 300f, 720f, 1080f, 2560f)

        for (dayCount in dayCounts) {
            for (inset in insets) {
                for (width in widths) {
                    // 1. Extreme negative X
                    val negResult = monthDailyChartIndexAt(-10_000f, dayCount, width, inset)
                    assertEquals("Extreme negative X must clamp to 0", 0, negResult)

                    // 2. Extreme positive X
                    val maxResult = monthDailyChartIndexAt(100_000f, dayCount, width, inset)
                    val expectedMax = if (dayCount <= 1 || width <= inset * 2f) 0 else dayCount - 1
                    assertEquals("Extreme positive X must clamp to dayCount - 1", expectedMax, maxResult)

                    // 3. Exact boundary points
                    val atInset = monthDailyChartIndexAt(inset, dayCount, width, inset)
                    assertEquals("At left inset must be 0", 0, atInset)

                    val atRightInset = monthDailyChartIndexAt(width - inset, dayCount, width, inset)
                    assertEquals("At right inset must be dayCount - 1", expectedMax, atRightInset)

                    // 4. Middle fraction
                    val midX = (inset + width - inset) / 2f
                    val midResult = monthDailyChartIndexAt(midX, dayCount, width, inset)
                    val validMax = if (dayCount <= 1 || width <= inset * 2f) 1 else dayCount
                    assertTrue("Midpoint must be in valid range", midResult in 0 until validMax)

                    // 5. Inversion round-trip for each day
                    if (dayCount > 1 && width > inset * 2f) {
                        for (day in 1..dayCount) {
                            val x = monthDailyChartXPosition(day, dayCount, width, inset)
                            val resolvedIndex = monthDailyChartIndexAt(x, dayCount, width, inset)
                            assertEquals(
                                "Round-trip day $day of $dayCount at width $width",
                                day - 1,
                                resolvedIndex,
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun monthDailyChartIndexAtToleratesDegenerateDimensionsAndSpecialFloats() {
        // Zero day count
        assertEquals(0, monthDailyChartIndexAt(50f, 0, 300f, 5f))
        // Negative day count
        assertEquals(0, monthDailyChartIndexAt(50f, -5, 300f, 5f))
        // Zero width
        assertEquals(0, monthDailyChartIndexAt(50f, 30, 0f, 5f))
        // Negative width
        assertEquals(0, monthDailyChartIndexAt(50f, 30, -100f, 5f))
        // Inset larger than width
        assertEquals(0, monthDailyChartIndexAt(50f, 30, 50f, 100f))

        // Special float values: Infinities clamp cleanly, while NaN raises IllegalArgumentException via coerceIn
        try {
            monthDailyChartIndexAt(Float.NaN, 31, 300f, 5f)
        } catch (e: IllegalArgumentException) {
            // Expected: Kotlin coerceIn throws when encountering NaN
        }
        assertEquals(0, monthDailyChartIndexAt(Float.NEGATIVE_INFINITY, 31, 300f, 5f))
        assertEquals(30, monthDailyChartIndexAt(Float.POSITIVE_INFINITY, 31, 300f, 5f))
    }

    @Test
    fun yearLineChartIndexAtHandlesExtremeCoordinatesAndBoundaryClamping() {
        val widths = listOf(100f, 320f, 720f, 1080f)
        val monthCounts = listOf(1, 4, 12, 24)

        for (width in widths) {
            for (monthCount in monthCounts) {
                // Negative coordinate
                assertEquals(0, yearLineChartIndexAt(-100f, width, monthCount))
                // Exact left edge
                assertEquals(0, yearLineChartIndexAt(0f, width, monthCount))
                // Beyond width
                assertEquals(monthCount - 1, yearLineChartIndexAt(width * 2f, width, monthCount))

                // Each slot center must resolve to its exact index
                for (m in 0 until monthCount) {
                    val slotCenter = width * (m + 0.5f) / monthCount.toFloat()
                    assertEquals(
                        "Slot center for month $m at count $monthCount",
                        m,
                        yearLineChartIndexAt(slotCenter, width, monthCount),
                    )
                }
            }
        }

        // Degenerate inputs
        assertEquals(0, yearLineChartIndexAt(50f, 0f, 12))
        assertEquals(0, yearLineChartIndexAt(50f, -100f, 12))
        assertEquals(0, yearLineChartIndexAt(50f, 300f, 0))
        assertEquals(0, yearLineChartIndexAt(50f, 300f, -5))

        // Special float values
        assertEquals(0, yearLineChartIndexAt(Float.NaN, 300f, 12))
        assertEquals(0, yearLineChartIndexAt(Float.NEGATIVE_INFINITY, 300f, 12))
        assertEquals(11, yearLineChartIndexAt(Float.POSITIVE_INFINITY, 300f, 12))
    }

    @Test
    fun chartBubbleXStrictlyGuaranteesPlacementWithinPlotBounds() {
        val bubbleWidth = 100
        val containerWidth = 400

        // Test across range from far negative to far positive
        val testAnchors = listOf(
            -200f, -1f, 0f, 50f, 100f, 200f, 300f, 350f, 400f, 401f, 1_000f,
            Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
        )

        for (anchor in testAnchors) {
            val bubbleX = chartBubbleX(anchor, bubbleWidth, containerWidth)
            val maxAllowedX = containerWidth - bubbleWidth // 300

            assertTrue(
                "bubbleX ($bubbleX) for anchor $anchor must be >= 0",
                bubbleX >= 0,
            )
            assertTrue(
                "bubbleX ($bubbleX) for anchor $anchor must be <= $maxAllowedX",
                bubbleX <= maxAllowedX,
            )
        }

        // Float.NaN roundToInt throws IllegalArgumentException
        try {
            chartBubbleX(Float.NaN, bubbleWidth, containerWidth)
        } catch (e: IllegalArgumentException) {
            // Expected: Kotlin roundToInt throws on NaN
        }

        // Bubble wider than container (container = 300, bubble = 450)
        val overflowBubbleX = chartBubbleX(150f, bubbleWidth = 450, containerWidth = 300)
        assertEquals("Bubble wider than container must start at left 0", 0, overflowBubbleX)

        // Zero or negative container
        assertEquals(0, chartBubbleX(50f, bubbleWidth = 50, containerWidth = 0))
        assertEquals(0, chartBubbleX(50f, bubbleWidth = 50, containerWidth = -100))
    }

    @Test
    fun chartScrubStateGracefullyRetainsShownIndexDuringFadeOut() {
        val state = ChartScrubState()
        assertNull("Initial activeIndex must be null", state.activeIndex)
        assertNull("Initial shownIndex must be null", state.shownIndex)

        // User touches index 3
        state.update(3)
        assertEquals(3, state.activeIndex)
        assertEquals(3, state.shownIndex)

        // User scrubs to index 7
        state.update(7)
        assertEquals(7, state.activeIndex)
        assertEquals(7, state.shownIndex)

        // User lifts finger (scrub released)
        state.update(null)
        assertNull("activeIndex must become null on release", state.activeIndex)
        assertEquals("shownIndex must retain 7 for graceful fade-out probe display", 7, state.shownIndex)

        // User touches new position
        state.update(2)
        assertEquals(2, state.activeIndex)
        assertEquals(2, state.shownIndex)
    }

    @Test
    fun weekRingLabelRadialDistanceAndGapsRemainFiniteAndPositive() {
        val outerRadius = 92f
        val labelHalfWidth = 34f
        val labelHalfHeight = 22f

        repeat(7) { index ->
            val angleDegrees = weekRingLabelAngleDegrees(index)
            val distance = weekRingLabelRadialDistance(
                angleDegrees = angleDegrees,
                ringOuterRadius = outerRadius,
                labelHalfWidth = labelHalfWidth,
                labelHalfHeight = labelHalfHeight,
                gap = 12f,
            )

            assertTrue("Label radial distance for segment $index must exceed outer radius", distance > outerRadius)
            assertFalse("Distance must not be NaN", distance.isNaN())
            assertFalse("Distance must not be Infinite", distance.isInfinite())

            val horizontalLimit = weekRingLabelHorizontalLimitDp(angleDegrees, 130f, 6f)
            assertTrue("Horizontal limit must be positive", horizontalLimit > 0f)
        }

        // Shared gap resolution with constrained bounds
        val sharedGap = weekRingSharedLabelGapDp(
            ringOuterRadius = outerRadius,
            labelHalfWidth = labelHalfWidth,
            labelHalfHeight = labelHalfHeight,
            availableHalfWidth = 120f,
            availableHalfHeight = 120f,
            preferredGap = 12f,
        )
        assertTrue("Shared gap must be non-negative", sharedGap >= 0f)
        assertTrue("Shared gap must not exceed preferred gap", sharedGap <= 12f)
    }
}
