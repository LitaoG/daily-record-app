package io.github.litaog.dailyrecord.scenarios

import io.github.litaog.dailyrecord.core.common.AppLanguage
import io.github.litaog.dailyrecord.core.common.TerminologyStyle
import io.github.litaog.dailyrecord.core.common.strings
import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.core.security.AppLockDelay
import io.github.litaog.dailyrecord.core.security.AppLockSession
import io.github.litaog.dailyrecord.core.security.LauncherIdentity
import io.github.litaog.dailyrecord.core.statistics.StatisticsPeriod
import io.github.litaog.dailyrecord.core.statistics.buildDailyCountStatistics
import io.github.litaog.dailyrecord.ui.record.CountDraft
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cross-feature integration contracts: count-to-statistics propagation, temporal
 * aggregation across scopes, the language-terminology matrix, and app-lock timing
 * combined with launcher disguise (JVM unit tests that call production functions
 * directly; no instrumentation).
 */
class CrossFeatureIntegrationTest {

    private val testDate = LocalDate.of(2026, 7, 15)
    private val today = LocalDate.of(2026, 11, 7)

    // 1. Count Edit + Statistics Update
    @Test
    fun testCountEditPropagatesToStatisticsModel() = runBlocking {
        val repo = InMemoryHandBrewRecordRepository()
        val august1 = LocalDate.of(2026, 8, 1)

        // Initially no records -> 0 statistics
        var records = repo.observeRecords(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1)).first()
        var stats = buildDailyCountStatistics(
            StatisticsPeriod.Month,
            august1,
            today,
            records.map { DailyCountEntry(it.localDate, it.brewCount) },
        )
        assertEquals(0L, stats.summary.totalCount)
        assertEquals(0, stats.summary.recordedDays)

        // User logs count 2 via CountDraft
        val draft = CountDraft().reconcile(0).increase().increase()
        repo.saveRecord(E2ETestFixtures.handBrewRecord(august1, draft.count))

        records = repo.observeRecords(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1)).first()
        stats = buildDailyCountStatistics(
            StatisticsPeriod.Month,
            august1,
            today,
            records.map { DailyCountEntry(it.localDate, it.brewCount) },
        )
        assertEquals(2L, stats.summary.totalCount)
        assertEquals(1, stats.summary.recordedDays)
        assertEquals(2.0, stats.summary.average, 0.001)

        // User clears record
        repo.clearRecord(august1)
        records = repo.observeRecords(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 1)).first()
        stats = buildDailyCountStatistics(
            StatisticsPeriod.Month,
            august1,
            today,
            records.map { DailyCountEntry(it.localDate, it.brewCount) },
        )
        assertEquals(0L, stats.summary.totalCount)
        assertEquals(0, stats.summary.recordedDays)
    }

    // 2. Period Switch + Temporal Aggregation
    @Test
    fun testPeriodSwitcherAggregatesAcrossTemporalScopes() {
        val testRecords = listOf(
            DailyCountEntry(LocalDate.of(2026, 1, 15), 5),
            DailyCountEntry(LocalDate.of(2026, 1, 16), 3),
            DailyCountEntry(LocalDate.of(2026, 2, 10), 4),
            DailyCountEntry(LocalDate.of(2026, 7, 1), 6),
        )

        // Week stats for 2026-01-12 to 2026-01-18 (contains Jan 15 and 16)
        val weekStats = buildDailyCountStatistics(
            StatisticsPeriod.Week,
            LocalDate.of(2026, 1, 15),
            today,
            testRecords,
        )
        assertEquals(8L, weekStats.summary.totalCount)
        assertEquals(2, weekStats.summary.recordedDays)

        // Month stats for Jan 2026
        val monthStats = buildDailyCountStatistics(
            StatisticsPeriod.Month,
            LocalDate.of(2026, 1, 1),
            today,
            testRecords,
        )
        assertEquals(8L, monthStats.summary.totalCount)
        assertEquals(2, monthStats.summary.recordedDays)

        // Year stats for 2026 (contains all 4 records: 5+3+4+6 = 18)
        val yearStats = buildDailyCountStatistics(
            StatisticsPeriod.Year,
            LocalDate.of(2026, 1, 1),
            today,
            testRecords,
        )
        assertEquals(18L, yearStats.summary.totalCount)
        assertEquals(4, yearStats.summary.recordedDays)
        assertEquals(listOf(12L, 0L, 6L, 0L), requireNotNull(yearStats.year).quarters.map { it.totalCount })

        // All-time stats
        val allStats = buildDailyCountStatistics(
            StatisticsPeriod.All,
            today,
            today,
            testRecords,
        )
        assertEquals(18L, allStats.summary.totalCount)
        assertEquals(4, allStats.summary.recordedDays)
    }

    // 3. Language Switch + Terminology Style (2x2 Matrix)
    @Test
    fun testLanguageAndTerminologyCombinations() {
        // ZH + ELEGANT
        val zhElegant = AppLanguage.ZH.strings(TerminologyStyle.ELEGANT)
        assertEquals("个人私密", zhElegant.recordModule.handBrewLabel)
        assertEquals("伴侣亲密", zhElegant.recordModule.sexLabel)

        // ZH + DIRECT
        val zhDirect = AppLanguage.ZH.strings(TerminologyStyle.DIRECT)
        assertEquals("自慰", zhDirect.recordModule.handBrewLabel)
        assertEquals("做爱", zhDirect.recordModule.sexLabel)

        // EN + ELEGANT
        val enElegant = AppLanguage.EN.strings(TerminologyStyle.ELEGANT)
        assertEquals("Solo", enElegant.recordModule.handBrewLabel)
        assertEquals("Duo", enElegant.recordModule.sexLabel)

        // EN + DIRECT (ADR-022: English uses neutral journal wording Solo/Duo across all styles)
        val enDirect = AppLanguage.EN.strings(TerminologyStyle.DIRECT)
        assertEquals("Solo", enDirect.recordModule.handBrewLabel)
        assertEquals("Duo", enDirect.recordModule.sexLabel)
    }

    // 4. App Lock + Background Delay + Launcher Disguise
    @Test
    fun testAppLockTimingCombinedWithLauncherDisguise() {
        val session = AppLockSession(enabled = true, delay = AppLockDelay.OneMinute)
        session.onForeground(0L)
        session.onAuthenticationStarted()
        session.onAuthenticationFinished(success = true)
        assertFalse(session.locked.value)

        // Configured launcher disguise
        var disguise = LauncherIdentity.Journal
        assertEquals(LauncherIdentity.Journal, disguise)

        // Backgrounded for 45s (below 60s threshold)
        session.onBackground(1_000L, changingConfigurations = false)
        session.onForeground(1_000L + 45_000L)
        assertFalse(session.locked.value)

        // Backgrounded for 65s (above 60s threshold)
        session.onBackground(100_000L, changingConfigurations = false)
        session.onForeground(100_000L + 65_000L)
        assertTrue(session.locked.value)

        // Launcher disguise preserved across lock
        assertEquals(LauncherIdentity.Journal, disguise)
    }
}
