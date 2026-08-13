package io.github.litaog.dailyrecord.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import io.github.litaog.dailyrecord.core.auth.AuthState
import io.github.litaog.dailyrecord.core.account.LocalDataAfterAccountDeletion
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.di.DailyRecordSyncScheduler
import io.github.litaog.dailyrecord.core.di.FirebaseServices
import io.github.litaog.dailyrecord.core.di.buildAccountDeletionCoordinator
import io.github.litaog.dailyrecord.core.di.buildAccountDeletionRecoveryCoordinator
import io.github.litaog.dailyrecord.core.di.buildCombinedSyncCoordinator
import io.github.litaog.dailyrecord.core.sync.AccountDeletionRecoveryCoordinator
import io.github.litaog.dailyrecord.core.sync.AccountDeletionRecoverySnapshot
import io.github.litaog.dailyrecord.core.common.runInteractiveCloudOperation
import io.github.litaog.dailyrecord.core.common.runCatchingPreservingCancellation
import io.github.litaog.dailyrecord.core.data.RoomHandBrewRecordRepository
import io.github.litaog.dailyrecord.core.data.RoomSexRecordRepository
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.sync.AccountSyncManager
import io.github.litaog.dailyrecord.core.sync.AndroidNetworkMonitor
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
import kotlinx.coroutines.launch

