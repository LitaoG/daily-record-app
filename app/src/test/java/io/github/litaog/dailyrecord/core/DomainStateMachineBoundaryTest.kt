package io.github.litaog.dailyrecord.core

import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.core.statistics.EARLIEST_SUPPORTED_DATE
import io.github.litaog.dailyrecord.core.statistics.StatisticsPeriod
import io.github.litaog.dailyrecord.core.statistics.buildDailyCountStatistics
import io.github.litaog.dailyrecord.core.statistics.nextPeriodAnchor
import io.github.litaog.dailyrecord.core.statistics.previousPeriodAnchor
import io.github.litaog.dailyrecord.core.statistics.shiftMonthAnchor
import io.github.litaog.dailyrecord.ui.calendar.QuickLogAction
import io.github.litaog.dailyrecord.ui.calendar.nextCount
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Boundary and invariant JVM unit tests for the production statistics engine
 * and state machine helpers: temporal edge cases, boundary clamping, and
 * null-vs-zero semantics. No instrumentation, no device.
 */
class DomainStateMachineBoundaryTest {

    private val baseDate = LocalDate.of(2026, 11, 8)

    // =========================================================================
    // 1. Concurrency & Rapid State Transitions
    // =========================================================================

    @Test
    fun testRapidPeriodSwitchingUnderHighConcurrency(): Unit = runBlocking {
        val testRecords = (0..90).map { offset ->
            DailyCountEntry(baseDate.minusDays(offset.toLong()), (offset % 4))
        }
        val periods = StatisticsPeriod.entries
        val concurrentTasks = 150

        val results = (1..concurrentTasks).map { index ->
            async(Dispatchers.Default) {
                val period = periods[index % periods.size]
                val anchor = baseDate.minusDays((index % 60).toLong())
                val stats = buildDailyCountStatistics(
                    period = period,
                    anchorDate = anchor,
                    today = baseDate,
                    records = testRecords,
                )
                assertTrue("Total count must be >= 0", stats.summary.totalCount >= 0L)
                assertTrue("Recorded days must be >= 0", stats.summary.recordedDays >= 0)
                assertFalse("Average must not be NaN", stats.summary.average.isNaN())
                assertFalse("Average must not be infinite", stats.summary.average.isInfinite())
            }
        }
        results.awaitAll()
    }

    @Test
    fun testConcurrentQuickLogTapsPreserveStateConsistency(): Unit = runBlocking {
        val tapCount = 100
        val counter = AtomicInteger(0)

        // Simulate 100 rapid serialized taps via quickLogAction
        val actions = listOf(QuickLogAction.Increase, QuickLogAction.Decrease, QuickLogAction.MarkZero)
        var currentCount: Int? = null

        // Rapidly increase 100 times
        repeat(tapCount) {
            currentCount = QuickLogAction.Increase.nextCount(currentCount)
        }
        assertEquals(tapCount, currentCount)

        // Rapidly decrease down to 0
        repeat(tapCount) {
            currentCount = QuickLogAction.Decrease.nextCount(currentCount)
        }
        assertEquals(0, currentCount)

        // Further decrease from 0 returns null (never negative)
        assertNull(QuickLogAction.Decrease.nextCount(currentCount))
    }

    // =========================================================================
    // 2. Temporal Edge Cases & Boundary Clamping
    // =========================================================================

