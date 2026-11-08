package io.github.litaog.dailyrecord.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.litaog.dailyrecord.core.statistics.EARLIEST_SUPPORTED_DATE
import io.github.litaog.dailyrecord.core.sync.SyncStatus
import io.github.litaog.dailyrecord.ui.account.AccountTopBar
import io.github.litaog.dailyrecord.ui.account.LocalAccountTopBar
import io.github.litaog.dailyrecord.ui.components.DailyRecordBottomBar
import io.github.litaog.dailyrecord.ui.components.DailyRecordSnackbarHost
import java.time.YearMonth

internal val earliestSupportedMonth: YearMonth = YearMonth.from(EARLIEST_SUPPORTED_DATE)

internal enum class TopDestination {
    Calendar,
    Statistics,
}

@Composable
internal fun MainAppScaffold(
    destination: TopDestination,
    moduleSpec: RecordModuleUiSpec,
    snackbarHostState: SnackbarHostState,
    showAccountDialog: Boolean,
    accountEmail: String?,
    syncStatus: SyncStatus,
    onSignIn: (() -> Unit)?,
    onOpenAccount: () -> Unit,
    onOpenSettings: () -> Unit,
    onDestinationSelected: (TopDestination) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = {
            if (!showAccountDialog) {
                DailyRecordSnackbarHost(
                    hostState = snackbarHostState,
                    colors = moduleSpec.colors,
                )
            }
        },
        topBar = {
            if (accountEmail != null) {
                AccountTopBar(
                    status = syncStatus,
                    colors = moduleSpec.colors,
                    onClick = onOpenAccount,
                    onSettings = onOpenSettings,
                )
            } else {
                LocalAccountTopBar(
                    colors = moduleSpec.colors,
                    onSignIn = onSignIn,
                    onSettings = onOpenSettings,
                )
            }
        },
        bottomBar = {
            DailyRecordBottomBar(
                selected = destination,
                colors = moduleSpec.colors,
                onSelected = onDestinationSelected,
            )
        },
    ) { contentPadding ->
        content(contentPadding)
    }
}
