package io.github.litaog.dailyrecord.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import com.google.firebase.FirebaseNetworkException

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
    fun loginAndRegistrationExplainVpnAndLocalOnlyBehavior() {
        setContent()

        composeRule.onNodeWithTag("vpn_auth_notice").assertIsDisplayed()
        composeRule.onNodeWithText(
            "登录和注册需要打开 VPN（梯子）。选择本机使用则无需开启，但不会同步到云端。",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("注册").performClick()
        composeRule.onNodeWithTag("vpn_auth_notice").assertIsDisplayed()
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

    @Test
    fun passwordResetPrefillsNormalizesAndUsesPrivacySafeSuccessMessage() {
        var submittedEmail = ""
        composeRule.setContent {
            DailyRecordTheme {
                AuthScreen(
                    productionConfigured = true,
                    onSignIn = { _, _ -> Result.success(Unit) },
                    onRegister = { _, _ -> Result.success(Unit) },
                    onPasswordReset = { submittedEmail = it; Result.success(Unit) },
                )
            }
        }

        composeRule.onNodeWithText("邮箱").performTextInput(" Brew@Example.Com ")
        composeRule.onNodeWithText("忘记密码？").performClick()
        composeRule.onNodeWithTag("password_reset_email").assertIsDisplayed()
        composeRule.onNodeWithText("发送重置邮件").performClick()
        composeRule.onNodeWithTag("password_reset_success").assertIsDisplayed()
        composeRule.onNodeWithText(
            "如果该邮箱已注册，重置邮件将在几分钟内送达。请检查收件箱和垃圾邮件。",
        ).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals("brew@example.com", submittedEmail) }
    }

    @Test
    fun passwordResetValidatesEmailAndLocksRepeatedSend() {
        val gate = CompletableDeferred<Result<Unit>>()
        var calls = 0
        composeRule.setContent {
            DailyRecordTheme {
                AuthScreen(
                    productionConfigured = true,
                    onSignIn = { _, _ -> Result.success(Unit) },
                    onRegister = { _, _ -> Result.success(Unit) },
                    onPasswordReset = { calls += 1; gate.await() },
                )
            }
        }

        composeRule.onNodeWithText("忘记密码？").performClick()
        composeRule.onNodeWithTag("password_reset_email").performTextInput("wrong")
        composeRule.onNodeWithText("请输入有效邮箱").assertIsDisplayed()
        composeRule.onNodeWithText("发送重置邮件").assertIsNotEnabled()
        composeRule.onNodeWithTag("password_reset_email").performTextReplacement("brew@example.com")
        composeRule.onNodeWithText("发送重置邮件").assertIsEnabled().performClick()
        composeRule.onNodeWithText("正在发送…").assertIsNotEnabled()
        composeRule.runOnIdle { assertEquals(1, calls) }
        gate.complete(Result.success(Unit))
    }

    @Test
    fun passwordResetShowsRetryableNetworkFailureWithoutClosingDialog() {
        composeRule.setContent {
            DailyRecordTheme {
                AuthScreen(
                    productionConfigured = true,
                    onSignIn = { _, _ -> Result.success(Unit) },
                    onRegister = { _, _ -> Result.success(Unit) },
                    onPasswordReset = { Result.failure(FirebaseNetworkException("offline")) },
                )
            }
        }

        composeRule.onNodeWithText("忘记密码？").performClick()
        composeRule.onNodeWithTag("password_reset_email").performTextInput("brew@example.com")
        composeRule.onNodeWithText("发送重置邮件").performClick()
        composeRule.onNodeWithText("网络不可用，邮件尚未发送。请打开 VPN（梯子）后重试。").assertIsDisplayed()
        composeRule.onNodeWithText("发送重置邮件").assertIsEnabled()
    }

    @Test
    fun passwordResetActionsRemainVisibleAt200PercentText() {
        composeRule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 2f)) {
                DailyRecordTheme {
                    AuthScreen(
                        productionConfigured = true,
                        onSignIn = { _, _ -> Result.success(Unit) },
                        onRegister = { _, _ -> Result.success(Unit) },
                    )
                }
            }
        }

        composeRule.onNodeWithText("忘记密码？").performClick()
        composeRule.onNodeWithText("发送重置邮件").assertIsDisplayed()
        composeRule.onNodeWithText("取消").assertIsDisplayed()
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
