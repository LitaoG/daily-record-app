package io.github.litaog.dailyrecord.ui.auth

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
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
import com.google.firebase.auth.FirebaseAuthException

class AuthScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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

        composeRule.onNodeWithText("登录后可恢复云端记录").assertIsDisplayed()
        composeRule.onNodeWithTag("vpn_auth_notice").assertIsDisplayed()
        composeRule.onNodeWithText(
            "需使用 VPN 进行登录\n若暂无 VPN 可先选择使用“本机记录”来进行记录活动",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("注册").performClick()
        composeRule.onNodeWithText("注册后可保存数据到云端方便长久记录").assertIsDisplayed()
        composeRule.onNodeWithTag("vpn_auth_notice").assertIsDisplayed()
        composeRule.onNodeWithText(
            "需使用 VPN 进行注册\n若暂无 VPN 可先选择使用“本机记录”来进行记录活动",
        ).assertIsDisplayed()
        composeRule.onNodeWithText("暂不注册，先使用“本机记录”").assertIsDisplayed()
    }

    @Test
    fun authErrorsDistinguishNetworkCredentialsAndRegistrationServiceFailures() {
        assertEquals(
            "网络不可用或连接云服务超时，请检查 VPN（梯子）后重试。",
            authErrorMessage(
                IllegalStateException("wrapped", FirebaseNetworkException("offline")),
                AuthMode.SignIn,
            ),
        )
        assertEquals(
            "邮箱或密码错误，请检查后重试。",
            authErrorMessage(
                FirebaseAuthException("ERROR_INVALID_CREDENTIAL", "invalid"),
                AuthMode.SignIn,
            ),
        )
        assertEquals(
            "暂时无法创建账号，请稍后重试",
            authErrorMessage(IllegalStateException("service failure"), AuthMode.Register),
        )
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
        composeRule.onNodeWithText("暂不登录，先使用“本机记录”").performScrollTo().assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertTrue(continuedOffline) }
    }

    @Test
    fun backReturnsToLocalModeWhenAuthWasOpenedFromTheLocalApp() {
        var returnedToLocalMode = false
        composeRule.setContent {
            DailyRecordTheme {
                AuthScreen(
                    productionConfigured = true,
                    onSignIn = { _, _ -> Result.success(Unit) },
                    onRegister = { _, _ -> Result.success(Unit) },
                    onBack = { returnedToLocalMode = true },
                )
            }
        }

        composeRule.activity.onBackPressedDispatcher.onBackPressed()

        composeRule.runOnIdle { assertTrue(returnedToLocalMode) }
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
            "请检查收件箱和垃圾邮件，并按邮件提示修改密码。",
        ).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals("brew@example.com", submittedEmail) }
    }

    @Test
    fun passwordResetSubmitsOnlyAfterValidEmail() {
        var calls = 0
        composeRule.setContent {
            DailyRecordTheme {
                AuthScreen(
                    productionConfigured = true,
                    onSignIn = { _, _ -> Result.success(Unit) },
                    onRegister = { _, _ -> Result.success(Unit) },
                    onPasswordReset = { calls += 1; Result.success(Unit) },
                )
            }
        }

        composeRule.onNodeWithText("忘记密码？").performClick()
        composeRule.onNodeWithTag("password_reset_email").performTextInput("wrong")
        composeRule.onNodeWithText("请输入有效邮箱").assertIsDisplayed()
        composeRule.onNodeWithText("发送重置邮件").assertIsNotEnabled()
        composeRule.onNodeWithTag("password_reset_email").performTextReplacement("brew@example.com")
        composeRule.onNodeWithText("发送重置邮件").assertIsEnabled().performClick()
        composeRule.onNodeWithTag("password_reset_success").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(1, calls) }
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
        composeRule.onNodeWithText("网络不可用，重置邮件未发送。请打开 VPN（梯子）后重试。").assertIsDisplayed()
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
