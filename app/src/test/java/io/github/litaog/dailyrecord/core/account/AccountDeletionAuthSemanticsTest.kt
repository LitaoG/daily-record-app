package io.github.litaog.dailyrecord.core.account

import io.github.litaog.dailyrecord.core.auth.AuthAccount
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDeletionAuthSemanticsTest {

    @Test
    fun unknownAuthDeletionKeepsRecoveryCopyAndDoesNotRequeueCloudResync() = runBlocking {
        val calls = mutableListOf<String>()
        val failure = IllegalStateException("auth delete failed")
        val auth = FakeAuthRepository(calls, deleteFailure = failure)
        val remote = FakeRemote(calls)
        val local = FakeLocalStore(calls)

        val result = coordinator(auth, remote, local).deleteAccount(
            ownerId = OWNER,
            password = "correct-password",
            localData = LocalDataAfterAccountDeletion.Keep,
        )

        assertTrue(result is AccountDeletionResult.AuthDeletionPending)
        assertSame(failure, (result as AccountDeletionResult.AuthDeletionPending).cause)
        assertEquals(
            listOf(
                "reauth",
                "delete-cloud",
                "stage-local",
                "delete-auth",
            ),
            calls,
        )
        assertTrue(local.localCopyStaged)
        assertFalse(local.ownerCacheDeleted)
        assertFalse(local.pendingForResync)
    }

    @Test
    fun unknownAuthDeletionWithoutLocalCopyDoesNotRequeueCloudResync() = runBlocking {
        val calls = mutableListOf<String>()
        val failure = IllegalStateException("auth delete failed")
        val auth = FakeAuthRepository(calls, deleteFailure = failure)
        val remote = FakeRemote(calls)
        val local = FakeLocalStore(calls)

        val result = coordinator(auth, remote, local).deleteAccount(
            ownerId = OWNER,
            password = "correct-password",
            localData = LocalDataAfterAccountDeletion.Delete,
        )

        assertTrue(result is AccountDeletionResult.AuthDeletionPending)
        assertSame(failure, (result as AccountDeletionResult.AuthDeletionPending).cause)
        assertEquals(
            listOf("reauth", "delete-cloud", "delete-auth"),
            calls,
        )
        assertFalse(local.pendingForResync)
    }

    @Test
    fun definitiveAuthFailureIsRetryableAndRequeuesCloudResync() = runBlocking {
        val calls = mutableListOf<String>()
        val failure = IllegalStateException("recent login required")
        val auth = FakeAuthRepository(
            calls,
            deleteFailure = failure,
            definitiveDeleteFailure = true,
        )
        val remote = FakeRemote(calls)
        val local = FakeLocalStore(calls)

        val result = runCatching {
            coordinator(auth, remote, local).deleteAccount(
                ownerId = OWNER,
                password = "correct-password",
                localData = LocalDataAfterAccountDeletion.Delete,
            )
        }

        assertSame(failure, result.exceptionOrNull())
        assertEquals(
            listOf("reauth", "delete-cloud", "delete-auth", "mark-pending"),
            calls,
        )
        assertFalse(auth.accountDeleted)
        assertTrue(local.pendingForResync)
    }

    @Test
    fun authDeletionCancellationAfterRequestStartsKeepsUnknownState() = runBlocking {
        val calls = mutableListOf<String>()
        val cancellation = kotlinx.coroutines.CancellationException("cancelled")
        val auth = FakeAuthRepository(calls, deleteFailure = cancellation)
        val remote = FakeRemote(calls)
        val local = FakeLocalStore(calls)

        val result = coordinator(auth, remote, local).deleteAccount(
            ownerId = OWNER,
            password = "correct-password",
            localData = LocalDataAfterAccountDeletion.Keep,
        )

        assertTrue(result is AccountDeletionResult.AuthDeletionPending)
        assertSame(cancellation, (result as AccountDeletionResult.AuthDeletionPending).cause)
        assertEquals(
            listOf(
                "reauth",
                "delete-cloud",
                "stage-local",
                "delete-auth",
            ),
            calls,
        )
        assertFalse(local.pendingForResync)
        assertTrue(local.localCopyStaged)
    }

    @Test
    fun authDeletionMarkersBracketTheAuthDeletionRequest() = runBlocking {
        val calls = mutableListOf<String>()
        val auth = FakeAuthRepository(calls)
        val remote = FakeRemote(calls)
        val local = FakeLocalStore(calls)

        val result = coordinator(
            auth = auth,
            remote = remote,
            local = local,
            markAuthDeletionStarted = { calls += "mark-auth-started" },
            markAuthDeletionComplete = { calls += "mark-auth-complete" },
        ).deleteAccount(
            ownerId = OWNER,
            password = "correct-password",
            localData = LocalDataAfterAccountDeletion.Keep,
        )

        // The started marker must land before the Auth request (so a crash
        // mid-request is recoverable as an unknown outcome) and the complete
        // marker immediately after it succeeds (it is the durable proof that
        // startup may finish local cleanup without guessing).
        assertEquals(
            listOf(
                "reauth",
                "delete-cloud",
                "stage-local",
                "mark-auth-started",
                "delete-auth",
                "mark-auth-complete",
                "delete-owner-cache",
                "promote-local",
            ),
            calls,
        )
        assertTrue(result is AccountDeletionResult.Completed)
    }

    @Test
    fun authDeletionCompleteMarkerFailureLeavesLocalCleanupPending() = runBlocking {
        val calls = mutableListOf<String>()
        val auth = FakeAuthRepository(calls)
        val remote = FakeRemote(calls)
        val local = FakeLocalStore(calls)
        val markerFailure = IllegalStateException("journal write failed")

        val result = runCatching {
            coordinator(
                auth = auth,
                remote = remote,
                local = local,
                markAuthDeletionComplete = { throw markerFailure },
            ).deleteAccount(
                ownerId = OWNER,
                password = "correct-password",
                localData = LocalDataAfterAccountDeletion.Keep,
            )
        }

        assertTrue(result.exceptionOrNull() is AccountDeletionLocalCleanupPendingException)
        assertTrue(auth.accountDeleted)
        assertFalse(local.ownerCacheDeleted)
    }

    @Test
    fun cancellationDuringLocalCleanupPropagatesWithoutPendingState() = runBlocking {
        val calls = mutableListOf<String>()
        val cancellation = kotlinx.coroutines.CancellationException("cancelled")
        val auth = FakeAuthRepository(calls)
        val remote = FakeRemote(calls)
        val local = FakeLocalStore(calls, deleteOwnerCacheCancellation = cancellation)

        val result = runCatching {
            coordinator(auth, remote, local).deleteAccount(
                ownerId = OWNER,
                password = "correct-password",
                localData = LocalDataAfterAccountDeletion.Keep,
            )
        }

        assertSame(cancellation, result.exceptionOrNull())
        assertEquals(
            listOf("reauth", "delete-cloud", "stage-local", "delete-auth", "delete-owner-cache"),
            calls,
        )
        assertTrue(auth.accountDeleted)
    }

    private companion object {
        const val OWNER = "owner-id"
    }
}
