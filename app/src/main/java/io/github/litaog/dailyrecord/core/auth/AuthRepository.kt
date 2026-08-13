package io.github.litaog.dailyrecord.core.auth

import kotlinx.coroutines.flow.Flow

data class AuthAccount(
    val uid: String,
    val email: String,
)

sealed interface AuthAccountPresence {
    data object Exists : AuthAccountPresence
    data object Absent : AuthAccountPresence
    /** The SDK has no current user; the resolver may need a later auth state. */
    data object SignedOut : AuthAccountPresence
    data class Unknown(val cause: Throwable) : AuthAccountPresence
}

sealed interface AuthDeletionResult {
    data object Completed : AuthDeletionResult
    /** The request was rejected before Auth could delete the account. */
    data class Failed(val cause: Throwable) : AuthDeletionResult
    /** The request outcome cannot be inferred from the client response. */
    data class Unknown(val cause: Throwable) : AuthDeletionResult
}

sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val account: AuthAccount) : AuthState
}

interface AuthRepository {
    val state: Flow<AuthState>

    suspend fun signIn(email: String, password: String): AuthAccount

    suspend fun register(email: String, password: String): AuthAccount

    suspend fun sendPasswordResetEmail(email: String)

    suspend fun reauthenticate(password: String)

    suspend fun deleteCurrentAccount(): AuthDeletionResult

    /**
     * Confirms whether an Auth deletion request left the account present.
     * A network or token error is deliberately returned as [AuthAccountPresence.Unknown].
     */
    suspend fun inspectAccountPresence(ownerId: String): AuthAccountPresence

    fun signOut()
}
