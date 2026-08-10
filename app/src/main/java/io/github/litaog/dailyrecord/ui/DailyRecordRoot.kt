package io.github.litaog.dailyrecord.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import io.github.litaog.dailyrecord.core.auth.AuthState
import io.github.litaog.dailyrecord.core.account.AccountDeletionCoordinator
import io.github.litaog.dailyrecord.core.account.AccountDeletionLocalCleanupPendingException
import io.github.litaog.dailyrecord.core.account.CombinedAccountDeletionLocalStore
import io.github.litaog.dailyrecord.core.account.LocalDataAfterAccountDeletion
import io.github.litaog.dailyrecord.core.cloud.FirebaseServices
import io.github.litaog.dailyrecord.core.cloud.runInteractiveCloudOperation
import io.github.litaog.dailyrecord.core.common.runCatchingPreservingCancellation
import io.github.litaog.dailyrecord.core.data.RoomHandBrewRecordRepository
import io.github.litaog.dailyrecord.core.data.RoomSexRecordRepository
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.sync.AccountSyncManager
import io.github.litaog.dailyrecord.core.sync.AndroidNetworkMonitor
import io.github.litaog.dailyrecord.core.sync.CombinedAccountRemoteDataStore
import io.github.litaog.dailyrecord.core.sync.CombinedSyncCoordinator
import io.github.litaog.dailyrecord.core.sync.HandBrewSyncCoordinator
import io.github.litaog.dailyrecord.core.sync.DailyRecordSyncScheduler
import io.github.litaog.dailyrecord.core.sync.RoomHandBrewSyncStore
import io.github.litaog.dailyrecord.core.sync.RoomSexSyncStore
import io.github.litaog.dailyrecord.core.sync.SexSyncCoordinator
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.ui.auth.AuthScreen
import io.github.litaog.dailyrecord.ui.theme.DailyRecordCanvas
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent
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
    val pendingCleanupPreference = remember(context) { PendingLocalCleanupPreference(context) }

    // A previous account deletion may have left an owner cache that could not
    // be cleared. Retry the local cleanup once on startup.
    LaunchedEffect(database) {
        val cleanupStore = CombinedAccountDeletionLocalStore(
            handBrew = RoomHandBrewSyncStore(database),
            sex = RoomSexSyncStore(database),
        )
        val durablePendingOwners = DailyRecordSyncScheduler.pendingCleanupOwnerIds(context)
        (pendingCleanupPreference.ownerIds + durablePendingOwners).forEach { pendingOwnerId ->
            runCatchingPreservingCancellation {
                cleanupStore.deleteOwnerCache(pendingOwnerId)
            }.onSuccess {
                pendingCleanupPreference.remove(pendingOwnerId)
                // Keep startup resilient if the durable journal cannot be
                // committed; the next launch will retry from the marker.
                runCatching {
                    DailyRecordSyncScheduler.completeDeletionCleanup(context, pendingOwnerId)
                }.onFailure {
                    pendingCleanupPreference.add(pendingOwnerId)
                }
            }
        }
    }

    var continueOffline by rememberSaveable {
        mutableStateOf(localModePreference.isEnabled)
    }
    var authOpenedFromLocal by rememberSaveable { mutableStateOf(false) }
    if (continueOffline) {
        LocalRoot(
            database = database,
            onSignIn = {
                authOpenedFromLocal = true
                continueOffline = false
            },
        )
        return
    }

    val services = remember(servicesProvider) { servicesProvider() }
    val authState by services.authRepository.state.collectAsState(initial = AuthState.Loading)
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
    pendingCleanupPreference: PendingLocalCleanupPreference,
    onAccountDeletedWithLocalRecords: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val ownerId = state.account.uid
    val networkMonitor = remember(ownerId, context) { AndroidNetworkMonitor(context) }
    val handBrewSyncStore = remember(database) { RoomHandBrewSyncStore(database) }
    val sexSyncStore = remember(database) { RoomSexSyncStore(database) }
    val coordinator = remember(
        ownerId,
        handBrewSyncStore,
        sexSyncStore,
        services.remoteDataSource,
        services.sexRemoteDataSource,
    ) {
        CombinedSyncCoordinator(
            handBrew = HandBrewSyncCoordinator(
                store = handBrewSyncStore,
                remote = services.remoteDataSource,
            ),
            sex = SexSyncCoordinator(
                store = sexSyncStore,
                remote = services.sexRemoteDataSource,
            ),
        )
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
            cloudWriteGate = DailyRecordSyncScheduler.cloudWriteGate(context),
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
    val deletionCoordinator = remember(
        ownerId,
        handBrewSyncStore,
        sexSyncStore,
        services.remoteDataSource,
        services.sexRemoteDataSource,
    ) {
        AccountDeletionCoordinator(
            authRepository = services.authRepository,
            remoteDataSource = CombinedAccountRemoteDataStore(
                handBrew = services.remoteDataSource,
                sex = services.sexRemoteDataSource,
            ),
            localStore = CombinedAccountDeletionLocalStore(
                handBrew = handBrewSyncStore,
                sex = sexSyncStore,
            ),
        )
    }
    val scope = rememberCoroutineScope()
    val syncStatus by syncManager.status.collectAsState()
    var deletionInProgress by remember(ownerId) { mutableStateOf(false) }
    var activeSyncJobs by remember(ownerId) { mutableStateOf<List<Job>>(emptyList()) }

    DisposableEffect(networkMonitor) {
        onDispose { networkMonitor.close() }
    }
    DisposableEffect(syncManager, scope, deletionInProgress) {
        val jobs = if (deletionInProgress) emptyList() else syncManager.start(scope)
        activeSyncJobs = jobs
        onDispose {
            jobs.forEach { it.cancel() }
            if (activeSyncJobs === jobs) activeSyncJobs = emptyList()
        }
    }
    LaunchedEffect(ownerId) {
        DailyRecordSyncScheduler.schedule(context, ownerId)
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
                var began = false
                var outcome = io.github.litaog.dailyrecord.core.sync.AccountDeletionOutcome.Interrupted
                var result: Result<Unit>? = null
                var cancellation: CancellationException? = null
                var barrierFailure: Exception? = null
                try {
                    // Persist the barrier before cancelling any producer. This
                    // closes the schedule-vs-delete race and survives process death.
                    DailyRecordSyncScheduler.beginDeletionBlock(context, ownerId)
                    began = true
                    deletionInProgress = true
                    activeSyncJobs.forEach { it.cancelAndJoin() }
                    DailyRecordSyncScheduler.awaitDeletionWriters()
                    val deletionResult = runCatchingPreservingCancellation {
                        DailyRecordSyncScheduler.cancelAndAwait(context)
                        deletionCoordinator.deleteAccount(
                            ownerId = ownerId,
                            password = password,
                            localData = localData,
                        )
                    }
                    val cleanupPending = deletionResult.exceptionOrNull() is
                        AccountDeletionLocalCleanupPendingException
                    outcome = when {
                        cleanupPending -> io.github.litaog.dailyrecord.core.sync.AccountDeletionOutcome.CleanupPending
                        deletionResult.isSuccess -> io.github.litaog.dailyrecord.core.sync.AccountDeletionOutcome.Completed
                        else -> io.github.litaog.dailyrecord.core.sync.AccountDeletionOutcome.RetryableFailure
                    }
                    if (cleanupPending) {
                        // Keep both the legacy marker and the durable journal;
                        // either one can recover local cleanup after a restart.
                        pendingCleanupPreference.add(ownerId)
                    }
                    if ((deletionResult.isSuccess || cleanupPending) &&
                        localData == LocalDataAfterAccountDeletion.Keep
                    ) {
                        onAccountDeletedWithLocalRecords()
                    }
                    result = if (cleanupPending) Result.success(Unit) else deletionResult
                } catch (error: CancellationException) {
                    // Do not clear an interrupted journal: a future sync must
                    // stay blocked until the user retries or finishes deletion.
                    cancellation = error
                } catch (error: Exception) {
                    outcome = io.github.litaog.dailyrecord.core.sync.AccountDeletionOutcome.RetryableFailure
                    result = Result.failure(error)
                } finally {
                    if (began) {
                        try {
                            DailyRecordSyncScheduler.endDeletionBlock(context, ownerId, outcome)
                        } catch (error: Exception) {
                            // Do not resume scheduling when the durable barrier
                            // could not be committed. The scheduler keeps a
                            // conservative in-process block until restart.
                            barrierFailure = error
                        }
                    }
                    if (barrierFailure != null) {
                        deletionInProgress = false
                        result = Result.failure(barrierFailure!!)
                    } else if (
                        outcome == io.github.litaog.dailyrecord.core.sync.AccountDeletionOutcome.RetryableFailure ||
                        outcome == io.github.litaog.dailyrecord.core.sync.AccountDeletionOutcome.Interrupted
                    ) {
                        deletionInProgress = false
                        if (outcome == io.github.litaog.dailyrecord.core.sync.AccountDeletionOutcome.RetryableFailure) {
                            // A failed deletion must not leave pending local
                            // records stuck: re-enable the normal background
                            // sync path.
                            DailyRecordSyncScheduler.schedule(context, ownerId)
                        }
                    }
                    cancellation?.let(completion::completeExceptionally)
                        ?: completion.complete(requireNotNull(result))
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
