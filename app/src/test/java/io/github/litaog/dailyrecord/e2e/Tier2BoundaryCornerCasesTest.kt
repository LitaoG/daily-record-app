package io.github.litaog.dailyrecord.e2e

import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.data.requireValidRecordRange
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.HandBrewRecordDetail
import io.github.litaog.dailyrecord.core.model.SexRecord
import io.github.litaog.dailyrecord.core.model.SexRecordDetail
import io.github.litaog.dailyrecord.core.security.AppLockDelay
import io.github.litaog.dailyrecord.core.security.AppLockSession
import io.github.litaog.dailyrecord.core.statistics.StatisticsPeriod
import io.github.litaog.dailyrecord.core.statistics.StatisticsSummary
import io.github.litaog.dailyrecord.core.statistics.buildDailyCountStatistics
import io.github.litaog.dailyrecord.ui.calendar.QuickLogAction
import io.github.litaog.dailyrecord.ui.calendar.nextCount
import io.github.litaog.dailyrecord.ui.navigation.clampWheelDate
import io.github.litaog.dailyrecord.ui.record.CountDraft
import io.github.litaog.dailyrecord.ui.theme.DailyRecordSizes
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 2 - Boundary & Corner Cases (Opaque-Box Resilience & Stress Verification).
 * Tests extreme inputs, null vs zero semantics, boundary clamping, rapid taps,
 * and security constraints.
 */
class Tier2BoundaryCornerCasesTest {

    private val earliestDate = LocalDate.of(1970, 1, 1)
    private val today = LocalDate.of(2026, 11, 7)

    // 1. Empty & Null Inputs
    @Test
    fun testEmptyDataStatisticsYieldsZeroWithoutArithmeticException() {
        val emptyStats = buildDailyCountStatistics(
            period = StatisticsPeriod.Month,
            anchorDate = today,
            today = today,
            records = emptyList(),
        )

        assertEquals(0L, emptyStats.summary.totalCount)
        assertEquals(0, emptyStats.summary.recordedDays)
        assertEquals(0.0, emptyStats.summary.average, 0.0)
    }

    @Test
    fun testNullVsZeroCountSemanticDistinction() {
        // ADR-004: null means unset date (no record row); 0 means explicitly recorded zero
        // Increase on null begins at 1
        assertEquals(1, QuickLogAction.Increase.nextCount(null))
        // MarkZero on null explicitly records 0
        assertEquals(0, QuickLogAction.MarkZero.nextCount(null))

        // Increase on 0 increments to 1
        assertEquals(1, QuickLogAction.Increase.nextCount(0))
        // Decrease on 0 cannot go negative (stays null/no action)
        assertNull(QuickLogAction.Decrease.nextCount(0))
        // MarkZero on 0 returns null (already recorded as 0)
        assertNull(QuickLogAction.MarkZero.nextCount(0))
    }

