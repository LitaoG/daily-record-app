package io.github.litaog.dailyrecord.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import io.github.litaog.dailyrecord.DailyRecordApplication
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import org.junit.Rule
import org.junit.Test

class LocalModeStartupTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun persistedLocalModeRendersWithoutInitializingFirebase() {
        val preference = LocalModePreference(composeRule.activity)
        preference.setEnabled(true)

        try {
            val application = composeRule.activity.application as DailyRecordApplication
            composeRule.setContent {
                DailyRecordTheme {
                    DailyRecordRoot(
                        database = application.database,
                        services = application.firebaseServices,
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithTag("calendar_screen")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithTag("calendar_screen").assertIsDisplayed()
        } finally {
            preference.setEnabled(false)
        }
    }
}
