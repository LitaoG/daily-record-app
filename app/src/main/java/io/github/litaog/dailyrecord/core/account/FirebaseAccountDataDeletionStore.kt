package io.github.litaog.dailyrecord.core.account

import com.google.firebase.functions.FirebaseFunctions
import io.github.litaog.dailyrecord.core.common.awaitResult

/**
 * Account-wide cloud deletion is intentionally callable-only. The client
 * cannot obtain a Firestore delete capability for record documents; the
 * trusted function checks the signed-in UID and recent reauthentication before
 * deleting both module collections.
 */
internal class FirebaseAccountDataDeletionStore(
    private val functions: FirebaseFunctions,
) : AccountRemoteDataStore {
    override suspend fun deleteAll(ownerId: String) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        functions.getHttpsCallable(DELETE_ACCOUNT_DATA_FUNCTION)
            .call(mapOf("ownerId" to ownerId))
            .awaitResult()
    }

    private companion object {
        const val DELETE_ACCOUNT_DATA_FUNCTION = "deleteAccountData"
    }
}
