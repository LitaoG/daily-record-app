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
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.sync.SyncFailureKind
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DailyRecordAccountDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

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
        composeRule.onNodeWithText(AppCopy.vpnSyncFailure).assertIsDisplayed()
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
        composeRule.onNodeWithText(AppCopy.Account.syncDialogMessage).assertIsDisplayed()
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
        composeRule.onNodeWithText(AppCopy.Account.syncDialogMessage).assertIsDisplayed()
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
        composeRule.onAllNodesWithText(AppCopy.vpnSyncFailure).assertCountEquals(0)
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
