package io.github.litaog.dailyrecord.scenarios

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.common.TerminologyStyle
import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.core.security.AppLockDelay
import io.github.litaog.dailyrecord.core.security.AppLockSession
import io.github.litaog.dailyrecord.core.security.LauncherIdentity
import io.github.litaog.dailyrecord.core.security.launcherAliasChanges
import io.github.litaog.dailyrecord.core.statistics.EARLIEST_SUPPORTED_DATE
import io.github.litaog.dailyrecord.core.statistics.StatisticsPeriod
import io.github.litaog.dailyrecord.core.statistics.StatisticsSummary
import io.github.litaog.dailyrecord.core.statistics.buildDailyCountStatistics
import io.github.litaog.dailyrecord.core.statistics.nextPeriodAnchor
import io.github.litaog.dailyrecord.core.statistics.previousPeriodAnchor
import io.github.litaog.dailyrecord.ui.IntimacyOnboardingPreference
import io.github.litaog.dailyrecord.ui.RecordDetailEntry
import io.github.litaog.dailyrecord.ui.TerminologyStylePreference
import io.github.litaog.dailyrecord.ui.calendar.MonthSwipe
import io.github.litaog.dailyrecord.ui.calendar.QuickLogAction
import io.github.litaog.dailyrecord.ui.calendar.monthSwipeFollowPx
import io.github.litaog.dailyrecord.ui.calendar.monthSwipeFor
import io.github.litaog.dailyrecord.ui.calendar.nextCount
import io.github.litaog.dailyrecord.ui.calendar.quickAdjustPopupOffset
import io.github.litaog.dailyrecord.ui.components.calculatePredictiveBackAlpha
import io.github.litaog.dailyrecord.ui.components.calculatePredictiveBackCornerRadius
import io.github.litaog.dailyrecord.ui.components.calculatePredictiveBackScale
import io.github.litaog.dailyrecord.ui.navigation.DateNavigationSelection
import io.github.litaog.dailyrecord.ui.navigation.clampWheelDate
import io.github.litaog.dailyrecord.ui.navigation.navigationJumpLabel
import io.github.litaog.dailyrecord.ui.record.CountDraft
import io.github.litaog.dailyrecord.ui.record.RecordDetailsDraft
import io.github.litaog.dailyrecord.ui.statistics.ChartScrubState
import io.github.litaog.dailyrecord.ui.statistics.chartBubbleX
import io.github.litaog.dailyrecord.ui.statistics.monthDailyChartIndexAt
import io.github.litaog.dailyrecord.ui.statistics.yearLineChartIndexAt
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Production function-level coverage for implemented features: baseline happy paths
 * and architectural contracts (JVM unit tests that call production functions directly;
 * no instrumentation).
 */
class FeatureCoverageTest {

    private val earliestSupportedDate = EARLIEST_SUPPORTED_DATE

    // Feature 3: Today Quick-Log Island
    @Test
    fun testFeature3_TodayQuickLog_ActionTransitions() {
        // Increase from unset (null) -> 1
        assertEquals(1, QuickLogAction.Increase.nextCount(null))
        // Increase from 0 -> 1
        assertEquals(1, QuickLogAction.Increase.nextCount(0))
        // Increase from 1 -> 2
        assertEquals(2, QuickLogAction.Increase.nextCount(1))

        // Decrease from 1 -> 0 (explicit 0 per ADR-004)
        assertEquals(0, QuickLogAction.Decrease.nextCount(1))
        // Decrease from 0 -> null (no underflow below 0)
        assertNull(QuickLogAction.Decrease.nextCount(0))

        // MarkZero fills unset (null) -> 0
        assertEquals(0, QuickLogAction.MarkZero.nextCount(null))
        // MarkZero does not alter an already recorded day
        assertNull(QuickLogAction.MarkZero.nextCount(0))
        assertNull(QuickLogAction.MarkZero.nextCount(2))
    }

