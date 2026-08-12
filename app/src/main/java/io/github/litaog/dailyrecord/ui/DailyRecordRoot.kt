package io.github.litaog.dailyrecord.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.litaog.dailyrecord.core.auth.AuthAccountPresence
import io.github.litaog.dailyrecord.core.auth.AuthState
import io.github.litaog.dailyrecord.core.account.AccountDeletionCoordinator
import io.github.litaog.dailyrecord.core.account.LocalDataAfterAccountDeletion
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.di.FirebaseServices
import io.github.litaog.dailyrecord.core.common.runInteractiveCloudOperation
import io.github.litaog.dailyrecord.core.common.runCatchingPreservingCancellation
import io.github.litaog.dailyrecord.core.data.RoomHandBrewRecordRepository
import io.github.litaog.dailyrecord.core.data.RoomSexRecordRepository
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.sync.AccountSyncManager
import io.github.litaog.dailyrecord.core.sync.AndroidNetworkMonitor
import io.github.litaog.dailyrecord.core.sync.buildCombinedAccountDeletionLocalStore
import io.github.litaog.dailyrecord.core.sync.buildCombinedAccountRemoteDataStore
import io.github.litaog.dailyrecord.core.sync.buildCombinedSyncCoordinator
import io.github.litaog.dailyrecord.core.sync.DailyRecordSyncScheduler
import io.github.litaog.dailyrecord.core.sync.DeletionBarrier
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.ui.auth.AuthScreen
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.theme.DailyRecordCanvas
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DELETION_RECOVERY_RETRY_DELAY_MILLIS = 5_000L

@Composable
internal fun DailyRecordRoot(
    database: DailyRecordDatabase,
    servicesProvider: () -> FirebaseServices,
) {
    val context = LocalContext.current
    val rootScope = rememberCoroutineScope()
    val localModePreference = remember(context) { LocalModePreference(context) }
    val pendingCleanupPreference = remember(context) { PendingLocalCleanupPreference(context) }
    var deletionRecoveryRevision by remember(database) { mutableIntStateOf(0) }
    var recoveryRestartRevision by remember(database) { mutableIntStateOf(0) }
    var continueOffline by rememberSaveable {
        mutableStateOf(localModePreference.isEnabled)
    }
    var authOpenedFromLocal by rememberSaveable { mutableStateOf(false) }
    // A ready copy is safe to use after definitive Auth completion; a ready
    // copy from an earlier pre-Auth crash is still unresolved and must keep the
    // global local namespace read-only until startup requeues or discards it.
    val localRecoveryReadyOwners = DeletionBarrier.localRecoveryCopyReadyOwnerIds()
    val confirmedCleanupOwners = DeletionBarrier.pendingCleanupOwnerIds()
    val localRecoveryBlocked =
        DeletionBarrier.authDeletionStartedOwnerIds().isNotEmpty() ||
            DeletionBarrier.localRecoveryCopyPendingOwnerIds().isNotEmpty() ||
            (localRecoveryReadyOwners - confirmedCleanupOwners).isNotEmpty()
    if (continueOffline) {
        // Keep Firebase lazy in persistent local mode. Pre-Auth recovery only
        // touches Room and the deletion journal; an unresolved Auth request
        // is intentionally revisited after the user enters the cloud path.
        LaunchedEffect(database) {
            while (true) {
                retryPendingOwnerCleanup(database, pendingCleanupPreference)
                resolvePreAuthDeletionMarkers(database)
                deletionRecoveryRevision += 1
                if (!hasPreAuthDeletionRecovery()) break
                delay(DELETION_RECOVERY_RETRY_DELAY_MILLIS)
            }
        }
        if (localRecoveryBlocked) {
            DeletionRecoveryRoot(
                onSignIn = {
                    authOpenedFromLocal = true
                    continueOffline = false
                },
            )
        } else {
            LocalRoot(
                database = database,
                onSignIn = {
                    authOpenedFromLocal = true
                    continueOffline = false
                },
            )
        }
        return
    }

    // Only reached when the user is not in persistent local mode. The provider
    // is remembered with the stable app instance by the caller, so calling it
    // here never recreates FirebaseServices; this read is what initializes
    // Firebase for the first time (local-only startup never reaches it).
    val services = servicesProvider()
    val authState by services.authRepository.state.collectAsState(initial = AuthState.Loading)
    val recoveryResolved = remember(database) { mutableStateOf(false) }
    LaunchedEffect(database, authState) {
        if (authState !is AuthState.Loading) {
            do {
                // A previous account deletion may have left an owner cache
                // that could not be cleared. Resolve pre-Auth markers and
                // retry the cloud-confirmed local cleanup before exposing
                // this account. A temporary network failure must not leave
                // the process waiting forever for an auth-state emission.
                retryPendingOwnerCleanup(database, pendingCleanupPreference)
                resolvePendingAuthDeletions(database, servicesProvider, pendingCleanupPreference)
                recoveryResolved.value = true
                deletionRecoveryRevision += 1
                recoveryRestartRevision += 1
                if (authState !is AuthState.SignedIn || !hasDeletionRecoveryPending()) break
                delay(DELETION_RECOVERY_RETRY_DELAY_MILLIS)
            } while (true)
        }
    }

    when (val state = authState) {
        AuthState.Loading -> LoadingRoot()
        AuthState.SignedOut -> {
            AuthScreen(
                productionConfigured = services.productionConfigured,
                onSignIn = { email, password ->
                    runInteractiveCloudOperation {
                        services.authRepository.signIn(email, password)
                    }.map { Unit }
                },
                onRegister = { email, password ->
                    runInteractiveCloudOperation {
                        services.authRepository.register(email, password)
                    }.map { Unit }
                },
                onPasswordReset = { email ->
                    runInteractiveCloudOperation {
                        services.authRepository.sendPasswordResetEmail(email)
                    }.map { Unit }
                },
                onBack = if (authOpenedFromLocal) {
                    {
                        localModePreference.setEnabled(true)
                        authOpenedFromLocal = false
                        continueOffline = true
                    }
                } else {
                    null
                },
                onContinueOffline = {
                    localModePreference.setEnabled(true)
                    authOpenedFromLocal = false
                    continueOffline = true
                },
            )
        }
        is AuthState.SignedIn -> {
            LaunchedEffect(state.account.uid) {
                localModePreference.setEnabled(false)
                authOpenedFromLocal = false
            }
            SignedInRoot(
                database = database,
                services = services,
                state = state,
                accountDeletionScope = rootScope,
                pendingCleanupPreference = pendingCleanupPreference,
                deletionRecoveryRevision = deletionRecoveryRevision,
                recoveryResolved = recoveryResolved.value,
                recoveryRestartRevision = recoveryRestartRevision,
                onAccountDeletedWithLocalRecords = {
                    localModePreference.setEnabled(true)
                    continueOffline = true
                },
            )
        }
    }
}

