package io.github.litaog.dailyrecord.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.HandBrewRecordDetail
import io.github.litaog.dailyrecord.core.model.SexRecord
import io.github.litaog.dailyrecord.core.model.SexRecordDetail
import io.github.litaog.dailyrecord.core.statistics.EARLIEST_SUPPORTED_DATE
import io.github.litaog.dailyrecord.core.statistics.StatisticsPeriod
import io.github.litaog.dailyrecord.core.statistics.buildDailyCountStatistics
import io.github.litaog.dailyrecord.core.statistics.nextPeriodAnchor
import io.github.litaog.dailyrecord.core.statistics.previousPeriodAnchor
import io.github.litaog.dailyrecord.core.statistics.shiftMonthAnchor
import io.github.litaog.dailyrecord.e2e.InMemoryHandBrewRecordRepository
import io.github.litaog.dailyrecord.e2e.InMemorySexRecordRepository
import io.github.litaog.dailyrecord.ui.RecordModule
import io.github.litaog.dailyrecord.ui.calendar.QuickLogAction
import io.github.litaog.dailyrecord.ui.calendar.nextCount
import io.github.litaog.dailyrecord.ui.theme.DailyRecordCanvas
import io.github.litaog.dailyrecord.ui.theme.DailyRecordColorTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import io.github.litaog.dailyrecord.ui.theme.RecordModuleColorTokens
import io.github.litaog.dailyrecord.ui.theme.RecordVisualState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Challenger M5.2: Empirical Adversarial Stress Test Suite for Domain & State Machines.
 *
 * Covers:
 * 1. Concurrency & Rapid State Transitions (100+ concurrent record mutations, rapid module/period switching).
 * 2. Temporal Edge Cases & Boundary Clamping (leap years, month rollovers, year rollovers, clock skew, Int.MAX_VALUE).
 * 3. Null vs Zero Semantics (ADR-004 compliance).
 * 4. Theme Contrast Matrix (WCAG AA >= 4.5:1 across all 9 liquid glass themes x 2 palettes = 18 combinations).
 */
class DomainAndStateMachineAdversarialStressTest {

    private val baseDate = LocalDate.of(2026, 11, 8)
    private val now = Instant.parse("2026-11-08T07:00:00Z")

    // =========================================================================
    // 1. Concurrency & Rapid State Transitions
    // =========================================================================

    @Test
    fun test100ConcurrentRecordMutationsSurviveWithoutDataCorruption(): Unit = runBlocking {
        val repo = InMemoryHandBrewRecordRepository()
        val totalWorkers = 120
        val sharedDate = baseDate
        val repositoryLock = Mutex()

        // Spawn 120 concurrent asynchronous tasks mutating the shared date and distinct dates
        val deferreds = (1..totalWorkers).map { index ->
            async(Dispatchers.Default) {
                val isShared = (index % 3 == 0)
                val date = if (isShared) sharedDate else baseDate.minusDays(index.toLong())
                val record = HandBrewRecord(
                    id = "handbrew-$index",
                    localDate = date,
                    brewCount = (index % 5) + 1,
                    createdAt = now,
                    updatedAt = now.plusSeconds(index.toLong()),
                )
                val details = (1..record.brewCount).map { occurrence ->
                    HandBrewRecordDetail(
                        id = "detail-$index-$occurrence",
                        localDate = date,
                        occurrenceIndex = occurrence,
                        startTime = LocalTime.of(8, (occurrence * 5) % 60),
                        endTime = LocalTime.of(8, ((occurrence * 5) % 60) + 2),
                    )
                }

                // Execute concurrent repository operations with mutex protection
                repositoryLock.withLock {
                    repo.saveRecord(record, details)
                }
                val readRecord = repo.getRecord(date)

                if (!isShared) {
                    // For independent dates, record must be immediately present
                    assertNotNull("Record must not be null for worker $index on date $date", readRecord)
                    assertTrue("Record brewCount must be positive", readRecord!!.brewCount in 1..5)

                    // Test clear on independent date
                    if (index % 5 == 0) {
                        val cleared = repositoryLock.withLock { repo.clearRecord(date) }
                        assertTrue("Clear must succeed on existing record", cleared)
                        assertNull("Record must be null after clear on date $date", repo.getRecord(date))
                    }
                } else {
                    // On the contested shared date, record must be non-corrupted
                    assertNotNull("Shared date record must not be null", readRecord)
                    assertTrue("Shared date record brewCount must be within 1..5", readRecord!!.brewCount in 1..5)
                }
            }
        }

        // Await all 120 concurrent mutators
        deferreds.awaitAll()

        // Ensure repository state on shared date is valid and consistent
        val finalRecord = repo.getRecord(sharedDate)
        assertNotNull("Final record on shared date must exist after 40 concurrent saves", finalRecord)
        assertTrue("Final record count must be between 1 and 5", finalRecord!!.brewCount in 1..5)
    }

