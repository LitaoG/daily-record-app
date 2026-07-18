package io.github.litaog.dailyrecord.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

class AuthScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loginValidatesEmailAndPasswordBeforeSubmitting() {
        setContent()

        composeRule.onNodeWithText("登录并恢复记录").assertIsNotEnabled()
        composeRule.onNodeWithText("邮箱").performTextInput("not-an-email")
        composeRule.onNodeWithText("密码").performTextInput("12345678")
        composeRule.onNodeWithText("请输入有效邮箱").assertIsDisplayed()
        composeRule.onNodeWithText("登录并恢复记录").assertIsNotEnabled()
    }

    @Test
    fun registrationRequiresMatchingPasswords() {
        setContent()

        composeRule.onNodeWithText("注册").performClick()
        composeRule.onNodeWithText("邮箱").performTextInput("brew@example.com")
        composeRule.onNodeWithText("密码").performTextInput("password-1")
        composeRule.onNodeWithText("再次输入密码").performTextInput("password-2")
        composeRule.onNodeWithText("两次输入的密码不一致").assertIsDisplayed()
        composeRule.onNodeWithText("创建账号").assertIsNotEnabled()
    }

    @Test
    fun repeatedSubmitIsLockedWhileRequestIsRunning() {
        val gate = CompletableDeferred<Result<Unit>>()
        var calls = 0
        composeRule.setContent {
            DailyRecordTheme {
                AuthScreen(
                    productionConfigured = true,
                    onSignIn = { _, _ -> calls += 1; gate.await() },
                    onRegister = { _, _ -> Result.success(Unit) },
                )
            }
        }
        composeRule.onNodeWithText("邮箱").performTextInput("brew@example.com")
        composeRule.onNodeWithText("密码").performTextInput("password-1")
        composeRule.onNodeWithText("登录并恢复记录").assertIsEnabled().performClick()
        composeRule.onNodeWithText("请稍候…").assertIsNotEnabled()
        assertEquals(1, calls)
        gate.complete(Result.success(Unit))
    }

    @Test
    fun keyboardDoneSubmitsAValidLogin() {
        var calls = 0
        composeRule.setContent {
            DailyRecordTheme {
                AuthScreen(
                    productionConfigured = true,
                    onSignIn = { _, _ -> calls += 1; Result.success(Unit) },
                    onRegister = { _, _ -> Result.success(Unit) },
                )
            }
        }

        composeRule.onNodeWithText("邮箱").performTextInput("brew@example.com")
        composeRule.onNodeWithText("密码").performTextInput("password-1")
        composeRule.onNodeWithText("密码").performImeAction()
        composeRule.runOnIdle { assertEquals(1, calls) }
    }

    @Test
    fun localFirstEntryDoesNotRequireCloudConfiguration() {
        var continuedOffline = false
        composeRule.setContent {
            DailyRecordTheme {
                AuthScreen(
                    productionConfigured = false,
                    onSignIn = { _, _ -> Result.success(Unit) },
                    onRegister = { _, _ -> Result.success(Unit) },
                    onContinueOffline = { continuedOffline = true },
                )
            }
        }

        composeRule.onNodeWithText("登录并恢复记录").assertIsNotEnabled()
        composeRule.onNodeWithText("先在本机使用").performScrollTo().assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertTrue(continuedOffline) }
    }

    private fun setContent() {
        composeRule.setContent {
            DailyRecordTheme {
                AuthScreen(
                    productionConfigured = true,
                    onSignIn = { _, _ -> Result.success(Unit) },
                    onRegister = { _, _ -> Result.success(Unit) },
                )
            }
        }
    }
}