    // Feature 4: Long-Press Quick Adjust
    @Test
    fun testFeature4_CalendarQuickAdjust_PopupOffset() {
        val window = IntSize(1080, 1920)
        val popup = IntSize(400, 160)
        val gap = 12

        // Mid-screen cell: centered horizontally, sits above the cell
        val midCell = IntRect(left = 340, top = 800, right = 480, bottom = 940)
        val midOffset = quickAdjustPopupOffset(midCell, window, popup, gap)
        // Center x is 410, popup width 400 -> left is 210
        assertEquals(210, midOffset.x)
        // Sits 12px above cell top (800 - 160 - 12 = 628)
        assertEquals(628, midOffset.y)

        // Top row cell (no room above): drops below cell
        val topCell = IntRect(left = 340, top = 80, right = 480, bottom = 220)
        val topOffset = quickAdjustPopupOffset(topCell, window, popup, gap)
        assertEquals(232, topOffset.y) // 220 + 12
    }

    // Feature 5: Month Swipe Navigation
    @Test
    fun testFeature5_MonthSwipe_DetectionAndFollow() {
        val threshold = 100f
        val maxFollow = 150f

        // Swipe right beyond threshold -> Previous month
        assertEquals(
            MonthSwipe.Previous,
            monthSwipeFor(dragPx = 120f, thresholdPx = threshold, canGoPrevious = true, canGoNext = true),
        )

        // Swipe left beyond threshold -> Next month
        assertEquals(
            MonthSwipe.Next,
            monthSwipeFor(dragPx = -120f, thresholdPx = threshold, canGoPrevious = true, canGoNext = true),
        )

        // Sub-threshold drag -> null
        assertNull(
            monthSwipeFor(dragPx = 50f, thresholdPx = threshold, canGoPrevious = true, canGoNext = true),
        )

        // Boundary constraint: cannot go previous
        assertNull(
            monthSwipeFor(dragPx = 120f, thresholdPx = threshold, canGoPrevious = false, canGoNext = true),
        )

        // Follow distance calculation with damping
        val availableFollow = monthSwipeFollowPx(dragPx = 200f, maxFollowPx = maxFollow, towardAvailableMonth = true)
        assertEquals(100f, availableFollow, 0.01f) // 200 * 0.5
    }

    // Feature 6: Date Navigation Wheel Dialog
    @Test
    fun testFeature6_DateNavigation_WheelClampingAndLabels() {
        val earliest = LocalDate.of(1970, 1, 1)
        val today = LocalDate.of(2026, 11, 7)

        // Direct clamping
        val clamped = clampWheelDate(year = 2024, month = 2, day = 31, earliestDate = earliest, latestDate = today)
        // 2024 is leap year, February has 29 days
        assertEquals(LocalDate.of(2024, 2, 29), clamped)

        // Clamping to today if future
        val future = clampWheelDate(year = 2030, month = 5, day = 10, earliestDate = earliest, latestDate = today)
        assertEquals(today, future)

        // Labels
        assertEquals("跳转到此日", navigationJumpLabel(DateNavigationSelection.Date))
        assertEquals("跳转到此月", navigationJumpLabel(DateNavigationSelection.Month))
        assertEquals("跳转到此年", navigationJumpLabel(DateNavigationSelection.Year))
    }

    // Feature 7: Date Record & Details Screen
    @Test
    fun testFeature7_RecordDetails_DraftStateAndValidation() {
        // CountDraft lifecycle
        val initial = CountDraft().reconcile(1)
        assertEquals(1, initial.count)
        assertFalse(initial.hasChanges)

        val increased = initial.increase()
        assertEquals(2, increased.count)
        assertTrue(increased.hasChanges)

        // RecordDetailsDraft with occurrence detail entry
        val detailsDraft = RecordDetailsDraft().reconcile(
            latest = listOf(
                RecordDetailEntry(occurrenceIndex = 1, feeling = "Relaxed"),
            ),
            count = 1,
        )
        assertEquals(1, detailsDraft.entries.size)
        assertEquals("Relaxed", detailsDraft.entries[0].feeling)
    }

    // Feature 8: Period Switcher (Wk/Mo/Yr/All)
    @Test
    fun testFeature8_PeriodSwitcher_AnchorsAndTransitions() {
        val anchor = LocalDate.of(2026, 6, 15)
        val today = LocalDate.of(2026, 11, 7)

        // Week navigation shifts 7 days
        val nextWeek = nextPeriodAnchor(StatisticsPeriod.Week, anchor, today)
        assertEquals(LocalDate.of(2026, 6, 22), nextWeek)

        val prevWeek = previousPeriodAnchor(StatisticsPeriod.Week, anchor, earliestSupportedDate)
        assertEquals(LocalDate.of(2026, 6, 8), prevWeek)

        // Month navigation shifts 1 month
        val nextMonth = nextPeriodAnchor(StatisticsPeriod.Month, anchor, today)
        assertEquals(LocalDate.of(2026, 7, 15), nextMonth)

        // Year navigation shifts 1 year
        val prevYear = previousPeriodAnchor(StatisticsPeriod.Year, anchor, earliestSupportedDate)
        assertEquals(LocalDate.of(2025, 6, 15), prevYear)
    }

