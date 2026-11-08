package io.github.litaog.dailyrecord.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.litaog.dailyrecord.core.auth.AuthState
import io.github.litaog.dailyrecord.core.common.AppLanguage
import io.github.litaog.dailyrecord.ui.theme.DailyRecordColorTheme
import io.github.litaog.dailyrecord.ui.lock.AppLockSettings
import io.github.litaog.dailyrecord.ui.settings.LauncherDisguiseSettings
import io.github.litaog.dailyrecord.core.di.FirebaseServices
import io.github.litaog.dailyrecord.core.di.buildAccountDeletionRecoveryCoordinator
import io.github.litaog.dailyrecord.core.sync.AccountDeletionRecoverySnapshot
import io.github.litaog.dailyrecord.core.common.runInteractiveCloudOperation
import io.github.litaog.dailyrecord.core.common.runCatchingPreservingCancellation
import io.github.litaog.dailyrecord.core.data.RoomHandBrewRecordRepository
import io.github.litaog.dailyrecord.core.data.RoomSexRecordRepository
import io.github.litaog.dailyrecord.core.database.DailyRecordDatabase
import io.github.litaog.dailyrecord.core.database.LOCAL_OWNER_ID
import io.github.litaog.dailyrecord.ui.auth.AuthScreen
import io.github.litaog.dailyrecord.ui.theme.DailyRecordCanvas
import io.github.litaog.dailyrecord.ui.theme.DailyRecordDefaultAccent
import kotlinx.coroutines.launch

@Composable
internal fun DailyRecordRoot(
    database: DailyRecordDatabase,
    servicesProvider: () -> FirebaseServices,
    onLanguageChanged: (AppLanguage) -> Unit = {},
    onColorThemeChanged: (DailyRecordColorTheme) -> Unit = {},
    appLockSettings: AppLockSettings? = null,
    launcherDisguiseSettings: LauncherDisguiseSettings? = null,
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
                onLanguageChanged = onLanguageChanged,
                onColorThemeChanged = onColorThemeChanged,
                appLockSettings = appLockSettings,
                launcherDisguiseSettings = launcherDisguiseSettings,
            )
        }
        return
    }

    // Only reached when the user is not in persistent local mode. The provider
    // is remembered with the stable app instance by the caller, so calling it
    // here never recreates FirebaseServices; this read is what initializes
    // Firebase for the first time (local-only startup never reaches it).
    val services = servicesProvider()
    val authState by services.authRepository.state.collectAsStateWithLifecycle(
        initialValue = AuthState.Loading,
    )
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
                onLanguageChanged = onLanguageChanged,
                onColorThemeChanged = onColorThemeChanged,
                appLockSettings = appLockSettings,
                launcherDisguiseSettings = launcherDisguiseSettings,
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
private fun LocalRoot(
    database: DailyRecordDatabase,
    onSignIn: () -> Unit,
    onLanguageChanged: (AppLanguage) -> Unit,
    onColorThemeChanged: (DailyRecordColorTheme) -> Unit,
    appLockSettings: AppLockSettings?,
    launcherDisguiseSettings: LauncherDisguiseSettings?,
) {
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
        onLanguageChanged = onLanguageChanged,
        onColorThemeChanged = onColorThemeChanged,
        appLockSettings = appLockSettings,
        launcherDisguiseSettings = launcherDisguiseSettings,
    )
}

@Composable
internal fun LoadingRoot() {
    Box(
        modifier = Modifier.fillMaxSize().background(DailyRecordCanvas),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = DailyRecordDefaultAccent)
    }
}
