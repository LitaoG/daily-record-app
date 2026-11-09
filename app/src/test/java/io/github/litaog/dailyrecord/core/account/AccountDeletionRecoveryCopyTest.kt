package io.github.litaog.dailyrecord.core.account

import io.github.litaog.dailyrecord.core.auth.AuthAccount
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDeletionRecoveryCopyTest {

    @Test
    fun recoveryCopyIntentFailureDoesNotTouchLocalCopy() = runBlocking {
        val calls = mutableListOf<String>()
        val auth = FakeAuthRepository(calls)
        val remote = FakeRemote(calls)
        val local = FakeLocalStore(calls)
        val markerFailure = IllegalStateException("disk full")

        val result = runCatching {
            coordinator(
                auth = auth,
                remote = remote,
                local = local,
                markLocalRecoveryCopyPending = { throw markerFailure },
            ).deleteAccount(
                ownerId = OWNER,
                password = "correct-password",
                localData = LocalDataAfterAccountDeletion.Keep,
            )
        }

        assertSame(markerFailure, result.exceptionOrNull())
        assertEquals(
            listOf("reauth", "delete-cloud", "mark-pending"),
            calls,
        )
        assertFalse(local.localCopyStaged)
        assertFalse(auth.accountDeleted)
        assertTrue(local.pendingForResync)
    }

    @Test
    fun recoveryCopyReadyMarkerIsWrittenAfterRoomCopy() = runBlocking {
        val calls = mutableListOf<String>()
        val auth = FakeAuthRepository(calls)
        val remote = FakeRemote(calls)
        val local = FakeLocalStore(calls)

        coordinator(
            auth = auth,
            remote = remote,
            local = local,
            markLocalRecoveryCopyPending = { calls += "mark-copy-pending" },
            markLocalRecoveryCopyReady = { calls += "mark-copy-ready" },
        ).deleteAccount(
            ownerId = OWNER,
            password = "correct-password",
            localData = LocalDataAfterAccountDeletion.Keep,
        )

        assertEquals(
            listOf(
                "reauth",
                "delete-cloud",
                "mark-copy-pending",
                "stage-local",
                "mark-copy-ready",
                "delete-auth",
                "delete-owner-cache",
                "promote-local",
            ),
            calls,
        )
    }

    @Test
    fun recoveryCopyFailureAfterWriteDiscardsTheCopy() = runBlocking {
        val calls = mutableListOf<String>()
        val auth = FakeAuthRepository(calls)
        val remote = FakeRemote(calls)
        val local = FakeLocalStore(calls)
        val markerFailure = IllegalStateException("disk full")

        val result = runCatching {
            coordinator(
                auth = auth,
                remote = remote,
                local = local,
                markLocalRecoveryCopyReady = { throw markerFailure },
            ).deleteAccount(
                ownerId = OWNER,
                password = "correct-password",
                localData = LocalDataAfterAccountDeletion.Keep,
            )
        }

        assertSame(markerFailure, result.exceptionOrNull())
        assertEquals(
            listOf("reauth", "delete-cloud", "stage-local", "discard-local", "mark-pending"),
            calls,
        )
        assertFalse(local.localCopyStaged)
        assertTrue(local.pendingForResync)
    }

    @Test
    fun recoveryCopyDiscardFailureKeepsARecoverableResult() = runBlocking {
        val calls = mutableListOf<String>()
        val auth = FakeAuthRepository(calls)
        val remote = FakeRemote(calls)
        val local = FakeLocalStore(
            calls,
            discardLocalRecoveryCopyFailure = IllegalStateException("local db unavailable"),
        )
        val markerFailure = IllegalStateException("copy marker unavailable")

        val result = runCatching {
            coordinator(
                auth = auth,
                remote = remote,
                local = local,
                markLocalRecoveryCopyReady = { throw markerFailure },
            ).deleteAccount(
                ownerId = OWNER,
                password = "correct-password",
                localData = LocalDataAfterAccountDeletion.Keep,
            )
        }

        val exception = result.exceptionOrNull()
        assertTrue(exception is AccountDeletionLocalRecoveryPendingException)
        assertEquals(OWNER, (exception as AccountDeletionLocalRecoveryPendingException).ownerId)
        assertFalse(local.pendingForResync)
        assertFalse(auth.accountDeleted)
    }

    @Test
    fun localCleanupFailureReportsPendingStateInsteadOfRetryableFailure() = runBlocking {
        val calls = mutableListOf<String>()
        val cleanupFailure = IllegalStateException("local db unavailable")
        val auth = FakeAuthRepository(calls)
        val remote = FakeRemote(calls)
        val local = FakeLocalStore(calls, deleteOwnerCacheFailure = cleanupFailure)

        val result = runCatching {
            coordinator(auth, remote, local).deleteAccount(
                ownerId = OWNER,
                password = "correct-password",
                localData = LocalDataAfterAccountDeletion.Keep,
            )
        }

        val exception = result.exceptionOrNull()
        assertTrue(exception is AccountDeletionLocalCleanupPendingException)
        assertEquals(OWNER, (exception as AccountDeletionLocalCleanupPendingException).ownerId)
        assertSame(cleanupFailure, exception.cause)
        assertEquals(
            listOf("reauth", "delete-cloud", "stage-local", "delete-auth", "delete-owner-cache"),
            calls,
        )
        assertTrue(auth.accountDeleted)
        assertTrue(local.localCopyStaged)
        assertFalse(local.ownerCacheDeleted)
    }

    @Test
    fun cloudDeletionPhaseIsPersistedBeforeLocalCleanup() = runBlocking {
        val calls = mutableListOf<String>()
        val auth = FakeAuthRepository(calls)
        val remote = FakeRemote(calls)
        val local = FakeLocalStore(calls)
        coordinator(auth, remote, local, markCloudDeletionComplete = { calls += "mark-cloud-deleted" })
            .deleteAccount(
                ownerId = OWNER,
                password = "correct-password",
                localData = LocalDataAfterAccountDeletion.Delete,
            )

        assertEquals(
            listOf("reauth", "delete-cloud", "mark-cloud-deleted", "delete-auth", "delete-owner-cache"),
            calls,
        )
    }

    private companion object {
        const val OWNER = "owner-id"
    }
}
