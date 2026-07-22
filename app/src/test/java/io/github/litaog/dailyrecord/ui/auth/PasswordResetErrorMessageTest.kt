package io.github.litaog.dailyrecord.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class PasswordResetErrorMessageTest {
    @Test
    fun mapsNetworkAndRetryLimitsToActionableMessages() {
        assertEquals(
            "网络不可用，邮件尚未发送。请检查连接后重试。",
            passwordResetErrorMessageFor(code = "", networkFailure = true),
        )
        assertEquals(
            "请求过于频繁，请稍后再试。",
            passwordResetErrorMessageFor(code = "ERROR_TOO_MANY_REQUESTS", networkFailure = false),
        )
        assertEquals(
            "今日发送额度已用完，请稍后再试。",
            passwordResetErrorMessageFor(code = "ERROR_QUOTA_EXCEEDED", networkFailure = false),
        )
    }

    @Test
    fun unknownFailureDoesNotExposeProviderDetails() {
        assertEquals(
            "暂时无法发送重置邮件，请稍后重试。",
            passwordResetErrorMessageFor(code = "", networkFailure = false),
        )
    }
}
