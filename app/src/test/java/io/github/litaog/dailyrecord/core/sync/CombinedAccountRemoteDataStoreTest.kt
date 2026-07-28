package io.github.litaog.dailyrecord.core.sync

import io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity
import io.github.litaog.dailyrecord.core.database.SexRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CombinedAccountRemoteDataStoreTest {
    @Test
    fun permanentAccountDeletionCoversBothCloudCollections() = runBlocking {
        val calls = mutableListOf<String>()
        val store = CombinedAccountRemoteDataStore(
            handBrew = DeleteOnlyHandBrewRemote(calls),
            sex = DeleteOnlySexRemote(calls),
        )

        store.deleteAll("owner")

        assertEquals(listOf("hand-brew", "sex"), calls)
    }
}

private class DeleteOnlyHandBrewRemote(
    private val calls: MutableList<String>,
) : HandBrewRemoteDataSource {
    override fun observe(ownerId: String): Flow<RemoteSnapshot> = emptyFlow()
    override suspend fun fetch(ownerId: String) = RemoteSnapshot(fromCache = false)
    override suspend fun commit(
        ownerId: String,
        local: HandBrewRecordEntity,
    ): RemoteHandBrewRecord = error("Not used")

    override suspend fun deleteAll(ownerId: String) {
        calls += "hand-brew"
    }
}

private class DeleteOnlySexRemote(
    private val calls: MutableList<String>,
) : SexRemoteDataSource {
    override fun observe(ownerId: String): Flow<RemoteSnapshot> = emptyFlow()
    override suspend fun fetch(ownerId: String) = RemoteSnapshot(fromCache = false)
    override suspend fun commit(
        ownerId: String,
        local: SexRecordEntity,
    ): RemoteSexRecord = error("Not used")

    override suspend fun deleteAll(ownerId: String) {
        calls += "sex"
    }
}