    @Test
    fun testInvalidRecordRangesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            requireValidRecordRange(startDate = today, endExclusive = today)
        }
        assertThrows(IllegalArgumentException::class.java) {
            requireValidRecordRange(startDate = today, endExclusive = today.minusDays(1))
        }
    }

    @Test
    fun testBlankRecordIdsAreRejected() {
        val now = Instant.now()
        assertThrows(IllegalArgumentException::class.java) {
            HandBrewRecord(id = "   ", localDate = today, brewCount = 1, createdAt = now, updatedAt = now)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SexRecord(id = "", localDate = today, sexCount = 1, createdAt = now, updatedAt = now)
        }
    }

    @Test
    fun testNegativeCountsAreRejected() {
        val now = Instant.now()
        assertThrows(IllegalArgumentException::class.java) {
            HandBrewRecord(id = "hb_neg", localDate = today, brewCount = -1, createdAt = now, updatedAt = now)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SexRecord(id = "sex_neg", localDate = today, sexCount = -5, createdAt = now, updatedAt = now)
        }
    }

    @Test
    fun testRecordUpdatedAtBeforeCreatedAtIsRejected() {
        val created = Instant.parse("2026-11-07T10:00:00Z")
        val before = Instant.parse("2026-11-07T09:00:00Z")
        assertThrows(IllegalArgumentException::class.java) {
            HandBrewRecord(id = "hb_time", localDate = today, brewCount = 1, createdAt = created, updatedAt = before)
        }
    }

    @Test
    fun testInvalidOccurrenceDetailsAreRejected() {
        // Occurrence index < 1 rejected
        assertThrows(IllegalArgumentException::class.java) {
            HandBrewRecordDetail(id = "d1", localDate = today, occurrenceIndex = 0)
        }

        // Start time after end time rejected
        assertThrows(IllegalArgumentException::class.java) {
            HandBrewRecordDetail(
                id = "d2",
                localDate = today,
                occurrenceIndex = 1,
                startTime = LocalTime.of(15, 0),
                endTime = LocalTime.of(14, 0),
            )
        }

        // Feeling longer than 100 visible characters rejected
        assertThrows(IllegalArgumentException::class.java) {
            SexRecordDetail(
                id = "d3",
                localDate = today,
                occurrenceIndex = 1,
                feeling = "A".repeat(101),
            )
        }
    }

    // 2. Extreme Numbers & Overflow Protection
    @Test
    fun testQuickLogOverflowProtectionAtIntMaxValue() {
        assertNull(QuickLogAction.Increase.nextCount(Int.MAX_VALUE))
    }

    @Test
    fun testCountDraftClampsAtIntMaxValueAndZero() {
        val zeroDraft = CountDraft(count = 0)
        assertEquals(0, zeroDraft.decrease().count)

        val maxDraft = CountDraft(count = Int.MAX_VALUE)
        assertSame(maxDraft, maxDraft.increase())
    }

    @Test
    fun testLargeSummaryDoesNotOverflow() {
        val largeSummary = StatisticsSummary(totalCount = 1_000_000_000L, recordedDays = 500_000)
        assertEquals(2000.0, largeSummary.average, 0.001)
    }

    // 3. Temporal Boundary Clamping
    @Test
    fun testEarliestSupportedDateBoundaryClamp() {
        val clampedBefore1970 = clampWheelDate(
            year = 1960,
            month = 5,
            day = 12,
            earliestDate = earliestDate,
            latestDate = today,
        )
        assertEquals(earliestDate, clampedBefore1970)
    }

    @Test
    fun testFutureDateBoundaryClamp() {
        val clampedFuture = clampWheelDate(
            year = 2050,
            month = 12,
            day = 25,
            earliestDate = earliestDate,
            latestDate = today,
        )
        assertEquals(today, clampedFuture)
    }

    @Test
    fun testLeapYearFebruaryClamping() {
        // 2024 is a leap year (Feb 29 exists)
        val leapClamped = clampWheelDate(
            year = 2024,
            month = 2,
            day = 31,
            earliestDate = earliestDate,
            latestDate = today,
        )
        assertEquals(LocalDate.of(2024, 2, 29), leapClamped)

        // 2025 is a non-leap year (Feb 28 max)
        val nonLeapClamped = clampWheelDate(
            year = 2025,
            month = 2,
            day = 31,
            earliestDate = earliestDate,
            latestDate = today,
        )
        assertEquals(LocalDate.of(2025, 2, 28), nonLeapClamped)
    }

    // 4. Rapid Tap Concurrency Simulation
    @Test
    fun testRapidConsecutiveTapsAppliedInStrictSequence() {
        var count: Int? = null
        // 10 rapid taps +1
        repeat(10) {
            count = QuickLogAction.Increase.nextCount(count) ?: count
        }
        assertEquals(10, count)

        // 3 rapid taps -1
        repeat(3) {
            count = QuickLogAction.Decrease.nextCount(count) ?: count
        }
        assertEquals(7, count)
    }

    // 5. Touch Target Standards (Accessibility)
    @Test
    fun testMinimumTouchTargetsAdhereTo48DpRequirement() {
        assertTrue(DailyRecordSizes.MinimumTouchTarget >= 48.dp)
        assertTrue(DailyRecordSizes.BottomBarMinHeight >= 48.dp)
        assertTrue(DailyRecordSizes.ModuleSelectorMinHeight >= 48.dp)
        assertTrue(DailyRecordSizes.SettingsButton >= 48.dp)
    }

    // 6. Security & App Lock Resilience
    @Test
    fun testClockSkewBackwardsTriggersImmediateLock() {
        val session = AppLockSession(enabled = true, delay = AppLockDelay.FiveMinutes)
        session.onForeground(0L)
        session.onAuthenticationStarted()
        session.onAuthenticationFinished(success = true)
        assertFalse(session.locked.value)

        // Device clock jumps backwards: background at 50,000, foreground at 40,000
        session.onBackground(50_000L, changingConfigurations = false)
        session.onForeground(40_000L)
        // Clock running backward must be treated as timeout expired (defensive lockdown)
        assertTrue(session.locked.value)
    }

    @Test
    fun testCancelledAuthenticationRetainsLockedState() {
        val session = AppLockSession(enabled = true)
        session.onForeground(0L)
        session.onAuthenticationStarted()
        session.onAuthenticationFinished(success = false)

        assertTrue(session.locked.value)
        assertTrue(session.attemptFailed.value)
    }

    @Test
    fun testStaleAuthenticationCallbackFromSupersededPromptIsIgnored() {
        val session = AppLockSession(enabled = true)
        session.onForeground(0L)
        val firstRequest = session.onAuthenticationStarted()
        val secondRequest = session.onAuthenticationStarted()

        // First prompt was superseded; its late failure should not cancel second request's success
        session.onAuthenticationFinished(success = false, request = firstRequest)
        session.onAuthenticationFinished(success = true, request = secondRequest)

        assertFalse(session.locked.value)
    }
}
