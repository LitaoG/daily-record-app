package io.github.litaog.dailyrecord.ui.account

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountDeletionErrorMessageTest {
    @Test
    fun knownFailuresHaveActionableMessages() {
        assertEquals(
            "密码不正确，请重新输入",
            accountDeletionErrorMessageForCode("ERROR_WRONG_PASSWORD"),
        )
        assertEquals(
            "网络不可用，请打开 VPN（梯子）后重试。",
            accountDeletionErrorMessageForCode("ERROR_NETWORK_REQUEST_FAILED"),
        )
        assertEquals(
            "登录状态已失效，请重新登录后再删除。",
            accountDeletionErrorMessageForCode("ERROR_REQUIRES_RECENT_LOGIN"),
        )
    }

    @Test
    fun unknownFailureDoesNotClaimThatDeletionSucceeded() {
        assertEquals(
            "删除未完成，本机记录仍保留。部分云端记录可能已删除，请重试。",
            accountDeletionErrorMessageForCode("UNKNOWN"),
        )
    }

    @Test
    fun networkFailureExplainsThatLocalRecordsRemainSafe() {
        assertEquals(
            "网络中断，删除未完成。本机记录仍保留，请打开 VPN（梯子）后重试。",
            accountDeletionErrorMessage(IOException("offline")),
        )
    }
}