private suspend fun retryPendingOwnerCleanup(
    database: DailyRecordDatabase,
    pendingCleanupPreference: PendingLocalCleanupPreference,
) {
    val cleanupStore = buildCombinedAccountDeletionLocalStore(database)
    val durablePendingOwners = DeletionBarrier.pendingCleanupOwnerIds()
    (pendingCleanupPreference.ownerIds + durablePendingOwners).forEach { pendingOwnerId ->
        runCatchingPreservingCancellation {
            cleanupStore.deleteOwnerCache(pendingOwnerId)
        }.onSuccess {
            pendingCleanupPreference.remove(pendingOwnerId)
            // Keep startup resilient if the durable marker cannot be
            // committed; the next launch will retry from the marker.
            runCatching {
                DeletionBarrier.completeDeletionCleanup(pendingOwnerId)
            }.onFailure {
                pendingCleanupPreference.add(pendingOwnerId)
            }
        }
    }
}

@Composable
private fun LocalRoot(database: DailyRecordDatabase, onSignIn: () -> Unit) {
    val handBrewRepository = remember(database) {
        RoomHandBrewRecordRepository(database = database, ownerId = LOCAL_OWNER_ID)
    }
    val sexRepository = remember(database) {
        RoomSexRecordRepository(database = database, ownerId = LOCAL_OWNER_ID)
    }
    DailyRecordApp(
        repository = handBrewRepository,
        sexRepository = sexRepository,
        onSignIn = onSignIn,
    )
}

