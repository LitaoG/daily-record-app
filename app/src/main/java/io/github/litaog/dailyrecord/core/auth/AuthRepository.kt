package io.github.litaog.dailyrecord.core.auth

import kotlinx.coroutines.flow.Flow

data class AuthAccount(
    val uid: String,
    val email: String,
)

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

    fun signOut()
}
