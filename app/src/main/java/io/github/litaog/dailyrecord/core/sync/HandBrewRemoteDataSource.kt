package io.github.litaog.dailyrecord.core.sync

import io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity
import kotlinx.coroutines.flow.Flow

internal interface HandBrewRemoteDataSource :
    DailyCountRemoteDataSource<HandBrewRecordEntity, RemoteHandBrewRecord> {
    override fun observe(ownerId: String): Flow<RemoteSnapshot>
    override suspend fun fetch(ownerId: String): RemoteSnapshot
    override fun recordsFrom(snapshot: RemoteSnapshot): List<RemoteHandBrewRecord> =
        snapshot.records.filterIsInstance<RemoteHandBrewRecord>()
    override suspend fun commit(
        ownerId: String,
        local: HandBrewRecordEntity,
    ): RemoteHandBrewRecord

    override fun matches(
        remote: RemoteHandBrewRecord,
        local: HandBrewRecordEntity,
    ): Boolean =
        remote.localDate == local.localDate &&
            remote.brewCount == local.brewCount &&
            remote.clientUpdatedAt == local.updatedAt &&
            remote.deleted == local.isDeleted

    override suspend fun deleteAll(ownerId: String)
}
