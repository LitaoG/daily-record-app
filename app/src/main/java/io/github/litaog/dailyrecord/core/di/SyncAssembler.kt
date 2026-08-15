package io.github.litaog.dailyrecord.core.di

import androidx.room.withTransaction
import io.github.litaog.dailyrecord.core.account.AccountDeletionCoordinator
import io.github.litaog.dailyrecord.core.sync.AccountDeletionRecoveryCoordinator
import io.github.litaog.dailyrecord.core.account.CombinedAccountDeletionLocalStore
import io.github.litaog.dailyrecord.core.account.CombinedAccountRemoteDataStore
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.sync.CombinedSyncCoordinator
import io.github.litaog.dailyrecord.core.sync.DeletionBarrier
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

internal fun buildAccountDeletionRecoveryCoordinator(
    database: DailyRecordDatabase,
): AccountDeletionRecoveryCoordinator = AccountDeletionRecoveryCoordinator(
    localStore = buildCombinedAccountDeletionLocalStore(database),
)

internal fun buildAccountDeletionCoordinator(
    database: DailyRecordDatabase,
    services: FirebaseServices,
): AccountDeletionCoordinator = AccountDeletionCoordinator(
    authRepository = services.authRepository,
    remoteDataSource = buildCombinedAccountRemoteDataStore(services),
    localStore = buildCombinedAccountDeletionLocalStore(database),
    markCloudDeletionComplete = DeletionBarrier::markCloudDeletionComplete,
    markAuthDeletionStarted = DeletionBarrier::markAuthDeletionStarted,
    markLocalRecoveryCopyPending = DeletionBarrier::markLocalRecoveryCopyPending,
    markLocalRecoveryCopyReady = DeletionBarrier::markLocalRecoveryCopyReady,
    markAuthDeletionComplete = DeletionBarrier::markAuthDeletionComplete,
)

internal fun buildCombinedAccountRemoteDataStore(
    services: FirebaseServices,
): CombinedAccountRemoteDataStore = CombinedAccountRemoteDataStore(
    listOf(
        services.accountDataDeletionStore,
    ),
)
