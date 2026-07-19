package io.github.litaog.dailyrecord.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import io.github.litaog.dailyrecord.DailyRecordApplication
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LocalModeStartupTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun persistedLocalModeRendersWithoutInitializingFirebase() {
        val preference = LocalModePreference(composeRule.activity)
        preference.setEnabled(true)
        var providerCalls = 0

        try {
            val database = (composeRule.activity.application as DailyRecordApplication).database
            composeRule.setContent {
                DailyRecordTheme {
                    DailyRecordRoot(
                        database = database,
                        servicesProvider = {
                            providerCalls += 1
                            error("Firebase must stay lazy while local mode is active")
                        },
                    )
                }
            }

            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithTag("calendar_screen")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            composeRule.onNodeWithTag("calendar_screen").assertIsDisplayed()
            assertEquals(0, providerCalls)
        } finally {
            preference.setEnabled(false)
        }
    }
}
