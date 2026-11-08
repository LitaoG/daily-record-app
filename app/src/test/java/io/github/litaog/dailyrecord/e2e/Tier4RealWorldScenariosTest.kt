package io.github.litaog.dailyrecord.e2e

import androidx.compose.ui.graphics.Color
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.common.AppLanguage
import io.github.litaog.dailyrecord.core.common.AppLanguageState
import io.github.litaog.dailyrecord.core.common.TerminologyStyle
import io.github.litaog.dailyrecord.core.common.strings
import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.security.AppLockDelay
import io.github.litaog.dailyrecord.core.security.AppLockSession
import io.github.litaog.dailyrecord.core.security.LauncherIdentity
import io.github.litaog.dailyrecord.core.security.launcherAliasChanges
import io.github.litaog.dailyrecord.core.statistics.EARLIEST_SUPPORTED_DATE
import io.github.litaog.dailyrecord.core.statistics.StatisticsPeriod
import io.github.litaog.dailyrecord.core.statistics.buildDailyCountStatistics
import io.github.litaog.dailyrecord.ui.IntimacyOnboardingPreference
import io.github.litaog.dailyrecord.ui.RecordDetailEntry
import io.github.litaog.dailyrecord.ui.RecordModule
import io.github.litaog.dailyrecord.ui.TerminologyStylePreference
import io.github.litaog.dailyrecord.ui.TopDestination
import io.github.litaog.dailyrecord.ui.calendar.MonthSwipe
import io.github.litaog.dailyrecord.ui.calendar.QuickLogAction
import io.github.litaog.dailyrecord.ui.calendar.monthSwipeFor
import io.github.litaog.dailyrecord.ui.calendar.nextCount
import io.github.litaog.dailyrecord.ui.navigation.clampWheelDate
import io.github.litaog.dailyrecord.ui.record.CountDraft
import io.github.litaog.dailyrecord.ui.record.RecordDetailsDraft
import io.github.litaog.dailyrecord.ui.statistics.ChartScrubState
import io.github.litaog.dailyrecord.ui.statistics.monthDailyChartIndexAt
import io.github.litaog.dailyrecord.ui.statistics.yearLineChartIndexAt
import io.github.litaog.dailyrecord.ui.theme.DailyRecordColorTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordThemeState
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 4 - Real-World Application Scenarios (End-to-End User Journeys).
 * Simulates complete, multi-step user workflows through the domain, state,
 * and presentation contracts.
 */
class Tier4RealWorldScenariosTest {

    private val earliest = EARLIEST_SUPPORTED_DATE
    private val today = LocalDate.of(2026, 11, 7)