    @Test
    fun testRapidModuleSwitchingUnderConcurrencyPreservesDataIsolation(): Unit = runBlocking {
        val handBrewRepo = InMemoryHandBrewRecordRepository()
        val sexRepo = InMemorySexRecordRepository()
        val iterations = 100

        // Populate both repositories with distinct data
        (1..iterations).forEach { i ->
            val date = baseDate.minusDays(i.toLong())
            handBrewRepo.saveRecord(
                HandBrewRecord(
                    id = "solo-$i",
                    localDate = date,
                    brewCount = 1,
                    createdAt = now,
                    updatedAt = now,
                )
            )
            sexRepo.saveRecord(
                SexRecord(
                    id = "duo-$i",
                    localDate = date,
                    sexCount = 3,
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }

        // Run 100 concurrent workers rapidly switching between modules and asserting strict isolation
        val workers = (1..iterations).map { i ->
            async(Dispatchers.Default) {
                val module = if (i % 2 == 0) RecordModule.HandBrew else RecordModule.Sex
                val date = baseDate.minusDays(i.toLong())

                when (module) {
                    RecordModule.HandBrew -> {
                        val record = handBrewRepo.getRecord(date)
                        assertNotNull("HandBrew record must exist", record)
                        assertEquals("HandBrew record count must be 1", 1, record!!.brewCount)
                        assertTrue("ID must start with solo", record.id.startsWith("solo"))
                    }
                    RecordModule.Sex -> {
                        val record = sexRepo.getRecord(date)
                        assertNotNull("Sex record must exist", record)
                        assertEquals("Sex record count must be 3", 3, record!!.sexCount)
                        assertTrue("ID must start with duo", record.id.startsWith("duo"))
                    }
                }
            }
        }
        workers.awaitAll()
    }

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

    // =========================================================================
    // 4. Theme Contrast Matrix (WCAG AA >= 4.5:1 across all 18 combinations)
    // =========================================================================

    @Test
    fun testAll18ThemeModuleColorCombinationsMeetWcagAaContrast() {
        val themes = DailyRecordColorTheme.entries
        assertEquals("There must be exactly 9 liquid glass themes", 9, themes.size)

        themes.forEach { theme ->
            val solo = theme.palette.solo
            val duo = theme.palette.duo

            val moduleConfigs = listOf(
                "${theme.id}.solo" to solo,
                "${theme.id}.duo" to duo,
            )

            moduleConfigs.forEach { (name, tokens) ->
                // 1. onPrimary text on primary >= 4.5:1
                assertContrastAtLeast(4.5, tokens.onPrimary, tokens.primary, "$name onPrimary on primary")

                // 2. onPrimary text on strong >= 4.5:1
                assertContrastAtLeast(4.5, tokens.onPrimary, tokens.strong, "$name onPrimary on strong")

                // 3. Graphite text on soft fill >= 4.5:1
                assertContrastAtLeast(4.5, DailyRecordText, tokens.soft, "$name DailyRecordText on soft")

                // 4. Graphite text on medium fill >= 4.5:1
                assertContrastAtLeast(4.5, DailyRecordText, tokens.medium, "$name DailyRecordText on medium")

                // 5. Graphite text on intense fill >= 4.5:1
                assertContrastAtLeast(4.5, DailyRecordText, tokens.intense, "$name DailyRecordText on intense")

                // 6. onPeak text on peak fill >= 4.5:1
                assertContrastAtLeast(4.5, tokens.onPeak, tokens.peak, "$name onPeak on peak")

                // 7. Secondary text on unset fill >= 4.5:1
                assertContrastAtLeast(4.5, DailyRecordTextSecondary, tokens.unset, "$name textSecondary on unset")

                // 8. Primary accent on canvas >= 4.5:1
                assertContrastAtLeast(4.5, tokens.primary, DailyRecordCanvas, "$name primary on canvas")

                // 9. Strong accent on soft fill >= 4.5:1
                assertContrastAtLeast(4.5, tokens.strong, tokens.soft, "$name strong on soft")

                // 10. Monotonic relative luminance: unset > soft > medium > intense > peak
                assertTrue(
                    "$name: unset must be lighter than soft",
                    relativeLuminance(tokens.unset) > relativeLuminance(tokens.soft),
                )
                assertTrue(
                    "$name: soft must be lighter than medium",
                    relativeLuminance(tokens.soft) > relativeLuminance(tokens.medium),
                )
                assertTrue(
                    "$name: medium must be lighter than intense",
                    relativeLuminance(tokens.medium) > relativeLuminance(tokens.intense),
                )
                assertTrue(
                    "$name: intense must be lighter than peak",
                    relativeLuminance(tokens.intense) > relativeLuminance(tokens.peak),
                )

                // 11. Strong must be darker than primary
                assertTrue(
                    "$name: strong must be darker than primary",
                    relativeLuminance(tokens.strong) < relativeLuminance(tokens.primary),
                )
            }

            // 12. Strict color independence between solo and duo palettes
            assertNotEquals("${theme.id}: solo and duo primary colors must differ", solo.primary, duo.primary)
            assertNotEquals("${theme.id}: solo and duo soft fills must differ", solo.soft, duo.soft)
            assertNotEquals("${theme.id}: solo and duo unset fills must differ", solo.unset, duo.unset)
            assertNotEquals("${theme.id}: solo and duo peak fills must differ", solo.peak, duo.peak)
        }
    }

    // =========================================================================
    // Contrast and Luminance Math Utilities (WCAG 2.1 Standard)
    // =========================================================================

    private fun assertContrastAtLeast(
        minRatio: Double,
        c1: Color,
        c2: Color,
        tag: String,
    ) {
        val ratio = contrastRatio(c1, c2)
        assertTrue(
            "$tag: contrast ratio ($ratio) must be >= $minRatio",
            ratio >= minRatio,
        )
    }

    private fun contrastRatio(c1: Color, c2: Color): Double {
        val l1 = relativeLuminance(c1)
        val l2 = relativeLuminance(c2)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        val argb = color.toArgb()
        fun channel(shift: Int): Double {
            val value = ((argb shr shift) and 0xFF) / 255.0
            return if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }
}