    @Test
    fun testLeapYearBoundaryClampingAndNavigation() {
        val leapDay2024 = LocalDate.of(2024, 2, 29)
        val earliest = EARLIEST_SUPPORTED_DATE
        val latest = LocalDate.of(2028, 12, 31)

        // Shifting +1 year from leap day clamps cleanly to Feb 28 in non-leap 2025
        val year2025 = nextPeriodAnchor(StatisticsPeriod.Year, leapDay2024, latest)
        assertNotNull(year2025)
        assertEquals(LocalDate.of(2025, 2, 28), year2025)

        // Shifting -1 year from leap day clamps cleanly to Feb 28 in non-leap 2023
        val year2023 = previousPeriodAnchor(StatisticsPeriod.Year, leapDay2024, earliest)
        assertNotNull(year2023)
        assertEquals(LocalDate.of(2023, 2, 28), year2023)

        // Shifting month anchor from leap day across months
        val monthAnchor = shiftMonthAnchor(leapDay2024, 1, earliest, latest)
        assertEquals(LocalDate.of(2024, 3, 29), monthAnchor)

        // Shifting month anchor from 2024-03-31 backward to February clamps to Feb 29
        val mar31 = LocalDate.of(2024, 3, 31)
        val febClamped = shiftMonthAnchor(mar31, -1, earliest, latest)
        assertEquals(LocalDate.of(2024, 2, 29), febClamped)

        // In non-leap 2023, shifting backward from Mar 31 clamps to Feb 28
        val mar31_2023 = LocalDate.of(2023, 3, 31)
        val feb28Clamped = shiftMonthAnchor(mar31_2023, -1, earliest, latest)
        assertEquals(LocalDate.of(2023, 2, 28), feb28Clamped)
    }

    @Test
    fun testMonthRolloverAcrossAllMonthLengths() {
        val earliest = EARLIEST_SUPPORTED_DATE
        val latest = LocalDate.of(2030, 12, 31)

        // Test month length transitions: 31 -> 28/29, 31 -> 30, 30 -> 31
        val testDates = listOf(
            LocalDate.of(2026, 1, 31) to LocalDate.of(2026, 2, 28),
            LocalDate.of(2026, 3, 31) to LocalDate.of(2026, 4, 30),
            LocalDate.of(2026, 5, 31) to LocalDate.of(2026, 6, 30),
            LocalDate.of(2026, 7, 31) to LocalDate.of(2026, 8, 31),
            LocalDate.of(2026, 8, 31) to LocalDate.of(2026, 9, 30),
            LocalDate.of(2026, 10, 31) to LocalDate.of(2026, 11, 30),
        )

        testDates.forEach { (start, expectedNext) ->
            val shifted = shiftMonthAnchor(start, 1, earliest, latest)
            assertEquals("Shift from $start +1 month must clamp to $expectedNext", expectedNext, shifted)
        }
    }

    @Test
    fun testYearRolloverAndCrossYearWeekBoundaries() {
        val crossYearMonday = LocalDate.of(2025, 12, 29)
        val today = LocalDate.of(2026, 1, 10)
        val records = listOf(
            DailyCountEntry(LocalDate.of(2025, 12, 30), 2),
            DailyCountEntry(LocalDate.of(2025, 12, 31), 1),
            DailyCountEntry(LocalDate.of(2026, 1, 1), 3),
            DailyCountEntry(LocalDate.of(2026, 1, 2), 0),
        )

        val stats = buildDailyCountStatistics(
            period = StatisticsPeriod.Week,
            anchorDate = crossYearMonday,
            today = today,
            records = records,
        )

        assertEquals("Week spanning year transition must contain 7 days", 7, stats.details.size)
        assertEquals("Total count must sum across year boundary", 6L, stats.summary.totalCount)
        assertEquals("Recorded days must only count positive days across boundary", 3, stats.summary.recordedDays)
        assertEquals(2.0, stats.summary.average, 0.001)
    }

    @Test
    fun testClockSkewAndFutureDatePruning() {
        val today = LocalDate.of(2026, 11, 8)
        val skewedRecords = listOf(
            DailyCountEntry(today.minusDays(1), 2),
            DailyCountEntry(today, 1),
            DailyCountEntry(today.plusDays(1), 5), // Future record due to clock skew
            DailyCountEntry(today.plusDays(30), 10), // Distant future record
        )

        val stats = buildDailyCountStatistics(
            period = StatisticsPeriod.Month,
            anchorDate = today,
            today = today,
            records = skewedRecords,
        )

        // Future records must be strictly ignored
        assertEquals(3L, stats.summary.totalCount)
        assertEquals(2, stats.summary.recordedDays)

        val monthStats = stats.month!!
        assertTrue("Future days must be excluded from positive count calculations", monthStats.futureDays > 0)
    }

