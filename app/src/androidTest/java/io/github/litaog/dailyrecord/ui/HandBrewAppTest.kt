package io.github.litaog.dailyrecord.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class HandBrewAppTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun navigationAndAllStatisticsTabsRemainClickable() {
        setAppContent()

        composeRule.onNodeWithContentDescription("统计，未选择").performClick()
        composeRule.onNodeWithTag("statistics_screen").assertIsDisplayed()
        listOf("月", "年", "全部", "周").forEach { period ->
            composeRule.onNodeWithContentDescription("${period}统计，未选择").performClick()
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithContentDescription("全部统计，未选择").performClick()
        composeRule.onNodeWithText("去日历记录").performClick()
        composeRule.onNodeWithText("2026年 7月").assertIsDisplayed()
    }

    @Test
    fun selectingAdjacentMonthDateReturnsToThatMonth() {
        setAppContent()

        composeRule
            .onNodeWithContentDescription("2026年6月29日，未填写")
            .performClick()
        composeRule.onNodeWithTag("record_screen").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("返回日历").performClick()
        composeRule.onNodeWithText("2026年 6月").assertIsDisplayed()
    }

    private fun setAppContent() {
        composeRule.setContent {
            DailyRecordTheme {
                HandBrewApp(
                    repository = FakeHandBrewRecordRepository(),
                    today = LocalDate.of(2026, 7, 17),
                )
            }
        }
        composeRule.waitForIdle()
    }
}
