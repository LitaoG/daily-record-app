package io.github.litaog.dailyrecord.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTheme
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PasswordResetDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @After
    fun resetCooldown() {
        // The cooldown tracker is process-global; leaving a recorded send
        // behind would leak into other auth tests.
        PasswordResetCooldown.resetForTesting()
    }

    @Test
    fun submittingAgainInsideTheCooldownShowsTheThrottlingHintWithoutSending() {
        val resetCalls = AtomicInteger()
        composeRule.setContent {
            DailyRecordTheme {
                PasswordResetDialog(
                    initialEmail = "user@example.com",
                    onDismiss = {},
                    onReset = {
                        resetCalls.incrementAndGet()
                        Result.success(Unit)
                    },
                    onEmailAccepted = {},
                )
            }
        }

        // A first successful send recorded the cooldown (issue #282's leak
        // exposed this branch): reopening the dialog and submitting again must
        // show the throttling hint and must not fire a second request.
        PasswordResetCooldown.recordSent("user@example.com")

        composeRule.onNodeWithText(AppCopy.Auth.sendResetEmail).performClick()
        composeRule.waitForIdle()

        composeRule
            .onNodeWithText(AppCopy.Auth.resetTooManyRequests)
            .assertIsDisplayed()
        assertEquals(0, resetCalls.get())
    }
}
