package io.github.litaog.dailyrecord.core.account

import io.github.litaog.dailyrecord.core.sync.RoomHandBrewSyncStore
import io.github.litaog.dailyrecord.core.sync.RoomSexSyncStore
import kotlinx.coroutines.CancellationException

internal class CombinedAccountDeletionLocalStore(
    private val handBrew: RoomHandBrewSyncStore,
    private val sex: RoomSexSyncStore,
) : AccountDeletionLocalStore {
    override suspend fun stageLocalRecoveryCopy(ownerId: String) {
        try {
            handBrew.stageLocalRecoveryCopy(ownerId)
            sex.stageLocalRecoveryCopy(ownerId)
        } catch (error: CancellationException) {
            discardBothSafely(error)
            throw error
        } catch (error: Exception) {
            discardBothSafely(error)
            throw error
        }
    }

    override suspend fun discardLocalRecoveryCopy() {
        handBrew.discardLocalRecoveryCopy()
        sex.discardLocalRecoveryCopy()
    }

    override suspend fun deleteOwnerCache(ownerId: String) {
        handBrew.deleteOwnerCache(ownerId)
        sex.deleteOwnerCache(ownerId)
    }

    private suspend fun discardBothSafely(primary: Throwable) {
        runCatching { handBrew.discardLocalRecoveryCopy() }
            .exceptionOrNull()
            ?.let(primary::addSuppressed)
        runCatching { sex.discardLocalRecoveryCopy() }
            .exceptionOrNull()
            ?.let(primary::addSuppressed)
    }
}
