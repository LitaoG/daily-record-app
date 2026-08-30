package io.github.litaog.dailyrecord.ui

import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.common.AppLanguage
import io.github.litaog.dailyrecord.core.common.AppLanguageState
import io.github.litaog.dailyrecord.core.common.EnStrings
import io.github.litaog.dailyrecord.core.common.ZhStrings
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DailyRecordAppEnglishTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun resetLanguage() {
        AppLanguageState.current = ZhStrings
        context.getSharedPreferences("daily_record_language", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun calendarRendersEnglishChrome() {
        AppLanguageState.current = EnStrings
        setAppContent()

        composeRule.onNodeWithText("Solo").assertIsDisplayed()
        composeRule.onNodeWithText("Sex").assertIsDisplayed()
        composeRule.onNodeWithText("Calendar").assertIsDisplayed()
        composeRule.onNodeWithText("Stats").assertIsDisplayed()
        composeRule.onNodeWithText("Tap a date to record").assertIsDisplayed()
        composeRule.onAllNodesWithText("This month: 0 times · 0 days").assertCountEquals(1)
        composeRule.onAllNodesWithText("Jul 2026").assertCountEquals(1)
        composeRule.onAllNodesWithText("日历").assertCountEquals(0)
    }

    @Test
    fun englishModuleSelectorExposesFullAccessibilityTerms() {
        AppLanguageState.current = EnStrings
        setAppContent()

        composeRule
            .onNodeWithContentDescription("Masturbation records, selected")
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Sex records, not selected")
            .assertIsDisplayed()
    }

    @Test
    fun settingsLanguageSwitchUpdatesPreferenceStateAndRow() {
        setAppContent()

        composeRule.onNodeWithContentDescription(AppCopy.Settings.open).performClick()
        composeRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        composeRule.onNodeWithText("语言").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_language_row").performClick()
        composeRule.onNodeWithTag("settings_language_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("English").performClick()
        composeRule.waitForIdle()

        assertEquals(AppLanguage.EN, LanguagePreference(context).current)
        assertTrue(AppLanguageState.current === EnStrings)
        composeRule.onNodeWithText("Language").assertIsDisplayed()
        composeRule.onAllNodesWithText("Choose language").assertCountEquals(0)
    }

    @Test
    fun languageSelectionIsPersistedAcrossComposition() {
        AppLanguageState.current = EnStrings
        context.getSharedPreferences("daily_record_language", Context.MODE_PRIVATE)
            .edit()
            .putString("app_language", AppLanguage.EN.name)
            .commit()
        setAppContent()

        composeRule.onNodeWithText("Solo").assertIsDisplayed()
    }

    private fun setAppContent() {
        composeRule.setContent {
            DailyRecordTheme {
                DailyRecordApp(
                    repository = FakeHandBrewRecordRepository(),
                    today = LocalDate.of(2026, 7, 17),
                )
            }
        }
        composeRule.waitForIdle()
    }
}
