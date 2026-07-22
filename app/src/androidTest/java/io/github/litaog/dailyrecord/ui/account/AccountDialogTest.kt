package io.github.litaog.dailyrecord.ui.account

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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

        composeRule.runOnIdle { status = SyncStatus.Failed("网络暂不可用") }
        composeRule.onNodeWithText("同步失败：网络暂不可用").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("立即同步").assertIsDisplayed()
    }
}
