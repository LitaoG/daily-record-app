package io.github.litaog.dailyrecord.core.sync

import kotlinx.coroutines.CancellationException

internal interface AccountRemoteDataStore {
    suspend fun deleteAll(ownerId: String)
}

internal class CombinedAccountRemoteDataStore(
    private val stores: List<AccountRemoteDataStore>,
) : AccountRemoteDataStore {
    constructor(
        handBrew: HandBrewRemoteDataSource,
        sex: SexRemoteDataSource,
    ) : this(listOf(handBrew, sex))

    init {
        require(stores.isNotEmpty()) { "At least one remote account store is required." }
    }

    override suspend fun deleteAll(ownerId: String) {
        var primary: Exception? = null
        stores.forEach { store ->
            try {
                store.deleteAll(ownerId)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (primary == null) primary = error else primary?.addSuppressed(error)
            }
        }
        primary?.let { throw it }
    }
}
