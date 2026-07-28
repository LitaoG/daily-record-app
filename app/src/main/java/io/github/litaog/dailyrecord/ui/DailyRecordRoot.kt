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
import io.github.litaog.dailyrecord.core.account.LocalDataAfterAccountDeletion
import io.github.litaog.dailyrecord.core.cloud.FirebaseServices
import io.github.litaog.dailyrecord.core.cloud.runInteractiveCloudOperation
import io.github.litaog.dailyrecord.core.common.runCatchingPreservingCancellation
import io.github.litaog.dailyrecord.core.data.RoomHandBrewRecordRepository
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.sync.AccountSyncManager
import io.github.litaog.dailyrecord.core.sync.AndroidNetworkMonitor
import io.github.litaog.dailyrecord.core.sync.HandBrewSyncCoordinator
import io.github.litaog.dailyrecord.core.sync.HandBrewSyncScheduler
import io.github.litaog.dailyrecord.core.sync.RoomHandBrewSyncStore
import io.github.litaog.dailyrecord.core.sync.SyncDiagnostics
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.ui.auth.AuthScreen
import io.github.litaog.dailyrecord.ui.diagnostics.createDiagnosticReport
import io.github.litaog.dailyrecord.ui.theme.Paper50
import io.github.litaog.dailyrecord.ui.theme.Terracotta500
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
    if (continueOffline) {
        LocalRoot(
            database = database,
            onSignIn = {
                localModePreference.setEnabled(false)
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
                onContinueOffline = {
                    localModePreference.setEnabled(true)
                    continueOffline = true
                },
            )
        }
        is AuthState.SignedIn -> {
            LaunchedEffect(state.account.uid) {
                localModePreference.setEnabled(false)
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
    val repository = remember(database) {
        RoomHandBrewRecordRepository(database = database, ownerId = LOCAL_OWNER_ID)
    }
    HandBrewApp(
        repository = repository,
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
    val coordinator = remember(ownerId, database, services.remoteDataSource) {
        HandBrewSyncCoordinator(
            store = RoomHandBrewSyncStore(database),
            remote = services.remoteDataSource,
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
    val repository = remember(ownerId, database) {
        RoomHandBrewRecordRepository(
            database = database,
            ownerId = ownerId,
            onLocalChange = { HandBrewSyncScheduler.schedule(context) },
        )
    }
    val deletionCoordinator = remember(ownerId, database, services.remoteDataSource) {
        AccountDeletionCoordinator(
            authRepository = services.authRepository,
            remoteDataSource = services.remoteDataSource,
            localStore = RoomHandBrewSyncStore(database),
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
        HandBrewSyncScheduler.schedule(context)
    }

    HandBrewApp(
        repository = repository,
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
                    HandBrewSyncScheduler.cancelAndAwait(context)
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
        modifier = Modifier.fillMaxSize().background(Paper50),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Terracotta500)
    }
}
