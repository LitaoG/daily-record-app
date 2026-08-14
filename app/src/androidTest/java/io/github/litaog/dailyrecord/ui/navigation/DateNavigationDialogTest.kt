package io.github.litaog.dailyrecord.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import io.github.litaog.dailyrecord.ui.theme.HandBrewColorTokens
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DateNavigationDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun fastShortSwipeUsesMomentumBeforeSettling() {
        var selectedDate: LocalDate? = null

        composeRule.setContent {
            DailyRecordTheme {
                DateNavigationDialog(
                    initialDate = LocalDate.of(2020, 1, 15),
                    earliestDate = LocalDate.of(2020, 1, 1),
                    latestDate = LocalDate.of(2026, 7, 17),
                    colors = HandBrewColorTokens,
                    onDismiss = {},
                    onDateSelected = { selectedDate = it },
                )
            }
        }

        composeRule.onNodeWithTag("date_navigation_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("date_wheel_year").performTouchInput {
            swipe(
                start = center,
                end = Offset(center.x, center.y - height * .45f),
                durationMillis = 50,
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(AppCopy.Navigation.jumpToDate).performClick()

        val result = requireNotNull(selectedDate)
        assertTrue(
            "A quick date-wheel swipe should carry momentum across multiple years: $result",
            result.year >= 2022,
        )
    }
}