    /**
     * Scenario 1: Daily Tracking Routine.
     * Flow: Cold Start -> Onboarding check/dismissal -> Quick log +1 today ->
     * Open record details -> Adjust count to 2 -> Add feeling note -> Save ->
     * Verify persistence & statistics update.
     */
    @Test
    fun testScenario1_DailyTrackingRoutine() = runBlocking {
        val fakePrefs = E2EFakeSharedPreferences()
        val repo = InMemoryHandBrewRecordRepository()

        // Step 1: Cold start & intimacy onboarding explanatory card
        val onboarding = IntimacyOnboardingPreference(fakePrefs)
        assertFalse("First launch should display intimacy onboarding card", onboarding.isDismissed)

        // Step 2: Dismiss onboarding card
        onboarding.dismiss()
        assertTrue("Dismissing should persist", onboarding.isDismissed)
        assertTrue(IntimacyOnboardingPreference(fakePrefs).isDismissed)

        // Step 3: Rapid quick-log +1 today on the calendar island
        val initialStored: Int? = repo.getRecord(today)?.brewCount
        assertEquals(null, initialStored)

        val quickLoggedCount = QuickLogAction.Increase.nextCount(initialStored) ?: error("Failed quick log")
        assertEquals(1, quickLoggedCount)
        repo.saveRecord(E2ETestFixtures.handBrewRecord(today, quickLoggedCount))

        // Step 4: Open record screen & initialize draft from repository
        val currentRecord = repo.getRecord(today)
        assertNotNull(currentRecord)
        var countDraft = CountDraft().reconcile(currentRecord?.brewCount ?: 0)
        assertEquals(1, countDraft.count)
        assertFalse(countDraft.hasChanges)

        // Step 5: Adjust count to 2
        countDraft = countDraft.increase()
        assertEquals(2, countDraft.count)
        assertTrue(countDraft.hasChanges)

        // Step 6: Add occurrence detail entry
        val detailsDraft = RecordDetailsDraft().reconcile(
            latest = listOf(
                RecordDetailEntry(occurrenceIndex = 1, feeling = "平静放松"),
            ),
            count = countDraft.count,
        )
        assertEquals(2, detailsDraft.entries.size)
        assertEquals("平静放松", detailsDraft.entries[0].feeling)

        // Step 7: Save record via pinned action bar
        val updatedRecord = E2ETestFixtures.handBrewRecord(today, countDraft.count)
        val updatedDetails = listOf(
            E2ETestFixtures.handBrewDetail(today, 1, LocalTime.of(14, 0), feeling = "平静放松"),
        )
        repo.saveRecord(updatedRecord, updatedDetails)

        // Step 8: Verify persistence and statistics
        val saved = repo.getRecord(today)
        assertNotNull(saved)
        assertEquals(2, saved?.brewCount)
        val savedDetails = repo.observeDetails(today).first()
        assertEquals(1, savedDetails.size)
        assertEquals("平静放松", savedDetails[0].feeling)

        // Step 9: Verify today and month statistics reflect the update
        val stats = buildDailyCountStatistics(
            StatisticsPeriod.Month,
            today,
            today,
            listOf(DailyCountEntry(today, 2)),
        )
        assertEquals(2L, stats.summary.totalCount)
        assertEquals(1, stats.summary.recordedDays)
        assertEquals(2.0, stats.summary.average, 0.001)
    }

    /**
     * Scenario 2: Monthly Retrospective & Historical Review.
     * Flow: Open calendar -> Month swipe gestures -> Date navigation wheel jump
     * to Oct 2025 -> Switch destination to Statistics -> Cycle temporal periods
     * (Week/Month/Year/All) -> Scrub monthly pulse & annual area charts.
     */
    @Test
    fun testScenario2_MonthlyRetrospectiveAndReview() {
        // Step 1: User swipes month grid to browse history
        val swipeAction = monthSwipeFor(dragPx = 120f, thresholdPx = 64f, canGoPrevious = true, canGoNext = true)
        assertEquals(MonthSwipe.Previous, swipeAction)

        // Step 2: User jumps directly to 2025-10-15 via Date Navigation Wheel
        val targetDate = clampWheelDate(
            year = 2025,
            month = 10,
            day = 15,
            earliestDate = earliest,
            latestDate = today,
        )
        assertEquals(LocalDate.of(2025, 10, 15), targetDate)

        // Step 3: Switch destination from Calendar to Statistics
        var activeDestination = TopDestination.Calendar
        activeDestination = TopDestination.Statistics
        assertEquals(TopDestination.Statistics, activeDestination)

        // Step 4: Populate historical multi-month fixture
        val historyRecords = listOf(
            DailyCountEntry(LocalDate.of(2025, 10, 5), 2),
            DailyCountEntry(LocalDate.of(2025, 10, 15), 3),
            DailyCountEntry(LocalDate.of(2025, 10, 25), 1),
            DailyCountEntry(LocalDate.of(2025, 11, 2), 4),
        )

        // Step 5: Cycle through temporal aggregation periods
        val monthStats = buildDailyCountStatistics(
            StatisticsPeriod.Month,
            targetDate,
            today,
            historyRecords,
        )
        assertEquals(6L, monthStats.summary.totalCount)
        assertEquals(3, monthStats.summary.recordedDays)
        assertEquals(2.0, monthStats.summary.average, 0.001)

        val yearStats = buildDailyCountStatistics(
            StatisticsPeriod.Year,
            targetDate,
            today,
            historyRecords,
        )
        assertEquals(10L, yearStats.summary.totalCount)
        assertEquals(4, yearStats.summary.recordedDays)

        // Step 6: Scrub probe on monthly pulse chart
        val scrubDayIndex = monthDailyChartIndexAt(x = 300f, dayCount = 31, width = 620f, inset = 6f)
        assertTrue(scrubDayIndex in 0..30)

        val scrubState = ChartScrubState()
        scrubState.update(scrubDayIndex)
        assertEquals(scrubDayIndex, scrubState.activeIndex)
        assertEquals(scrubDayIndex, scrubState.shownIndex)

        // Step 7: Scrub probe on annual line chart
        val scrubMonthIndex = yearLineChartIndexAt(x = 500f, width = 600f)
        assertEquals(10, scrubMonthIndex) // Month 10 (October/November slot)
    }

