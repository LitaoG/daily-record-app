package io.github.litaog.dailyrecord.core.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.EmailAuthProvider
import io.github.litaog.dailyrecord.core.common.awaitResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.CancellationException
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

    override suspend fun sendPasswordResetEmail(email: String) {
        auth.useAppLanguage()
        try {
            auth.sendPasswordResetEmail(email.normalizedEmail()).awaitResult()
        } catch (error: FirebaseAuthException) {
            // A reset request must not reveal whether an account exists for the address.
            if (error.errorCode != "ERROR_USER_NOT_FOUND") throw error
        }
    }

    override suspend fun reauthenticate(password: String) {
        val user = requireNotNull(auth.currentUser) { "No signed-in account" }
        val email = requireNotNull(user.email) { "Signed-in account has no email" }
        user.reauthenticate(
            EmailAuthProvider.getCredential(email.normalizedEmail(), password),
        ).awaitResult()
    }

    override suspend fun deleteCurrentAccount(): AuthDeletionResult {
        val user = auth.currentUser
            ?: return AuthDeletionResult.Unknown(IllegalStateException("No signed-in account"))
        return try {
            user.delete().awaitResult()
            AuthDeletionResult.Completed
        } catch (error: CancellationException) {
            // The Firebase Task may still complete after the coroutine is
            // cancelled. The coordinator records this as an unknown outcome.
            throw error
        } catch (error: Exception) {
            AuthDeletionResult.Unknown(error)
        }
    }

    override suspend fun inspectAccountPresence(ownerId: String): AuthAccountPresence {
        val user = auth.currentUser ?: return AuthAccountPresence.SignedOut
        if (user.uid != ownerId) {
            return AuthAccountPresence.Unknown(
                IllegalStateException("The signed-in account does not match the pending owner"),
            )
        }
        return try {
            user.reload().awaitResult()
            if (auth.currentUser?.uid == ownerId) {
                AuthAccountPresence.Exists
            } else {
                AuthAccountPresence.Absent
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: FirebaseAuthException) {
            if (error.errorCode == "ERROR_USER_NOT_FOUND") {
                AuthAccountPresence.Absent
            } else {
                AuthAccountPresence.Unknown(error)
            }
        } catch (error: Exception) {
            AuthAccountPresence.Unknown(error)
        }
    }

    override fun signOut() {
        auth.signOut()
    }
}

private fun String.normalizedEmail(): String = trim().lowercase()

private fun FirebaseUser.toAccount(): AuthAccount = AuthAccount(
    uid = uid,
    email = email.orEmpty(),
)