@Composable
private fun SignedInRoot(
    database: DailyRecordDatabase,
    services: FirebaseServices,
    state: AuthState.SignedIn,
    accountDeletionScope: kotlinx.coroutines.CoroutineScope,
    pendingCleanupPreference: PendingLocalCleanupPreference,
    deletionRecoveryRevision: Int,
    recoveryResolved: Boolean,
    recoveryRestartRevision: Int,
    onAccountDeletedWithLocalRecords: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val ownerId = state.account.uid
    var deletionInProgress by remember(ownerId) { mutableStateOf(false) }
    var deletionAttempted by remember(ownerId) { mutableStateOf(false) }
    var deletionRecoveryRefresh by remember(ownerId) { mutableIntStateOf(0) }
    var syncRestartRevision by remember(ownerId) { mutableIntStateOf(0) }
    val deletionBarrierBlocked = remember(
        ownerId,
        deletionRecoveryRevision,
        deletionRecoveryRefresh,
        deletionInProgress,
    ) {
        DeletionBarrier.isDeletionBlocked(ownerId)
    }
    // Keep the account surface mounted for the deletion attempt itself so an
    // Auth-unknown result can reach AccountDeletionDialog. On a fresh process
    // (where deletionAttempted is false), unresolved durable markers still
    // gate the account before any sync or local-account preparation starts.
    val deletionRecoveryPending = !recoveryResolved ||
        (deletionBarrierBlocked && !deletionAttempted)

    LaunchedEffect(ownerId, deletionInProgress, deletionAttempted) {
        if (!deletionInProgress && deletionAttempted) {
            runCatchingPreservingCancellation {
                resolvePendingAuthDeletions(
                    database = database,
                    servicesProvider = { services },
                    pendingCleanupPreference = pendingCleanupPreference,
                )
            }
            deletionRecoveryRefresh += 1
            // The resolver may have cleared an Auth-pending marker. This is a
            // separate restart signal from the deletion callback: the latter
            // deliberately restarts nothing while the durable barrier remains.
            syncRestartRevision += 1
        }
    }

    if (deletionRecoveryPending) {
        LoadingRoot()
        return
    }

    val networkMonitor = remember(ownerId, context) { AndroidNetworkMonitor(context) }
    val coordinator = remember(ownerId, database, services) {
        buildCombinedSyncCoordinator(database, services)
    }
    var accountPrepared by remember(ownerId) { mutableStateOf(false) }
    LaunchedEffect(ownerId, coordinator) {
        coordinator.prepareLocalAccount(ownerId)
        accountPrepared = true
    }
    if (!accountPrepared) {
        LoadingRoot()
        return
    }
    val syncManager = remember(ownerId, coordinator, services.productionConfigured, context) {
        AccountSyncManager(
            ownerId,
            coordinator,
            services.productionConfigured,
            networkMonitor.availability,
            cloudWriteGate = DeletionBarrier.cloudWriteGate(),
            sessionActive = { services.currentUserId() == ownerId },
        )
    }
    val handBrewRepository = remember(ownerId, database) {
        RoomHandBrewRecordRepository(
            database = database,
            ownerId = ownerId,
            onLocalChange = { DailyRecordSyncScheduler.schedule(context, ownerId) },
        )
    }
    val sexRepository = remember(ownerId, database) {
        RoomSexRecordRepository(
            database = database,
            ownerId = ownerId,
            onLocalChange = { DailyRecordSyncScheduler.schedule(context, ownerId) },
        )
    }
    val deletionCoordinator = remember(ownerId, database, services) {
        AccountDeletionCoordinator(
            authRepository = services.authRepository,
            remoteDataSource = buildCombinedAccountRemoteDataStore(services),
            localStore = buildCombinedAccountDeletionLocalStore(database),
            markCloudDeletionComplete = DeletionBarrier::markCloudDeletionComplete,
            markAuthDeletionStarted = DeletionBarrier::markAuthDeletionStarted,
            markLocalRecoveryCopyPending = DeletionBarrier::markLocalRecoveryCopyPending,
            markLocalRecoveryCopyReady = DeletionBarrier::markLocalRecoveryCopyReady,
            markAuthDeletionComplete = DeletionBarrier::markAuthDeletionComplete,
        )
    }
    val scope = rememberCoroutineScope()
    val syncStatus by syncManager.status.collectAsState()
    var activeSyncJobs by remember(ownerId) { mutableStateOf<List<Job>>(emptyList()) }
    val deletionOrchestrator = AccountDeletionOrchestrator(
        cancelAndAwait = {
            // AccountDeletionOrchestrator has already persisted the
            // owner barrier and waited for the current cloud writer. Only
            // now cancel in-memory producers and WorkManager, so no
            // cancellation window exists before the durable barrier.
            activeSyncJobs.toList().forEach { it.cancelAndJoin() }
            DailyRecordSyncScheduler.cancelAndAwait(context)
        },
        performDeletion = deletionCoordinator::deleteAccount,
        markCleanupPending = pendingCleanupPreference::add,
        onLocalRecordsKept = onAccountDeletedWithLocalRecords,
        onScheduleSync = { ownerId -> DailyRecordSyncScheduler.schedule(context, ownerId) },
    )

    DisposableEffect(networkMonitor) {
        onDispose { networkMonitor.close() }
    }
    DisposableEffect(
        syncManager,
        scope,
        syncRestartRevision,
        recoveryRestartRevision,
    ) {
        if (DeletionBarrier.isDeletionBlocked(ownerId)) {
            activeSyncJobs = emptyList()
            onDispose { }
        } else {
            val jobs = syncManager.start(scope)
            activeSyncJobs = jobs
            onDispose {
                jobs.forEach { it.cancel() }
                if (activeSyncJobs === jobs) activeSyncJobs = emptyList()
            }
        }
    }
    LaunchedEffect(ownerId) {
        if (!DeletionBarrier.isDeletionBlocked(ownerId)) {
            DailyRecordSyncScheduler.schedule(context, ownerId)
        }
    }

    DailyRecordApp(
        repository = handBrewRepository,
        sexRepository = sexRepository,
        accountEmail = state.account.email,
        syncStatus = syncStatus,
        onSyncNow = { scope.launch { syncManager.syncNow() } },
        onSignOut = services.authRepository::signOut,
        onDeleteAccount = { password, localData ->
            val completion = CompletableDeferred<Result<Unit>>()
            accountDeletionScope.launch {
                try {
                    deletionInProgress = true
                    deletionAttempted = true
                    completion.complete(
                        deletionOrchestrator.deleteAccount(ownerId, password, localData),
                    )
                } catch (error: CancellationException) {
                    completion.completeExceptionally(error)
                } finally {
                    deletionInProgress = false
                    syncRestartRevision += 1
                }
            }
            completion.await()
        },
    )
}

