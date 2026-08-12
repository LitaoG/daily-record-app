package io.github.litaog.dailyrecord.ui.account

import io.github.litaog.dailyrecord.core.common.AppCopy
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import io.github.litaog.dailyrecord.core.sync.SyncFailureKind
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccountDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun allSyncStatesRemainReadableAndActionable() {
        var status by mutableStateOf<SyncStatus>(SyncStatus.Offline)
        var signOutRequested = false
        composeRule.setContent {
            DailyRecordTheme {
                AccountDialog(
                    email = "demo@example.com",
                    status = status,
                    onSyncNow = {},
                    onDeleteAccount = {},
                    onSignOut = { signOutRequested = true },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("当前离线，记录已保存在本机").assertIsDisplayed()
        composeRule.onAllNodesWithText("查看诊断信息").assertCountEquals(0)
        composeRule.onNodeWithText("删除账号与云端数据").assertIsDisplayed()

        composeRule.runOnIdle { status = SyncStatus.Syncing }
        composeRule.onNodeWithContentDescription("正在同步").assertIsNotEnabled()

        composeRule.runOnIdle { status = SyncStatus.Pending(3) }
        composeRule.onNodeWithText("有 3 条记录待同步").assertIsDisplayed()

        composeRule.runOnIdle {
            status = SyncStatus.Failed(
                "网络连接异常，记录仍在本机。",
                kind = SyncFailureKind.Network,
            )
        }
        composeRule.onNodeWithText("网络连接异常").assertIsDisplayed()
        composeRule.onNodeWithText("网络连接异常，记录仍在本机。").assertIsDisplayed()
        composeRule.onNodeWithText(AppCopy.Account.syncDialogMessage).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("立即同步").assertIsDisplayed()

        composeRule.runOnIdle {
            status = SyncStatus.Failed(
                "登录状态已失效，记录仍在本机。",
                kind = SyncFailureKind.Authentication,
            )
        }
        composeRule.onNodeWithText("登录状态已失效").assertIsDisplayed()
        composeRule.onNodeWithText("请重新登录账号，然后再次同步本机记录。").assertIsDisplayed()
        composeRule.onAllNodesWithTag("account_vpn_sync_guidance").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("重新登录").performClick()
        composeRule.runOnIdle { assertTrue(signOutRequested) }
    }

    @Test
    fun networkGuidanceAndActionsRemainVisibleAt200PercentText() {
        val density = composeRule.activity.resources.displayMetrics.density
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                DailyRecordTheme {
                    AccountDialog(
                        email = "demo@example.com",
                        status = SyncStatus.Failed(
                            message = "网络连接异常，记录仍在本机。",
                            kind = SyncFailureKind.Network,
                        ),
                        onSyncNow = {},
                        onDeleteAccount = {},
                        onSignOut = {},
                        onDismiss = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag("account_vpn_sync_guidance").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("立即同步").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("关闭").assertIsDisplayed()
    }
}