    // Feature 9: Single KPI Summary Card
    @Test
    fun testFeature9_KpiSummary_Calculation() {
        val summary = StatisticsSummary(totalCount = 42, recordedDays = 14)
        assertEquals(42L, summary.totalCount)
        assertEquals(14, summary.recordedDays)
        assertEquals(3.0, summary.average, 0.001)

        val emptySummary = StatisticsSummary(totalCount = 0, recordedDays = 0)
        assertEquals(0.0, emptySummary.average, 0.0)
    }

    // Feature 10: Weekly 7-Segment Ring
    @Test
    fun testFeature10_WeeklyRing_MondayToSundayAggregation() {
        // Monday 2026-11-02 to Sunday 2026-11-08
        val monday = LocalDate.of(2026, 11, 2)
        val today = LocalDate.of(2026, 11, 7)
        val records = listOf(
            DailyCountEntry(monday, 2),
            DailyCountEntry(monday.plusDays(1), 1),
            DailyCountEntry(monday.plusDays(3), 3),
        )

        val weekStats = buildDailyCountStatistics(StatisticsPeriod.Week, monday, today, records)
        // 7 days from Monday to Sunday in details
        assertEquals(7, weekStats.details.size)
        assertEquals(6L, weekStats.summary.totalCount)
        assertEquals(3, weekStats.summary.recordedDays)
    }

    // Feature 11: Monthly Pulse Chart & Distribution
    @Test
    fun testFeature11_MonthlyPulseChart_DailyLinesAndDistribution() {
        val anchor = LocalDate.of(2026, 11, 1)
        val today = LocalDate.of(2026, 11, 7)
        val records = listOf(
            DailyCountEntry(LocalDate.of(2026, 11, 2), 2),
            DailyCountEntry(LocalDate.of(2026, 11, 5), 4),
        )

        val monthStats = buildDailyCountStatistics(StatisticsPeriod.Month, anchor, today, records)
        // November has 30 days
        assertEquals(30, monthStats.details.size)
        // Days after today are flagged future
        assertTrue(monthStats.details.last().future)
    }

    // Feature 12: Annual 12-Month Area Chart
    @Test
    fun testFeature12_AnnualAreaChart_QuarterlyBreakdown() {
        val anchor = LocalDate.of(2026, 11, 1)
        val today = LocalDate.of(2026, 11, 7)
        val records = listOf(
            DailyCountEntry(LocalDate.of(2026, 1, 10), 10),
            DailyCountEntry(LocalDate.of(2026, 4, 10), 20),
            DailyCountEntry(LocalDate.of(2026, 7, 10), 30),
            DailyCountEntry(LocalDate.of(2026, 10, 10), 40),
        )

        val yearStats = buildDailyCountStatistics(StatisticsPeriod.Year, anchor, today, records)
        assertNotNull(yearStats.year)
        val yearModel = requireNotNull(yearStats.year)
        assertEquals(4, yearModel.quarters.size)
        assertEquals(10L, yearModel.quarters[0].totalCount)
        assertEquals(20L, yearModel.quarters[1].totalCount)
        assertEquals(30L, yearModel.quarters[2].totalCount)
        assertEquals(40L, yearModel.quarters[3].totalCount)
    }

    // Feature 13: Interactive Scrub Probe
    @Test
    fun testFeature13_ChartScrub_IndexResolution() {
        // Month daily chart scrub
        val dayIndex = monthDailyChartIndexAt(x = 150f, dayCount = 30, width = 600f, inset = 6f)
        assertTrue(dayIndex in 0 until 30)

        // Year line chart scrub
        val monthIndex = yearLineChartIndexAt(x = 300f, width = 600f)
        assertEquals(6, monthIndex)

        // Bubble centering
        val bubbleX = chartBubbleX(anchorX = 200f, bubbleWidth = 100, containerWidth = 600)
        assertEquals(150, bubbleX)

        // Scrub state
        val state = ChartScrubState()
        state.update(3)
        assertEquals(3, state.activeIndex)
        assertEquals(3, state.shownIndex)
        state.update(null)
        assertNull(state.activeIndex)
        assertEquals(3, state.shownIndex)
    }

