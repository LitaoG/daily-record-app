package io.github.litaog.dailyrecord.ui.account

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import org.junit.Rule
import org.junit.Test

class AccountDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun allSyncStatesRemainReadableAndActionable() {
        var status by mutableStateOf<SyncStatus>(SyncStatus.Offline)
        composeRule.setContent {
            DailyRecordTheme {
                AccountDialog(
                    email = "demo@example.com",
                    status = status,
                    onSyncNow = {},
                    onSignOut = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("当前离线，记录已保存在本机").assertIsDisplayed()

        composeRule.runOnIdle { status = SyncStatus.Syncing }
        composeRule.onNodeWithContentDescription("正在同步").assertIsNotEnabled()

        composeRule.runOnIdle { status = SyncStatus.Pending(3) }
        composeRule.onNodeWithText("有 3 条记录等待同步").assertIsDisplayed()

        composeRule.runOnIdle {
            status = SyncStatus.Failed("网络暂不可用", networkRelated = true)
        }
        composeRule.onNodeWithText("同步失败：网络暂不可用").assertIsDisplayed()
        composeRule.onNodeWithText(VPN_SYNC_DIALOG_MESSAGE).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("立即同步").assertIsDisplayed()
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
                            message = "网络连接不稳定，记录已保存在本机",
                            networkRelated = true,
                        ),
                        onSyncNow = {},
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
