package io.github.litaog.dailyrecord.core.account

import io.github.litaog.dailyrecord.core.auth.AuthAccount
import io.github.litaog.dailyrecord.core.auth.AuthAccountPresence
import io.github.litaog.dailyrecord.core.auth.AuthDeletionResult
import io.github.litaog.dailyrecord.core.auth.AuthRepository
import io.github.litaog.dailyrecord.core.auth.AuthState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

internal fun coordinator(
        auth: AuthRepository,
        remote: AccountRemoteDataStore,
        local: AccountDeletionLocalStore,
        markCloudDeletionComplete: (String) -> Unit = {},
        markAuthDeletionStarted: (String) -> Unit = {},
        markLocalRecoveryCopyPending: (String) -> Unit = {},
        markLocalRecoveryCopyReady: (String) -> Unit = {},
        markAuthDeletionComplete: (String) -> Unit = {},
    ) = AccountDeletionCoordinator(
        auth,
        remote,
        local,
        markCloudDeletionComplete,
        markAuthDeletionStarted,
        markLocalRecoveryCopyPending,
        markLocalRecoveryCopyReady,
        markAuthDeletionComplete,
    )

internal const val OWNER = "owner-id"

internal class FakeAuthRepository(
    private val calls: MutableList<String>,
    private val reauthFailure: Exception? = null,
    private val deleteFailure: Exception? = null,
    private val definitiveDeleteFailure: Boolean = false,
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

    override suspend fun deleteCurrentAccount(): AuthDeletionResult {
        calls += "delete-auth"
        deleteFailure?.let {
            if (it is kotlinx.coroutines.CancellationException) throw it
            return if (definitiveDeleteFailure) {
                AuthDeletionResult.Failed(it)
            } else {
                AuthDeletionResult.Unknown(it)
            }
        }
        accountDeleted = true
        return AuthDeletionResult.Completed
    }

    override suspend fun inspectAccountPresence(ownerId: String): AuthAccountPresence =
        AuthAccountPresence.Exists

    override fun signOut() = Unit
}

internal class FakeRemote(
    private val calls: MutableList<String>,
    private val deleteFailure: Exception? = null,
) : AccountRemoteDataStore {
    override suspend fun deleteAll(ownerId: String) {
        calls += "delete-cloud"
        deleteFailure?.let { throw it }
    }
}

internal class FakeLocalStore(
    private val calls: MutableList<String>,
    private val deleteOwnerCacheFailure: Exception? = null,
    private val deleteOwnerCacheCancellation: kotlinx.coroutines.CancellationException? = null,
    private val discardLocalRecoveryCopyFailure: Exception? = null,
) : AccountDeletionLocalStore {
    var localCopyStaged = false
    var localCopyPromoted = false
    var ownerCacheDeleted = false
    var pendingForResync = false

    override suspend fun stageLocalRecoveryCopy(ownerId: String) {
        calls += "stage-local"
        localCopyStaged = true
    }

    override suspend fun discardLocalRecoveryCopy(ownerId: String) {
        calls += "discard-local"
        discardLocalRecoveryCopyFailure?.let { throw it }
        localCopyStaged = false
    }

    override suspend fun promoteLocalRecoveryCopy(ownerId: String) {
        calls += "promote-local"
        localCopyPromoted = true
        localCopyStaged = false
    }

    override suspend fun hasLocalRecoveryConflict(ownerId: String): Boolean = false


    override suspend fun deleteOwnerCache(ownerId: String) {
        calls += "delete-owner-cache"
        deleteOwnerCacheCancellation?.let { throw it }
        deleteOwnerCacheFailure?.let { throw it }
        ownerCacheDeleted = true
    }

    override suspend fun markOwnerPendingForResync(ownerId: String) {
        calls += "mark-pending"
        pendingForResync = true
    }
}