@Composable
internal fun DailyRecordRoot(
    database: DailyRecordDatabase,
    servicesProvider: () -> FirebaseServices,
) {
    val context = LocalContext.current
    val rootScope = rememberCoroutineScope()
    val localModePreference = remember(context) { LocalModePreference(context) }
    val recoveryCoordinator = remember(database) {
        buildAccountDeletionRecoveryCoordinator(database)
    }
    var recoverySnapshot by remember(database) {
        mutableStateOf<AccountDeletionRecoverySnapshot>(recoveryCoordinator.snapshot())
    }
    var deletionRecoveryRevision by remember(database) { mutableIntStateOf(0) }
    var recoveryRestartRevision by remember(database) { mutableIntStateOf(0) }
    var recoveryRetryRevision by remember(database) { mutableIntStateOf(0) }
    var recoveryConflictOwners by remember(database) { mutableStateOf<Set<String>>(emptySet()) }
    var recoveryAuthOpen by rememberSaveable { mutableStateOf(false) }
    var continueOffline by rememberSaveable {
        mutableStateOf(localModePreference.isEnabled)
    }
    var authOpenedFromLocal by rememberSaveable { mutableStateOf(false) }
    val localRecoveryBlocked = recoverySnapshot.blocksLocalMode
    if (continueOffline) {
        // Keep Firebase lazy in persistent local mode. Pre-Auth recovery only
        // touches Room and the deletion journal; an unresolved Auth request
        // is intentionally revisited after the user enters the cloud path.
        LaunchedEffect(database, recoveryRetryRevision) {
            recoveryCoordinator.resolvePreAuthUntilSettled(
                onAttemptCompleted = { snapshot ->
                    recoverySnapshot = snapshot
                    recoveryConflictOwners = snapshot.conflictOwnerIds
                    deletionRecoveryRevision += 1
                },
            )
        }
        if (localRecoveryBlocked) {
            DeletionRecoveryRoot(
                onSignIn = {
                    authOpenedFromLocal = true
                    continueOffline = false
                },
                onRetry = { recoveryRetryRevision += 1 },
                hasRecoveryConflict = recoveryConflictOwners.isNotEmpty(),
                onReplaceExistingLocal = {
                    val ownerId = recoveryConflictOwners.firstOrNull() ?: return@DeletionRecoveryRoot
                    rootScope.launch {
                        runCatchingPreservingCancellation {
                            recoveryCoordinator.resolveRecoveryConflict(ownerId)
                        }.onSuccess {
                            recoveryConflictOwners -= ownerId
                            recoverySnapshot = recoveryCoordinator.snapshot(recoveryConflictOwners)
                            recoveryRetryRevision += 1
                        }
                    }
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
    LaunchedEffect(database, authState, recoveryRetryRevision) {
        if (authState !is AuthState.Loading) {
            // A previous account deletion may have left an owner cache that
            // could not be cleared. Resolve all durable recovery work before
            // exposing the account or restarting its sync producers.
            recoveryCoordinator.resolveUntilSettled(
                authRepository = services.authRepository,
                shouldContinue = { authState is AuthState.SignedIn },
                onAttemptCompleted = { snapshot ->
                    recoverySnapshot = snapshot
                    recoveryConflictOwners = snapshot.conflictOwnerIds
                    recoveryResolved.value = true
                    deletionRecoveryRevision += 1
                    recoveryRestartRevision += 1
                },
            )
        }
    }

    when (val state = authState) {
        AuthState.Loading -> LoadingRoot()
        AuthState.SignedOut -> {
            if ((recoveryConflictOwners.isNotEmpty() || recoverySnapshot.hasPendingRecovery) && !recoveryAuthOpen) {
                DeletionRecoveryRoot(
                    onSignIn = { recoveryAuthOpen = true },
                    onRetry = { recoveryRetryRevision += 1 },
                    hasRecoveryConflict = recoveryConflictOwners.isNotEmpty(),
                    onReplaceExistingLocal = {
                        val ownerId = recoveryConflictOwners.firstOrNull() ?: return@DeletionRecoveryRoot
                        rootScope.launch {
                            runCatchingPreservingCancellation {
                                recoveryCoordinator.resolveRecoveryConflict(ownerId)
                            }.onSuccess {
                                recoveryConflictOwners -= ownerId
                                recoverySnapshot = recoveryCoordinator.snapshot(recoveryConflictOwners)
                                recoveryRetryRevision += 1
                            }
                        }
                    },
                )
            } else AuthScreen(
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
                onBack = when {
                    recoveryAuthOpen -> ({ recoveryAuthOpen = false })
                    authOpenedFromLocal -> {
                        {
                            localModePreference.setEnabled(true)
                            authOpenedFromLocal = false
                            continueOffline = true
                        }
                    }
                    else -> null
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
                recoveryAuthOpen = false
            }
            SignedInRoot(
                database = database,
                services = services,
                state = state,
                accountDeletionScope = rootScope,
                recoveryCoordinator = recoveryCoordinator,
                deletionRecoveryRevision = deletionRecoveryRevision,
                recoveryResolved = recoveryResolved.value,
                recoveryRestartRevision = recoveryRestartRevision,
                onRetryDeletionRecovery = { recoveryRetryRevision += 1 },
                hasRecoveryConflict = recoveryConflictOwners.isNotEmpty(),
                onReplaceExistingLocal = {
                    val ownerId = recoveryConflictOwners.firstOrNull() ?: return@SignedInRoot
                    rootScope.launch {
                        runCatchingPreservingCancellation {
                            recoveryCoordinator.resolveRecoveryConflict(ownerId)
                        }.onSuccess {
                            recoveryConflictOwners -= ownerId
                            recoverySnapshot = recoveryCoordinator.snapshot(recoveryConflictOwners)
                            recoveryRetryRevision += 1
                        }
                    }
                },
                onAccountDeletedWithLocalRecords = {
                    localModePreference.setEnabled(true)
                    continueOffline = true
                },
            )
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
    recoveryCoordinator: AccountDeletionRecoveryCoordinator,
    deletionRecoveryRevision: Int,
    recoveryResolved: Boolean,
    recoveryRestartRevision: Int,
    onRetryDeletionRecovery: () -> Unit,
    hasRecoveryConflict: Boolean,
    onReplaceExistingLocal: () -> Unit,
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
        recoveryCoordinator.isDeletionBlocked(ownerId)
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
                recoveryCoordinator.resolvePendingAuthDeletions(services.authRepository)
            }
            deletionRecoveryRefresh += 1
            // The resolver may have cleared an Auth-pending marker. This is a
            // separate restart signal from the deletion callback: the latter
            // deliberately restarts nothing while the durable barrier remains.
            syncRestartRevision += 1
        }
    }

    if (deletionRecoveryPending) {
        DeletionRecoveryRoot(
            onRetry = onRetryDeletionRecovery,
            hasRecoveryConflict = hasRecoveryConflict,
            onReplaceExistingLocal = onReplaceExistingLocal,
        )
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
        buildAccountDeletionCoordinator(database, services)
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
        if (recoveryCoordinator.isDeletionBlocked(ownerId)) {
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
        if (!recoveryCoordinator.isDeletionBlocked(ownerId)) {
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
private fun DeletionRecoveryRoot(
    onSignIn: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    hasRecoveryConflict: Boolean = false,
    onReplaceExistingLocal: (() -> Unit)? = null,
) {
    var confirmReplace by rememberSaveable { mutableStateOf(false) }
    Box(
        modifier = Modifier.fillMaxSize().background(DailyRecordCanvas),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (hasRecoveryConflict) {
                    AppCopy.Deletion.recoveryConflict
                } else {
                    AppCopy.Deletion.recoveryRetryGuidance
                },
                color = DailyRecordTextSecondary,
            )
            onRetry?.let {
                PrimaryActionButton(
                    label = AppCopy.Deletion.retryRecovery,
                    onClick = it,
                    modifier = Modifier.padding(top = 20.dp),
                )
            }
            onSignIn?.let {
                PrimaryActionButton(
                    label = AppCopy.Account.signInSync,
                    onClick = it,
                    modifier = Modifier.padding(top = if (onRetry == null) 20.dp else 12.dp),
                )
            }
            if (hasRecoveryConflict && onReplaceExistingLocal != null) {
                PrimaryActionButton(
                    label = AppCopy.Deletion.replaceLocalAndRestore,
                    onClick = { confirmReplace = true },
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
    if (confirmReplace && onReplaceExistingLocal != null) {
        AlertDialog(
            onDismissRequest = { confirmReplace = false },
            title = { Text(AppCopy.Deletion.replaceLocalAndRestoreTitle) },
            text = { Text(AppCopy.Deletion.replaceLocalAndRestoreMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmReplace = false
                        onReplaceExistingLocal()
                    },
                ) { Text(AppCopy.Deletion.replaceLocalAndRestore) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReplace = false }) {
                    Text(AppCopy.Deletion.cancelRecoveryReplacement)
                }
            },
        )
    }
}
