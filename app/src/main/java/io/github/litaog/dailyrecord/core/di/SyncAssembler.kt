package io.github.litaog.dailyrecord.core.di

import androidx.room.withTransaction
import io.github.litaog.dailyrecord.core.account.CombinedAccountDeletionLocalStore
import io.github.litaog.dailyrecord.core.account.CombinedAccountRemoteDataStore
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.sync.CombinedSyncCoordinator
import io.github.litaog.dailyrecord.core.sync.RoomHandBrewSyncStore
import io.github.litaog.dailyrecord.core.sync.RoomSexSyncStore
import io.github.litaog.dailyrecord.core.sync.moduleSyncCoordinator

/**
 * Single composition point for the isolated daily-count modules.
 *
 * The worker, the app root and the account components all assemble through
 * these functions, so adding a third module changes exactly one place per
 * component instead of every call site.
 */
internal fun buildCombinedSyncCoordinator(
    database: DailyRecordDatabase,
    services: FirebaseServices,
): CombinedSyncCoordinator = CombinedSyncCoordinator(
    listOf(
        moduleSyncCoordinator(RoomHandBrewSyncStore(database), services.remoteDataSource),
        moduleSyncCoordinator(RoomSexSyncStore(database), services.sexRemoteDataSource),
    ),
)

internal fun buildCombinedAccountDeletionLocalStore(
    database: DailyRecordDatabase,
): CombinedAccountDeletionLocalStore = CombinedAccountDeletionLocalStore(
    listOf(
        RoomHandBrewSyncStore(database),
        RoomSexSyncStore(database),
    ),
    transactionRunner = { operation -> database.withTransaction { operation() } },
)

internal fun buildCombinedAccountRemoteDataStore(
    services: FirebaseServices,
): CombinedAccountRemoteDataStore = CombinedAccountRemoteDataStore(
    listOf(
        services.remoteDataSource,
        services.sexRemoteDataSource,
    ),
)
