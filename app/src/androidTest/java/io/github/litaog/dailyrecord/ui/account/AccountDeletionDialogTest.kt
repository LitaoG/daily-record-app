package io.github.litaog.dailyrecord.ui.account

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.litaog.dailyrecord.core.account.LocalDataAfterAccountDeletion
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccountDeletionDialogTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun keepLocalIsDefaultAndPasswordIsRequiredForSecondConfirmation() {
        var submittedPassword = ""
        var submittedChoice: LocalDataAfterAccountDeletion? = null
        composeRule.setContent {
            DailyRecordTheme {
                AccountDeletionDialog(
                    onDeleteAccount = { password, choice ->
                        submittedPassword = password
                        submittedChoice = choice
                        Result.success(Unit)
                    },
                    onDismiss = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("保留在本机（推荐），已选择")
            .assertIsDisplayed()
        composeRule.onNodeWithText("继续验证身份").performClick()
        composeRule.onNodeWithText("再次确认永久删除").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("永久删除账号").performClick()
        composeRule.runOnIdle {
            assertTrue(submittedPassword.isEmpty())
            assertEquals(null, submittedChoice)
        }

        composeRule.onNodeWithTag("account_deletion_password").performTextInput("test-password")
        composeRule.onNodeWithContentDescription("永久删除账号").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { submittedChoice != null }
        composeRule.runOnIdle {
            assertEquals("test-password", submittedPassword)
            assertEquals(LocalDataAfterAccountDeletion.Keep, submittedChoice)
        }
    }

    @Test
    fun deleteLocalChoiceAndFailureRemainInsideDialog() {
        composeRule.setContent {
            DailyRecordTheme {
                AccountDeletionDialog(
                    onDeleteAccount = { _, choice ->
                        assertEquals(LocalDataAfterAccountDeletion.Delete, choice)
                        Result.failure(IllegalStateException("simulated"))
                    },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("同时删除本机记录，未选择").performClick()
        composeRule.onNodeWithContentDescription("同时删除本机记录，已选择").assertIsDisplayed()
        composeRule.onNodeWithText("继续验证身份").performClick()
        composeRule.onNodeWithTag("account_deletion_password").performTextInput("test-password")
        composeRule.onNodeWithContentDescription("永久删除账号").performClick()

        composeRule.onNodeWithTag("account_deletion_error").assertIsDisplayed()
        composeRule
            .onNodeWithText("删除未完成，本机记录仍保留；部分云端记录可能已先删除，请直接重试")
            .assertIsDisplayed()
    }
}
