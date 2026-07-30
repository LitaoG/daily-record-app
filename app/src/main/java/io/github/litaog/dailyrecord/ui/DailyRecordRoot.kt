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
import io.github.litaog.dailyrecord.core.sync.SyncDiagnostics
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.ui.auth.AuthScreen
import io.github.litaog.dailyrecord.ui.diagnostics.createDiagnosticReport
import io.github.litaog.dailyrecord.ui.theme.DailyRecordCanvas
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent
import kotlinx.coroutines.CompletableDeferred
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
        diagnosticReport = createDiagnosticReport(
            status = SyncStatus.NotConfigured,
            diagnostics = SyncDiagnostics(),
        ),
    )
}

@Composable
private fun SignedInRoot(
    database: DailyRecordDatabase,
    services: FirebaseServices,
    state: AuthState.SignedIn,
    accountDeletionScope: kotlinx.coroutines.CoroutineScope,
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
    val syncManager = remember(ownerId, coordinator, services.productionConfigured) {
        AccountSyncManager(
            ownerId,
            coordinator,
            services.productionConfigured,
            networkMonitor.availability,
        )
    }
    val handBrewRepository = remember(ownerId, database) {
        RoomHandBrewRecordRepository(
            database = database,
            ownerId = ownerId,
            onLocalChange = { DailyRecordSyncScheduler.schedule(context) },
        )
    }
    val sexRepository = remember(ownerId, database) {
        RoomSexRecordRepository(
            database = database,
            ownerId = ownerId,
            onLocalChange = { DailyRecordSyncScheduler.schedule(context) },
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
    val syncDiagnostics by syncManager.diagnostics.collectAsState()
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
        DailyRecordSyncScheduler.schedule(context)
    }

    DailyRecordApp(
        repository = handBrewRepository,
        sexRepository = sexRepository,
        accountEmail = state.account.email,
        syncStatus = syncStatus,
        onSyncNow = { scope.launch { syncManager.syncNow() } },
        onSignOut = services.authRepository::signOut,
        diagnosticReport = createDiagnosticReport(syncStatus, syncDiagnostics),
        onDeleteAccount = { password, localData ->
            val completion = CompletableDeferred<Result<Unit>>()
            accountDeletionScope.launch {
                deletionInProgress = true
                activeSyncJobs.forEach { it.cancelAndJoin() }
                val result = runCatchingPreservingCancellation {
                    DailyRecordSyncScheduler.cancelAndAwait(context)
                    deletionCoordinator.deleteAccount(
                        ownerId = ownerId,
                        password = password,
                        localData = localData,
                    )
                }
                if (result.isSuccess && localData == LocalDataAfterAccountDeletion.Keep) {
                    onAccountDeletedWithLocalRecords()
                }
                if (result.isFailure) deletionInProgress = false
                completion.complete(result)
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