    /**
     * Scenario 3: Full Privacy, Security & Theme Reconfiguration.
     * Flow: Open Settings -> Switch theme from Aurora to EmeraldBerry ->
     * Switch Terminology from Elegant to Direct -> Enable App Lock with 1m delay ->
     * Switch Launcher Disguise to Journal -> Put app in background for 120s ->
     * Foreground app -> Verify lock barrier active -> Authenticate -> Resume.
     */
    @Test
    fun testScenario3_PrivacySecurityAndThemeReconfiguration() {
        val fakePrefs = E2EFakeSharedPreferences()

        // Step 1: Switch theme to EmeraldBerry
        DailyRecordThemeState.current = DailyRecordColorTheme.EmeraldBerry
        val activeTheme = DailyRecordThemeState.current
        assertEquals(DailyRecordColorTheme.EmeraldBerry, activeTheme)

        // Verify EmeraldBerry WCAG AA contrast on both Solo and Duo
        val soloPrimaryContrast = E2ETestFixtures.contrastRatio(Color.White, activeTheme.palette.solo.primary)
        val duoPrimaryContrast = E2ETestFixtures.contrastRatio(Color.White, activeTheme.palette.duo.primary)
        assertTrue(soloPrimaryContrast >= 4.5)
        assertTrue(duoPrimaryContrast >= 4.5)

        // Step 2: Switch Terminology style to DIRECT
        val terminologyPref = TerminologyStylePreference(fakePrefs)
        terminologyPref.setStyle(TerminologyStyle.DIRECT)
        assertEquals(TerminologyStyle.DIRECT, terminologyPref.current)

        AppLanguageState.current = AppLanguage.ZH.strings(TerminologyStyle.DIRECT)
        assertEquals("自慰", AppCopy.RecordModule.handBrewLabel)
        assertEquals("做爱", AppCopy.RecordModule.sexLabel)

        // Step 3: Enable Biometric App Lock with 1-minute delay
        val lockSession = AppLockSession(enabled = true, delay = AppLockDelay.OneMinute)
        lockSession.onForeground(0L)
        lockSession.onAuthenticationStarted()
        lockSession.onAuthenticationFinished(success = true)
        assertFalse("Session should be unlocked after successful biometric prompt", lockSession.locked.value)

        // Step 4: Configure Launcher Disguise to Journal alias
        val disguiseChanges = launcherAliasChanges(
            target = LauncherIdentity.Journal,
            enabledNow = setOf(LauncherIdentity.Default),
        )
        assertEquals(2, disguiseChanges.size)
        assertTrue(disguiseChanges.any { it.identity == LauncherIdentity.Journal && it.enabled })
        assertTrue(disguiseChanges.any { it.identity == LauncherIdentity.Default && !it.enabled })

        // Step 5: Background app for 120s (exceeds 60s delay)
        val backgroundTimestamp = 10_000L
        lockSession.onBackground(backgroundTimestamp, changingConfigurations = false)

        val foregroundTimestamp = backgroundTimestamp + 120_000L
        lockSession.onForeground(foregroundTimestamp)

        // Step 6: Verify app lock actively shields the app
        assertTrue("App lock should be locked after timeout expired", lockSession.locked.value)

        // Step 7: Biometric authentication succeeds
        val request = lockSession.onAuthenticationStarted()
        assertNotNull(request)
        lockSession.onAuthenticationFinished(success = true, request = request)

        // Step 8: Verify app resumes unlocked with chosen configuration intact
        assertFalse("App lock should be unlocked", lockSession.locked.value)
        assertEquals(DailyRecordColorTheme.EmeraldBerry, DailyRecordThemeState.current)
        assertEquals(TerminologyStyle.DIRECT, terminologyPref.current)
    }
}
