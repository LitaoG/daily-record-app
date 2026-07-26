package io.github.litaog.dailyrecord.core.account

import io.github.litaog.dailyrecord.core.auth.AuthRepository
import io.github.litaog.dailyrecord.core.sync.HandBrewRemoteDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

enum class LocalDataAfterAccountDeletion {
    Keep,
    Delete,
}

internal interface AccountDeletionLocalStore {
    suspend fun stageLocalRecoveryCopy(ownerId: String)

    suspend fun discardLocalRecoveryCopy()

    suspend fun deleteOwnerCache(ownerId: String)
}

internal class AccountDeletionCoordinator(
    private val authRepository: AuthRepository,
    private val remoteDataSource: HandBrewRemoteDataSource,
    private val localStore: AccountDeletionLocalStore,
) {
    suspend fun deleteAccount(
        ownerId: String,
        password: String,
        localData: LocalDataAfterAccountDeletion,
    ) {
        require(ownerId.isNotBlank()) { "ownerId must not be blank" }
        require(password.isNotBlank()) { "password must not be blank" }

        authRepository.reauthenticate(password)
        remoteDataSource.deleteAll(ownerId)

        val stagedLocalCopy = localData == LocalDataAfterAccountDeletion.Keep
        if (stagedLocalCopy) {
            localStore.stageLocalRecoveryCopy(ownerId)
        }
        try {
            authRepository.deleteCurrentAccount()
        } catch (error: CancellationException) {
            if (stagedLocalCopy) localStore.discardLocalRecoveryCopySafely(error)
            throw error
        } catch (error: Exception) {
            if (stagedLocalCopy) localStore.discardLocalRecoveryCopySafely(error)
            throw error
        }
        localStore.deleteOwnerCache(ownerId)
    }
}

private suspend fun AccountDeletionLocalStore.discardLocalRecoveryCopySafely(primary: Throwable) {
    try {
        withContext(NonCancellable) {
            discardLocalRecoveryCopy()
        }
    } catch (cleanupError: Exception) {
        primary.addSuppressed(cleanupError)
    }
}
