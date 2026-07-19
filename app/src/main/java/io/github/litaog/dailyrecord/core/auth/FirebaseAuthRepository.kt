package io.github.litaog.dailyrecord.core.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.github.litaog.dailyrecord.core.cloud.awaitResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

internal class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
) : AuthRepository {
    override val state: Flow<AuthState> = callbackFlow {
        fun publish(user: FirebaseUser?) {
            trySend(user?.toAccount()?.let(AuthState::SignedIn) ?: AuthState.SignedOut)
        }

        val listener = FirebaseAuth.AuthStateListener { publish(it.currentUser) }
        trySend(AuthState.Loading)
        auth.addAuthStateListener(listener)
        publish(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String): AuthAccount {
        val result = auth.signInWithEmailAndPassword(email.trim().lowercase(), password).awaitResult()
        return requireNotNull(result.user).toAccount()
    }

    override suspend fun register(email: String, password: String): AuthAccount {
        val result = auth.createUserWithEmailAndPassword(email.trim().lowercase(), password).awaitResult()
        return requireNotNull(result.user).toAccount()
    }

    override fun signOut() {
        auth.signOut()
    }
}

private fun FirebaseUser.toAccount(): AuthAccount = AuthAccount(
    uid = uid,
    email = email.orEmpty(),
)