private suspend fun resolvePendingAuthDeletions(
    database: DailyRecordDatabase,
    servicesProvider: () -> FirebaseServices,
    pendingCleanupPreference: PendingLocalCleanupPreference,
) {
    resolvePreAuthDeletionMarkers(database)
    val authPendingOwners = DeletionBarrier.authDeletionStartedOwnerIds()
    if (authPendingOwners.isNotEmpty()) {
        val services = runCatching { servicesProvider() }.getOrNull() ?: return
        val localStore = buildCombinedAccountDeletionLocalStore(database)
        authPendingOwners.forEach { ownerId ->
            val hasPendingCopy = ownerId in DeletionBarrier.localRecoveryCopyPendingOwnerIds()
            val hasReadyCopy = ownerId in DeletionBarrier.localRecoveryCopyReadyOwnerIds()
            when (val presence = services.authRepository.inspectAccountPresence(ownerId)) {
                AuthAccountPresence.Absent -> {
                    val recoveryCopyReady = if (hasPendingCopy && !hasReadyCopy) {
                        runCatchingPreservingCancellation {
                            localStore.stageLocalRecoveryCopy(ownerId)
                            DeletionBarrier.markLocalRecoveryCopyReady(ownerId)
                        }.isSuccess
                    } else {
                        true
                    }
                    if (recoveryCopyReady) {
                        // Auth is definitively gone; only now may the startup
                        // cleanup path remove the account cache.
                        runCatching { DeletionBarrier.promoteAuthDeletionCleanup(ownerId) }
                    }
                }
                AuthAccountPresence.SignedOut -> Unit
                AuthAccountPresence.Exists -> {
                    // The account survived. Discard only the staged recovery
                    // copy, re-queue the account rows, then release the block.
                    runCatchingPreservingCancellation {
                        if (hasPendingCopy && !hasReadyCopy) {
                            localStore.stageLocalRecoveryCopy(ownerId)
                            DeletionBarrier.markLocalRecoveryCopyReady(ownerId)
                        }
                        if (hasPendingCopy || hasReadyCopy) {
                            localStore.discardLocalRecoveryCopy()
                        }
                        localStore.markOwnerPendingForResync(ownerId)
                        DeletionBarrier.clearLocalRecoveryCopyPending(ownerId)
                        DeletionBarrier.resolveAuthDeletionAccountStillExists(ownerId)
                    }
                }
                is AuthAccountPresence.Unknown -> Unit
            }
        }
    }
    retryPendingOwnerCleanup(database, pendingCleanupPreference)
}

/**
 * Resolves only deletion phases before Firebase Auth was invoked. This helper
 * deliberately has no Firebase dependency so persistent local mode can remain
 * fully offline and lazy.
 */
