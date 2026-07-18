package io.github.litaog.dailyrecord.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

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
        composeRule.onNodeWithText("去日历记录").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("calendar_screen").assertIsDisplayed()
        assertMonth(2026, 7)
    }

    @Test
    fun selectingAdjacentMonthDateReturnsToThatMonth() {
        setAppContent()

        composeRule
            .onNodeWithContentDescription("2026年6月29日，未填写")
            .performClick()
        composeRule.onNodeWithTag("record_screen").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("返回日历").performClick()
        assertMonth(2026, 6)
    }

    @Test
    fun monthControlsHandleRepeatedClicksAndReturnToToday() {
        setAppContent()

        composeRule.onNodeWithContentDescription("上个月").performClick()
        composeRule.onNodeWithContentDescription("上个月").performClick()
        assertMonth(2026, 5)

        composeRule.onNodeWithContentDescription("下个月").performClick()
        composeRule.onNodeWithContentDescription("下个月").performClick()
        assertMonth(2026, 7)
        composeRule.onNodeWithContentDescription("下个月").assertIsNotEnabled()

        composeRule.onNodeWithContentDescription("上个月").performClick()
        composeRule.onNodeWithContentDescription("回到今天").performClick()
        assertMonth(2026, 7)
    }

    @Test
    fun tappingMonthTitleOpensFastDatePicker() {
        setAppContent()

        composeRule
            .onNodeWithContentDescription("选择年份和日期，当前2026年7月")
            .performClick()
        composeRule.onNodeWithText("选择要查看的年份和日期").assertIsDisplayed()
        composeRule.onNodeWithText("取消").performClick()
        assertMonth(2026, 7)
    }

    @Test
    fun datePickerCanJumpDirectlyToAnotherYearAndDate() {
        setAppContent()

        composeRule
            .onNodeWithContentDescription("选择年份和日期，当前2026年7月")
            .performClick()
        composeRule.onNodeWithContentDescription("Switch to selecting a year").performClick()
        composeRule.onNodeWithText("Navigate to year 2025").performClick()
        composeRule.onNodeWithText("Thursday, July 3, 2025").performClick()
        composeRule.onNodeWithText("查看此日期").performClick()

        assertMonth(2025, 7)
    }

    @Test
    fun statisticsFollowHistoricalCalendarMonthAndKeepAnchorAcrossTabs() {
        setAppContent()

        composeRule.onNodeWithContentDescription("上个月").performClick()
        composeRule.onNodeWithContentDescription("上个月").performClick()
        composeRule.onNodeWithContentDescription("统计，未选择").performClick()
        composeRule.onNodeWithContentDescription("月统计，未选择").performClick()
        composeRule.onNodeWithText("2026年 5月").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("年统计，未选择").performClick()
        composeRule.onNodeWithText("2026年").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("月统计，未选择").performClick()
        composeRule.onNodeWithText("2026年 5月").assertIsDisplayed()
    }

    @Test
    fun accountDialogShowsSyncStateAndConfirmsSignOut() {
        composeRule.setContent {
            DailyRecordTheme {
                HandBrewApp(
                    repository = FakeHandBrewRecordRepository(),
                    today = LocalDate.of(2026, 7, 17),
                    accountEmail = "brew@example.com",
                    syncStatus = SyncStatus.UpToDate,
                )
            }
        }

        composeRule.onNodeWithContentDescription("账号与云同步，云端已同步").performClick()
        composeRule.onNodeWithText("brew@example.com").assertIsDisplayed()
        composeRule.onNodeWithText("退出登录").performClick()
        composeRule.onNodeWithText("确认退出登录？").assertIsDisplayed()
        composeRule.onNodeWithText("取消").performClick()
        composeRule.onNodeWithText("账号与云同步").assertIsDisplayed()
    }

    @Test
    fun localModeOffersLoginWithoutBlockingCalendar() {
        var requestedSignIn = false
        composeRule.setContent {
            DailyRecordTheme {
                HandBrewApp(
                    repository = FakeHandBrewRecordRepository(),
                    today = LocalDate.of(2026, 7, 17),
                    onSignIn = { requestedSignIn = true },
                )
            }
        }

        assertMonth(2026, 7)
        composeRule.onNodeWithContentDescription("登录账号并开启云同步").performClick()
        assertTrue(requestedSignIn)
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

    private fun assertMonth(year: Int, month: Int) {
        composeRule
            .onNodeWithContentDescription("选择年份和日期，当前${year}年${month}月")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
