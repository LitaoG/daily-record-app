package io.github.litaog.dailyrecord.ui.diagnostics

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DiagnosticDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun reportCanBeReviewedCopiedAndSharedInsideAppDialog() {
        val report = "Hand Brew Calendar diagnostics\nsync_status=offline"
        var copied = ""
        var shared = ""
        composeRule.setContent {
            DailyRecordTheme {
                DiagnosticDialog(
                    report = report,
                    onDismiss = {},
                    onCopy = {
                        copied = it
                        true
                    },
                    onShare = {
                        shared = it
                        true
                    },
                )
            }
        }

        composeRule.onNodeWithTag("diagnostic_dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("diagnostic_report").assertIsDisplayed()
        composeRule.onNodeWithText("不包含邮箱、手冲日期、次数或密码").assertIsDisplayed()

        composeRule.onNodeWithText("复制诊断信息").performClick()
        composeRule.onNodeWithText("诊断信息已复制").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(report, copied) }

        composeRule.onNodeWithText("分享诊断信息").performClick()
        composeRule.runOnIdle {
            assertEquals(report, shared)
            assertTrue(copied.isNotBlank())
        }
    }
}
