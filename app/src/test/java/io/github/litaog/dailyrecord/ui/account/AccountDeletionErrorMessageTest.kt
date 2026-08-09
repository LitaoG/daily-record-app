package io.github.litaog.dailyrecord.ui.account

import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalCleanupPendingException
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

    @Test
    fun localCleanupPendingExplainsThatAccountIsAlreadyDeleted() {
        assertEquals(
            "账号和云端数据已删除，但本机记录清理未完成，将在下次启动时自动完成。",
            accountDeletionErrorMessage(
                AccountDeletionLocalCleanupPendingException("owner", IOException("db")),
            ),
        )
    }
}
