package io.github.litaog.dailyrecord.core.database

import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Shared record DAO contract. Each module DAO keeps its own table, SQL and
 * entity type (AGENTS: module tables stay independent) and only inherits this
 * shape so the Room-backed repository base can be shared.
 */
internal interface DailyCountRecordDao<RecordEntity : Any> {
    fun observeByDate(ownerId: String, localDate: LocalDate): Flow<RecordEntity?>

    suspend fun getByDate(ownerId: String, localDate: LocalDate): RecordEntity?

    fun observeForRange(
        ownerId: String,
        startDate: LocalDate,
        endExclusive: LocalDate,
    ): Flow<List<RecordEntity>>

    suspend fun upsert(record: RecordEntity)

    suspend fun markDeleted(
        ownerId: String,
        id: String,
        expectedUpdatedAt: Instant,
        updatedAt: Instant,
    ): Int

    suspend fun getPending(ownerId: String): List<RecordEntity>

    suspend fun getAllForSync(ownerId: String): List<RecordEntity>

    suspend fun setRemoteRevisionForUnbasedPending(
        ownerId: String,
        localDate: LocalDate,
        remoteId: String,
        remoteRevision: Long,
    ): Int

    suspend fun setRemoteRevisionForPending(
        ownerId: String,
        localDate: LocalDate,
        remoteRevision: Long,
    ): Int

    fun observePendingCount(ownerId: String): Flow<Int>

    suspend fun countPending(ownerId: String): Int

    suspend fun countForOwner(ownerId: String): Int

    suspend fun markOwnerPendingForResync(ownerId: String): Int

    suspend fun deleteOwnerCache(ownerId: String): Int

    suspend fun deleteByOwnerDate(ownerId: String, localDate: LocalDate): Int
}

/** Shared detail DAO contract; same independence rule as [DailyCountRecordDao]. */
internal interface DailyCountRecordDetailDao<DetailEntity : Any> {
    fun observeByDate(ownerId: String, localDate: LocalDate): Flow<List<DetailEntity>>

    suspend fun getByDate(ownerId: String, localDate: LocalDate): List<DetailEntity>

    suspend fun upsertAll(details: List<DetailEntity>)

    suspend fun deleteByOwnerDate(ownerId: String, localDate: LocalDate): Int

    suspend fun countForOwner(ownerId: String): Int

    suspend fun deleteOwnerCache(ownerId: String): Int
}
