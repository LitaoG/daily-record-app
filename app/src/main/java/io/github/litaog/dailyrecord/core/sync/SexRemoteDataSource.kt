package io.github.litaog.dailyrecord.core.sync

import io.github.litaog.dailyrecord.core.database.SexRecordEntity
import kotlinx.coroutines.flow.Flow

internal interface SexRemoteDataSource :
    DailyCountRemoteDataSource<SexRecordEntity, RemoteSexRecord> {
    override fun observe(ownerId: String): Flow<RemoteSnapshot>
    override suspend fun fetch(ownerId: String): RemoteSnapshot
    override fun recordsFrom(snapshot: RemoteSnapshot): List<RemoteSexRecord> =
        snapshot.records.filterIsInstance<RemoteSexRecord>()
    override suspend fun commit(ownerId: String, local: SexRecordEntity): RemoteSexRecord

    override fun matches(remote: RemoteSexRecord, local: SexRecordEntity): Boolean =
        remote.localDate == local.localDate &&
            remote.sexCount == local.sexCount &&
            remote.clientUpdatedAt == local.updatedAt &&
            remote.deleted == local.isDeleted

}
