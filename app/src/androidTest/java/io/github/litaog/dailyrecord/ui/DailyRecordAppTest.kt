package io.github.litaog.dailyrecord.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.sync.SyncFailureKind
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.account.VPN_SYNC_DIALOG_MESSAGE
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DailyRecordAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun recordsLoadingStateDoesNotPretendTheDatabaseIsEmpty() {
        val delayedRecords = MutableSharedFlow<List<io.github.litaog.dailyrecord.core.model.HandBrewRecord>>(
            extraBufferCapacity = 1,
        )
        composeRule.setContent {
            DailyRecordTheme {
                DailyRecordApp(
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
        composeRule.onAllNodesWithText(AppCopy.Statistics.title).assertCountEquals(1)
        listOf("月", "年", "全部", "周").forEach { period ->
            composeRule.onNodeWithContentDescription("${period}统计，未选择").performClick()
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithContentDescription("全部统计，未选择").performClick()
        composeRule.onNodeWithText("去日历填写").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("calendar_screen").assertIsDisplayed()
        assertMonth(2026, 7)
    }

    @Test
    fun settingsHubOpensFromHomeAndReturnsToCalendar() {
        setAppContent()

        composeRule.onNodeWithContentDescription(AppCopy.Settings.open).performClick()
        composeRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        composeRule.onNodeWithText(AppCopy.Settings.accountSection).assertIsDisplayed()
        composeRule.onAllNodesWithText("记录偏好").assertCountEquals(0)
        composeRule.onNodeWithTag("settings_version").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithContentDescription(AppCopy.Settings.back).performClick()
        composeRule.onNodeWithTag("calendar_screen").assertIsDisplayed()
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
        // The date wheels offer the past years directly; jumping to 2025 keeps
        // the selected month and day and moves the calendar to July 2025.
        composeRule.onNodeWithContentDescription("2025年").performClick()
        composeRule.onNodeWithText("跳转到此日").performClick()

        assertMonth(2025, 7)
    }

    @Test
    fun datePickerWheelsExcludeFutureDatesMonthsAndYears() {
        setAppContent()

        composeRule
            .onNodeWithContentDescription("选择年份和日期，当前2026年7月")
            .performClick()
        // Today is 2026-07-17: the wheels only offer days up to today, months
        // up to the current month and years up to the current year.
        composeRule.onAllNodesWithContentDescription("18日").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("八月").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("2027年").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("17日").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("七月").assertIsDisplayed()
    }

    @Test
    fun statisticsFollowHistoricalCalendarMonthAndKeepAnchorAcrossTabs() {
        setAppContent()

        composeRule.onNodeWithContentDescription("上个月").performClick()
        composeRule.onNodeWithContentDescription("上个月").performClick()
        composeRule.onNodeWithContentDescription("统计，未选择").performClick()
        composeRule.onNodeWithContentDescription("月统计，未选择").performClick()
        composeRule.onNodeWithText("2026年 5月").assertIsDisplayed()
        composeRule.onNodeWithTag("month_daily_count_card").assertIsDisplayed()
        composeRule.onNodeWithText("每日次数").assertIsDisplayed()
        composeRule.onAllNodesWithText("点此快速跳转").assertCountEquals(0)

        composeRule
            .onNodeWithContentDescription("选择统计范围，当前2026年 5月")
            .performClick()
        composeRule.onNodeWithTag("date_navigation_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("直接选择年份和月份").assertIsDisplayed()
        composeRule.onNodeWithText("一月").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("切换年份，当前2026年").performClick()
        composeRule.onNodeWithContentDescription("选择2025年").performClick()
        composeRule.onNodeWithText("2025年").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("选择2025年6月").performClick()
        composeRule.onNodeWithText("跳转到此月").performClick()
        composeRule.onNodeWithText("2025年 6月").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("年统计，未选择").performClick()
        composeRule.onNodeWithText("2025年").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("选择统计范围，当前2025年")
            .performClick()
        composeRule.onAllNodesWithText("直接选择年份").assertCountEquals(0)
        composeRule.onAllNodesWithText("上下滑动选择年份").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("返回日期选择").assertCountEquals(0)
        composeRule.onNodeWithText("选择年份").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("选择2024年").performClick()
        composeRule.onNodeWithText("跳转到此年").performClick()
        composeRule.onNodeWithText("2024年").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("月统计，未选择").performClick()
        composeRule.onNodeWithText("2024年 6月").assertIsDisplayed()
        composeRule.onNodeWithTag("month_composition_card").performScrollTo()
        composeRule.onNodeWithText("次数分布").assertIsDisplayed()
        composeRule.onNodeWithTag("month_extremes_card").performScrollTo().assertExists()
        composeRule.onNodeWithText("单日最高与最低").assertExists()
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
                DailyRecordApp(
                    repository = FakeHandBrewRecordRepository(),
                    today = LocalDate.of(2026, 7, 17),
                    accountEmail = "brew@example.com",
                    syncStatus = SyncStatus.UpToDate,
                )
            }
        }

        composeRule.onNodeWithContentDescription("账号与云同步状态：云端已同步").performClick()
        composeRule.onNodeWithTag("account_sync_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("brew@example.com").assertIsDisplayed()
        composeRule.onAllNodesWithText("查看诊断信息").assertCountEquals(0)
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
                DailyRecordApp(
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
                kind = SyncFailureKind.Network,
            )
        }
        composeRule.onNodeWithText(VPN_SYNC_FAILURE_MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithTag("daily_record_snackbar").assertIsDisplayed()
    }

    @Test
    fun openingAccountDialogMovesVpnGuidanceAboveTheModalScrim() {
        val status = androidx.compose.runtime.mutableStateOf<SyncStatus>(SyncStatus.Syncing)
        composeRule.setContent {
            DailyRecordTheme {
                DailyRecordApp(
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
                kind = SyncFailureKind.Network,
            )
        }
        composeRule.onNodeWithTag("daily_record_snackbar").assertIsDisplayed()

        composeRule.onNodeWithContentDescription(
            "账号与云同步状态：网络连接异常",
        ).performClick()

        composeRule.onNodeWithTag("account_sync_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("account_vpn_sync_guidance").assertIsDisplayed()
        composeRule.onNodeWithText(VPN_SYNC_DIALOG_MESSAGE).assertIsDisplayed()
        composeRule.onAllNodesWithTag("daily_record_snackbar").assertCountEquals(0)
    }

    @Test
    fun networkFailureWhileAccountDialogIsOpenStaysInsideTheDialog() {
        val status = androidx.compose.runtime.mutableStateOf<SyncStatus>(SyncStatus.UpToDate)
        composeRule.setContent {
            DailyRecordTheme {
                DailyRecordApp(
                    repository = FakeHandBrewRecordRepository(),
                    today = LocalDate.of(2026, 7, 17),
                    accountEmail = "brew@example.com",
                    syncStatus = status.value,
                )
            }
        }

        composeRule.onNodeWithContentDescription("账号与云同步状态：云端已同步").performClick()
        composeRule.onNodeWithTag("account_sync_dialog").assertIsDisplayed()

        composeRule.runOnIdle {
            status.value = SyncStatus.Failed(
                message = "网络连接不稳定，记录已保存在本机",
                kind = SyncFailureKind.Network,
            )
        }

        composeRule.onNodeWithTag("account_vpn_sync_guidance").assertIsDisplayed()
        composeRule.onNodeWithText(VPN_SYNC_DIALOG_MESSAGE).assertIsDisplayed()
        composeRule.onAllNodesWithTag("daily_record_snackbar").assertCountEquals(0)
    }

    @Test
    fun nonNetworkSyncFailureDoesNotShowVpnGuidance() {
        val status = androidx.compose.runtime.mutableStateOf<SyncStatus>(SyncStatus.Syncing)
        composeRule.setContent {
            DailyRecordTheme {
                DailyRecordApp(
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
                kind = SyncFailureKind.Service,
            )
        }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText(VPN_SYNC_FAILURE_MESSAGE).assertCountEquals(0)
        composeRule.onAllNodesWithTag("daily_record_snackbar").assertCountEquals(0)
    }

    @Test
    fun localModeOffersLoginWithoutBlockingCalendar() {
        var requestedSignIn = false
        composeRule.setContent {
            DailyRecordTheme {
                DailyRecordApp(
                    repository = FakeHandBrewRecordRepository(),
                    today = LocalDate.of(2026, 7, 17),
                    onSignIn = { requestedSignIn = true },
                )
            }
        }

        assertMonth(2026, 7)
        composeRule.onAllNodesWithText("诊断信息").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("查看本机诊断信息").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("登录账号并同步记录").performClick()
        assertTrue(requestedSignIn)
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

    private fun assertMonth(year: Int, month: Int) {
        composeRule
            .onNodeWithContentDescription("选择年份和日期，当前${year}年${month}月")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
