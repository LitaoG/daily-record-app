package io.github.litaog.dailyrecord.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

class HandBrewAppTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun recordsLoadingStateDoesNotPretendTheDatabaseIsEmpty() {
        val delayedRecords = MutableSharedFlow<List<io.github.litaog.dailyrecord.core.model.HandBrewRecord>>(
            extraBufferCapacity = 1,
        )
        composeRule.setContent {
            DailyRecordTheme {
                HandBrewApp(
                    repository = FakeHandBrewRecordRepository(recordsFlowOverride = delayedRecords),
                    today = LocalDate.of(2026, 7, 17),
                )
            }
        }

        composeRule.onNodeWithTag("records_loading").assertIsDisplayed()
        composeRule.onAllNodesWithText("0 次 · 0 天").assertCountEquals(0)

        composeRule.runOnIdle { delayedRecords.tryEmit(emptyList()) }
        composeRule.onNodeWithTag("calendar_screen").assertIsDisplayed()
    }

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
    fun adjacentMonthDatesAreHiddenFromTheCalendarGrid() {
        setAppContent()

        composeRule
            .onAllNodesWithContentDescription("2026年6月29日，未填写")
            .assertCountEquals(0)
        composeRule
            .onAllNodesWithContentDescription("2026年8月1日，未来日期，不可记录")
            .assertCountEquals(0)
        composeRule.onNodeWithContentDescription("上个月").performClick()
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

        composeRule.onAllNodesWithText("点此快速跳转").assertCountEquals(0)
        composeRule
            .onNodeWithContentDescription("选择年份和日期，当前2026年7月")
            .performClick()
        composeRule.onNodeWithTag("date_navigation_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("快速跳转").assertIsDisplayed()
        composeRule.onNodeWithText("取消").performClick()
        assertMonth(2026, 7)
    }

    @Test
    fun datePickerCanJumpDirectlyToAnotherYearAndDate() {
        setAppContent()

        composeRule
            .onNodeWithContentDescription("选择年份和日期，当前2026年7月")
            .performClick()
        composeRule.onNodeWithContentDescription("切换年份，当前2026年").performClick()
        composeRule.onNodeWithContentDescription("选择2025年").performClick()
        composeRule.onNodeWithContentDescription("2025年7月3日，星期四").performClick()
        composeRule.onNodeWithText("跳转到此日").performClick()

        assertMonth(2025, 7)
    }

    @Test
    fun datePickerDisablesFutureDatesAndMonths() {
        setAppContent()

        composeRule
            .onNodeWithContentDescription("选择年份和日期，当前2026年7月")
            .performClick()
        composeRule.onNodeWithContentDescription("2026年7月18日，星期六").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("快速跳转下个月").assertIsNotEnabled()
    }

    @Test
    fun statisticsFollowHistoricalCalendarMonthAndKeepAnchorAcrossTabs() {
        setAppContent()

        composeRule.onNodeWithContentDescription("上个月").performClick()
        composeRule.onNodeWithContentDescription("上个月").performClick()
        composeRule.onNodeWithContentDescription("统计，未选择").performClick()
        composeRule.onNodeWithContentDescription("月统计，未选择").performClick()
        composeRule.onNodeWithText("2026年 5月").assertIsDisplayed()
        composeRule.onNodeWithTag("month_distribution_card").assertIsDisplayed()
        composeRule.onAllNodesWithText("点此快速跳转").assertCountEquals(0)

        composeRule
            .onNodeWithContentDescription("选择统计日期，当前2026年 5月")
            .performClick()
        composeRule.onNodeWithTag("date_navigation_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("取消").performClick()

        composeRule.onNodeWithContentDescription("年统计，未选择").performClick()
        composeRule.onNodeWithText("2026年").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("月统计，未选择").performClick()
        composeRule.onNodeWithText("2026年 5月").assertIsDisplayed()
    }

    @Test
    fun weeklyStatisticsUseTheDistributionCard() {
        setAppContent()

        composeRule.onNodeWithContentDescription("统计，未选择").performClick()
        composeRule.onNodeWithTag("week_distribution_card").assertIsDisplayed()
        composeRule.onNodeWithText("每日分布").assertIsDisplayed()
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
        composeRule.onNodeWithTag("account_sync_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("brew@example.com").assertIsDisplayed()
        composeRule.onNodeWithText("退出登录").performClick()
        composeRule.onNodeWithText("确认退出登录？").assertIsDisplayed()
        composeRule.onNodeWithText("返回").performClick()
        composeRule.onNodeWithText("账号与云同步").assertIsDisplayed()
    }

    @Test
    fun networkSyncFailureShowsTemporaryVpnGuidance() {
        val status = androidx.compose.runtime.mutableStateOf<SyncStatus>(SyncStatus.Syncing)
        composeRule.setContent {
            DailyRecordTheme {
                HandBrewApp(
                    repository = FakeHandBrewRecordRepository(),
                    today = LocalDate.of(2026, 7, 17),
                    accountEmail = "brew@example.com",
                    syncStatus = status.value,
                )
            }
        }

        composeRule.runOnIdle {
            status.value = SyncStatus.Failed(
                message = "网络连接不稳定，记录已保存在本机",
                networkRelated = true,
            )
        }
        composeRule.onNodeWithText(VPN_SYNC_FAILURE_MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithTag("hand_brew_snackbar").assertIsDisplayed()
    }

    @Test
    fun nonNetworkSyncFailureDoesNotShowVpnGuidance() {
        val status = androidx.compose.runtime.mutableStateOf<SyncStatus>(SyncStatus.Syncing)
        composeRule.setContent {
            DailyRecordTheme {
                HandBrewApp(
                    repository = FakeHandBrewRecordRepository(),
                    today = LocalDate.of(2026, 7, 17),
                    accountEmail = "brew@example.com",
                    syncStatus = status.value,
                )
            }
        }

        composeRule.runOnIdle {
            status.value = SyncStatus.Failed(
                message = "云端数据暂时无法读取",
                networkRelated = false,
            )
        }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText(VPN_SYNC_FAILURE_MESSAGE).assertCountEquals(0)
        composeRule.onAllNodesWithTag("hand_brew_snackbar").assertCountEquals(0)
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
