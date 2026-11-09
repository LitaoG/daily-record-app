package io.github.litaog.dailyrecord.ui.statistics

import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.core.statistics.StatisticsPeriod
import io.github.litaog.dailyrecord.core.statistics.buildDailyCountStatistics
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Boundary and invariant stress tests for the chart scrub pure functions (JVM unit tests).
 */
class ChartScrubStressTest {

    @Test
    fun monthDailyChartScrubHandlesZeroRecordsAcrossAllMonthLengths() {
        val anchorDates = listOf(
            LocalDate.of(2023, 2, 15), // 28 days
            LocalDate.of(2024, 2, 15), // 29 days (leap)
            LocalDate.of(2026, 4, 15), // 30 days
            LocalDate.of(2026, 1, 15), // 31 days
        )

        for (anchor in anchorDates) {
            val stats = buildDailyCountStatistics(
                period = StatisticsPeriod.Month,
                anchorDate = anchor,
                today = LocalDate.of(2026, 11, 8),
                records = emptyList(),
            )
            val month = stats.month!!
            val days = month.days.size
            val scale = monthDailyChartScale(month)

            // Empty month must produce valid baseline scale [0, 1]
            assertEquals(1L, scale.maximum)
            assertEquals(listOf(0L, 1L), scale.ticks)

            // Probe clamping at extreme coordinates
            val probeCoords = listOf(-500f, -1f, 0f, 10f, 150f, 300f, 301f, 1000f)
            for (x in probeCoords) {
                val index = monthDailyChartIndexAt(x, days, width = 300f, inset = 6f)
                assertTrue("Index $index must be in 0 until $days", index in 0 until days)
            }
        }
    }

    @Test
    fun monthDailyChartScrubHandlesSingleRecordMonthAccurately() {
        val today = LocalDate.of(2026, 11, 30)
        val recordDate = LocalDate.of(2026, 11, 15)
        val recordCount = 42

        val stats = buildDailyCountStatistics(
            period = StatisticsPeriod.Month,
            anchorDate = recordDate,
            today = today,
            records = listOf(DailyCountEntry(recordDate, recordCount)),
        )
        val month = stats.month!!
        val days = month.days.size
        val recordDay = recordDate.dayOfMonth

        val scale = monthDailyChartScale(month)
        assertTrue("Scale max must accommodate recordCount 42", scale.maximum >= 42L)

        // At exactly the x-position of day 15, scrubber must resolve index 14 (day 15 - 1)
        val x = monthDailyChartXPosition(recordDay, days, width = 620f, inset = 10f)
        val resolvedIndex = monthDailyChartIndexAt(x, days, width = 620f, inset = 10f)
        assertEquals(recordDay - 1, resolvedIndex)
    }

    @Test
    fun monthDailyChartScrubHandlesDenseRecordsWithMonotonicInversion() {
        val days = 31
        val widths = listOf(320f, 480f, 720f, 1080f, 1440f)

        for (width in widths) {
            val inset = 12f
            for (day in 1..days) {
                val x = monthDailyChartXPosition(day, days, width, inset)
                val resolvedIndex = monthDailyChartIndexAt(x, days, width, inset)
                assertEquals(
                    "Day $day of $days at width $width must invert monotonically",
                    day - 1,
                    resolvedIndex,
                )
            }
        }
    }

    @Test
    fun annualAreaChartScrubHandlesEmptySingleAndDenseMonths() {
        val today = LocalDate.of(2026, 12, 31)
        val widths = listOf(320f, 600f, 1080f)

        for (width in widths) {
            // 1. Empty year (12 months of zero counts)
            val emptyYear = buildDailyCountStatistics(
                period = StatisticsPeriod.Year,
                anchorDate = today,
                today = today,
                records = emptyList(),
            ).year!!
            val emptyScale = yearLineChartScale(emptyYear)
            assertEquals(1L, emptyScale.maximum)
            assertEquals(listOf(0L, 1L), emptyScale.ticks)

            // Slot mapping for each month 0..11
            for (month in 0 until 12) {
                val slotCenter = width * (month + 0.5f) / 12f
                val resolved = yearLineChartIndexAt(slotCenter, width, 12)
                assertEquals("Month $month slot center must resolve to $month", month, resolved)
            }

            // Extreme clamping
            assertEquals(0, yearLineChartIndexAt(-100f, width, 12))
            assertEquals(11, yearLineChartIndexAt(width + 100f, width, 12))

            // 2. Dense year with distinct monthly counts
            val denseRecords = (1..12).map { month ->
                DailyCountEntry(LocalDate.of(2026, month, 10), month * 3)
            }
            val denseYear = buildDailyCountStatistics(
                period = StatisticsPeriod.Year,
                anchorDate = today,
                today = today,
                records = denseRecords,
            ).year!!
            val denseScale = yearLineChartScale(denseYear)
            assertTrue("Dense year scale maximum must accommodate max month count 36", denseScale.maximum >= 36L)
            assertTrue(denseScale.ticks.size in 3..6)
        }
    }

    @Test
    fun chartBubblePlacementStrictlyAdheresToContainerBounds() {
        val containerWidth = 360
        val bubbleWidth = 90
        val maxAllowedX = containerWidth - bubbleWidth // 270

        val anchorCoordinates = listOf(-100f, 0f, 45f, 100f, 180f, 270f, 315f, 360f, 500f)
        for (anchor in anchorCoordinates) {
            val bubbleX = chartBubbleX(anchor, bubbleWidth, containerWidth)
            assertTrue("bubbleX ($bubbleX) must be >= 0", bubbleX >= 0)
            assertTrue("bubbleX ($bubbleX) must be <= $maxAllowedX", bubbleX <= maxAllowedX)
        }

        // Over-wide bubble clamps to 0
        assertEquals(0, chartBubbleX(180f, bubbleWidth = 400, containerWidth = 360))
        // Zero container clamps to 0
        assertEquals(0, chartBubbleX(50f, bubbleWidth = 50, containerWidth = 0))
    }

    @Test
    fun chartScrubStateUnderConcurrentRapidTouchesMaintainsDeterministicShownIndex() {
        val state = ChartScrubState()

        for (i in 0 until 1_000) {
            val target = i % 31
            state.update(target)
            assertEquals(target, state.activeIndex)
            assertEquals(target, state.shownIndex)

            // Lift finger
            state.update(null)
            assertNull(state.activeIndex)
            assertEquals("shownIndex must be preserved after lift for fade-out probe", target, state.shownIndex)
        }
    }
}
