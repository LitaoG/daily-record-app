package io.github.litaog.dailyrecord.core.sync

internal interface AccountRemoteDataStore {
    suspend fun deleteAll(ownerId: String)
}

internal class CombinedAccountRemoteDataStore(
    private val handBrew: HandBrewRemoteDataSource,
    private val sex: SexRemoteDataSource,
) : AccountRemoteDataStore {
    override suspend fun deleteAll(ownerId: String) {
        handBrew.deleteAll(ownerId)
        sex.deleteAll(ownerId)
    }
}
