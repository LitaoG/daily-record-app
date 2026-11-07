package io.github.litaog.dailyrecord.ui.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.common.AppLanguage
import io.github.litaog.dailyrecord.core.security.AppLockDelay
import io.github.litaog.dailyrecord.core.security.LauncherIdentity
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.lock.AppLockSettings
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Settings rows of the app lock (ADR-023) and the home screen disguise (ADR-024). */
class SettingsPrivacyControlsTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val isSwitch = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)

    private fun appLock(
        enabled: Boolean = false,
        deviceSecure: Boolean = true,
        onEnabledChange: (Boolean) -> Unit = {},
    ) = AppLockSettings(
        enabled = enabled,
        delay = AppLockDelay.Immediately,
        deviceSecure = deviceSecure,
        blocksScreenshots = false,
        onEnabledChange = onEnabledChange,
        onDelayChange = {},
    )

    private fun disguise(
        identity: LauncherIdentity = LauncherIdentity.Default,
        onIdentityChange: (LauncherIdentity) -> Unit = {},
    ) = LauncherDisguiseSettings(identity = identity, onIdentityChange = onIdentityChange)

    private fun setSettings(appLock: AppLockSettings, disguise: LauncherDisguiseSettings) {
        composeRule.setContent {
            DailyRecordTheme {
                SettingsScreen(
                    versionName = "test",
                    accountEmail = null,
                    syncStatus = SyncStatus.NotConfigured,
                    moduleColors = HandBrewColorTokens,
                    language = AppLanguage.ZH,
                    onLanguageSelected = {},
                    appLockSettings = appLock,
                    launcherDisguiseSettings = disguise,
                    onBack = {},
                    onOpenAccount = {},
                    onSignIn = null,
                )
            }
        }
    }

    /** The settings list is lazy; bring a row into view before asserting on it. */
    private fun scrollTo(tag: String) {
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasTestTag(tag))
    }

    @Test
    fun appLockRowIsASwitchThatSpeaksTheAppLanguage() {
        var requested: Boolean? = null
        setSettings(appLock(onEnabledChange = { requested = it }), disguise())
        scrollTo("settings_app_lock_row")

        composeRule.onNodeWithTag("settings_app_lock_row")
            .assertIsOff()
            .assert(isSwitch)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, AppCopy.AppLock.settingOff))
            .performClick()

        assertEquals(true, requested)
    }

    @Test
    fun appLockCannotBeTurnedOnWithoutAScreenLock() {
        setSettings(appLock(deviceSecure = false), disguise())
        scrollTo("settings_app_lock_row")

        composeRule.onNodeWithTag("settings_app_lock_row").assertIsNotEnabled()
        composeRule.onNodeWithText(AppCopy.AppLock.needsScreenLock).assertIsDisplayed()
    }

    @Test
    fun relockDelayRowIsHiddenWhileTheLockIsOff() {
        setSettings(appLock(enabled = false), disguise())
        // The disguise row shares the card, so the card is composed here.
        scrollTo("settings_launcher_disguise_row")

        composeRule.onAllNodesWithTag("settings_app_lock_delay_row").assertCountEquals(0)
    }

    @Test
    fun relockDelayRowAppearsWhileTheLockIsOn() {
        setSettings(appLock(enabled = true), disguise())
        scrollTo("settings_app_lock_delay_row")

        composeRule.onNodeWithTag("settings_app_lock_delay_row").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_app_lock_row").assertIsOn()
    }

    @Test
    fun disguiseRowTurnsTheJournalIdentityOn() {
        var requested: LauncherIdentity? = null
        setSettings(appLock(), disguise(onIdentityChange = { requested = it }))
        scrollTo("settings_launcher_disguise_row")

        composeRule.onNodeWithTag("settings_launcher_disguise_row")
            .assertIsOff()
            .assert(isSwitch)
            .performClick()

        assertEquals(LauncherIdentity.Journal, requested)
    }

    @Test
    fun disguiseRowTurnsBackToTheDefaultIdentity() {
        var requested: LauncherIdentity? = null
        setSettings(
            appLock(),
            disguise(identity = LauncherIdentity.Journal, onIdentityChange = { requested = it }),
        )
        scrollTo("settings_launcher_disguise_row")

        composeRule.onNodeWithTag("settings_launcher_disguise_row")
            .assertIsOn()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, AppCopy.AppLock.settingOn))
            .performClick()

        assertEquals(LauncherIdentity.Default, requested)
    }
}
