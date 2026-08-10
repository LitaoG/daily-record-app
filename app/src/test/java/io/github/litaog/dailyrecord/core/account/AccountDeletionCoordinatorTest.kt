package io.github.litaog.dailyrecord.core.account

import io.github.litaog.dailyrecord.core.auth.AuthAccount
import io.github.litaog.dailyrecord.core.auth.AuthRepository
import io.github.litaog.dailyrecord.core.auth.AuthState
import io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity
import io.github.litaog.dailyrecord.core.sync.HandBrewRemoteDataSource
import io.github.litaog.dailyrecord.core.sync.RemoteHandBrewRecord
import io.github.litaog.dailyrecord.core.sync.RemoteSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDeletionCoordinatorTest {
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
            listOf("reauth", "delete-cloud", "stage-local", "delete-auth", "delete-owner-cache"),
            calls,
        )
        assertTrue(auth.accountDeleted)
        assertTrue(local.localCopyStaged)
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
        assertEquals(listOf("reauth", "delete-cloud"), calls)
        assertFalse(auth.accountDeleted)
        assertFalse(local.localCopyStaged)
    }

    @Test
    fun authDeletionFailureRollsBackStagedLocalCopy() = runBlocking {
        val calls = mutableListOf<String>()
        val failure = IllegalStateException("auth delete failed")
        val auth = FakeAuthRepository(calls, deleteFailure = failure)
        val remote = FakeRemote(calls)
        val local = FakeLocalStore(calls)

        val result = runCatching {
            coordinator(auth, remote, local).deleteAccount(
                ownerId = OWNER,
                password = "correct-password",
                localData = LocalDataAfterAccountDeletion.Keep,
            )
        }

        assertSame(failure, result.exceptionOrNull())
        assertEquals(
            listOf("reauth", "delete-cloud", "stage-local", "delete-auth", "discard-local"),
            calls,
        )
        assertFalse(local.localCopyStaged)
        assertFalse(local.ownerCacheDeleted)
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

    private fun coordinator(
        auth: AuthRepository,
        remote: HandBrewRemoteDataSource,
        local: AccountDeletionLocalStore,
    ) = AccountDeletionCoordinator(auth, remote, local)

    private companion object {
        const val OWNER = "owner-id"
    }
}

private class FakeAuthRepository(
    private val calls: MutableList<String>,
    private val reauthFailure: Exception? = null,
    private val deleteFailure: Exception? = null,
) : AuthRepository {
    var accountDeleted = false

    override val state: Flow<AuthState> = emptyFlow()

    override suspend fun signIn(email: String, password: String) = AuthAccount("owner-id", email)

    override suspend fun register(email: String, password: String) = AuthAccount("owner-id", email)

    override suspend fun sendPasswordResetEmail(email: String) = Unit

    override suspend fun reauthenticate(password: String) {
        calls += "reauth"
        reauthFailure?.let { throw it }
    }

    override suspend fun deleteCurrentAccount() {
        calls += "delete-auth"
        deleteFailure?.let { throw it }
        accountDeleted = true
    }

    override fun signOut() = Unit
}

private class FakeRemote(
    private val calls: MutableList<String>,
    private val deleteFailure: Exception? = null,
) : HandBrewRemoteDataSource {
    override fun observe(ownerId: String): Flow<RemoteSnapshot> = emptyFlow()

    override suspend fun fetch(ownerId: String) = RemoteSnapshot(emptyList(), fromCache = false)

    override suspend fun commit(
        ownerId: String,
        local: HandBrewRecordEntity,
    ): RemoteHandBrewRecord = error("Not used")

    override suspend fun deleteAll(ownerId: String) {
        calls += "delete-cloud"
        deleteFailure?.let { throw it }
    }
}

private class FakeLocalStore(
    private val calls: MutableList<String>,
    private val deleteOwnerCacheFailure: Exception? = null,
    private val deleteOwnerCacheCancellation: kotlinx.coroutines.CancellationException? = null,
) : AccountDeletionLocalStore {
    var localCopyStaged = false
    var ownerCacheDeleted = false

    override suspend fun stageLocalRecoveryCopy(ownerId: String) {
        calls += "stage-local"
        localCopyStaged = true
    }

    override suspend fun discardLocalRecoveryCopy() {
        calls += "discard-local"
        localCopyStaged = false
    }

    override suspend fun deleteOwnerCache(ownerId: String) {
        calls += "delete-owner-cache"
        deleteOwnerCacheCancellation?.let { throw it }
        deleteOwnerCacheFailure?.let { throw it }
        ownerCacheDeleted = true
    }
}