    // Feature 14: Secondary Settings Center
    @Test
    fun testFeature14_SettingsCenter_PreferenceDefaults() {
        val fakePrefs = E2EFakeSharedPreferences()
        val terminologyPref = TerminologyStylePreference(fakePrefs)
        assertEquals(TerminologyStyle.ELEGANT, terminologyPref.current)

        val onboardingPref = IntimacyOnboardingPreference(fakePrefs)
        assertFalse(onboardingPref.isDismissed)
    }

    // Feature 15: Biometric App Lock
    @Test
    fun testFeature15_BiometricAppLock_SessionLocking() {
        val session = AppLockSession(enabled = true, delay = AppLockDelay.Immediately)
        // Cold start enabled lock is locked
        assertTrue(session.locked.value)

        // Authenticate
        session.onForeground(0L)
        session.onAuthenticationStarted()
        session.onAuthenticationFinished(success = true)
        assertFalse(session.locked.value)

        // Background and return -> locks immediately
        session.onBackground(100L, changingConfigurations = false)
        session.onForeground(101L)
        assertTrue(session.locked.value)
    }

    // Feature 16: Launcher Disguise
    @Test
    fun testFeature16_LauncherDisguise_IdentityAliases() {
        assertEquals(LauncherIdentity.Default, LauncherIdentity.fromId("default"))
        assertEquals(LauncherIdentity.Journal, LauncherIdentity.fromId("journal"))
        // Fallback on unknown
        assertEquals(LauncherIdentity.Default, LauncherIdentity.fromId("unknown_alias"))

        // Alias changes calculation
        val changes = launcherAliasChanges(target = LauncherIdentity.Journal, enabledNow = setOf(LauncherIdentity.Default))
        assertEquals(2, changes.size)
        assertTrue(changes.any { it.identity == LauncherIdentity.Journal && it.enabled })
        assertTrue(changes.any { it.identity == LauncherIdentity.Default && !it.enabled })
    }

    // Feature 18: Terminology Style Mode
    @Test
    fun testFeature18_TerminologyStyle_ElegantVsDirect() {
        val fakePrefs = E2EFakeSharedPreferences()
        val pref = TerminologyStylePreference(fakePrefs)
        assertEquals(TerminologyStyle.ELEGANT, pref.current)

        pref.setStyle(TerminologyStyle.DIRECT)
        assertEquals(TerminologyStyle.DIRECT, pref.current)
    }

    // Feature 19: Intimacy Guidance Card
    @Test
    fun testFeature19_IntimacyGuidanceCard_FirstOpenDismissal() {
        val fakePrefs = E2EFakeSharedPreferences()
        val pref = IntimacyOnboardingPreference(fakePrefs)
        assertFalse(pref.isDismissed)

        pref.dismiss()
        assertTrue(pref.isDismissed)
        assertTrue(pref.dismissed.value)
    }

    // Feature 25: Pinned Sticky Save Action Bar
    @Test
    fun testFeature25_PinnedStickyActionBar_SaveEligibility() {
        val draft = CountDraft().reconcile(2)
        // Clean draft cannot be saved (no changes)
        assertFalse(draft.hasChanges)

        // Modified draft has changes and is eligible for Save
        val modified = draft.increase()
        assertTrue(modified.hasChanges)
        assertEquals(3, modified.count)
    }

    // Feature 27: Fluid Screen Transitions
    @Test
    fun testFeature27_FluidTransitions_PredictiveBackInterpolation() {
        assertEquals(1.0f, calculatePredictiveBackScale(0f), 0.001f)
        assertEquals(0.96f, calculatePredictiveBackScale(0.5f), 0.001f)
        assertEquals(0.92f, calculatePredictiveBackScale(1.0f), 0.001f)

        assertEquals(1.0f, calculatePredictiveBackAlpha(0f), 0.001f)
        assertEquals(0.85f, calculatePredictiveBackAlpha(1.0f), 0.001f)

        assertEquals(0.dp, calculatePredictiveBackCornerRadius(0f))
        assertEquals(24.dp, calculatePredictiveBackCornerRadius(1.0f))
    }
}
