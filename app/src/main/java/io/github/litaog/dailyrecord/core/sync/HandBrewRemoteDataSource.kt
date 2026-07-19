package io.github.litaog.dailyrecord.core.sync

import io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity
import kotlinx.coroutines.flow.Flow

internal interface HandBrewRemoteDataSource {
    fun observe(ownerId: String): Flow<RemoteSnapshot>

    suspend fun fetch(ownerId: String): RemoteSnapshot

    suspend fun commit(ownerId: String, local: HandBrewRecordEntity): RemoteHandBrewRecord
}
