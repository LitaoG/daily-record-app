package io.github.litaog.dailyrecord.core.account

import io.github.litaog.dailyrecord.core.auth.AuthAccount
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDeletionCoreFlowsTest {

    @Test
    fun keepLocalDeletesCloudThenAuthAndRetainsRecoveryCopy() = runBlocking {
        val calls = mutableListOf<String>()
        val auth = FakeAuthRepository(calls)
        val remote = FakeRemote(calls)
        val local = FakeLocalStore(calls)

        coordinator(auth, remote, local).deleteAccount(
            ownerId = OWNER,
            password = "correct-password",
            localData = LocalDataAfterAccountDeletion.Keep,
        )

        assertEquals(
            listOf("reauth", "delete-cloud", "stage-local", "delete-auth", "delete-owner-cache", "promote-local"),
            calls,
        )
        assertTrue(auth.accountDeleted)
        assertTrue(local.localCopyPromoted)
        assertTrue(local.ownerCacheDeleted)
    }

    @Test
    fun deleteLocalSkipsRecoveryCopyAndDeletesOwnerCacheAfterAuth() = runBlocking {
        val calls = mutableListOf<String>()
        val auth = FakeAuthRepository(calls)
        val remote = FakeRemote(calls)
        val local = FakeLocalStore(calls)

        coordinator(auth, remote, local).deleteAccount(
            ownerId = OWNER,
            password = "correct-password",
            localData = LocalDataAfterAccountDeletion.Delete,
        )

        assertEquals(
            listOf("reauth", "delete-cloud", "delete-auth", "delete-owner-cache"),
            calls,
        )
        assertFalse(local.localCopyStaged)
        assertTrue(local.ownerCacheDeleted)
    }

    @Test
    fun reauthenticationFailureMakesNoDataChanges() = runBlocking {
        val calls = mutableListOf<String>()
        val failure = IllegalStateException("wrong password")
        val auth = FakeAuthRepository(calls, reauthFailure = failure)
        val remote = FakeRemote(calls)
        val local = FakeLocalStore(calls)

        val result = runCatching {
            coordinator(auth, remote, local).deleteAccount(
                ownerId = OWNER,
                password = "wrong-password",
                localData = LocalDataAfterAccountDeletion.Keep,
            )
        }

        assertSame(failure, result.exceptionOrNull())
        assertEquals(listOf("reauth"), calls)
        assertFalse(auth.accountDeleted)
        assertFalse(local.localCopyStaged)
    }

    @Test
    fun cloudFailureLeavesAuthAndLocalDataUntouchedForRetry() = runBlocking {
        val calls = mutableListOf<String>()
        val failure = IllegalStateException("cloud unavailable")
        val auth = FakeAuthRepository(calls)
        val remote = FakeRemote(calls, deleteFailure = failure)
        val local = FakeLocalStore(calls)

        val result = runCatching {
            coordinator(auth, remote, local).deleteAccount(
                ownerId = OWNER,
                password = "correct-password",
                localData = LocalDataAfterAccountDeletion.Keep,
            )
        }

        assertSame(failure, result.exceptionOrNull())
        assertEquals(listOf("reauth", "delete-cloud", "mark-pending"), calls)
        assertFalse(auth.accountDeleted)
        assertFalse(local.localCopyStaged)
        assertTrue(local.pendingForResync)
    }

    @Test
    fun cloudDeletionCancellationRequeuesCloudResync() = runBlocking {
        val calls = mutableListOf<String>()
        val cancellation = kotlinx.coroutines.CancellationException("cancelled")
        val auth = FakeAuthRepository(calls)
        val remote = FakeRemote(calls, deleteFailure = cancellation)
        val local = FakeLocalStore(calls)

        val result = runCatching {
            coordinator(auth, remote, local).deleteAccount(
                ownerId = OWNER,
                password = "correct-password",
                localData = LocalDataAfterAccountDeletion.Keep,
            )
        }

        assertSame(cancellation, result.exceptionOrNull())
        assertEquals(listOf("reauth", "delete-cloud", "mark-pending"), calls)
        assertFalse(auth.accountDeleted)
        assertTrue(local.pendingForResync)
    }

    private companion object {
        const val OWNER = "owner-id"
    }
}
