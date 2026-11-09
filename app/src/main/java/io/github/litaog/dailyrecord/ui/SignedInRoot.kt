package io.github.litaog.dailyrecord.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.litaog.dailyrecord.core.auth.AuthState
import io.github.litaog.dailyrecord.core.common.AppCopy
import io.github.litaog.dailyrecord.core.common.AppLanguage
import io.github.litaog.dailyrecord.ui.theme.DailyRecordColorTheme
import io.github.litaog.dailyrecord.ui.lock.AppLockSettings
import io.github.litaog.dailyrecord.ui.settings.LauncherDisguiseSettings
import io.github.litaog.dailyrecord.core.di.DailyRecordSyncScheduler
import io.github.litaog.dailyrecord.core.di.FirebaseServices
import io.github.litaog.dailyrecord.core.di.buildAccountDeletionCoordinator
import io.github.litaog.dailyrecord.core.di.buildCombinedSyncCoordinator
import io.github.litaog.dailyrecord.core.sync.AccountDeletionRecoveryCoordinator
import io.github.litaog.dailyrecord.core.common.runCatchingPreservingCancellation
import io.github.litaog.dailyrecord.core.data.RoomHandBrewRecordRepository
import io.github.litaog.dailyrecord.core.data.RoomSexRecordRepository
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.sync.AccountSyncManager
import io.github.litaog.dailyrecord.core.sync.AndroidNetworkMonitor
import io.github.litaog.dailyrecord.core.sync.DeletionBarrier
import io.github.litaog.dailyrecord.core.sync.cloudWriteGate
import io.github.litaog.dailyrecord.ui.components.DailyRecordConfirmationDialog
import io.github.litaog.dailyrecord.ui.components.PrimaryActionButton
import io.github.litaog.dailyrecord.ui.theme.DailyRecordCanvas
import io.github.litaog.dailyrecord.ui.theme.DailyRecordTextSecondary
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

@Composable
internal fun SignedInRoot(
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
    onLanguageChanged: (AppLanguage) -> Unit,
    onColorThemeChanged: (DailyRecordColorTheme) -> Unit,
    appLockSettings: AppLockSettings?,
    launcherDisguiseSettings: LauncherDisguiseSettings?,
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
    val syncStatus by syncManager.status.collectAsStateWithLifecycle()
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
        onLanguageChanged = onLanguageChanged,
        onColorThemeChanged = onColorThemeChanged,
        appLockSettings = appLockSettings,
        launcherDisguiseSettings = launcherDisguiseSettings,
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
internal fun DeletionRecoveryRoot(
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
        DailyRecordConfirmationDialog(
            title = AppCopy.Deletion.replaceLocalAndRestoreTitle,
            message = AppCopy.Deletion.replaceLocalAndRestoreMessage,
            cancelLabel = AppCopy.Deletion.cancelRecoveryReplacement,
            confirmLabel = AppCopy.Deletion.replaceLocalAndRestore,
            testTag = "replace_local_records_dialog",
            onDismiss = { confirmReplace = false },
            onConfirm = {
                confirmReplace = false
                onReplaceExistingLocal()
            },
        )
    }
}
