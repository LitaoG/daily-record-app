package io.github.litaog.dailyrecord.ui

import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.common.AppLanguage
import io.github.litaog.dailyrecord.core.common.AppLanguageState
import io.github.litaog.dailyrecord.core.common.TerminologyStyle
import io.github.litaog.dailyrecord.core.common.strings
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.account.label
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import io.github.litaog.dailyrecord.ui.theme.SexColorTokens
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Accessibility copy, semantics and privacy-shielding contracts for the app shell (JVM unit tests).
 */
class AppShellSemanticsTest {

    @Test
    fun moduleSelectorSemanticsAndRoleBindingsFollowAccessibilitySpec() {
        val previousStrings = AppLanguageState.current
        try {
            // Test Chinese and English language environments
            val languages = listOf(
                AppLanguage.ZH to TerminologyStyle.ELEGANT,
                AppLanguage.ZH to TerminologyStyle.DIRECT,
                AppLanguage.EN to TerminologyStyle.ELEGANT,
            )

            for ((lang, style) in languages) {
                AppLanguageState.current = lang.strings(style)

                RecordModule.entries.forEach { module ->
                    val spec = module.uiSpec()
                    val label = AppCopy.RecordModule.recordLabel(spec.semanticCountLabel)
                    assertFalse(label.isBlank())

                    // Verify active and inactive selectedState descriptions
                    val activeDescription = AppCopy.selectedState(label, true)
                    val inactiveDescription = AppCopy.selectedState(label, false)

                    assertFalse(activeDescription.isBlank())
                    assertFalse(inactiveDescription.isBlank())
                    assertNotEquals(activeDescription, inactiveDescription)
                    assertTrue(activeDescription.contains(label))
                    assertTrue(inactiveDescription.contains(label))
                }
            }
        } finally {
            AppLanguageState.current = previousStrings
        }
    }

    @Test
    fun bottomBarDestinationSemanticsProduceDistinctSelectedStates() {
        val previousStrings = AppLanguageState.current
        try {
            AppLanguageState.current = AppLanguage.ZH.strings()
            val calendarLabel = AppCopy.NavigationBar.calendar
            val statisticsLabel = AppCopy.NavigationBar.statistics

            assertFalse(calendarLabel.isBlank())
            assertFalse(statisticsLabel.isBlank())
            assertNotEquals(calendarLabel, statisticsLabel)

            val calActive = AppCopy.selectedState(calendarLabel, true)
            val calInactive = AppCopy.selectedState(calendarLabel, false)
            assertNotEquals(calActive, calInactive)
            assertTrue(calActive.contains(calendarLabel))

            val statActive = AppCopy.selectedState(statisticsLabel, true)
            val statInactive = AppCopy.selectedState(statisticsLabel, false)
            assertNotEquals(statActive, statInactive)
            assertTrue(statActive.contains(statisticsLabel))
        } finally {
            AppLanguageState.current = previousStrings
        }
    }

    @Test
    fun syncStatusChipAccessibilityDescriptionCoversAllSyncVariants() {
        val previousStrings = AppLanguageState.current
        try {
            AppLanguageState.current = AppLanguage.ZH.strings()

            val statuses: List<SyncStatus> = listOf(
                SyncStatus.UpToDate,
                SyncStatus.Syncing,
                SyncStatus.Pending(1),
                SyncStatus.Pending(10),
                SyncStatus.Offline,
                SyncStatus.Failed("network error"),
                SyncStatus.NotConfigured,
            )

            statuses.forEach { status ->
                val longLabel = status.label()
                val description = AppCopy.Account.syncChipDescription(longLabel)

                assertFalse("label must not be empty for $status", longLabel.isBlank())
                assertFalse("description must not be empty for $status", description.isBlank())
                assertTrue(
                    "description must contain status label for $status",
                    description.contains(longLabel),
                )
            }
        } finally {
            AppLanguageState.current = previousStrings
        }
    }

    @Test
    fun appLockContentEnforcesStrictPrivacyShieldingAndIsolation() {
        // App Lock Screen (ADR-023) MUST use HandBrewColorTokens (the neutral default accent),
        // and NEVER bind to SexColorTokens or any active module state.
        // It must NOT reveal whether the user was recording masturbation or intimacy.
        val neutralColors = HandBrewColorTokens
        val duoColors = SexColorTokens

        assertNotEquals(
            "Neutral lock colors must differ from duo module colors",
            neutralColors.primary,
            duoColors.primary,
        )

        val previousStrings = AppLanguageState.current
        try {
            AppLanguageState.current = AppLanguage.ZH.strings()

            val title = AppCopy.AppLock.lockedTitle
            val body = AppCopy.AppLock.lockedBody
            val notCompleted = AppCopy.AppLock.notCompleted
            val unlock = AppCopy.AppLock.unlock

            assertFalse(title.isBlank())
            assertFalse(body.isBlank())
            assertFalse(notCompleted.isBlank())
            assertFalse(unlock.isBlank())

            // Privacy verification: The copy on the lock screen must NEVER contain module names
            val forbiddenTerms = listOf("做爱", "自慰", "Sex", "Solo", "Duo", "Masturbation")
            for (term in forbiddenTerms) {
                assertFalse("Lock title must not leak module term '$term'", title.contains(term))
                assertFalse("Lock body must not leak module term '$term'", body.contains(term))
                assertFalse("Lock notCompleted must not leak module term '$term'", notCompleted.contains(term))
            }
        } finally {
            AppLanguageState.current = previousStrings
        }
    }
}
