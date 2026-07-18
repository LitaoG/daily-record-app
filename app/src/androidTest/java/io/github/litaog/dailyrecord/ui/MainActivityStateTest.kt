package io.github.litaog.dailyrecord.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import io.github.litaog.dailyrecord.MainActivity
import io.github.litaog.dailyrecord.DailyRecordApplication
import org.junit.Rule
import org.junit.Test

class MainActivityStateTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun localModeAndHistoricalMonthSurviveActivityRecreation() {
        (composeRule.activity.application as DailyRecordApplication)
            .firebaseServices
            .authRepository
            .signOut()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("先在本机使用").fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithText("先在本机使用").performClick()
        composeRule.onNodeWithContentDescription("上个月").performClick()
        composeRule.onNodeWithContentDescription("上个月").performClick()
        val historicalMonth = java.time.LocalDate.now().minusMonths(2)
        val historicalMonthDescription =
            "选择年份和日期，当前${historicalMonth.year}年${historicalMonth.monthValue}月"
        composeRule.onNodeWithContentDescription(historicalMonthDescription).assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("登录同步").fetchSemanticsNodes().size == 1
        }
        composeRule.onNodeWithContentDescription(historicalMonthDescription).assertIsDisplayed()
        composeRule.onNodeWithText("登录同步").assertIsDisplayed()
    }
}
