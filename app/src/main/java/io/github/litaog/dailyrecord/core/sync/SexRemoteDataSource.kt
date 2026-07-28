package io.github.litaog.dailyrecord.core.sync

import io.github.litaog.dailyrecord.core.database.SexRecordEntity
import kotlinx.coroutines.flow.Flow

internal interface SexRemoteDataSource : AccountRemoteDataStore {
    fun observe(ownerId: String): Flow<RemoteSnapshot>

    suspend fun fetch(ownerId: String): RemoteSnapshot

    suspend fun commit(ownerId: String, local: SexRecordEntity): RemoteSexRecord

    override suspend fun deleteAll(ownerId: String)
}