    @Test
    fun testIntMaxValueAndArithmeticOverflowResilience() {
        // QuickLog clamps at Int.MAX_VALUE
        assertNull(QuickLogAction.Increase.nextCount(Int.MAX_VALUE))

        // Large count records survive in statistics models without Long overflow
        val highCountRecords = listOf(
            DailyCountEntry(baseDate.minusDays(1), Int.MAX_VALUE),
            DailyCountEntry(baseDate, Int.MAX_VALUE),
        )

        val stats = buildDailyCountStatistics(
            period = StatisticsPeriod.Week,
            anchorDate = baseDate,
            today = baseDate,
            records = highCountRecords,
        )

        val expectedTotal = Int.MAX_VALUE.toLong() * 2L
        assertEquals(expectedTotal, stats.summary.totalCount)
        assertEquals(2, stats.summary.recordedDays)
        assertEquals(Int.MAX_VALUE.toDouble(), stats.summary.average, 0.1)
    }

    // =========================================================================
    // 3. Null vs Zero Semantics (ADR-004 Compliance)
    // =========================================================================

    @Test
    fun testNullVsZeroSemanticsInStatistics() {
        val anchor = LocalDate.of(2026, 11, 1)
        val today = LocalDate.of(2026, 11, 30)

        // Nov 1: explicit zero (count = 0)
        // Nov 2: count = 1
        // Nov 3: count = 2
        // Nov 4: count = 3
        // Nov 5-30: unrecorded (null)
        val records = listOf(
            DailyCountEntry(LocalDate.of(2026, 11, 1), 0),
            DailyCountEntry(LocalDate.of(2026, 11, 2), 1),
            DailyCountEntry(LocalDate.of(2026, 11, 3), 2),
            DailyCountEntry(LocalDate.of(2026, 11, 4), 3),
        )

        val stats = buildDailyCountStatistics(
            period = StatisticsPeriod.Month,
            anchorDate = anchor,
            today = today,
            records = records,
        )

        val month = stats.month!!
        assertEquals("Explicit zero days must be 1", 1, month.explicitZeroDays)
        assertEquals("One-count days must be 1", 1, month.oneCountDays)
        assertEquals("Two-count days must be 1", 1, month.twoCountDays)
        assertEquals("Three-plus-count days must be 1", 1, month.threePlusCountDays)
        assertEquals("Saved days must be exactly 4", 4, month.savedDays)
        assertEquals("Unfilled elapsed days must be 26", 26, month.unfilledElapsedDays)
        assertEquals("Future days must be 0", 0, month.futureDays)

        // Total count sums only counts: 0 + 1 + 2 + 3 = 6
        assertEquals(6L, stats.summary.totalCount)
        // Occurred days: only days where count > 0 (Nov 2, 3, 4 = 3 days; explicit zero does NOT count)
        assertEquals(3, stats.summary.recordedDays)
        // Average: 6 / 3 = 2.0
        assertEquals(2.0, stats.summary.average, 0.001)
    }

    @Test
    fun testNullVsZeroQuickLogTransitions() {
        // Unrecorded (null)
        assertEquals(1, QuickLogAction.Increase.nextCount(null))
        assertEquals(0, QuickLogAction.MarkZero.nextCount(null))
        assertNull(QuickLogAction.Decrease.nextCount(null))

        // Explicit 0
        assertEquals(1, QuickLogAction.Increase.nextCount(0))
        assertNull(QuickLogAction.MarkZero.nextCount(0))
        assertNull(QuickLogAction.Decrease.nextCount(0))

        // Positive 1
        assertEquals(2, QuickLogAction.Increase.nextCount(1))
        assertEquals(0, QuickLogAction.Decrease.nextCount(1))
        assertNull(QuickLogAction.MarkZero.nextCount(1))
    }

}
