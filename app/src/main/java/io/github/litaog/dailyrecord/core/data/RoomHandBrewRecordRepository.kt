package io.github.litaog.dailyrecord.core.data

import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.HandBrewRecordDetailEntity
import io.github.litaog.dailyrecord.core.database.HandBrewRecordEntity
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.core.database.asEntity
import io.github.litaog.dailyrecord.core.database.asExternalModel
import io.github.litaog.dailyrecord.core.model.HandBrewRecord
import io.github.litaog.dailyrecord.core.model.HandBrewRecordDetail
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

internal class RoomHandBrewRecordRepository(
    database: DailyRecordDatabase,
    ownerId: String = LOCAL_OWNER_ID,
    clock: Clock = Clock.systemUTC(),
    onLocalChange: () -> Unit = {},
) : RoomDailyCountRecordRepository<
        HandBrewRecord,
        HandBrewRecordDetail,
        HandBrewRecordEntity,
        HandBrewRecordDetailEntity,
    >(
        database = database,
        recordDao = database.handBrewRecordDao(),
        detailDao = database.handBrewRecordDetailDao(),
        ownerId = ownerId,
        clock = clock,
        onLocalChange = onLocalChange,
    ),
    HandBrewRecordRepository {
    override val recordLabel: String = "Hand-brew"

    override fun observeDetails(localDate: LocalDate): Flow<List<HandBrewRecordDetail>> =
        super<RoomDailyCountRecordRepository>.observeDetails(localDate)

    override suspend fun saveRecord(
        record: HandBrewRecord,
        details: List<HandBrewRecordDetail>,
    ): HandBrewRecord = super<RoomDailyCountRecordRepository>.saveRecord(record, details)

    override fun HandBrewRecordEntity.toModel(): HandBrewRecord = asExternalModel()

    override fun HandBrewRecord.toRecordEntity(
        ownerId: String,
        remoteRevision: Long,
    ): HandBrewRecordEntity = asEntity(ownerId = ownerId, remoteRevision = remoteRevision)

    override fun HandBrewRecordDetailEntity.toDetailModel(): HandBrewRecordDetail = asExternalModel()

    override fun HandBrewRecordDetail.toDetailEntity(
        ownerId: String,
        createdAt: Instant,
        updatedAt: Instant,
    ): HandBrewRecordDetailEntity = asEntity(
        ownerId = ownerId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    override fun HandBrewRecordDetail.idOf(): String = id

    override fun HandBrewRecordDetail.localDateOf(): LocalDate = localDate

    override fun HandBrewRecordDetail.occurrenceIndexOf(): Int = occurrenceIndex

    override fun HandBrewRecordDetail.createdAtOf(): Instant = createdAt

    override fun HandBrewRecordDetail.updatedAtOf(): Instant = updatedAt

    override fun HandBrewRecordDetail.withIdentity(
        id: String,
        createdAt: Instant,
        updatedAt: Instant,
    ): HandBrewRecordDetail = copy(id = id, createdAt = createdAt, updatedAt = updatedAt)

    override fun HandBrewRecord.withRecordIdentity(
        id: String,
        createdAt: Instant,
        updatedAt: Instant,
    ): HandBrewRecord = copy(id = id, createdAt = createdAt, updatedAt = updatedAt)

    override fun HandBrewRecordEntity.isDeletedOf(): Boolean = isDeleted

    override fun HandBrewRecordEntity.remoteRevisionOf(): Long = remoteRevision
}
