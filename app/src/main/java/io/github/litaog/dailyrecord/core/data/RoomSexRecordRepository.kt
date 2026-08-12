package io.github.litaog.dailyrecord.core.data

import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.core.database.SexRecordDetailEntity
import io.github.litaog.dailyrecord.core.database.SexRecordEntity
import io.github.litaog.dailyrecord.core.database.asEntity
import io.github.litaog.dailyrecord.core.database.asExternalModel
import io.github.litaog.dailyrecord.core.model.SexRecord
import io.github.litaog.dailyrecord.core.model.SexRecordDetail
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

internal class RoomSexRecordRepository(
    database: DailyRecordDatabase,
    ownerId: String = LOCAL_OWNER_ID,
    clock: Clock = Clock.systemUTC(),
    onLocalChange: () -> Unit = {},
) : RoomDailyCountRecordRepository<
        SexRecord,
        SexRecordDetail,
        SexRecordEntity,
        SexRecordDetailEntity,
    >(
        database = database,
        recordDao = database.sexRecordDao(),
        detailDao = database.sexRecordDetailDao(),
        ownerId = ownerId,
        clock = clock,
        onLocalChange = onLocalChange,
    ),
    SexRecordRepository {
    override val recordLabel: String = "Sex"

    override fun observeDetails(localDate: LocalDate): Flow<List<SexRecordDetail>> =
        super<RoomDailyCountRecordRepository>.observeDetails(localDate)

    override suspend fun saveRecord(
        record: SexRecord,
        details: List<SexRecordDetail>,
    ): SexRecord = super<RoomDailyCountRecordRepository>.saveRecord(record, details)

    override fun SexRecordEntity.toModel(): SexRecord = asExternalModel()

    override fun SexRecord.toRecordEntity(
        ownerId: String,
        remoteRevision: Long,
    ): SexRecordEntity = asEntity(ownerId = ownerId, remoteRevision = remoteRevision)

    override fun SexRecordDetailEntity.toDetailModel(): SexRecordDetail = asExternalModel()

    override fun SexRecordDetail.toDetailEntity(
        ownerId: String,
        createdAt: Instant,
        updatedAt: Instant,
    ): SexRecordDetailEntity = asEntity(
        ownerId = ownerId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    override fun SexRecordDetail.idOf(): String = id

    override fun SexRecordDetail.localDateOf(): LocalDate = localDate

    override fun SexRecordDetail.occurrenceIndexOf(): Int = occurrenceIndex

    override fun SexRecordDetail.createdAtOf(): Instant = createdAt

    override fun SexRecordDetail.updatedAtOf(): Instant = updatedAt

    override fun SexRecordDetail.withIdentity(
        id: String,
        createdAt: Instant,
        updatedAt: Instant,
    ): SexRecordDetail = copy(id = id, createdAt = createdAt, updatedAt = updatedAt)

    override fun SexRecord.withRecordIdentity(
        id: String,
        createdAt: Instant,
        updatedAt: Instant,
    ): SexRecord = copy(id = id, createdAt = createdAt, updatedAt = updatedAt)

    override fun SexRecordEntity.isDeletedOf(): Boolean = isDeleted

    override fun SexRecordEntity.remoteRevisionOf(): Long = remoteRevision
}
