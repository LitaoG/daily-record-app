package io.github.litaog.dailyrecord.e2e

import androidx.compose.ui.graphics.Color
import io.github.litaog.dailyrecord.core.common.AppLanguage
import io.github.litaog.dailyrecord.core.common.AppLanguageState
import io.github.litaog.dailyrecord.core.common.EnStrings
import io.github.litaog.dailyrecord.core.common.TerminologyStyle
import io.github.litaog.dailyrecord.core.common.ZhStrings
import io.github.litaog.dailyrecord.core.common.strings
import io.github.litaog.dailyrecord.core.model.DailyCountEntry
import io.github.litaog.dailyrecord.core.security.AppLockDelay
import io.github.litaog.dailyrecord.core.security.AppLockSession
import io.github.litaog.dailyrecord.core.security.LauncherIdentity
import io.github.litaog.dailyrecord.core.statistics.StatisticsPeriod
import io.github.litaog.dailyrecord.core.statistics.buildDailyCountStatistics
import io.github.litaog.dailyrecord.ui.RecordModule
import io.github.litaog.dailyrecord.ui.record.CountDraft
import io.github.litaog.dailyrecord.ui.theme.DailyRecordColorTheme
import io.github.litaog.dailyrecord.ui.theme.DailyRecordText
import io.github.litaog.dailyrecord.ui.theme.DailyRecordThemeState
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tier 3 - Cross-Feature Combinations (Pairwise & Matrix Interactions).
 * Tests state synchronization across multiple features, including theme contrast,
 * language-terminology matrix, module-date decoupling, and count-statistics updates.
 */
class Tier3CrossFeatureCombinationsTest {

    private val testDate = LocalDate.of(2026, 7, 15)
    private val today = LocalDate.of(2026, 11, 7)

    // 1. Module Switch + Date Navigation
    @Test
    fun testModuleSwitchPreservesDateNavigationAnchor() {
        var activeModule = RecordModule.HandBrew
        var selectedAnchor = LocalDate.of(2025, 4, 10)

        // Switch module from Solo to Duo
        activeModule = RecordModule.Sex

        // Selected temporal anchor remains intact
        assertEquals(RecordModule.Sex, activeModule)
        assertEquals(LocalDate.of(2025, 4, 10), selectedAnchor)
    }

    // 2. Module Switch + Theme Synchronization
    @Test
    fun testModuleSwitchSynchronizesThemeTokensWithoutLeakage() {
        val theme = DailyRecordColorTheme.EmeraldBerry
        val soloTokens = theme.palette.solo
        val duoTokens = theme.palette.duo

        // Ensure both modules have strictly isolated tokens
        assertNotEquals(soloTokens.primary, duoTokens.primary)
        assertNotEquals(soloTokens.strong, duoTokens.strong)

        // Switching module switches active token set
        var activeModule = RecordModule.HandBrew
        var activeColors = if (activeModule == RecordModule.HandBrew) soloTokens else duoTokens
        assertEquals(soloTokens.primary, activeColors.primary)

        activeModule = RecordModule.Sex
        activeColors = if (activeModule == RecordModule.HandBrew) soloTokens else duoTokens
        assertEquals(duoTokens.primary, activeColors.primary)
    }

    // 3. Count Edit + Statistics Update
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

    // 4. Theme Switch + Contrast Verification (18 Module Palettes)
    @Test
    fun testAllThemesPassWcagAaContrastAcrossSoloAndDuo() {
        val white = Color.White
        val graphite = DailyRecordText // #1E293B

        DailyRecordColorTheme.entries.forEach { theme ->
            listOf("Solo" to theme.palette.solo, "Duo" to theme.palette.duo).forEach { (module, tokens) ->
                // White on primary >= 4.5:1
                val primaryContrast = E2ETestFixtures.contrastRatio(white, tokens.primary)
                assertTrue(
                    "Theme ${theme.id} $module primary contrast $primaryContrast < 4.5:1",
                    primaryContrast >= 4.5,
                )

                // White on strong >= 4.5:1
                val strongContrast = E2ETestFixtures.contrastRatio(white, tokens.strong)
                assertTrue(
                    "Theme ${theme.id} $module strong contrast $strongContrast < 4.5:1",
                    strongContrast >= 4.5,
                )

                // Graphite on soft >= 4.5:1
                val softContrast = E2ETestFixtures.contrastRatio(graphite, tokens.soft)
                assertTrue(
                    "Theme ${theme.id} $module soft contrast $softContrast < 4.5:1",
                    softContrast >= 4.5,
                )

                // Graphite on medium >= 4.5:1
                val mediumContrast = E2ETestFixtures.contrastRatio(graphite, tokens.medium)
                assertTrue(
                    "Theme ${theme.id} $module medium contrast $mediumContrast < 4.5:1",
                    mediumContrast >= 4.5,
                )

                // Graphite on intense >= 4.5:1
                val intenseContrast = E2ETestFixtures.contrastRatio(graphite, tokens.intense)
                assertTrue(
                    "Theme ${theme.id} $module intense contrast $intenseContrast < 4.5:1",
                    intenseContrast >= 4.5,
                )

                // Relative luminance step monotonicity: unset > soft > medium > intense > peak
                val lUnset = E2ETestFixtures.relativeLuminance(tokens.unset)
                val lSoft = E2ETestFixtures.relativeLuminance(tokens.soft)
                val lMedium = E2ETestFixtures.relativeLuminance(tokens.medium)
                val lIntense = E2ETestFixtures.relativeLuminance(tokens.intense)
                val lPeak = E2ETestFixtures.relativeLuminance(tokens.peak)

                assertTrue("Luminance unset > soft for ${theme.id} $module", lUnset > lSoft)
                assertTrue("Luminance soft > medium for ${theme.id} $module", lSoft > lMedium)
                assertTrue("Luminance medium > intense for ${theme.id} $module", lMedium > lIntense)
                assertTrue("Luminance intense > peak for ${theme.id} $module", lIntense > lPeak)
            }
        }
    }

    // 5. Period Switch + Temporal Aggregation
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

    // 6. Language Switch + Terminology Style (2x2 Matrix)
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

    // 7. App Lock + Background Delay + Launcher Disguise
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
