package io.github.litaog.dailyrecord.core.sync

import io.github.litaog.dailyrecord.core.account.CombinedAccountDeletionLocalStore
import io.github.litaog.dailyrecord.core.account.CombinedAccountRemoteDataStore
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.di.FirebaseServices

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
)

internal fun buildCombinedAccountRemoteDataStore(
    services: FirebaseServices,
): CombinedAccountRemoteDataStore = CombinedAccountRemoteDataStore(
    listOf(
        services.remoteDataSource,
        services.sexRemoteDataSource,
    ),
)