private suspend fun resolvePreAuthDeletionMarkers(database: DailyRecordDatabase) {
    val authPendingOwners = DeletionBarrier.authDeletionStartedOwnerIds()
    val cloudPendingOwners = DeletionBarrier.cloudDeletionPendingOwnerIds() - authPendingOwners
    val cleanupPendingOwners = DeletionBarrier.pendingCleanupOwnerIds()
    val recoveryCopyOwners = DeletionBarrier.localRecoveryCopyPendingOwnerIds() +
        DeletionBarrier.localRecoveryCopyReadyOwnerIds()
    val interruptedOwners = (
        DeletionBarrier.inProgressOwnerIds() + recoveryCopyOwners
        ) - authPendingOwners - cloudPendingOwners - cleanupPendingOwners
    if (cloudPendingOwners.isEmpty() && interruptedOwners.isEmpty()) return

    val localStore = buildCombinedAccountDeletionLocalStore(database)
    // If Auth was not even invoked, the account definitely still exists.
    // Re-queue its now-cloudless local rows before releasing the barrier.
    cloudPendingOwners.forEach { ownerId ->
        runCatchingPreservingCancellation {
            if (ownerId in DeletionBarrier.localRecoveryCopyPendingOwnerIds() ||
                ownerId in DeletionBarrier.localRecoveryCopyReadyOwnerIds()
            ) {
                localStore.discardLocalRecoveryCopy()
            }
            localStore.markOwnerPendingForResync(ownerId)
            DeletionBarrier.clearLocalRecoveryCopyPending(ownerId)
            DeletionBarrier.resolveAuthDeletionAccountStillExists(ownerId)
        }
    }

    interruptedOwners.forEach { ownerId ->
        runCatchingPreservingCancellation {
            if (ownerId in DeletionBarrier.localRecoveryCopyPendingOwnerIds() ||
                ownerId in DeletionBarrier.localRecoveryCopyReadyOwnerIds()
            ) {
                localStore.discardLocalRecoveryCopy()
                DeletionBarrier.clearLocalRecoveryCopyPending(ownerId)
            }
            localStore.markOwnerPendingForResync(ownerId)
            DeletionBarrier.resolveInterruptedDeletion(ownerId)
        }
    }

}

private fun hasPreAuthDeletionRecovery(): Boolean {
    val authPendingOwners = DeletionBarrier.authDeletionStartedOwnerIds()
    val cloudPendingOwners = DeletionBarrier.cloudDeletionPendingOwnerIds() - authPendingOwners
    val cleanupPendingOwners = DeletionBarrier.pendingCleanupOwnerIds()
    val recoveryCopyOwners = DeletionBarrier.localRecoveryCopyPendingOwnerIds() +
        DeletionBarrier.localRecoveryCopyReadyOwnerIds()
    val interruptedOwners = (
        DeletionBarrier.inProgressOwnerIds() + recoveryCopyOwners
        ) - authPendingOwners - cloudPendingOwners - cleanupPendingOwners
    return cloudPendingOwners.isNotEmpty() ||
        cleanupPendingOwners.isNotEmpty() ||
        interruptedOwners.isNotEmpty()
}

private fun hasDeletionRecoveryPending(): Boolean {
    val cleanupPendingOwners = DeletionBarrier.pendingCleanupOwnerIds()
    val recoveryCopyReadyOwners = DeletionBarrier.localRecoveryCopyReadyOwnerIds()
    return DeletionBarrier.inProgressOwnerIds().isNotEmpty() ||
        DeletionBarrier.cloudDeletionPendingOwnerIds().isNotEmpty() ||
        DeletionBarrier.authDeletionStartedOwnerIds().isNotEmpty() ||
        DeletionBarrier.localRecoveryCopyPendingOwnerIds().isNotEmpty() ||
        (recoveryCopyReadyOwners - cleanupPendingOwners).isNotEmpty() ||
        cleanupPendingOwners.isNotEmpty()
}

@Composable
private fun LoadingRoot() {
    Box(
        modifier = Modifier.fillMaxSize().background(DailyRecordCanvas),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = DailyRecordDefaultAccent)
    }
}

@Composable
private fun DeletionRecoveryRoot(onSignIn: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(DailyRecordCanvas),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = AppCopy.Deletion.authDeletionPending,
                color = DailyRecordTextSecondary,
            )
            PrimaryActionButton(
                label = AppCopy.Account.signInSync,
                onClick = onSignIn,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}
